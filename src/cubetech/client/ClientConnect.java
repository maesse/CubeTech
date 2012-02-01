package cubetech.client;

import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.common.*;
import cubetech.common.Common.ErrorCode;
import cubetech.entities.EntityState;
import cubetech.gfx.ResourceManager;
import cubetech.input.Input;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.net.*;
import cubetech.net.NetChan.NetSource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Mostly network related stuff
 * @author mads
 */
public class ClientConnect {
    public ConnectState state = ConnectState.DISCONNECTED;
    public String servername;
    
    public int ClientNum;

    public int LastPacketTime;
    public int LastPacketSentTime;

    public int challenge; // server sends this number when we connect
                          // expecting it being sent back.
    public NetChan netchan;
    public InetSocketAddress ServerAddr;
    public int ConnectTime;
    public int ConnectPacketCount;

    public int reliableSequence;
    public int reliableAcknowlege;
    public String[] reliableCommands = new String[64];

    public int serverMessageSequence;
    public int serverCommandSequence;
    public int lastExecutedServerCommand;
    public String[] serverCommands = new String[64];
    public String servermessage;

    // Download
    ByteBuffer download = null;
    public String downloadName = null;
    int downloadNumber = 0;
    int downloadBlock;
    public int downloadCount;
    public int downloadTime;
    public int downloadSize;
    Queue<String> downloadList = new LinkedList<String>();
    boolean downloadRestart = false; //
    public NetBuffer mapdata = null;

    // Demo stuff
    int timeDemoFrames;
    int timeDemoStart;
    int timeDemoBaseTime;
    int timeDemoLastFrame;
    int timeDemoMinDuration;
    int timeDemoMaxDuration;
    

    public ClientConnect() {
        Ref.commands.AddCommand("connect", cmd_Connect);
        Ref.commands.AddCommand("disconnect", cmd_Disconnect);
        Ref.commands.AddCommand("cmd", cmd_Cmd);
        Ref.commands.AddCommand("downloadfile",cmd_downloadfile);
        Ref.commands.AddCommand("cl_dropin", cmd_dropinout);
        Ref.commands.AddCommand("cl_dropout", cmd_dropinout);
    }

    /*
    =====================
    CL_ParseCommandString

    Command strings are just saved off until cgame asks for them
    when it transitions a snapshot
    =====================
    */
    public void ParseCommandString(NetBuffer buf) {
        int seq = buf.ReadInt();
        String s = buf.ReadString();

        // see if we have already executed stored it off
        if(serverCommandSequence >= seq) return;

        serverCommandSequence = seq;
        serverCommands[seq & 63] = s;
    }
    
    /*
    ===================
    CL_DisconnectPacket

    Sometimes the server can drop the client and the netchan based
    disconnect can be lost.  If the client continues to send packets
    to the server, the server will send out of band disconnect packets
    to the client so it doesn't have to wait for the full timeout period.
    ===================
    */
    private void DisconnectPacket(InetSocketAddress address) {
        if(state.ordinal() < ConnectState.CONNECTING.ordinal()) return;

        // if not from our server, ignore it
        if(!address.equals(ServerAddr)) return;

        // drop the connection
        Common.Log("Server disconnected for unknown reasons.");
        Ref.cvars.Set2("errorMessage", "Server disconnected for unknown reasons.", true);
        Ref.client.Disconnect(true);
    }

    private void InitDownloads() {
        // ListenServer client doesn't need the map
        if(downloadList != null) {
            state = ConnectState.CONNECTED;
            downloadName = null;
            NextDownload();
            return;
        }

        DownloadsComplete();
    }

