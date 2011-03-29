package cubetech.client;

import cubetech.common.CS;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.common.Info;
import cubetech.entities.EntityState;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.net.ConnectState;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mads
 */
public class ClientActive {
    public int serverTime; // may be paused during play
    public int oldServerTime; // to prevent time from flowing bakcwards
    public int oldFrameServerTime; // to check tournament restarts
    public int serverTimeDelta;  // cl.serverTime = cls.realtime + cl.serverTimeDelta
                                 // this value changes as net lag varies
    public boolean extrapolatedSnapshot; // set if any cgame frame has been forced to extrapolate
                                        // cleared when CL_AdjustTimeDelta looks at it
    public boolean newsnapshots;  // set on parse of any valid packet
    public String mapname; // extracted from CS_SERVERINFO
    public int parseEntitiesNum;  // index (not anded off) into cl_parse_entities[]
    public PlayerInput[] cmds = new PlayerInput[64]; // each mesage will send several old cmds
    public int cmdNumber; // incremented each frame, because multiple
                          // frames may need to be packed into a single packet
    public HashMap<Integer, String> GameState = new HashMap<Integer, String>();  // configstrings
    public CLSnapshot snap = new CLSnapshot(); // latest received from server
    public CLSnapshot[] snapshots = new CLSnapshot[32];
    public EntityState[] entityBaselines = new EntityState[Common.MAX_GENTITIES];
    public EntityState[] parseEntities = new EntityState[Common.MAX_GENTITIES];
    public int timeoutCount; // it requres several frames in a timeout condition
                             // to disconnect, preventing debugging breaks from
                             // causing immediate disconnects on continue
    public int serverid; // included in each client message so the server
                         // can tell if it is for a prior map_restart

    public OutPacket[] outPackets = new OutPacket[32];

    public ClientActive() {
        serverid = Integer.MAX_VALUE;
        for (int i= 0; i < parseEntities.length; i++) {
            parseEntities[i] = new EntityState();
        }
        for (int i= 0; i < entityBaselines.length; i++) {
            entityBaselines[i] = new EntityState();
        }
        for (int i= 0; i < snapshots.length; i++) {
            snapshots[i] = new CLSnapshot();
        }
        for (int i= 0; i < outPackets.length; i++) {
            outPackets[i] = new OutPacket();
        }
    }

    public void SetCGameTime() {
        if(Ref.client.state != ConnectState.ACTIVE)
        {
            if(Ref.client.state != ConnectState.PRIMED)
                return;

            if(newsnapshots) {
                newsnapshots = false;
                FirstSnapshot();
            }

            if(Ref.client.state != ConnectState.ACTIVE)
                return; // Still not ACTIVE
        }

        // if we have gotten to this point, cl.snap is guaranteed to be valid
        if(!snap.valid)
        {
            Common.Log("SetCGameGtime(): !snap.valid");
            return;
        }

        // allow pause in single player
        if(Ref.common.sv_paused.iValue == 1 && Ref.client.CheckPaused() && Ref.common.sv_running.iValue == 1)
            return; // paused

        if(snap.serverTime < oldFrameServerTime) {
            Common.Log("SetCGameTime(): snap.servertime < oldFrameServerTime");
            return;
        }
        oldFrameServerTime = snap.serverTime;

        // cl_timeNudge is a user adjustable cvar that allows more
        // or less latency to be added in the interest of better
        // smoothness or better responsiveness.
        int tn = 0;
        CVar tnvar = Ref.cvars.Find("cl_timenudge");
        if(tnvar != null)
            tn = tnvar.iValue;

        // get our current view of time
        serverTime = Ref.client.realtime + serverTimeDelta - tn;

        // guarantee that time will never flow backwards, even if
        // serverTimeDelta made an adjustment or cl_timeNudge was changed
        if(serverTime < oldServerTime) {
            serverTime = oldServerTime;
        }
        oldServerTime = serverTime;

        // note if we are almost past the latest frame (without timeNudge),
        // so we will try and adjust back a bit when the next snapshot arrives
        if(Ref.client.realtime + serverTimeDelta >= snap.serverTime )
            extrapolatedSnapshot = true;

        // if we have gotten new snapshots, drift serverTimeDelta
        // don't do this every frame, or a period of packet loss would
        // make a huge adjustment
        if(newsnapshots)
            AdjustTimeDelta();
    }

