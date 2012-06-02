package cubetech.net;

import cubetech.misc.Ref;
import java.net.InetSocketAddress;

/**
 * A received packet
 * @author mads
 */
public class Packet {
    public InetSocketAddress endPoint; // internet socket of sender
    public NetBuffer buf; // data buffer
    public ReceiverType type; // who the packet is for (server/client)
    public int time; // time when this packet was received
    public boolean outOfBand; // true if packet was OOB
    
    public enum ReceiverType {
        CLIENT,
        SERVER
    }

    public Packet(ReceiverType type, InetSocketAddress endpoint, NetBuffer buf, boolean OutOfBand, int time) {
        this.outOfBand = OutOfBand;
        this.endPoint = endpoint;
        this.type = type;
        this.buf = buf;
        this.time = time;
    }
}