    private void DownloadsComplete() {
        if(state == ConnectState.ACTIVE) return;
            
        // notify server that we are done downloading
        if(downloadRestart) {
            downloadRestart = false;

            AddReliableCommand("donedl", false);

//            return; //
        }

        // let the client game init and load data
        state = ConnectState.LOADING;

        // Pump the loop, this may change gamestate!
        Ref.common.EventLoop();

        // if the gamestate was changed by calling Com_EventLoop
        // then we loaded everything already and we don't want to do it again.
        if(state != ConnectState.LOADING) return;

        Ref.cvars.Set2("ui_fullscreen", "0", true);
        Ref.client.FlushMemory();

        // initialize the CGame
        Ref.client.InitCGame();

        WritePacket();
        WritePacket();
        WritePacket();

        Ref.cvars.Set2("cl_paused", "0", true);
    }

    private void NextDownload() {
        if(downloadName != null) {
            // finished downloading a file.. check checksum?
            downloadName = null;
        }

        String dlStr = downloadList.poll();
        if(dlStr != null) {
            BeginDownload(dlStr);
            downloadRestart = true;
            return;
        }

        DownloadsComplete();
    }

    private void BeginDownload(String file) {
        Common.LogDebug("Begin download: " + file);

        downloadName = file;
        downloadBlock = 0;
        downloadCount = 0;
        downloadTime = Ref.client.realtime;

        AddReliableCommand("download " + file, false);
    }

    // Returns true if everything was parsed Ok.
    private boolean ParseDownload(NetBuffer buf) {
//        if(clc.downloadName == null) {
//            Common.Log("Recieving unrequested download");
//            AddReliableCommand("stopdl", false);
//            return false;
//        }

        int block = buf.ReadInt();
        if(block == 0) {
            // block zero is special, contains file size
            downloadSize = buf.ReadInt();

            // size -1 is an errormessage
            if(downloadSize <= -1)
            {
                if(state == ConnectState.ACTIVE) {
                    // Don't drop when already fully connected
                    Common.Log("Could not download file (%s): %s", downloadName, buf.ReadString());
                    downloadName = null;
                    download = null;
                    downloadCount = 0;
                    NextDownload();
                    return true;
                }
                Ref.common.Error(ErrorCode.DROP, buf.ReadString());
                return true;
            }
            String filename = buf.ReadString();
            downloadBlock = 0;
                downloadCount = 0;
                downloadTime = Ref.client.realtime;
                downloadName = filename;
        }

        int size = buf.ReadInt();
        if(size < 0) {
            Ref.common.Error(ErrorCode.DROP, "ParseDownload: Invalid block " + block + ", got " + size);
            return false;
        }

        byte[] data = new byte[size];
        buf.GetBuffer().get(data);

        if(downloadBlock != block) {
            Common.Log("ParseDownload: Expected block " + downloadBlock + ", got " + block);
            return false;
        }

        // open the file if not opened yet
        if(download == null) {
            download = ByteBuffer.allocate(downloadSize);
        }

        if(size > 0) {
            download.put(data);
        }

        AddReliableCommand("nextdl " + downloadBlock, false);
        downloadBlock++;

        downloadCount += size;

        if(size == 0) {
            // got EOF
            if(download != null) {
                finishedDownload();

                download = null;
            }

            // send intentions now
            // We need this because without it, we would hold the last nextdl and then start
            // loading right away.  If we take a while to load, the server is happily trying
            // to send us that last block over and over.
            // Write it twice to help make sure we acknowledge the download
            WritePacket();
            WritePacket();

            NextDownload();
        }
        return true;
    }

    private void finishedDownload() {
        if(download == null || download.limit() == 0) return;
        download.position(0);
        try {
            if(downloadName.equals("@cube")) {
                if(Ref.cgame != null && Ref.cgame.map != null) {
                    CubeMap.parseCubeData(download, -1, Ref.cgame.map.chunks);
                }
            }
            else if(downloadName.equalsIgnoreCase("map")) {
                // load map
                download.limit(download.position());
                download.flip();
                mapdata = NetBuffer.CreateCustom(download);
            } else
            {
                // Save file
                if(!ResourceManager.CreatePath("downloads/"+downloadName)) {
                    Common.Log("Cannot create destination folder: "+Helper.getPath("downloads/"+downloadName));
                    downloadName = Helper.stripPath(downloadName);
                    Common.Log("Saving as: " + "downloads/" + downloadName);
                }
                FileChannel chan = new FileOutputStream("downloads/"+downloadName, false).getChannel();
                download.limit(download.position());
                download.flip();
                chan.write(download);
                chan.close();

            }
        } catch (IOException ex) {
            Common.Log(Common.getExceptionString(ex));
        }
    }
    
    
    