    /*
    =================
    CL_AdjustTimeDelta

    Adjust the clients view of server time.

    We attempt to have cl.serverTime exactly equal the server's view
    of time plus the timeNudge, but with variable latencies over
    the internet it will often need to drift a bit to match conditions.

    Our ideal time would be to have the adjusted time approach, but not pass,
    the very latest snapshot.

    Adjustments are only made when a new snapshot arrives with a rational
    latency, which keeps the adjustment process framerate independent and
    prevents massive overadjustment during times of significant packet loss
    or bursted delayed packets.
    =================
    */
    private void AdjustTimeDelta() {
        newsnapshots = false;

        int newdelta = snap.serverTime - Ref.client.realtime;
        int deltaDelta = Math.abs(newdelta - serverTimeDelta);
//        System.out.println(""+deltaDelta);
        if(deltaDelta > 500) {
            // Mega difference, just forward time to what server says it is
            serverTimeDelta = newdelta;
            oldServerTime = snap.serverTime;
            serverTime = snap.serverTime;
        }
        else if(deltaDelta > 100) {
            // fast adjust, cut the difference in half
            serverTimeDelta = (serverTimeDelta + newdelta) / 2;
        } else {
            // slow drift adjust, only move 1 or 2 msec
            // if any of the frames between this and the previous snapshot
            // had to be extrapolated, nudge our sense of time back a little
            // the granularity of +1 / -2 is too high for timescale modified frametimes
            if(Ref.common.com_timescale.iValue == 0 || Ref.common.com_timescale.iValue == 1) {
                if(extrapolatedSnapshot) {
                    extrapolatedSnapshot = false;
                    serverTimeDelta -= 1;
                   // System.out.println("Back");
                } else if(deltaDelta > Ref.client.cl_netquality.iValue)
                {
                    serverTimeDelta++;
                   // System.out.println("Forward");
                }
            }
        }
    }

    private void FirstSnapshot() {
        // ignore snapshots that don't have entities
        if((snap.snapFlag & CLSnapshot.SF_NOT_ACTIVE) == CLSnapshot.SF_NOT_ACTIVE)
            return;

        Ref.client.state = ConnectState.ACTIVE;

        // set the timedelta so we are exactly on this first frame
        serverTimeDelta = snap.serverTime - Ref.client.realtime;
        oldServerTime = snap.serverTime;
    }

    /*
    ==================
    CL_SystemInfoChanged

    The systeminfo configstring has been changed, so parse
    new information out of it.  This will happen at every
    gamestate, and possibly during gameplay.
    ==================
    */
    void SystemInfoChanged() {
        String systeminfo = GameState.get(CS.CS_SYSTEMINFO);
        // when the serverId changes, any further messages we send to the server will use this new serverId
        // https://zerowing.idsoftware.com/bugzilla/show_bug.cgi?id=475
        // in some cases, outdated cp commands might get sent with this news serverId
        try {
            serverid = Integer.parseInt(Info.ValueForKey(systeminfo, "sv_serverid"));
        } catch(NumberFormatException e) {
            Common.Log("serverid not included in systeminfo - derp.");
            return;
        }

        // scan through all the variables in the systeminfo and locally set cvars to match
        Map<String, String> map = Info.GetPairs(systeminfo);
        
        for (String key:map.keySet()) {
            String val = map.get(key);
            CVar oldvar = Ref.cvars.Find(key);
            if(oldvar == null)
                Ref.cvars.Get(key, val, EnumSet.of(CVarFlags.SERVER_CREATED, CVarFlags.ROM));
            else
                Ref.cvars.Set2(key, val, true);
        }

    }

    void ConfigStringModified(String cmd) {
        String[] tokens = Commands.TokenizeString(cmd, false);
        if(tokens.length < 2)
            Ref.common.Error(Common.ErrorCode.DROP, "ConfigStringModified(): Invalid input: " + cmd);

        int index;
        try {
            index = Integer.parseInt(tokens[1]);
        } catch(NumberFormatException ex) {
            Ref.common.Error(Common.ErrorCode.DROP, "Could not parse CS index: " + cmd);
            return;
        }

        if(index < 0 || index  >= CS.CS_MAX)
            Ref.common.Error(Common.ErrorCode.DROP, "configstring > CS.MAX");

        // get everything after "cs <num>"
        String s = Commands.ArgsFrom(tokens, 2);
        String old = GameState.get(index);
        if(s.equals(old))
            return; // unchanged

        GameState.put(index, s);
        if(index == CS.CS_SYSTEMINFO)
            SystemInfoChanged();
    }
}
