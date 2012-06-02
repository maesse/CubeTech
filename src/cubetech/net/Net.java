package cubetech.net;

import cubetech.common.Common;
import cubetech.misc.Ref;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mads
 */
public class Net {
    // Recieved packets must start with one of these integers
    public final static int MAGIC_NUMBER = 0x43124510; // for connected peers
    public final static int OOB_MAGIC = 0xffffffff; // out of band packets
    private final static int LOWDELAY_FLAG = 0x10; // Socket Flag
    
    // Server socket
    protected DatagramChannel srvChannel;
    protected DatagramSocket  srvSocket;

    // Client socket
    protected DatagramChannel cliChannel;
    protected DatagramSocket  cliSocket;
    
    // Stats
    public NetStats stats = new NetStats();
    private int lastUpdateTime;
    
    // Set to true when net finishes initializing
    boolean initialized = false; 
    
    // Queued outgoing packets
    private Queue<Packet> packets = new LinkedList<Packet>();

    public Net(int port) {
        try {
            openServerSocket(port);
            openClientSocket();
            initialized = true;
        } catch (IOException ex) {
            Logger.getLogger(DefaultNet.class.getName()).log(Level.SEVERE, null, ex);
            initialized = false;
        }
    }
    
    public Net() {
        try {
            openClientSocket();
            initialized = true;
        } catch (IOException ex) {
            Logger.getLogger(DefaultNet.class.getName()).log(Level.SEVERE, null, ex);
            initialized = false;
        }
    }    
    
    public void updateStats() {
        if(Ref.client == null || !initialized) return;
        if(lastUpdateTime + 1000 > Ref.client.realtime) return;
        lastUpdateTime = Ref.client.realtime;
        
        stats.updateStats();
    }

    public void SendOutOfBandPacket(Packet.ReceiverType source, InetSocketAddress dest, String data) {
        checkInitialized();
        NetBuffer buf = NetBuffer.GetNetBuffer(false, false);
        // OOB packets need this
        buf.Write(OOB_MAGIC);
        buf.Write(data);
        SendPacket(source, buf, dest);
    }