    public String GetServerCommand(int commandNumber) {
        // if we have irretrievably lost a reliable command, drop the connection
        if(commandNumber <= serverCommandSequence - 64) {
            // when a demo record was started after the client got a whole bunch of
            // reliable commands then the client never got those first reliable commands
            if(Ref.client.demo.isPlaying()) {
                return null;
            }
            Ref.common.Error(Common.ErrorCode.DROP, "GetServerCommand: A reliable command was cycled");
            return null;
        }

        if(commandNumber > serverCommandSequence)
        {
            Ref.common.Error(Common.ErrorCode.DROP, "GetServerCommand: Requested command doesn't exist");
            return null;
        }

        String cmd = serverCommands[commandNumber & 63];
        lastExecutedServerCommand = commandNumber;

        if(cmd == null && Ref.client.demo.isPlaying()) {
            return null;
        }

        Common.LogDebug("ServerCommand: " + cmd);

        String[] tokens = Commands.TokenizeString(cmd, false);
        if(tokens[0].equalsIgnoreCase("disconnect")) {
            // allow server to indicate why they were disconnected
            if(tokens.length > 1)
                Ref.common.Error(Common.ErrorCode.SERVERDISCONNECT, "Server disconnected - " + Commands.ArgsFrom(tokens, 1));
            else
                Ref.common.Error(Common.ErrorCode.SERVERDISCONNECT, "Server disconnected.");
        }

        if(tokens[0].equalsIgnoreCase("cs")) {
            Ref.client.cl.ConfigStringModified(cmd);
            return cmd;
        }

        if(tokens[0].equalsIgnoreCase("map_restart")) {
            Ref.client.cl.cmds = new PlayerInput[4][64];
            return cmd;
        }

        return cmd;
    }
    
    public void PacketEvent(Packet data) {
        LastPacketTime = Ref.client.realtime;

        if(data.OutOfBand)
        {
            ConnectionlessPacket(data);
            return;
        }

        if(state.ordinal() < ConnectState.CONNECTED.ordinal())
            return;

        //
        // packet from server
        //
        if(!data.endpoitn.equals(ServerAddr)) {
            Common.LogDebug("Sequence packet without connection");
            return;
        }

        if(!netchan.Process(data))
            return; // out of order, duplicate, etc..

        ByteBuffer buf = data.buf.GetBuffer();
        int currPos = buf.position();
        buf.rewind();

        buf.getInt(); // Remove magic int
        serverMessageSequence = buf.getInt();
        buf.position(currPos);

        // uncompress
        int endPos = buf.limit() - 4;
        if(endPos > currPos) {
            NetBuffer uncompressed = NetBuffer.GetNetBuffer(false, true);
            byte[] dst = uncompressed.GetBuffer().array();
            try {
                int read = CubeChunk.uncompressData(data.buf.GetBuffer().array(), currPos, endPos, dst);
                uncompressed.GetBuffer().limit(read+4);
                uncompressed.GetBuffer().putInt(read, SVC.OPS_EOF);
                uncompressed.GetBuffer().position(0);
                data.buf = uncompressed;
            } catch (Exception ex) {
                Common.Log("Failed to uncompress packet: " + ex.toString());
                return;
            }
        }

        ParseServerMessage(data.buf);

        //
	// we don't know if it is ok to save a demo message until
	// after we have parsed the frame
	//
        if(Ref.client.demo.isRecording()) {
            Ref.client.demo.writeMessage(data, 0);
        }
    }
    
