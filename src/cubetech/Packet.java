/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

/**
 *
 * @author mads
 */
public class Packet {
    public int endpoitn;
    public NetBuffer buf;
    public SourceType type;
    
    public enum SourceType {
        CLIENT,
        SERVER
    }

    public Packet(SourceType type, int endpoint, NetBuffer buf) {
        this.endpoitn = endpoint;
        this.type = type;
        this.buf = buf;
    }
}
