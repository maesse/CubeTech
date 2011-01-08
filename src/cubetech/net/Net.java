/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.nio.channels.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mads
 */
public final class Net {
    final static int DEFAULT_PORT = 27015;
    final static int LOWDELAY_FLAG = 0x10;
    final static int POOL_SIZE = 32;
    public final static int MAGIC_NUMBER = 0x43124510; // for connected peers
    public final static int OOB_MAGIC = 0xffffffff; // out of band packets
    DatagramChannel srvChannel;
    DatagramSocket  srvSocket;

    DatagramChannel cliChannel;
    DatagramSocket  cliSocket;
    
    NetBuffer[] BufferPool = new NetBuffer[POOL_SIZE];
    int PoolIndex = 0;
    public Queue<Packet> packets = new LinkedList<Packet>();
   // public Queue<Packet> outPackets = new LinkedList<Packet>();

    public Net() {
        try {
            // Create a bunch of netbuffers now to we don't have to create
            // all those buffers constantly, later on
            for (int i = 0; i < POOL_SIZE; i++) {
                BufferPool[i] = new NetBuffer();
            }
            InitNet();
        } catch (IOException ex) {
            Logger.getLogger(Net.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public NetBuffer GetNetBuffer(boolean writeMagicHeader) {
        NetBuffer buf = BufferPool[PoolIndex++];
        if(PoolIndex >= POOL_SIZE)
            PoolIndex = 0; // wrap
        buf.Clear();
        if(writeMagicHeader)
            buf.Write(MAGIC_NUMBER);
        return buf;
    }

    public void SendPacket(NetChan.NetSource source, NetBuffer buffer, SocketAddress dest) {
        ByteBuffer buf = buffer.GetBuffer();
       // System.out.println("Client: Sending " + buf.position() + "bytes");
        buf.flip();
        try {
            if(source == NetChan.NetSource.CLIENT)
                cliChannel.send(buf, dest);
            else
                srvChannel.send(buf, dest);
        } catch (IOException ex) {
            Logger.getLogger(Net.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Packet GetPacket() {
        return packets.poll();
    }

    public void ConnectClient(SocketAddress addr) throws SocketException {
        cliSocket.connect(addr);
    }

    // Keep the water flowin', baby
    public void PumpNet() {
        
        try {
            // Recieve Server
            NetBuffer dest = GetNetBuffer(false);
            ByteBuffer destBuf = dest.GetBuffer();
            SocketAddress addr;
            // While we got packets..
            while ((addr = srvChannel.receive(destBuf)) != null) {
                destBuf.flip();
                int size = destBuf.limit();
                // Check if it's a packet we know
                int magic = destBuf.getInt();
                if(magic == MAGIC_NUMBER || magic == OOB_MAGIC)
                {
                    size -= 4;
                    System.out.println("Server: Recieved " + size + "bytes");
                    Packet packet = new Packet(Packet.SourceType.SERVER, addr, dest, magic == OOB_MAGIC);
                    // Enqueue it
                    packets.add(packet);
                }
                // Read next
                dest = GetNetBuffer(false);
                destBuf = dest.GetBuffer();
            }

            // Recieve Client
            while ((addr = cliChannel.receive(destBuf)) != null) {
                destBuf.flip();
                int size = destBuf.limit();
                // Check if it's a packet we know
                int magic = destBuf.getInt();
                if(magic == MAGIC_NUMBER || magic == OOB_MAGIC)
                {
                    size -= 4;
                    System.out.println("Client: Recieved " + size + "bytes");
                    Packet packet = new Packet(Packet.SourceType.CLIENT, addr, dest, magic == OOB_MAGIC);
                    // Enqueue it
                    packets.add(packet);
                }
                // Read next
                dest = GetNetBuffer(false);
                destBuf = dest.GetBuffer();
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Net.class.getName()).log(Level.SEVERE, null, ex);
        }

        
    }



    void InitNet() throws IOException {
        if(srvChannel != null)
            DestroyServerSocket();

        srvChannel = DatagramChannel.open();
        srvChannel.configureBlocking(false); // non-blocking
        srvSocket = srvChannel.socket();
        srvSocket.bind(new InetSocketAddress("0.0.0.0",DEFAULT_PORT));
        srvSocket.setTrafficClass(LOWDELAY_FLAG);

        if(cliChannel != null)
            DestroyClientSocket();

        cliChannel = DatagramChannel.open();
        cliChannel.configureBlocking(false);
        cliSocket = cliChannel.socket();
        cliSocket.bind(null);
    }

    void DestroyClientSocket() {
        cliSocket = null;
        cliChannel = null;
    }

    void DestroyServerSocket() {
        // CLean up the socket
        srvSocket = null;
        srvChannel = null;
    }
}
