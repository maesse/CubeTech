/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Common;
import cubetech.misc.Ref;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.nio.channels.*;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mads
 */
public final class Net {
    public final static int DEFAULT_PORT = 27015;
    final static int LOWDELAY_FLAG = 0x10; // Socket Flag

    // Recieved packets must start with one of these integers
    public final static int MAGIC_NUMBER = 0x43124510; // for connected peers
    public final static int OOB_MAGIC = 0xffffffff; // out of band packets
    
    private DatagramChannel srvChannel;
    private DatagramSocket  srvSocket;

    private DatagramChannel cliChannel;
    private DatagramSocket  cliSocket;

    // incomming/outgoing Stats
    public int clAvgBytesIn, clAvgPacketsIn;
    public int clLastBytesIn, clLastBytesOut;
    public int clAvgBytesOut, clAvgPacketsOut;
    public int svAvgBytesIn, svAvgPacketsIn;
    public int svAvgBytesOut, svAvgPacketsOut;

    private int lastUpdateTime;
    private int tclAvgBytesIn, tclAvgPacketsIn;
    private int tclAvgBytesOut, tclAvgPacketsOut;
    private int tsvAvgBytesIn, tsvAvgPacketsIn;
    private int tsvAvgBytesOut, tsvAvgPacketsOut;

    public CVar net_svport = Ref.cvars.Get("net_svport", ""+DEFAULT_PORT, EnumSet.of(CVarFlags.TEMP));
    public CVar net_clport = Ref.cvars.Get("net_clport", "", EnumSet.of(CVarFlags.TEMP));
    public CVar net_graph = Ref.cvars.Get("net_graph", "0", EnumSet.of(CVarFlags.ARCHIVE));
    boolean netInited = false; // Set to true when net finishes initializing
    // Queued outgoing packets
    public Queue<Packet> packets = new LinkedList<Packet>();

    private void UpdateStats() {
        if(Ref.client == null)
            return; // client system not running, no reason to record anything
        
        if(lastUpdateTime + 1000 < Ref.client.realtime)
        {
            lastUpdateTime = Ref.client.realtime;
        } else
            return;
        
        clAvgPacketsIn = tclAvgPacketsIn;
        clAvgPacketsOut = tclAvgPacketsOut;
        svAvgPacketsIn = tsvAvgPacketsIn;
        svAvgPacketsOut = tsvAvgPacketsOut;
        clAvgBytesIn = tclAvgBytesIn ;
        clAvgBytesOut = tclAvgBytesOut ;
        svAvgBytesIn = tsvAvgBytesIn;
        svAvgBytesOut = tsvAvgBytesOut;
        tclAvgPacketsIn = 0;
        tclAvgPacketsOut = 0;
        tsvAvgPacketsIn = 0;
        tsvAvgPacketsOut = 0;
        tclAvgBytesIn = 0;
        tclAvgBytesOut = 0;
        tsvAvgBytesIn = 0;
        tsvAvgBytesOut = 0;
    }

    public Net() {
        try {
            InitNet();
            netInited = true;
        } catch (IOException ex) {
            Logger.getLogger(Net.class.getName()).log(Level.SEVERE, null, ex);
            netInited = false;
        }

        // Get a random unsigned short for qport
        Ref.cvars.Get("net_qport", "" + (Ref.rnd.nextInt(65535)&0xffff), EnumSet.of(CVarFlags.INIT));
    }

    public void SendOutOfBandPacket(NetChan.NetSource source, InetSocketAddress dest, String data) {
        NetBuffer buf = NetBuffer.GetNetBuffer(false, false);
        // OOB packets need this
        buf.Write(OOB_MAGIC);
        buf.Write(data);
        SendPacket(source, buf, dest);
    }

