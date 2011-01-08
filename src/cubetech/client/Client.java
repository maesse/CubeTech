/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.client;

import cubetech.misc.Commands;
import cubetech.misc.Ref;
import cubetech.net.NetChan;
import cubetech.net.NetChan.NetSource;
import cubetech.net.Packet;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mads
 */
public class Client {
    int frametime;
    int realtime;
    public State state = State.DISCONNECTED;
    public ClientConnect clc = null;

    public void PacketEvent(Packet data) {
        clc.LastPacketTime = realtime;

        if(data.OutOfBand)
        {
            ConnectionlessPacket(data);
            return;
        }

        if(state == State.DISCONNECTED)
            return;

        //
        // packet from server
        //
        if(data.endpoitn != clc.ServerAddr) {
            System.out.println("Sequence packet without connection");
            return;
        }

        if(!clc.netchan.Process(data))
            return; // out of order, duplicate, etc..

        ByteBuffer buf = data.buf.GetBuffer();
        int currPos = buf.position();
        buf.rewind();
        clc.serverMessageSequence = buf.getInt();
        buf.position(currPos);

        ParseServerMessage(data);
    }

    void ParseServerMessage(Packet packet) {
        // get the reliable sequence acknowledge number
        clc.reliableAcknowlege = packet.buf.ReadInt();
        if(clc.reliableAcknowlege < clc.reliableSequence - 64) {
            clc.reliableSequence = clc.reliableAcknowlege;
        }

        // parse the message
        while(true) {
            int cmd = packet.buf.ReadInt();
            
            if(cmd == Commands.SVC_OPS_EOF)
                break;

            switch(cmd) {
                case Commands.CLC_OPS_NOP:
                    break;
                default:
                    System.out.println("Illegable server message.");
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
        if(c.equals("connectResponse")) {
            if(state != State.DISCONNECTED)
            {
                System.out.println("Duplicate connection recieved. Rejected.");
                return;
            }

            if(packet.endpoitn != clc.ServerAddr)
            {
                System.out.println("ConnectResponse from wrong address. ignored.");
                return;
            }

            clc.netchan = new NetChan(NetSource.CLIENT, packet.endpoitn, 1);
            try {
                Ref.net.ConnectClient(packet.endpoitn);
            } catch (SocketException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            state = state.CONNECTED;
            clc.LastPacketSentTime = -999;
            return;
        }

        System.out.println("Unknown OOB packet from " + packet.endpoitn.toString());
    }

    public enum State {
        DISCONNECTED,
        CONNECTED
    }
    
    public void Frame(int msec) {
        frametime = msec;
        realtime += msec;

        //CheckUserInfo();

        //CheckTimeout();

        Ref.Input.SendCommand();
        // Update input
        // Send command

        // Checkforresend

        // setcgametime

        // updatescreen
        // endframe
    }
}
