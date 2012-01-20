package cubetech.client;

import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.common.ICommand;
import cubetech.common.Info;
import cubetech.misc.MasterServer;
import cubetech.misc.Ref;
import cubetech.net.Net;
import cubetech.net.NetChan.NetSource;
import cubetech.net.Packet;
import cubetech.ui.ServerListUI;
import cubetech.ui.ServerListUI.ServerSource;
import java.net.InetSocketAddress;
import java.util.Map;


/**
 *
 * @author Mads
 */
public class ServerBrowser {
    // Server browser
    Ping[] cl_pinglist = new Ping[128];
    public ServerInfo[] cl_localServers = new ServerInfo[128];
    public int cl_nLocalServers = 0;
    public ServerInfo[] cl_globalServers = new ServerInfo[128];
    public int cl_nGlobalServers = 0;
    public ServerStatus[] cl_serverStatusList = new ServerStatus[16];
    public int serverStatusCount = 0;
    private ServerInfo[] serversourceList = cl_localServers;
    private int max;
    public ServerListUI.ServerSource serversource = ServerListUI.ServerSource.LAN;
    
    public ServerBrowser() {
        for (int i= 0; i < cl_pinglist.length; i++) {
            cl_pinglist[i] = new Ping();
        }
        for (int i= 0; i < cl_localServers.length; i++) {
            cl_localServers[i] = new ServerInfo();
        }
        for (int i= 0; i < cl_globalServers.length; i++) {
            cl_globalServers[i] = new ServerInfo();
        }
        Ref.commands.AddCommand("localservers", new cmd_LocalServers());
        Ref.commands.AddCommand("internetservers", new cmd_InternetServers());
    }
    
    public boolean handlePacket(Packet packet, String[] tokens, String c) {
        // server responding to an info broadcast
        if(c.equalsIgnoreCase("infoResponse")) {
            ServerInfoPacket(packet.endpoitn, tokens);
            return true;
        }

        // server responding to a get playerlist
        if(c.equalsIgnoreCase("statusResponse")) {
            ServerStatusResponse(packet.endpoitn, tokens);
            return true;
        }
        
        return false;
    }
    
    public void updateServerPinging(ServerListUI.ServerSource source) {
        serversource = source;

        if(serversource == ServerSource.LAN) {
            serversourceList = cl_localServers;
            max = cl_nLocalServers;
        }
        else {
            serversourceList = cl_globalServers;
            max = cl_nGlobalServers;
        }

        for (int i= 0; i < max; i++) {
            ServerInfo info = serversourceList[i];
            if(info.ping > 0)
                continue;

            int j = 0;
            int firstEmpty = -1;
            for (j= 0; j < cl_pinglist.length; j++) {
                if(cl_pinglist[j].adr == null && firstEmpty == -1)
                    firstEmpty = j;
                if(cl_pinglist[j].adr != null
                        && cl_pinglist[j].adr.equals(info.adr)) {
                    firstEmpty = j;
                    break;
                }
            }
            if(firstEmpty != -1) {
                cl_pinglist[firstEmpty].adr = info.adr;
                cl_pinglist[firstEmpty].time = 0;
                cl_pinglist[firstEmpty].start = Ref.common.Milliseconds();
                Ref.net.SendOutOfBandPacket(NetSource.CLIENT, info.adr, "getinfo");
            }
        }


    }

    private int getPingQueueCount() {
        int count = 0;
        for (Ping ping : cl_pinglist) {
            if(ping.adr != null)
                count++;
        }
        return count;
    }
    
    private class cmd_InternetServers implements ICommand {
        public void RunCommand(String[] args) {
            Common.LogDebug("Scanning for global servers...");

            // reset the list, waiting for response
            serversourceList = cl_globalServers;
            max = 0;
            serversource = ServerSource.INTERNET;
            cl_nGlobalServers = 0;
            for (int i= 0; i < cl_globalServers.length; i++) {
//                boolean b = cl_globalServers[i].visible;
                cl_globalServers[i] = new ServerInfo();
//                cl_globalServers[i].visible = b;
            }

            InetSocketAddress[] adr = MasterServer.getServerList();
            for (InetSocketAddress inetSocketAddress : adr) {
                cl_globalServers[cl_nGlobalServers].adr = inetSocketAddress;
                cl_nGlobalServers++;
            }

            updateServerPinging(ServerSource.INTERNET);
//            String msg = "getinfo";
//
//            // send each message twice in case one is dropped
//            for (int i= 0; i < 2; i++) {
//                // send a broadcast packet on each server port
//		// we support multiple server ports so a single machine
//		// can nicely run multiple servers
//                for (int j= 0; j < 5; j++) {
//
//                    InetSocketAddress to = new InetSocketAddress(Ref.net.GetBroadcastAddress(), Net.DEFAULT_PORT + j);
//                    Ref.net.SendOutOfBandPacket(NetSource.CLIENT, to, msg);
//                }
//            }
        }
    }
    
