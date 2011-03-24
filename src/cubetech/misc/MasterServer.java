/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mads
 */
public class MasterServer {
    public static void sendHeartbeat(int port) {
        try {
            URL url = new URL("http://pd-eastcoast.com/rgj6/heartbeat.php?port=" + port);
            url.openStream().close();
        } catch (IOException ex) {
            System.out.println("Failed to send heartbeat");
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static int lasthearbeat = -1;
    private static final int HEARTBEAT_DELAY = 1 * 60 * 1000; // 1 minute

    public static void autohandleHeartbeat(int time) {
        if(lasthearbeat == -1 || lasthearbeat + HEARTBEAT_DELAY < time) {
            lasthearbeat = time;
            sendHeartbeat(Ref.cvars.Find("net_svport").iValue);
        }
    }

    private static String[] cachedServerList = null;
    private static int lastCheckTime = -1;
    private static final int SERVERLIST_DELAY = 10 * 1000; // 10 seconds

    private static InetSocketAddress[] addresses = new InetSocketAddress[0];

    private static void buildAddressList() {
        addresses = new InetSocketAddress[0];

        if(cachedServerList == null || cachedServerList.length == 0)
            return;

        ArrayList<InetSocketAddress> adrs = new ArrayList<InetSocketAddress>();
        for (String string : cachedServerList) {
            String[] splt = string.split(":");
            if(splt.length != 2)
                continue;
            int port = -1;
            try {
                port = Integer.parseInt(splt[1]);
            } catch(NumberFormatException ex) { continue;}
            try {
                InetSocketAddress entry = new InetSocketAddress(splt[0], port);
                adrs.add(entry);
            } catch (IllegalArgumentException ex) {continue;}
        }

        addresses = new InetSocketAddress[adrs.size()];
        adrs.toArray(addresses);
    }

    public static InetSocketAddress[] getServerList() {
        int time = -1;
        if(Ref.common != null) {
            time = Ref.common.Milliseconds();
            if(cachedServerList != null && time != -1 && lastCheckTime + SERVERLIST_DELAY > time)
                return addresses;
        }
        try {
            ArrayList<String> servers = new ArrayList<String>();
            URL url = new URL("http://pd-eastcoast.com/rgj6/serverlist.php");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while((line = reader.readLine()) != null) {
                servers.add(line.trim());
            }
            reader.close();
            String[] dst = new String[servers.size()];

            dst = servers.toArray(dst);
            cachedServerList = dst;
            lastCheckTime = time;

            buildAddressList();
            return addresses;
        } catch (IOException ex) {
            System.out.println("Failed to get serverlist from master server");
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        cachedServerList = null;
        buildAddressList();
        return addresses;
    }
}