    public void SendPacket(Packet.ReceiverType source, NetBuffer buffer, InetSocketAddress dest) {
        checkInitialized();
        ByteBuffer buf = buffer.GetBuffer();
        int size = buf.position();
       // System.out.println("Client: Sending " + buf.position() + "bytes");
        buf.flip();
        try {
            if(source == Packet.ReceiverType.CLIENT) {
                stats.tclAvgBytesOut += size;
                stats.clLastBytesOut = size;
                stats.tclAvgPacketsOut++;
                if(cliChannel.send(buf, dest) == 0)
                    Common.LogDebug("Net.Client: Outgoing packet queued by JVM/OS");
            } else {
                stats.tsvAvgBytesOut += size;
                stats.tsvAvgPacketsOut++;
                if(srvChannel.send(buf, dest) == 0)
                    Common.LogDebug("Net.Server: Outgoing packet queued by JVM/OS");
            }
        } catch (IOException ex) {
            Logger.getLogger(DefaultNet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Packet GetPacket() {
        return packets.poll();
    }

    public void ConnectClient(InetSocketAddress addr) throws SocketException {
        checkInitialized();
        cliSocket.connect(addr);
    }
    
    public void disconnectClient() {
        checkInitialized();
        cliSocket.disconnect();
    }

    // Keep the water flowin', baby
    public void PumpNet(int time) {
        if(!initialized) return;
        
        try {
            // Recieve Server
            NetBuffer dest = NetBuffer.GetNetBuffer(false, false);
            ByteBuffer destBuf = dest.GetBuffer();
            InetSocketAddress addr;
            // While we got packets..
            while ((addr = (InetSocketAddress) srvChannel.receive(destBuf)) != null) {
                destBuf.flip();
                int size = destBuf.limit();
                stats.tsvAvgBytesIn += size;
                stats.tsvAvgPacketsIn++;
                // Check if it's a packet we know
                int magic = destBuf.getInt();
                if(magic == MAGIC_NUMBER || magic == OOB_MAGIC)
                {
                    size -= 4;
                    //System.out.println("Server: Recieved " + size + "bytes");
                    Packet packet = new Packet(Packet.ReceiverType.SERVER, addr, dest, magic == OOB_MAGIC, time);
                    // Enqueue it
                    packets.add(packet);
                }
                // Read next
                dest = NetBuffer.GetNetBuffer(false, false);
                destBuf = dest.GetBuffer();
            }

            // Recieve Client
            while ((addr = (InetSocketAddress) cliChannel.receive(destBuf)) != null) {
                destBuf.flip();
                int size = destBuf.limit();
                stats.tclAvgBytesIn += size;
                stats.clLastBytesIn = size;
                stats.tclAvgPacketsIn++;
                // Check if it's a packet we know
                int magic = destBuf.getInt();
                if(magic == MAGIC_NUMBER || magic == OOB_MAGIC)
                {
                    size -= 4;
                    //System.out.println("Client: Recieved " + size + "bytes");
                    Packet packet = new Packet(Packet.ReceiverType.CLIENT, addr, dest, magic == OOB_MAGIC, time);
                    // Enqueue it
                    packets.add(packet);
                }
                // Read next
                dest = NetBuffer.GetNetBuffer(false, false);
                destBuf = dest.GetBuffer();
            }
        } catch (IOException ex) {
            Logger.getLogger(DefaultNet.class.getName()).log(Level.SEVERE, null, ex);
        }

        updateStats();
    }

    public void shutdown() {
        DestroyClientSocket();
        DestroyServerSocket();
    }
    
    public void openServerSocket(int port) throws IOException {
        if(srvChannel != null) DestroyServerSocket();            

        srvChannel = DatagramChannel.open();
        srvChannel.configureBlocking(false); // non-blocking
        srvSocket = srvChannel.socket();
        
        // Try DEFAULT_PORT first, then increment by 1, etc..
        for (int i= 0; i < 5; i++) {
            try {
                srvSocket.bind(new InetSocketAddress("0.0.0.0",port + i));
                break;
            } catch (BindException e) {
                if(i < 4) continue;
                Common.Log("Failed to find a bindable server port.");
                throw e;
            }
        }
        
        srvSocket.setTrafficClass(LOWDELAY_FLAG);
    }
    
    private void openClientSocket() throws IOException {
        if(cliChannel != null) DestroyClientSocket();

        cliChannel = DatagramChannel.open();
        cliChannel.configureBlocking(false);
        cliSocket = cliChannel.socket();
        cliSocket.bind(null);
        cliSocket.setTrafficClass(LOWDELAY_FLAG);
    }

    private void DestroyClientSocket() {
        cliSocket.close();
        cliSocket = null;
        
        try {
            cliChannel.close();
        } catch (IOException ex) {
            Logger.getLogger(DefaultNet.class.getName()).log(Level.SEVERE, null, ex);
        }
        cliChannel = null;
    }

    private void DestroyServerSocket() {
        // CLean up the socket
        srvSocket.close();
        srvSocket = null;
        
        try {
            srvChannel.close();
        } catch (IOException ex) {
            Logger.getLogger(DefaultNet.class.getName()).log(Level.SEVERE, null, ex);
        }
        srvChannel = null;
    }
    
    private void checkInitialized() {
        if(!initialized) throw new RuntimeException("Net is disabled.");
    }

    public InetSocketAddress LookupHost(String server, int defaultPort) {
        int portIndex = server.lastIndexOf(":");
        int port = defaultPort;
        if(portIndex > 0) {
            try {
                port = Integer.parseInt(server.substring(portIndex+1, server.length()));
            } catch(NumberFormatException e) {
                Common.Log("Failed parsing portnumber.");
            }

            server = server.substring(0, portIndex);
        }
        InetSocketAddress addr = new InetSocketAddress(server, port);
        return addr;
    }
    
    public static String GetBroadcastAddress() {
        String str;
        try {
            str = InetAddress.getLocalHost().getHostAddress();
            int lastdot = str.lastIndexOf('.');
            str = str.substring(0, lastdot) + ".255";
            return str;
        } catch (UnknownHostException ex) {
            Logger.getLogger(DefaultNet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "255.255.255.255";
    }
}