    private void ServerInfoPacket(InetSocketAddress from, String[] tokens) {
        String info = Commands.ArgsFrom(tokens, 1);

        try {
            // if this isn't the correct protocol version, ignore it
            int protocol = Integer.parseInt(Info.ValueForKey(info, "protocol"));
            if(protocol != Net.MAGIC_NUMBER) {
                Common.Log("Different protocol info packet: " + protocol);
                return;
            }

            for (int i= 0; i < cl_pinglist.length; i++) {
                Ping ping = cl_pinglist[i];
                if(ping.adr != null && ping.time <= 0 && from.equals(ping.adr)) {
                    // calc ping time
                    ping.time = Ref.common.Milliseconds() - ping.start;
                    Common.Log("Ping time " + ping.time + "ms from " + ping.adr);

                    // save of info
                    ping.info = info;
                    SetServerInfoByAddress(ping.adr, info, ping.time);
                    return;
                }
            }

            // TODO: Ignore if not requsting LAN servers
            int i;
            for (i= 0; i < serversourceList.length; i++) {
                if(serversourceList[i].adr == null)
                    break;

                // avoid duplicate
                if(serversourceList[i].adr.equals(from)) {
                    serversourceList[i].SetInfo(info, -1);
                    return;
                }
            }

            if(i == serversourceList.length) {
                Common.LogDebug("No room for more servers in the list");
                return;
            }

            // add this to the list

            ServerInfo dest = serversourceList[max];
            
            dest.adr = from;
            dest.nClients = 0;
            dest.hostname = "";
            dest.mapname = "";
            dest.maxClients = 0;
            dest.ping = -1;
            dest.gametype = 0;
            dest.SetInfo(info, -1);
            if(serversource == ServerSource.LAN)
                cl_nLocalServers++;
            else
                cl_nGlobalServers++;
            
        } catch(NumberFormatException e) {
            Common.LogDebug("Failed parsing ServerInfo packet: " + e.getMessage());
        }
    }

    private void SetServerInfoByAddress(InetSocketAddress adr, String info, int ping) {
        for(ServerInfo si : cl_localServers) {
            if(si.adr != null && si.adr.equals(adr))
                si.SetInfo(info, ping);
        }

        for(ServerInfo si : cl_globalServers) {
            if(si.adr != null && si.adr.equals(adr))
                si.SetInfo(info, ping);
        }
    }

    private void ServerStatusResponse(InetSocketAddress from, String[] tokens) {
        ServerStatus serverStatus = null;
        for (int i = 0; i < cl_serverStatusList.length; i++) {
            if(cl_serverStatusList[i].adr != null && cl_serverStatusList[i].adr.equals(from)) {
                serverStatus = cl_serverStatusList[i];
                break;
            }
        }

        // if we didn't request this server status
        if(serverStatus == null)
            return;

        String s = Commands.ArgsFrom(tokens, 1);
        serverStatus.str = s;
        if(serverStatus.print) {
            System.out.println("Server settings:");
            // print cvars
            Map<String, String> vars = Info.GetPairs(s);
            for(String key : vars.keySet()) {
                System.out.println(String.format(" %s \t: %s", key, vars.get(key)));
            }
        }

        serverStatus.time = Ref.common.Milliseconds();
        serverStatus.adr = from;
        serverStatus.pending = false;
        if(serverStatus.print)
            serverStatus.retrieved = true;
    }
    
    // Console commands
    private class cmd_LocalServers implements ICommand {
        public void RunCommand(String[] args) {
            Common.LogDebug("Scanning for local servers on the network...");

            // reset the list, waiting for response+
            serversourceList = cl_localServers;
            max = 0;
            serversource = ServerSource.LAN;
            cl_nLocalServers = 0;
            for (int i= 0; i < cl_localServers.length; i++) {
                boolean b = cl_localServers[i].visible;
                cl_localServers[i] = new ServerInfo();
                cl_localServers[i].visible = b;
            }

            String msg = "getinfo";

            // send each message twice in case one is dropped
            for (int i= 0; i < 2; i++) {
                // send a broadcast packet on each server port
		// we support multiple server ports so a single machine
		// can nicely run multiple servers
                for (int j= 0; j < 5; j++) {

                    InetSocketAddress to = new InetSocketAddress(Ref.net.GetBroadcastAddress(), Net.DEFAULT_PORT + j);
                    Ref.net.SendOutOfBandPacket(NetSource.CLIENT, to, msg);
                }
            }
        }
    }
}
