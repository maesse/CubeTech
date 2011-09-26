package cubetech.misc;

import cubetech.CGame.ViewParams;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.gfx.ResourceManager;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CameraTrack {
    private static class Frame {
        int frametime; Vector3f origin; Vector3f angles; float fov;
        public Frame(int frametime, Vector3f origin, Vector3f angles, float fov) {
            this.frametime = frametime;
            this.origin = new Vector3f(origin);
            this.angles = new Vector3f(angles);
            this.fov = fov;
        }
    }
    private static final int MAGIC = 'T'<<24|'R'<<16|'A'<<8|'C';
    private boolean recording = true;
    private ArrayList<Frame> frames = new ArrayList<Frame>();
    private int play_lastFrameOffset = 0;
    private int play_lastFrameTime = 0;

    public CameraTrack() {
        Ref.commands.AddCommand("save_track", save);
    }

    public boolean playingTrack() {
        return !recording;
    }

    public void readFrame(int time, ViewParams ref) {
        // Read a frame
        if(time < play_lastFrameTime) {
            Ref.common.Error(Common.ErrorCode.FATAL, "CameraTrack.readFrame: Time going in the negative direction :S");
        }
        play_lastFrameTime = time;
        int start = play_lastFrameOffset;
        int max = frames.size();
        Frame f1 = null;
        for (int i= start; i < max; i++) {
            Frame f = frames.get(i);
            if(f.frametime > time) {
                if(f1 == null) return; // not yet...
                // Got end

                int deltaTime = f.frametime - f1.frametime;
                float frac = ((time-f1.frametime)/deltaTime);
                if(frac < 0) frac = 0; if(frac > 1) frac = 1;
                Vector3f delta = Vector3f.sub(f.origin, f1.origin, null);
                Helper.VectorMA(f1.origin, frac, delta, ref.Origin);
                delta = Vector3f.sub(f.angles, f1.angles, delta);
                Helper.VectorMA(f1.angles, frac, delta, ref.Angles);
                play_lastFrameOffset = i-1;
                return;
            } else {
                f1 = f;
            }
        }
    }

    public static CameraTrack load(String demoname, String trackname) {
        String filename = demoname + "_" + trackname + ".track";
        BufferedInputStream in;
        try {
            // Open file
            in = ResourceManager.OpenFileAsInputStream("demos\\" + filename);
            DataInputStream data = new DataInputStream(in);
            
            // verify magic
            int magic = data.readInt();
            if(magic != MAGIC) {
                Common.Log("Invalid CameraTrack file");
                return new CameraTrack();
            }

            // Create track
            CameraTrack track = new CameraTrack();
            track.recording = false;

            // Read frames
            int count = data.readInt();
            for (int i= 0; i < count; i++) {
                Frame f = new Frame(data.readInt(), readVector(data), readVector(data), data.readFloat());
                track.frames.add(f);
            }

            data.close();
            return track;
        } catch (IOException ex) {
            Common.Log(ex);
            return new CameraTrack();
        }
    }
    
    public void recordFrame(int frametime, Vector3f origin, Vector3f angles, float fov) {
        if(recording) {
            Frame f = new Frame(frametime, origin, angles, fov);
            frames.add(f);
        }
    }

    public ICommand save = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length < 2) {
                Common.Log("usage: save_track <trackname>");
                return;
            }

            if(frames.isEmpty()) {
                Common.Log("No recorded frames to save");
                return;
            }

            DataOutputStream w = null;
            String filename = Ref.client.demo.getDemoName() + "_" + args[1] + ".track";
            try {

                w = new DataOutputStream(new FileOutputStream("demos\\" + filename));
                w.writeInt(MAGIC);
                w.writeInt(frames.size());
                for (Frame frame : frames) {
                    w.writeInt(frame.frametime);
                    writeVector(frame.origin, w);
                    writeVector(frame.angles, w);
                    w.writeFloat(frame.fov);
                }
                w.close();
                Common.Log("Camera track saved as %s. (%d frames)", filename, frames.size());
            } catch (IOException ex) {
                Common.Log(Common.getExceptionString(ex));
            }
        }
    };
    
    private static Vector3f readVector(DataInputStream in) throws IOException {
        return new Vector3f(in.readFloat(),in.readFloat(),in.readFloat());
    }
    
    private static void writeVector(Vector3f v, DataOutputStream out) throws IOException {
        out.writeFloat(v.x);
        out.writeFloat(v.y);
        out.writeFloat(v.z);
    }
}