    void ParseServerMessage(NetBuffer buf) {
        // get the reliable sequence acknowledge number
        reliableAcknowlege = buf.ReadInt();
        if(reliableAcknowlege < reliableSequence - 64) {
            reliableSequence = reliableAcknowlege;
        }

        // parse the message
        while(true) {
            int cmd = buf.ReadInt();
            
            if(cmd == SVC.OPS_EOF)
                break;

            switch(cmd) {
                case SVC.OPS_NOP:
                    break;
                case SVC.OPS_SERVERCOMMAND:
                    ParseCommandString(buf);
                    break;
                case SVC.OPS_GAMESTATE:
                    ParseGameState(buf);
                    break;
                case SVC.OPS_SNAPSHOT:
                    Ref.client.cl.ParseSnapshot(buf);
                    break;
                case SVC.OPS_DOWNLOAD:
                    if(!ParseDownload(buf))
                        return;
                    break;
                default:
                    // TODO: should be drop error
                    Common.Log("Illegible server message.");
                    break;
            }
        }
    }

    void ConnectionlessPacket(Packet packet) {
        String str = packet.buf.ReadString();
        String[] tokens = Commands.TokenizeString(str, false);
        if(tokens.length <= 0)
            return;

        String c = tokens[0];
        if(c.equalsIgnoreCase("challengeResponse")) {
            if(state != ConnectState.CONNECTING) {
                Common.Log("Unwanted challenge response recived. Ignored");
                return;
            }

            if(!packet.endpoitn.equals(ServerAddr))
            {
                // This challenge response is not coming from the expected address.
                // Check whether we have a matching client challenge to prevent
                // connection hi-jacking.
                c = tokens[2];
                try {
                    if(challenge != Integer.parseInt(c)){
                        Common.Log("Challenge response was recived from an unexpected source");
                        return;
                    }
                } catch(NumberFormatException e) {
                    Common.Log("Bogus challenge response was recived from an unexpected source");
                    return;
                }
            }

            // start sending challenge response instead of challenge request packets
            challenge = Integer.parseInt(tokens[1]);
            state = ConnectState.CHALLENGING;
            ConnectPacketCount = 0;
            ConnectTime = -9999;

            // take this address as the new server address.  This allows
            // a server proxy to hand off connections to multiple servers
            ServerAddr = packet.endpoitn;
            //System.out.println("Got challenge response.");
            return;
        }
        if(c.equalsIgnoreCase("connectResponse")) {
            if(state.ordinal() >= ConnectState.CONNECTED.ordinal())
            {
                Common.Log("Duplicate connection recieved. Rejected.");
                return;
            }

            if(state != ConnectState.CHALLENGING) {
                Common.Log("ConnectResonse while not connecting. Ignoring.");
                return;
            }

            if(!packet.endpoitn.equals(ServerAddr))
            {
                Common.Log("ConnectResponse from wrong address. ignored.");
                return;
            }

            netchan = new NetChan(NetSource.CLIENT, packet.endpoitn, Ref.cvars.Find("net_qport").iValue);
            try {
                Ref.net.ConnectClient(packet.endpoitn);
            } catch (SocketException ex) {
                Ref.common.Error(ErrorCode.DROP, "Couldn't establish connection to host: \n" + Common.getExceptionString(ex));
            }
            state = ConnectState.CONNECTED;
            LastPacketSentTime = -999;
            return;
        }

        if(Ref.client.serverBrowser.handlePacket(packet, tokens, c)) return;

        // a disconnect message from the server, which will happen if the server
	// dropped the connection but it is still getting packets from us
        if(c.equalsIgnoreCase("disconnect")) {
            DisconnectPacket(packet.endpoitn);
            return;
        }

        // echo request from server
        if(c.equalsIgnoreCase("echo")) {
            Ref.net.SendOutOfBandPacket(NetSource.CLIENT, packet.endpoitn, Commands.ArgsFrom(tokens, 1));
            return;
        }

        // echo request from server
        if(c.equalsIgnoreCase("print")) {
            servermessage = tokens[1];
            Common.Log(servermessage);
            return;
        }

        // list of servers sent back by a master server
        if(c.equalsIgnoreCase("getserversResponse")) {
            // TODO: Add master server
            return;
        }

        Common.LogDebug("Unknown OOB packet from " + packet.endpoitn.toString());
    }