    public void SendPacket(NetChan.NetSource source, NetBuffer buffer, InetSocketAddress dest) {
        ByteBuffer buf = buffer.GetBuffer();
        int size = buf.position();
       // System.out.println("Client: Sending " + buf.position() + "bytes");
        buf.flip();
        try {
            if(source == NetChan.NetSource.CLIENT) {
                tclAvgBytesOut += size;
                clLastBytesOut = size;
                tclAvgPacketsOut++;
                if(cliChannel.send(buf, dest) == 0)
                    Common.LogDebug("Net.Client: Outgoing packet queued by JVM/OS");
            } else {
                tsvAvgBytesOut += size;
                tsvAvgPacketsOut++;
                if(srvChannel.send(buf, dest) == 0)
                    Common.LogDebug("Net.Server: Outgoing packet queued by JVM/OS");
            }
        } catch (IOException ex) {
            Logger.getLogger(Net.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String GetBroadcastAddress() {
        String str;
        try {
            str = InetAddress.getLocalHost().getHostAddress();
            int lastdot = str.lastIndexOf('.');
            str = str.substring(0, lastdot) + ".255";
            return str;
        } catch (UnknownHostException ex) {
            Logger.getLogger(Net.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "255.255.255.255";
        
    }

    public Packet GetPacket() {
        return packets.poll();
    }

    public void ConnectClient(InetSocketAddress addr) throws SocketException {
        cliSocket.connect(addr);
    }

    // Keep the water flowin', baby
    public void PumpNet() {
//        netInited = false;
        if(!netInited)
            return;
        
        try {
            // Recieve Server
            NetBuffer dest = NetBuffer.GetNetBuffer(false, false);
            ByteBuffer destBuf = dest.GetBuffer();
            InetSocketAddress addr;
            // While we got packets..
            while ((addr = (InetSocketAddress) srvChannel.receive(destBuf)) != null) {
                destBuf.flip();
                int size = destBuf.limit();
                tsvAvgBytesIn += size;
                tsvAvgPacketsIn++;
                // Check if it's a packet we know
                int magic = destBuf.getInt();
                if(magic == MAGIC_NUMBER || magic == OOB_MAGIC)
                {
                    size -= 4;
                    //System.out.println("Server: Recieved " + size + "bytes");
                    Packet packet = new Packet(Packet.SourceType.SERVER, addr, dest, magic == OOB_MAGIC);
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
                tclAvgBytesIn += size;
                clLastBytesIn = size;
                tclAvgPacketsIn++;
                // Check if it's a packet we know
                int magic = destBuf.getInt();
                if(magic == MAGIC_NUMBER || magic == OOB_MAGIC)
                {
                    size -= 4;
                    //System.out.println("Client: Recieved " + size + "bytes");
                    Packet packet = new Packet(Packet.SourceType.CLIENT, addr, dest, magic == OOB_MAGIC);
                    // Enqueue it
                    packets.add(packet);
                }
                // Read next
                dest = NetBuffer.GetNetBuffer(false, false);
                destBuf = dest.GetBuffer();
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Net.class.getName()).log(Level.SEVERE, null, ex);
        }

        UpdateStats();
    }

    void InitNet() throws IOException {
        if(srvChannel != null)
            DestroyServerSocket();

        srvChannel = DatagramChannel.open();
        srvChannel.configureBlocking(false); // non-blocking
        srvSocket = srvChannel.socket();
        // Try DEFAULT_PORT first, then increment by 1, etc..
        for (int i= 0; i < 5; i++) {
            try {
                srvSocket.bind(new InetSocketAddress("0.0.0.0",DEFAULT_PORT + i));
                Ref.cvars.Set2("net_svport", ""+(DEFAULT_PORT + i), true);
                break;
            } catch (BindException e) {
                if(i < 4)
                    continue;
                Common.Log("Failed to find a bindable server port.");
                throw e;
            }
        }
        
        srvSocket.setTrafficClass(LOWDELAY_FLAG);

        if(cliChannel != null)
            DestroyClientSocket();

        cliChannel = DatagramChannel.open();
        cliChannel.configureBlocking(false);
        cliSocket = cliChannel.socket();
        cliSocket.bind(null);
        
//        int buf = cliSocket.getReceiveBufferSize();
//        cliSocket.setReceiveBufferSize(1400);
//        cliSocket.setSendBufferSize(1400);
//        srvSocket.setReceiveBufferSize(1400);
//        srvSocket.setSendBufferSize(1400);
        cliSocket.setTrafficClass(LOWDELAY_FLAG);
        Ref.cvars.Set2("net_clport", ""+cliSocket.getPort(), true);
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

    public InetSocketAddress LookupHost(String server) {
        int portIndex = server.lastIndexOf(":");
        int port = DEFAULT_PORT;
        
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
}
