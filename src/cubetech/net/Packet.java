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
public class Packet {
    public SocketAddress endpoitn;
    public NetBuffer buf;
    public SourceType type;
    public int Time;
    public boolean OutOfBand;
    
    public enum SourceType {
        CLIENT,
        SERVER
    }

    public Packet(SourceType type, SocketAddress endpoint, NetBuffer buf, boolean OutOfBand) {
        this.endpoitn = endpoint;
        this.type = type;
        this.buf = buf;
        this.Time = Ref.common.Milliseconds();
    }
}
