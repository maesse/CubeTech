/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import cubetech.misc.Ref;
import java.net.InetSocketAddress;

/**
 *
 * @author mads
 */
public class Packet {
    public InetSocketAddress endpoitn;
    public NetBuffer buf;
    public SourceType type;
    public int Time;
    public boolean OutOfBand;
    
    public enum SourceType {
        CLIENT,
        SERVER
    }

    public Packet(SourceType type, InetSocketAddress endpoint, NetBuffer buf, boolean OutOfBand) {
        this.OutOfBand = OutOfBand;
        this.endpoitn = endpoint;
        this.type = type;
        this.buf = buf;
        this.Time = Ref.common.Milliseconds();
    }
}
