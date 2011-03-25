package cubetech.CGame;

import cubetech.common.CS;
import cubetech.common.Info;
import cubetech.misc.Ref;
import java.util.HashMap;

/**
 * The client game static (cgs) structure hold everything
 *  loaded or calculated from the gamestate.  It will NOT
 *  be cleared when a tournement restart is done, allowing
 *  all clients to begin playing instantly
 * @author mads
 */
public final class CGameStatic {
    public HashMap<Integer, String> gameState; // gamestate from server
    public int serverCommandSequence; // reliable command stream counter
    public int processedSnapshotNum; // the number of snapshots cgame has requested
    public boolean localServer; // detected on startup by checking sv_running

    // parsed from serverinfo
    public int maxclients;
    public String mapname = "";
    public int gametype;
    public int levelStartTime;
    public ClientInfo[] clientinfo = new ClientInfo[64];
    public Media media = new Media();

    // Constructor
    public CGameStatic(HashMap<Integer, String> state, int processed, int servercmdSeq) {
        gameState = state;
        processedSnapshotNum = processed;
        serverCommandSequence = servercmdSeq;

        if(Ref.common.sv_running.iValue == 1)
            localServer = true; // running local server

        levelStartTime = Integer.parseInt(ConfigString(CS.CS_LEVEL_START_TIME));
        ParseServerInfo();
    }

    public void ParseServerInfo() {
        String info = ConfigString(CS.CS_SERVERINFO);
        gametype = Integer.parseInt(Info.ValueForKey(info, "g_gametype"));
        Ref.cvars.Set2("g_gametype", ""+gametype, true);
        Ref.cvars.Set2("sv_speed", Info.ValueForKey(info, "sv_speed"), true);
        maxclients = Integer.parseInt(Info.ValueForKey(info, "sv_maxclients"));
        mapname = Info.ValueForKey(info, "mapname");
    }

    // Loads sounds
    public void RegisterSound() {

    }

    // Loads graphics
    public void RegisterGraphics() {
        
    }

    public void RegisterClients(int clientNum) {
        NewClientInfo(clientNum); // Info for local client
        for (int i= 0; i < clientinfo.length; i++) {
            if(clientNum == i)
                continue;

            String cs = ConfigString(CS.CS_PLAYERS + i);
            if(cs == null || cs.isEmpty())
                continue;
            
            NewClientInfo(i);
        }
    }

    public String ConfigString(int index) {
        return gameState.get(index);
    }

    void NewClientInfo(int clientNum) {
        String cs = ConfigString(CS.CS_PLAYERS + clientNum);
        if(cs == null || cs.isEmpty()) {
            // Player just left
            clientinfo[clientNum] = null;
            return;
        }

        ClientInfo info = ClientInfo.Parse(cs);
        clientinfo[clientNum] = info;
    }

    void SetConfigValues() {
        try {
        levelStartTime = Integer.parseInt(gameState.get(CS.CS_LEVEL_START_TIME));
        } catch(NumberFormatException ex) {
            System.out.println("SetConfigValues: Couldn't parse levelstarttime: " + gameState.get(CS.CS_LEVEL_START_TIME));
        }
    }
}
