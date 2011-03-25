package cubetech.client;

import cubetech.common.Common;
import cubetech.common.IThinkMethod;
import cubetech.common.Info;
import java.net.InetSocketAddress;

/**
 *
 * @author mads
 */
public class ServerInfo {
    public InetSocketAddress adr;
    public String hostname = "N/A";
    public String mapname = "N/A";
    int nettype; //?
    public int gametype;
    public int nClients;
    public int maxClients;
    public int ping;
    public boolean visible;

    public IThinkMethod onUpdate = null;

    void SetInfo(String info, int ping) {
        if(!info.isEmpty()) {
            try {
                nClients = Integer.parseInt(Info.ValueForKey(info, "clients"));
                hostname = Info.ValueForKey(info, "hostname");
                mapname = Info.ValueForKey(info, "mapname");
                nClients = Integer.parseInt(Info.ValueForKey(info, "clients"));
                maxClients = Integer.parseInt(Info.ValueForKey(info, "sv_maxclients"));
                gametype = Integer.parseInt(Info.ValueForKey(info, "gametype"));
            } catch(NumberFormatException e) {
                Common.LogDebug("ServerInfo: Couldn't fully parse the info: " + info + ": " + e.getMessage());
            }
        }
        this.ping = ping;
        if(onUpdate != null)
            onUpdate.think(null);
    }
}
