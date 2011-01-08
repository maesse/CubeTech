/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import cubetech.misc.Ref;
import java.net.SocketAddress;

/**
 *
 * @author mads
 */
public class NetChan {
    public int incomingSequence;
    public int outgoingSequence;
    public int qport;
    public int dropped;
    public NetSource source;
    public SocketAddress addr;

    public enum NetSource {
        CLIENT,
        SERVER
    }
    
    public NetChan(NetSource source, SocketAddress address, int qport) {
        addr = address;
        this.source = source;
        this.qport = qport;
        incomingSequence = 0;
        outgoingSequence = 1;
    }

    public void Transmit(NetBuffer buf) {
        NetBuffer newbuf = Ref.net.GetNetBuffer(false);
        newbuf.Write(outgoingSequence);
        outgoingSequence++;

        if(source == NetSource.CLIENT)
            newbuf.Write(qport);
        buf.Flip();
        newbuf.GetBuffer().put(buf.GetBuffer());
        Ref.net.SendPacket(source, newbuf, addr);
    }

    public boolean Process(Packet packet) {
        // get seq
        int seq = packet.buf.ReadInt();

        // read qport if server
        int qport = 0;
        if(source == NetSource.SERVER)
            qport = packet.buf.ReadInt();

        //
        // discard out of order or duplicated packets
        //
        if(seq <= seq - (incomingSequence + 1)) {
            System.out.println("Out of order packetseq " + seq + " at " + incomingSequence);
            return false;
        }

        //
        // dropped packets don't keep the message from being used
        //
        dropped = seq - (incomingSequence + 1);
        if(dropped > 0)
            System.out.println("Dropped " + dropped + " at " + seq);

        incomingSequence = seq;
        return true;
    }
}
