/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.client;

import cubetech.net.NetBuffer;
import cubetech.net.NetChan;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author mads
 */
public class ClientConnect {
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
        if(serverCommandSequence >= seq)
            return;

        serverCommandSequence = seq;
        serverCommands[seq & 63] = s;
    }
}
