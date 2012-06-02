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
public final class DefaultNet extends Net {
    public final static int DEFAULT_PORT = 27015;

    public CVar net_svport = Ref.cvars.Get("net_svport", ""+DEFAULT_PORT, EnumSet.of(CVarFlags.TEMP));
    public CVar net_clport = Ref.cvars.Get("net_clport", "", EnumSet.of(CVarFlags.TEMP));
    public CVar net_graph = Ref.cvars.Get("net_graph", "0", EnumSet.of(CVarFlags.ARCHIVE));
    
    

    public DefaultNet() {
        super(DEFAULT_PORT);
        
        // Get a random unsigned short for qport
        Ref.cvars.Get("net_qport", "" + (Ref.rnd.nextInt(65535)&0xffff), EnumSet.of(CVarFlags.INIT));
        Ref.cvars.Set2("net_svport", ""+srvSocket.getPort(), true);
        Ref.cvars.Set2("net_clport", ""+cliSocket.getPort(), true);
    }
    
    public void PumpNet() {
        PumpNet(Ref.common.Milliseconds());
    }
    

    public InetSocketAddress LookupHost(String server) {
        return LookupHost(server, DEFAULT_PORT);
    }
}