    public void ForwardCommandToServer(String str, String[] tokens) {
        String cmd = tokens[0];

        // ignore key up
        if(cmd.charAt(0) == '-')
            return;

        if(state.ordinal() < ConnectState.CONNECTED.ordinal() || cmd.charAt(0) == '+' || Ref.client.demo.isPlaying())
        {
            Common.Log("Unknown command " + cmd);
            return;
        }

        if(tokens.length > 1)
            AddReliableCommand(str, false); // concatted
        else
            AddReliableCommand(cmd, false); // single command
    }

    public void AddReliableCommand(String cmd, boolean isDisconnect) {
        int unacknowledged = reliableSequence - reliableAcknowlege;
        // if we would be losing an old command that hasn't been acknowledged,
        // we must drop the connection
        // also leave one slot open for the disconnect command in this case.
        if((isDisconnect && unacknowledged > 64) ||
                (!isDisconnect  && unacknowledged >= 64)) {
            Ref.common.Error(ErrorCode.DROP, "Client command overflow.");
        }

        reliableCommands[++reliableSequence & 63] = cmd;
    }
    
    public void CheckForResend() {
        if(Ref.client.demo.isPlaying()) return;

        // resend if we haven't gotten a reply yet
        if(state != ConnectState.CONNECTING && state != ConnectState.CHALLENGING)
            return;

        if(Ref.client.realtime - ConnectTime < 3000) // wait 3 secs before resending
            return;

        ConnectTime = Ref.client.realtime;
        ConnectPacketCount++;

        if(ConnectPacketCount == 5) {
            state = ConnectState.DISCONNECTED;
            ConnectPacketCount = 0;

            Ref.common.Error(Common.ErrorCode.DROP, "Could not connect. No response from server");
            //System.out.println("Could not connect. No response from server.");
            //Disconnect(true);
            return;
        }

        switch(state) {
            case CONNECTING:
                String data = "getchallenge " + challenge;
                Ref.net.SendOutOfBandPacket(NetSource.CLIENT, ServerAddr, data);
                Common.Log("Connecting...");
                break;
            case CHALLENGING:
                // sending back the challenge
                int port = Ref.cvars.Find("net_qport").iValue;
                
                String cs = Ref.cvars.InfoString(CVarFlags.USER_INFO);
                cs = Info.SetValueForKey(cs, "qport", ""+port);
                cs = Info.SetValueForKey(cs, "challenge", ""+challenge);

                data = String.format("connect \"%s\"", cs);
                Ref.net.SendOutOfBandPacket(NetSource.CLIENT, ServerAddr, data);
                Ref.cvars.modifiedFlags.remove(CVarFlags.USER_INFO);
//                System.out.println("CL: Got challenge.");
                break;
        }
    }
    
    private void ParseGameState(NetBuffer buf) {
        Ref.Console.Close();


        ConnectPacketCount = 0;
        // wipe local client state
        Ref.client.cl = new ClientActive();
        // a gamestate always marks a server command sequence
        serverCommandSequence = buf.ReadInt();
        // parse all the configstrings and baselines
        EntityState nullstate2 = new EntityState();
        while(true) {
            int cmd = buf.ReadInt();

            if(cmd == SVC.OPS_EOF)
                break;

            if(cmd == SVC.OPS_CONFIGSTRING) {
                // Read index and string
                int index = buf.ReadInt();
                String s = buf.ReadString();
                Ref.client.cl.GameState.put(index, s);
            }
            else if(cmd == SVC.OPS_BASELINE) {
                // Read baseline
                int newnum = buf.ReadShort();
                if(newnum < 0 || newnum >= Common.MAX_GENTITIES) {
                    Ref.common.Error(ErrorCode.DROP, "ParseGameState: Baseline number out of range");
                }
                Ref.client.cl.entityBaselines[newnum].ReadDeltaEntity(buf, nullstate2, newnum);
            } else {
                Ref.common.Error(ErrorCode.DROP, "ParseGameState(): Bad command byte");
            }
        }

        ClientNum = buf.ReadInt();

        // parse serverId and other cvars
        Ref.client.cl.SystemInfoChanged();

        InitDownloads();
    }

