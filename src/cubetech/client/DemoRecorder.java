package cubetech.client;

import cubetech.collision.ClientCubeMap;
import cubetech.collision.CubeMap;
import cubetech.common.Common;
import cubetech.common.ICommand;
import cubetech.entities.EntityState;
import cubetech.gfx.ResourceManager;
import cubetech.misc.CameraTrack;
import cubetech.misc.Ref;
import cubetech.net.ConnectState;
import cubetech.net.NetBuffer;
import cubetech.net.Packet;
import cubetech.net.SVC;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import cubetech.misc.FastZipOutputStream;
import cubetech.misc.FasterZip;
import java.util.zip.ZipOutputStream;
import org.lwjgl.BufferUtils;

/**
 * Also plays
 * @author mads
 */
public class DemoRecorder {
    // recording
    private boolean recording;
    private boolean demowaiting;
    private FileChannel fc;

    // playback
    private boolean demoplaying;
    private boolean firstDemoFrameSkipped;
    private ByteBuffer fileBuffer;

    // video creation
    public FasterZip zipStream = null;
    public int startTime;
    public CameraTrack track = new CameraTrack();

    private Client cl;
    private String demoName;

    public DemoRecorder(Client cl) {
        this.cl = cl;

        // init console commands
        Ref.commands.AddCommand("record", cmd_record);
        Ref.commands.AddCommand("stoprecord", cmd_stoprecord);
        Ref.commands.AddCommand("demo", cmd_demo);
        
        Ref.cvars.Get("record_fps", "30", null);
    }

    private void initDemo(String filename) {
        String demoDir = "demos\\";
        File dir = new File(demoDir);
        if(!dir.exists()) {
            dir.mkdir();
        }

        File filehandle = new File(demoDir+filename+".dem");
        boolean fileOk;
        try {
            fileOk = filehandle.createNewFile();
            fc = new FileOutputStream(filehandle, false).getChannel();
        } catch (IOException ex) {
            Common.Log(Common.getExceptionString(ex));
            fileOk = false;
        }

        if(!fileOk) {
            Common.Log("Couldn't create file for demo");
            return;
        }

        // We made it!
        recording = true;
        demowaiting = true;
        try {
            // Save clients current view of the map
            CubeMap.serializeChunks(fc, Ref.cgame.map.chunks);
        } catch (IOException ex) {
            Common.Log(Common.getExceptionString(ex));
            recording = false;
            return;
        }

        writeGamestate();
    }

    private void stopRecording() {
        if(!recording) {
            Common.Log("Not recording");
            return;
        }

        ByteBuffer buf = BufferUtils.createByteBuffer(4);
        buf.putInt(-1).position(0);
        try {
            fc.write(buf); buf.position(0);
            fc.write(buf);
            Common.Log("Finished recording to demo");
            fc.close();
        } catch (IOException ex) {
            Common.Log("Failed to finish demo");
            Common.Log(Common.getExceptionString(ex));
        }

        fc = null;
        recording = false;
    }

    private void writeGamestate() {
        NetBuffer nbuf = NetBuffer.GetNetBuffer(false, true);
        
        nbuf.Write(cl.clc.reliableSequence);
        
        nbuf.Write(SVC.OPS_GAMESTATE);
        nbuf.Write(cl.clc.serverCommandSequence);

        // configstrings
        for (int integer : cl.cl.GameState.keySet()) {
            nbuf.Write(SVC.OPS_CONFIGSTRING);
            nbuf.Write(integer);
            nbuf.Write(cl.cl.GameState.get(integer));
        }

        // baselines
        EntityState nullstate = new EntityState();
        for (int i= 0; i < Common.MAX_GENTITIES; i++) {
            EntityState bases = cl.cl.entityBaselines[i];
            if(bases == null || bases.ClientNum <= 0)
                continue;

            nbuf.Write(SVC.OPS_BASELINE);
            bases.WriteDeltaEntity(nbuf, nullstate, true);
        }

        nbuf.Write(SVC.OPS_EOF);
        // finished writing the gamestate stuff
        // write the client num
        nbuf.Write(cl.clc.ClientNum);
        // finished writing the client packet
        nbuf.Write(SVC.OPS_EOF);
        try {
            // write it to the demo file
            ByteBuffer singleBuf = (ByteBuffer) BufferUtils.createByteBuffer(4).putInt(cl.clc.serverMessageSequence-1).position(0);
            fc.write(singleBuf);
            int bufsize = nbuf.GetBuffer().position();
            nbuf.GetBuffer().limit(bufsize);
            nbuf.GetBuffer().position(0);
            singleBuf.position(0);
            singleBuf.putInt(bufsize).position(0);
            fc.write(singleBuf);
            fc.write(nbuf.GetBuffer());

        } catch (IOException ex) {
            Common.Log("Failed to write gamestate to header");
            Common.Log(Common.getExceptionString(ex));
            recording = false;
            if(fc != null) {
                try { fc.close(); } catch (IOException ex1) {}
                fc = null;
            }
        }


    }

