/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import cubetech.common.Common.ErrorCode;
import cubetech.misc.Ref;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 *
 * @author mads
 */
public class NetChan {
    public static final int FRAGMENT_BIT = (1<<30);
    public static final int MSG_LEN = 16348;
    public static final int MAX_PACKET_SIZE = 1400;
    public static final int FRAGMENT_SIZE = MAX_PACKET_SIZE - 100;

    // Sequencing
    public int incomingSequence;
    public int outgoingSequence;

    public NetSource source;
    public InetSocketAddress addr;
    
    public int qport; // qport value to write when transmitting
    public int dropped; // between last packet and previous

    // Incomming fragment assembly
    int fragmentSequence;
    int fragmentLenght;
    byte[] fragmentBuffer = new byte[MSG_LEN];

    // Outgoing fragment buffer
    // we need to space out the sending of large fragmented messages
    public boolean unsentFragments;
    public int unsentFragmentStart;
    public int unsentLenght;
    byte[] unsentBuffer = new byte[MSG_LEN];

    public void TransmitNextFragment() {
        NetBuffer newbuf = NetBuffer.GetNetBuffer(true);
        newbuf.Write(outgoingSequence | FRAGMENT_BIT);

        if(source == NetSource.CLIENT)
            newbuf.Write(qport);

        int fragLen = FRAGMENT_SIZE;
        if(unsentFragmentStart + fragLen > unsentLenght) {
            fragLen = unsentLenght - unsentFragmentStart;
        }

        newbuf.Write(unsentFragmentStart);
        newbuf.Write(fragLen);
        newbuf.Write(unsentBuffer, unsentFragmentStart, fragLen);

        Ref.net.SendPacket(source, newbuf, addr);

        unsentFragmentStart += fragLen;

        // this exit condition is a little tricky, because a packet
	// that is exactly the fragment length still needs to send
	// a second packet of zero length so that the other side
	// can tell there aren't more to follow
        if(unsentFragmentStart == unsentLenght && fragmentLenght != FRAGMENT_SIZE) {
            outgoingSequence++;
            unsentFragments = false;
        }
    }

    

    public enum NetSource {
        CLIENT,
        SERVER
    }
    
    public NetChan(NetSource source, InetSocketAddress address, int qport) {
        addr = address;
        this.source = source;
        this.qport = qport;
        incomingSequence = 0;
        outgoingSequence = 1;
    }

    public void Transmit(NetBuffer buf) {
        if(buf.GetBuffer().position() > MSG_LEN)
            Ref.common.Error(ErrorCode.DROP, "NetChan.Transmit(): lenght > MSG_MAX");
        unsentFragmentStart = 0;

        // fragment large reliable messages
        if(buf.GetBuffer().position() > FRAGMENT_SIZE) {
            unsentFragments = true;
            unsentLenght = buf.GetBuffer().position();

            buf.Flip();
            buf.GetBuffer().get(unsentBuffer, 0, unsentLenght);

            TransmitNextFragment();
            return;
        }

        NetBuffer newbuf = NetBuffer.GetNetBuffer(true);
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
        

        boolean fragmented = false;
        if((seq & FRAGMENT_BIT) > 0) {
            seq &= ~FRAGMENT_BIT;
            fragmented = true;
        }

//        System.out.println("seq:" + seq);

        // read qport if server
        if(source == NetSource.SERVER)
            qport = packet.buf.ReadInt();

        // Read the fragment information
        int fragStart = 0;
        int fragLen = 0;
        if(fragmented) {
            fragStart = packet.buf.ReadInt();
            fragLen = packet.buf.ReadInt();
        }

        //
        // discard out of order or duplicated packets
        //
        //if(seq <= seq - (incomingSequence + 1)) {
        if(seq <= incomingSequence) {
            System.out.println("Out of order packetseq " + seq + " at " + incomingSequence);
            return false;
        }

        //
        // dropped packets don't keep the message from being used
        //
        dropped = seq - (incomingSequence + 1);
        if(dropped > 0)
            System.out.println("Dropped " + dropped + " at " + seq);

        //
	// if this is the final framgent of a reliable message,
	// bump incoming_reliable_sequence
	//
        if(fragmented) {
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
                System.out.println("Dumping fragment");
                return false;
            }

            // copy the fragment to the fragment buffer
            if(fragLen < 0 || packet.buf.GetBuffer().position() + fragLen > packet.buf.GetBuffer().limit()
                    || fragmentLenght + fragLen > fragmentBuffer.length) {
                System.out.println("Illegal fragment lenght");
                return false;
            }

            packet.buf.GetBuffer().get(fragmentBuffer, fragmentLenght, fragLen);
            fragmentLenght += fragLen;

            // if this wasn't the last fragment, don't process anything
            if(fragLen == FRAGMENT_SIZE) {
                return false;
            }

            if(fragmentLenght > MSG_LEN) {
                System.out.println("FragmentLenght > MAX_LEN");
                return false;
            }

            // copy the full message over the partial fragment
            packet.buf = NetBuffer.GetNetBuffer(false);
            ByteBuffer buf = packet.buf.GetBuffer();
            buf.position(0);
            buf.putInt(seq);
            buf.putInt(seq);
            buf.put(fragmentBuffer, 0, fragmentLenght);
            fragmentLenght = 0;
            buf.flip();
            buf.getInt();
            buf.getInt();
//            System.out.println("got full fragment");
//            incomingSequence = seq;
//            return true;
        }


        incomingSequence = seq;
        return true;
    }
}
