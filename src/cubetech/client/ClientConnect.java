/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.client;

import cubetech.net.NetChan;
import java.net.SocketAddress;

/**
 *
 * @author mads
 */
public class ClientConnect {
    public int LastPacketTime;
    public int ClientNum;
    public int LastPacketSentTime;
    public SocketAddress ServerAddr;
    public int ConnectTime;
    public int ConnectPacketCount;

    public int reliableSequence;
    public int reliableAcknowlege;
    public String[] reliableCommands = new String[64];

    public int serverMessageSequence;
    public int serverCommandSequence;
    public int lastExecutedServerCommand;
    public String[] serverCommands = new String[64];
    public NetChan netchan;
    

    public ClientConnect() {
        
    }
}
