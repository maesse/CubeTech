/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.nio.channels.*;

/**
 *
 * @author mads
 */
public class Net {
    final static int DEFAULT_PORT = 27015;
    final static int LOWDELAY_FLAG = 0x10;
    DatagramChannel srvChannel;
    DatagramSocket  srvSocket;
    public Queue<Packet> packets = new LinkedList<Packet>();

    public void SendPacket(Packet pack) {
        packets.add(pack);
    }

    public Packet GetPacket() {
        return packets.poll();
    }

    void InitServerSocket() throws IOException {
        if(srvChannel != null)
            DestroyServerSocket();

        srvChannel = DatagramChannel.open();
        srvChannel.configureBlocking(false); // non-blocking
        srvSocket = srvChannel.socket();
        srvSocket.bind(new InetSocketAddress(DEFAULT_PORT));
        srvSocket.setTrafficClass(LOWDELAY_FLAG);
        
    }

    void DestroyServerSocket() {
        // CLean up the socket
        srvChannel = null;
        srvSocket = null;
    }
}