    // Called every frame to builds and sends a command packet to the server.
    public void SendCommand() {
        // don't send any message if not connected
        if(state.ordinal() < ConnectState.CONNECTED.ordinal()) return;

        // don't send commands if paused
        if(Ref.common.sv_running.isTrue() && 
                Ref.common.sv_paused.isTrue() && 
                Ref.common.cl_paused.isTrue()) return;

        // we create commands even if a demo is playing,
        Ref.client.cl.CreateNewCommands();

        // don't send a packet if the last packet was sent too recently
        if(!ReadyToSendPacket()) return;

        // Write packet
        WritePacket();
    }

    // Limits packetrate during connect and according to cl_cmdrate
    private boolean ReadyToSendPacket() {
        if(Ref.client.demo.isPlaying()) return false;

        // if we don't have a valid gamestate yet, only send one packet a second
        boolean limitPackets = state != ConnectState.ACTIVE && 
                state != ConnectState.PRIMED && 
                downloadName == null;
        if(limitPackets && (Ref.client.realtime - LastPacketSentTime) < 1000) return false;

        int oldpacketnum = (netchan.outgoingSequence - 1) & 31;
        int delta = Ref.client.realtime - Ref.client.cl.outPackets[oldpacketnum].realtime;

        // the accumulated commands will go out in the next packet
        if(delta < 1000 / Ref.client.cl_cmdrate.fValue) return false;

        return true;
    }

    /*
    ===================
    CL_WritePacket

    Create and send the command packet to the server
    Including both the reliable commands and the usercmds

    During normal gameplay, a client packet will contain something like:

    4	sequence number
    2	qport
    4	serverid
    4	acknowledged sequence number
    4	clc.serverCommandSequence
    <optional reliable commands>
    1	clc_move or clc_moveNoDelta
    1	command count
    <count * usercmds>

    ===================
    */
    PlayerInput nullstate = new PlayerInput();
    public void WritePacket() {
        if(Ref.client.demo.isPlaying()) {
            return;
        }
        
        ClientActive cl = Ref.client.cl;

        NetBuffer msg = NetBuffer.GetNetBuffer(false, false); // Netchan will add the magic

        // write the current serverId so the server
        // can tell if this is from the current gameState
        msg.Write(cl.serverid);

        // write the last message we received, which can
        // be used for delta compression, and is also used
        // to tell if we dropped a gamestate
        msg.Write(serverMessageSequence);

        // write the last reliable message we received
        msg.Write(serverCommandSequence);

        // write any unacknowledged clientCommands
        for (int i = reliableAcknowlege+1; i <= reliableSequence; i++) {
            msg.Write(CLC.OPS_CLIENTCOMMAND);
            msg.Write(i);
            msg.Write(reliableCommands[i & 63]);
//            System.out.println("Sending: " + clc.reliableCommands[i & 63]);
        }

        // we want to send all the usercmds that were generated in the last
        // few packet, so even if a couple packets are dropped in a row,
        // all the cmds will make it to the server
        int oldpacketnum = (netchan.outgoingSequence-1-Ref.client.cl_cmdbackup.iValue)&31;
        int count = cl.cmdNumber - cl.outPackets[oldpacketnum].cmdNumber;
        if(count > 32)
        {
            count = 32;
            Common.LogDebug("WritePacket: Can't send more than 32 usercommands pr frame");
        }

        PlayerInput old = nullstate;
        if(count >= 1) {
            for (int i = 0; i < 4; i++) {
                if(i > 0 && cl.snap.valid && cl.snap.lcIndex[i] == -1) continue;
                // begin a client move command
                if(!cl.snap.valid || Ref.client.demo.isAwaitingFullSnapshot()
                        || serverMessageSequence != cl.snap.messagenum
                        || Ref.client.cl_nodelta.iValue > 0)
                    msg.Write(CLC.OPS_MOVENODELTA);
                else
                    msg.Write(CLC.OPS_MOVE);

                // Client client index
                msg.WriteByte(i);

                // write the command count
                msg.WriteByte(count);

                // write all the commands, including the predicted command
                old = nullstate;
                for (int j= 0; j < count; j++) {
                    int index = (cl.cmdNumber - count + 1 + j) & 63;
                    cl.cmds[i][index].WriteDeltaUserCmd(msg, old);
                    old = cl.cmds[i][index];
                }
            }
        }

        // deliver the message
        int packetnum = netchan.outgoingSequence & 31;
        cl.outPackets[packetnum].realtime = Ref.client.realtime;
        cl.outPackets[packetnum].servertime = old.serverTime;
        cl.outPackets[packetnum].cmdNumber = cl.cmdNumber;
        LastPacketSentTime = Ref.client.realtime;

        // Send!
        msg.Write(CLC.OPS_EOF);
        netchan.Transmit(msg);
    }

