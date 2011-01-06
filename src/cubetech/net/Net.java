/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author mads
 */
public class Net {
    public Queue<Packet> packets = new LinkedList<Packet>();

    public void SendPacket(Packet pack) {
        packets.add(pack);
    }

    public Packet GetPacket() {
        return packets.poll();
    }
}
