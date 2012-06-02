/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import cubetech.misc.Ref;
import cubetech.net.Packet.ReceiverType;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Netchan packet format:
 * 1 bit fragment indicator
 * 31 bits (outgoing) sequence number
 * if source is client: 32 bits ident number
 * if fragment bit set: 16 bits fragment offset
 * if fragment bit set: 16 bits fragment length
 * extra data
 * @author mads
 */
public class NetChan {
    public static final int FRAGMENT_BIT = (1<<30); // bit flag for fragmented packets
    public static final int MSG_LEN = 16348; // max total length
    public static final int MAX_PACKET_SIZE = 1400;
    public static final int FRAGMENT_SIZE = MAX_PACKET_SIZE - 100; // when to fragment

    // Sequencing
    public int incomingSequence;
    public int outgoingSequence;

    public Packet.ReceiverType source; // this socket represents a client or server
    public InetSocketAddress addr;
    
    private int ident; // qport value to write when transmitting
    public int dropped; // between last packet and previous

    // Incomming fragment assembly
    private int fragmentSequence;
    private int fragmentLenght;
    private byte[] fragmentBuffer = new byte[MSG_LEN];

    // Outgoing fragment buffer
    // we need to space out the sending of large fragmented messages
    private boolean unsentFragments;
    private int unsentFragmentStart;
    private int unsentLenght;
    private byte[] unsentBuffer = new byte[MSG_LEN];
    
    private Net net;

    public boolean isBot = false;
    
    public NetChan(Net net, ReceiverType source, InetSocketAddress address, int ident) {
        this.net = net;
        addr = address;
        this.source = source;
        this.ident = ident;
        incomingSequence = 0;
        outgoingSequence = 1;
    }

    public void TransmitNextFragment() {
        NetBuffer newbuf = NetBuffer.GetNetBuffer(true, false);
        
        // Write header
        newbuf.Write(outgoingSequence | FRAGMENT_BIT);
        if(source == ReceiverType.CLIENT) newbuf.Write(getIdent());

        int fragLen = FRAGMENT_SIZE;
        if(unsentFragmentStart + fragLen > unsentLenght) {
            fragLen = unsentLenght - unsentFragmentStart;
        }
        // Write fragment header
        newbuf.Write(unsentFragmentStart);
        newbuf.Write(fragLen);
        
        // Write data
        newbuf.Write(unsentBuffer, unsentFragmentStart, fragLen);

        // Send
        net.SendPacket(source, newbuf, addr);

        unsentFragmentStart += fragLen;

        // this exit condition is a little tricky, because a packet
        // that is exactly the fragment length still needs to send
        // a second packet of zero length so that the other side
        // can tell there aren't more to follow
        if(unsentFragmentStart == unsentLenght && fragmentLenght != FRAGMENT_SIZE) {
            outgoingSequence++;
            setUnsentFragments(false);
        }
    }

    public void Transmit(NetBuffer buf) {
        if(buf.GetBuffer().position() > MSG_LEN)
            Ref.common.Error(ErrorCode.DROP, "NetChan.Transmit(): lenght > MSG_MAX");
        unsentFragmentStart = 0;

        buf.Flip();
        
        if(isBot) return;
        
        // fragment large reliable messages
        if(buf.GetBuffer().limit() > FRAGMENT_SIZE) {
            // Reset fragment state
            setUnsentFragments(true);
            unsentLenght = buf.GetBuffer().limit();
            
            // Copy data to sendbuffer
            buf.GetBuffer().get(unsentBuffer, 0, unsentLenght);

            // Send first fragment
            TransmitNextFragment();
            return;
        }

        // Create final buffer
        NetBuffer newbuf = NetBuffer.GetNetBuffer(true, false);
        
        // Write header
        newbuf.Write(outgoingSequence++);
        if(source == ReceiverType.CLIENT) newbuf.Write(getIdent());
        
        // Write data
        newbuf.GetBuffer().put(buf.GetBuffer());
        
        // Send
        net.SendPacket(source, newbuf, addr);
    }

    public boolean Process(Packet packet) {
        // get seq
        int seq = packet.buf.ReadInt();

        boolean fragmented = false;
        if((seq & FRAGMENT_BIT) != 0) {
            seq &= ~FRAGMENT_BIT;
            fragmented = true;
        }

        // read qport if server
        if(source == ReceiverType.SERVER) ident = packet.buf.ReadInt();        

        //
        // discard out of order or duplicated packets
        //
        //if(seq <= seq - (incomingSequence + 1)) {
        if(seq <= incomingSequence) {
            Common.LogDebug("Out of order packetseq " + seq + " at " + incomingSequence);
            return false;
        }

        //
        // dropped packets don't keep the message from being used
        //
        dropped = seq - (incomingSequence + 1);
        if(dropped > 0) Common.LogDebug("Dropped " + dropped + " at " + seq);

        //
        // if this is the final framgent of a reliable message,
        // bump incoming_reliable_sequence
        //
        if(fragmented) {
            // Read the fragment information
            int fragStart = packet.buf.ReadInt();
            int fragLen = packet.buf.ReadInt();
            
            // TTimo
            // make sure we add the fragments in correct order
            // either a packet was dropped, or we received this one too soon
            // we don't reconstruct the fragments. we will wait till this fragment gets to us again
            // (NOTE: we could probably try to rebuild by out of order chunks if needed)
            if(seq != fragmentSequence) {
                fragmentSequence = seq;
                fragmentLenght = 0;
            }

            // if we missed a fragment, dump the message
            if(fragStart != fragmentLenght) {
                // we can still keep the part that we have so far,
                // so we don't need to clear chan->fragmentLength
                Common.LogDebug("Dumping fragment");
                return false;
            }

            // copy the fragment to the fragment buffer
            if(fragLen < 0 || packet.buf.GetBuffer().position() + fragLen > packet.buf.GetBuffer().limit()
                    || fragmentLenght + fragLen > fragmentBuffer.length) {
                Common.LogDebug("Illegal fragment lenght");
                return false;
            }

            packet.buf.GetBuffer().get(fragmentBuffer, fragmentLenght, fragLen);
            fragmentLenght += fragLen;

            // if this wasn't the last fragment, don't process anything
            if(fragLen == FRAGMENT_SIZE) {
                return false;
            }

            if(fragmentLenght > MSG_LEN) {
                Common.LogDebug("FragmentLenght > MAX_LEN");
                return false;
            }

            // copy the full message over the partial fragment
            packet.buf = NetBuffer.GetNetBuffer(false, true);
            ByteBuffer buf = packet.buf.GetBuffer();
            // Fake header
            buf.putInt(seq);
            buf.putInt(seq);
            // put data
            buf.put(fragmentBuffer, 0, fragmentLenght);
            
            buf.flip();
            buf.position(4*2);
            
            fragmentLenght = 0;
        }

        incomingSequence = seq;
        return true;
    }
    
    public boolean isUnsentFragments() {
        return unsentFragments;
    }

    public void setUnsentFragments(boolean unsentFragments) {
        this.unsentFragments = unsentFragments;
    }

    public int getIdent() {
        return ident;
    }

    public boolean isLocalhost() {
        if(addr == null) return false;
        return (addr.getAddress().isLoopbackAddress());
    }

    public boolean isLAN() {
        if(addr == null) return isBot;
        // todo: test
        return (addr.getAddress().isAnyLocalAddress());
    }
    
    public int getUnsentBytes() {
        if(unsentFragments) return unsentLenght - unsentFragmentStart;
        return 0;
    }
}