    private ICommand cmd_dropinout = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length < 2)  {
                Common.Log("usage: cl_drop<in/out> <0-3>");
                return;
            }
            if(state != ConnectState.ACTIVE || Ref.client.demo.isPlaying()) return;
            // get player index
            int player;
            try {
                player = Integer.parseInt(args[1]);
                player = Helper.Clamp(player, 0, 3);
            } catch(NumberFormatException ex) { return; }
            
            boolean in = args[0].toLowerCase().contains("cl_dropin");
            String cmd = "drop" + (in ? "in" : "out") + " " + player;
            // maybe fix: send userinfo here
            AddReliableCommand(cmd, false);
        }
    };
    
    private ICommand cmd_downloadfile = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length > 1) {
                downloadList.add(args[1]);
                
                if(downloadName == null)
                    NextDownload();
            }
            else
                Common.Log("usage: downloadfile <filename> - queues the file for retrieval");
        }
    };
    
    // Console commands
    private ICommand cmd_Connect = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length != 2) {
                Common.Log("usage: connect <hostname[:ip]>");
                return;
            }

            String server = args[1];
            servermessage = "";

            if(Ref.common.sv_running.iValue == 1 && !server.equalsIgnoreCase("localhost")) {
                // If running a server, shut it down
                Ref.server.Shutdown("Server quit");
            }

            // Make sure the local server is killed
            Ref.cvars.Set2("sv_killserver", "1", true);
            Ref.server.Frame(0);

            Ref.client.Disconnect(true);
            Ref.Console.Close();

            InetSocketAddress endp = Ref.net.LookupHost(server);
            if(endp == null || endp.getAddress() == null) {
                Common.Log("Connect failed: Could not lookup hostname");
                state = ConnectState.DISCONNECTED;
                return;
            }

            if(endp.getHostName() != null) {
                Common.Log(String.format("%s resolved to %s", endp.getHostName(), endp.getAddress().getHostAddress()));
            }

            servername = server;
            ServerAddr = endp;
            state = ConnectState.CONNECTING;
            challenge = Ref.rnd.nextInt(9999999);
            ConnectTime = -99999;
            ConnectPacketCount = 0;
            Ref.Input.SetKeyCatcher(Input.KEYCATCH_NONE);
        }
    };

    // Console commands
    private ICommand cmd_Disconnect = new ICommand() {
        public void RunCommand(String[] args) {
            if(state != ConnectState.DISCONNECTED)
                Ref.common.Error(Common.ErrorCode.DISCONNECT, "Disconnected from server");
        }
    };

    // Console commands
    private ICommand cmd_Cmd = new ICommand() {
        public void RunCommand(String[] args) {
            if(state != ConnectState.ACTIVE || Ref.client.demo.isPlaying()) {
                Common.Log("Not connected to a server.");
                return;
            }

            // don't forward the first argument
            if(args.length > 1) {
                AddReliableCommand(Commands.Args(args), false);
            }
        }
    };
    
}
