package cubetech.client;

import cubetech.CGame.Snapshot;
import cubetech.common.CS;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.common.Info;
import cubetech.common.PlayerState;
import cubetech.common.items.Weapon;
import cubetech.entities.EntityState;
import cubetech.input.Input;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.net.ConnectState;
import cubetech.net.NetBuffer;
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
    public PlayerInput[][] cmds = new PlayerInput[4][64]; // each mesage will send several old cmds
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

    public ActiveLocalClient[] localClients = new ActiveLocalClient[4];
    
    

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
        for (int i = 0; i < 4; i++) {
            localClients[i] = new ActiveLocalClient();
        }
    }

    
    // Called every frame to builds and sends a command packet to the server.
    public void CreateNewCommands() {
        // don't send any message if not connected
        if(Ref.client.clc.state.ordinal() < ConnectState.PRIMED.ordinal()) return;

        

        boolean catchInput = (Ref.Input.GetKeyCatcher() & (Input.KEYCATCH_CONSOLE | Input.KEYCATCH_MESSAGE | Input.KEYCATCH_UI)) != 0;
        int cmdNum = ++cmdNumber & 63;
        for (int i = 0; i < 4; i++) {
//            if(snap.valid && snap.lcIndex[i] == -1) continue;
            
            if(catchInput && cmds[i][(cmdNumber-1) & 63] != null) {
                // don't update input to server, when client is using it
                cmds[i][cmdNum] = cmds[i][(cmdNumber-1) & 63].Clone(); 
            } else {
                cmds[i][cmdNum] = Ref.Input.getClient(i).CreateCmd();
            }
            cmds[i][cmdNum].serverTime = serverTime;
        }
        
    }


    public void SetCGameTime() {
        if(Ref.client.clc.state != ConnectState.ACTIVE)
        {
            if(Ref.client.clc.state != ConnectState.PRIMED)
                return;

            if(Ref.client.demo.isPlaying()) {
                // we shouldn't get the first snapshot on the same frame
                // as the gamestate, because it causes a bad time skip
                if(!Ref.client.demo.isFirstFrameSkipped()) {
                    Ref.client.demo.setFirstFrameSkipped();
                    return;
                }
                Ref.client.demo.readDemoMessage();
            }

            if(newsnapshots) {
                newsnapshots = false;
                FirstSnapshot();
            }

            if(Ref.client.clc.state != ConnectState.ACTIVE)
                return; // Still not ACTIVE
        }

        // if we have gotten to this point, cl.snap is guaranteed to be valid
        if(!snap.valid)
        {
            Ref.common.Error(Common.ErrorCode.DROP, "SetCGameGtime(): !snap.valid");
        }

        // allow pause in single player
        if(Ref.common.sv_paused.iValue == 1 && Ref.client.CheckPaused() && Ref.common.sv_running.iValue == 1)
            return; // paused

        if(snap.serverTime < oldFrameServerTime) {
            Ref.common.Error(Common.ErrorCode.DROP, "SetCGameTime(): snap.servertime < oldFrameServerTime");
            return;
        }
        oldFrameServerTime = snap.serverTime;

        if(!Ref.client.demo.isPlaying() || true) {

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
            if(Ref.client.realtime + serverTimeDelta >= snap.serverTime - 5)
                extrapolatedSnapshot = true;
        }

        // if we have gotten new snapshots, drift serverTimeDelta
        // don't do this every frame, or a period of packet loss would
        // make a huge adjustment
        if(newsnapshots)
            AdjustTimeDelta();

        if(Ref.client.demo.isPlaying()) {
            // if we are playing a demo back, we can just keep reading
            // messages from the demo file until the cgame definately
            // has valid snapshots to interpolate between

            // a timedemo will always use a deterministic set of time samples
            // no matter what speed machine it is run on,
            // while a normal demo may have different time samples
            // each time it is played back
            ClientConnect clc = Ref.client.clc;
            if(Ref.client.cl_demorecord.isTrue()) {
                int now = Ref.common.Milliseconds();

                if(clc.timeDemoStart == 0) {
                    clc.timeDemoStart = clc.timeDemoLastFrame = now;
                    clc.timeDemoMinDuration = Integer.MAX_VALUE;
                    clc.timeDemoMaxDuration = 0;
                }
                
                int frameDuration = now - clc.timeDemoLastFrame;
                
                // Ignore the first measurement as it'll always be 0
                if(clc.timeDemoFrames > 0) {
                    if(frameDuration > clc.timeDemoMaxDuration)
                        clc.timeDemoMaxDuration = frameDuration;
                    
                    if(frameDuration < clc.timeDemoMinDuration)
                        clc.timeDemoMinDuration = frameDuration;
                }
                
                clc.timeDemoFrames++;

                int frameRate = 1000 / Ref.cvars.Find("record_fps").iValue;
                serverTime = clc.timeDemoBaseTime + clc.timeDemoFrames * frameRate;
            }
            while(serverTime >= snap.serverTime) {
                // feed another messag, which should change
		// the contents of cl.snap
                Ref.client.demo.readDemoMessage();
                if(Ref.client.clc.state != ConnectState.ACTIVE) {
                    return; // end of demo
                }
            }
        }
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

        // the delta never drifts when replaying a demo
        if(Ref.client.demo.isPlaying()) {
            return;
        }

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
//                    System.out.println("Back");
                } else if(deltaDelta > Ref.client.cl_netquality.iValue)
                {
                    serverTimeDelta++;
//                    System.out.println("Forward");
                }
            }
        }
    }

    private void FirstSnapshot() {
        // ignore snapshots that don't have entities
        if((snap.snapFlag & CLSnapshot.SF_NOT_ACTIVE) == CLSnapshot.SF_NOT_ACTIVE)
            return;

        Ref.client.clc.state = ConnectState.ACTIVE;

        // set the timedelta so we are exactly on this first frame
        serverTimeDelta = snap.serverTime - Ref.client.realtime;
        oldServerTime = snap.serverTime;

        Ref.client.clc.timeDemoBaseTime = snap.serverTime;
    }
    
    public boolean GetSnapshot(int snapNumber, Snapshot dest) {
        if(snapNumber > snap.messagenum)
            Ref.common.Error(Common.ErrorCode.DROP, "GetSnapshot: snapNumber > cl.snap.messagenum");

        // if the frame has fallen out of the circular buffer, we can't return it
        if(snap.messagenum - snapNumber >= 32)
            return false;

        // if the frame is not valid, we can't return it
        CLSnapshot clSnap = snapshots[snapNumber & 31];
        if(!clSnap.valid)
            return false;

        // if the entities in the frame have fallen out of their
	// circular buffer, we can't return it
        // TODO: Checkup on limit
        if(parseEntitiesNum - clSnap.parseEntitiesNum >= Common.MAX_GENTITIES)
            return false;

        // write the snapshot
//        if(clSnap.snapFlag != 0) {
//            System.out.println("SnapFlags: " + clSnap.snapFlag);
//        }
        dest.snapFlags = clSnap.snapFlag;
        dest.serverCommandSequence = clSnap.serverCommandSequence;
        dest.ping = clSnap.ping;
        dest.serverTime = clSnap.serverTime;
        dest.numPS = clSnap.numPS;
        for (int i = 0; i < 4; i++) {
            dest.lcIndex[i] = clSnap.lcIndex[i];
            dest.pss[i] = clSnap.pss[i];
        }
        
        dest.numEntities = clSnap.numEntities;
        if(dest.numEntities > Snapshot.MAX_ENTITIES_IN_SNAPSHOT) {
            Common.LogDebug(String.format("GetSnapshot(): truncated %d entities to %d", dest.numEntities, Snapshot.MAX_ENTITIES_IN_SNAPSHOT));
            dest.numEntities = Snapshot.MAX_ENTITIES_IN_SNAPSHOT;
        }
        for (int i= 0; i < dest.numEntities; i++) {
            dest.entities[i] = parseEntities[(clSnap.parseEntitiesNum + i) & Common.ENTITYNUM_NONE];
        }
        
        // FIXME: configstring changes and server commands!!!

        return true;
    }
    
    /*
    ================
    CL_ParseSnapshot

    If the snapshot is parsed properly, it will be copied to
    cl.snap and saved in cl.snapshots[].  If the snapshot is invalid
    for any reason, no changes to the state will be made at all.
    ================
    */
    public void ParseSnapshot(NetBuffer buf) {
        // read in the new snapshot to a temporary buffer
        // we will only copy to cl.snap if it is valid
        CLSnapshot newsnap = new CLSnapshot();
        CLSnapshot oldsnap = null;

        // we will have read any new server commands in this
        // message before we got to svc_snapshot
        newsnap.serverCommandSequence = Ref.client.clc.serverCommandSequence;
        newsnap.serverTime = buf.ReadInt();

        Ref.common.cl_paused.modified = false;
        newsnap.messagenum = Ref.client.clc.serverMessageSequence;
        
        int deltaNum = buf.ReadInt();
        if(deltaNum <= 0) {
            newsnap.deltanum = -1;
        }else {
            newsnap.deltanum = newsnap.messagenum - deltaNum;
        }
        newsnap.snapFlag = buf.ReadInt();

        // If the frame is delta compressed from data that we
        // no longer have available, we must suck up the rest of
        // the frame, but not use it, then ask for a non-compressed
        // message
        if(newsnap.deltanum <= 0) {
            newsnap.valid = true; // uncompressed frame
            Ref.client.demo.recievedFullSnapshot();
        } else {
            oldsnap = snapshots[newsnap.deltanum & 31];
            if(!oldsnap.valid) {
                // Should never happen
                Common.Log("ParseSnapshot: Delta from invalid frame");
            } else if(oldsnap.messagenum != newsnap.deltanum) {
                // The frame that the server did the delta from
                // is too old, so we can't reconstruct it properly.
                Common.LogDebug("Delta frame too old");
            } else if(parseEntitiesNum - oldsnap.parseEntitiesNum > Common.MAX_GENTITIES - 128) {
                Common.LogDebug("ParseSnapshot: Delta parseEntitiesnum too old");
            } else newsnap.valid = true; // valid delta parse
        }

        // read playerinfo
        if((newsnap.snapFlag & CLSnapshot.SF_MULTIPS) != 0) {
            newsnap.numPS = buf.ReadByte();
            for (int i = 0; i < 4; i++) {
                newsnap.lcIndex[i] = buf.ReadByte();
                if(newsnap.lcIndex[i] != -1) newsnap.pss[i] = new PlayerState();
            }
        } else {
            newsnap.numPS = 1;
            newsnap.lcIndex[0] = 0;
            newsnap.pss[0] = new PlayerState();
            for (int i = 1; i < 4; i++) {
                newsnap.lcIndex[i] = -1;
            }
        }
        
        try {
            for (int i = 0; i < 4; i++) {
                if(newsnap.lcIndex[i] == -1) continue;
                if(oldsnap != null && oldsnap.lcIndex[i] != -1) {
                    newsnap.pss[newsnap.lcIndex[i]].ReadDelta(buf, oldsnap.pss[oldsnap.lcIndex[i]]);
                } else {
                    newsnap.pss[newsnap.lcIndex[i]].ReadDelta(buf, null);
                }
            }
        } catch (NullPointerException ex) {
            int test = 2;
        }

        // read packet entities
        ParsePacketEntities(buf, oldsnap, newsnap);

         // if not valid, dump the entire thing now that it has
         // been properly read
        if(!newsnap.valid) return;

        // clear the valid flags of any snapshots between the last
        // received and this one, so if there was a dropped packet
        // it won't look like something valid to delta from next
        // time we wrap around in the buffer
        int oldMessageNum = snap.messagenum + 1;
        if(newsnap.messagenum - oldMessageNum >= 32) {
            oldMessageNum = newsnap.messagenum-31;
        }

        for (; oldMessageNum < newsnap.messagenum; oldMessageNum++) {
            snapshots[oldMessageNum&31].valid = false;
        }

        // copy to the current good spot
        snap = newsnap;
        snap.ping = 999;
        if(Ref.client.clc.netchan != null) {
            for (int i= 0; i < 32; i++) {
                int packNum = (Ref.client.clc.netchan.outgoingSequence - 1 - i) & 31;
                if(snap.pss[0].commandTime >= outPackets[packNum].servertime) {
                    snap.ping = Ref.client.realtime - outPackets[packNum].realtime;
                    break;
                }
            }
        }

        // save the frame off in the backup array for later delta comparisons
        snapshots[snap.messagenum & 31] = snap;
        newsnapshots = true;

    }

    private void ParsePacketEntities(NetBuffer buf, CLSnapshot oldsnap, CLSnapshot newsnap) {
        newsnap.parseEntitiesNum = parseEntitiesNum;
        newsnap.numEntities = 0;

        // delta from the entities present in oldframe
        int oldindex = 0, oldnum = 0;
        EntityState oldstate = null;
        if(oldsnap == null)
            oldnum = 99999;
        else {
            if(oldindex >= oldsnap.numEntities)
                oldnum = 99999;
            else
            {
                oldstate = parseEntities[(oldsnap.parseEntitiesNum + oldindex) & Common.MAX_GENTITIES-1];
                oldnum = oldstate.number;
            }
        }

        while(true) {
            // read the entity index number
            int newnum = buf.ReadShort();

            if(newnum >= Common.MAX_GENTITIES-1)
                break;

            while(oldnum < newnum) {
                // one or more entities from the old packet are unchanged
                DeltaEntity(buf, newsnap, oldnum, oldstate, true);
                oldindex++;

                if(oldindex >= oldsnap.numEntities)
                    oldnum = 99999;
                else {
                    oldstate = parseEntities[(oldsnap.parseEntitiesNum + oldindex) & Common.MAX_GENTITIES-1];
                    oldnum = oldstate.number;
                }
            }

            if(oldnum == newnum) {
                // delta from previous state
                DeltaEntity(buf, newsnap, newnum, oldstate, false);

                oldindex++;

                if(oldindex >= oldsnap.numEntities)
                    oldnum = 99999;
                else {
                    oldstate = parseEntities[(oldsnap.parseEntitiesNum + oldindex) & Common.MAX_GENTITIES-1];
                    oldnum = oldstate.number;
                }
                continue;
            }

            if(oldnum > newnum) {
                // delta from baseline
                DeltaEntity(buf, newsnap, newnum, entityBaselines[newnum], false);
                continue;
            }
        }

        // any remaining entities in the old frame are copied over
        while(oldnum != 99999) {
            // one or more entities from the old packet are unchanged
            DeltaEntity(buf, newsnap, oldnum, oldstate, true);

            oldindex++;

            if(oldindex >= oldsnap.numEntities)
                oldnum = 99999;
            else
            {
                oldstate = parseEntities[(oldsnap.parseEntitiesNum + oldindex) & Common.MAX_GENTITIES-1];
                oldnum = oldstate.number;
            }
        }
    }

    private void DeltaEntity(NetBuffer msg, CLSnapshot frame, int newnum, EntityState old, boolean unchanged) {
        // save the parsed entity state into the big circular buffer so
        // it can be used as the source for a later delta
        EntityState ent = parseEntities[parseEntitiesNum & Common.MAX_GENTITIES-1];
        if(unchanged)
            old.Clone(ent);
        else
            ent.ReadDeltaEntity(msg, old, newnum);

        parseEntities[parseEntitiesNum & Common.MAX_GENTITIES-1] = ent;
        if(ent.number == Common.MAX_GENTITIES-1)
            return; // entity was removed

        parseEntitiesNum++;
        frame.numEntities++;
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

        if(Ref.client.demo.isPlaying()) return;

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