    void writeMessage(Packet data, int currPos) {
        try {
            // Seek packet buffer to start
            ByteBuffer bb = data.buf.GetBuffer();
            bb.position(currPos);

            // write the packet sequence
            ByteBuffer smallBuf = BufferUtils.createByteBuffer(4);
            smallBuf.putInt(cl.clc.serverMessageSequence); smallBuf.position(0);
            fc.write(smallBuf);smallBuf.position(0);

            // write packet size + data
            int size = bb.limit() - bb.position();
            smallBuf.putInt(size); smallBuf.position(0);
            fc.write(smallBuf);
            fc.write(bb);
        } catch (Exception ex) {
            Common.Log("Failed to write demo message");
            Common.Log(Common.getExceptionString(ex));
            recording = false;
            if(fc != null) {
                try { fc.close(); } catch(Exception e) {}
                fc = null;
            }
        }
    }

    public void recievedFullSnapshot() {
        // it's okay to start saving delta snapshots now
        demowaiting = false; 
    }

    public boolean isAwaitingFullSnapshot() {
        return demowaiting;
    }

    public boolean isRecording() {
        return recording && !demowaiting;
    }

    void readDemoMessage() {
        if(fileBuffer == null) {
            playCompleted(true);
            return;
        }

        // get the sequence number
        try {
            int seq = fileBuffer.getInt();
            cl.clc.serverMessageSequence = seq;

            int len = fileBuffer.getInt();
            if(len == -1) {
                playCompleted(true);
                return;
            }
            byte[] pdata = new byte[len];
            fileBuffer.get(pdata);

            NetBuffer packet = NetBuffer.CreateCustom(ByteBuffer.wrap(pdata));
            cl.clc.LastPacketTime = cl.realtime;
            cl.ParseServerMessage(packet);

        } catch(BufferUnderflowException ex) {
            Common.Log("Hit the end of demo");
            playCompleted(true);
            return;
        }

    }

    public boolean isPlaying() {
        return demoplaying;
    }

    // playback
    public void playCompleted(boolean disconnect) {
        fileBuffer = null;
        if(disconnect) cl.Disconnect(true);
        demoplaying = false;
        if(zipStream != null) {
            zipStream.close();
            zipStream = null;
        }
        if(Ref.cvars.Find("cl_demorecord").isTrue() && startTime != 0) {
            int deltams = Ref.common.Milliseconds() - startTime;
            Common.Log("Demo playback took " + deltams / 1000f + " seconds");
            startTime = 0;
        }
    }

    ICommand cmd_stoprecord = new ICommand() {
        public void RunCommand(String[] args) {
            stopRecording();
        }
    };

    ICommand cmd_demo = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length < 2) {
                Common.Log("demo <demoname> [track]");
                return;
            }

            if(recording) {
                Common.Log("Can't play while recording");
                return;
            }

            // make sure a local server is killed
            // 2 means don't force disconnect of local client
            Ref.cvars.Set2("sv_killserver", "2", true);
            cl.Disconnect(true);

            String name = args[1];
            try {
                fileBuffer = ResourceManager.OpenFileAsByteBuffer("demos/"+name+".dem", false).getKey();
            } catch (IOException ex) {
                Common.Log("Couldn't open " + name);
                Common.Log(Common.getExceptionString(ex));
                return;
            }

            fileBuffer.order(ByteOrder.nativeOrder());

            Ref.Console.Close();
            cl.state = ConnectState.CONNECTED;
            demoplaying = true;
            cl.servername = name;
            demoName = name;            
            fileBuffer.order(ByteOrder.nativeOrder());

            if(args.length > 2) {
                track = CameraTrack.load(demoName, args[2]);
            } else {
                track = new CameraTrack(); // allow for camera recording
            }

            ClientCubeMap cmap = new ClientCubeMap();
            if(Ref.cgame != null && Ref.cgame.map != null) Ref.cgame.map.dispose();

            CubeMap.unserialize(fileBuffer, cmap.chunks);

            // read demo messages until connected
            while(cl.state.ordinal() >= ConnectState.CONNECTED.ordinal()
                    && cl.state.ordinal() < ConnectState.PRIMED.ordinal()) {
                readDemoMessage();
            }

            Ref.cgame.map = cmap;

            // don't get the first snapshot this frame, to prevent the long
            // time from the gamestate load from messing causing a time skip
            firstDemoFrameSkipped = false;

            startTime = Ref.common.Milliseconds();
            
        }
    };

    public String getDemoName() {
        return demoName;
    }

    ICommand cmd_record = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length != 2) {
                Common.Log("record <demoname>");
                return;
            }

            if(recording) {
                Common.Log("Already recording");
                return;
            }

            if(cl.state != ConnectState.ACTIVE) {
                Common.Log("Must be in level to record");
                return;
            }

            if(ResourceManager.OpenFileAsFile(args[1]) != null) {
                Common.Log("File %s already exists.", args[1]);
                return;
            }

            initDemo(args[1]);
        }
    };

    boolean isFirstFrameSkipped() {
        return firstDemoFrameSkipped;
    }

    void setFirstFrameSkipped() {
        firstDemoFrameSkipped = true;
    }

}
