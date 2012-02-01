/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;


import cubetech.common.Score;
import cubetech.client.CLSnapshot;
import cubetech.client.LocalClient;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.common.Move;
import cubetech.common.MoveQuery;
import cubetech.common.PlayerState;
import cubetech.common.ScoreList;
import cubetech.common.items.Weapon;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.input.PlayerInput;
import cubetech.iqm.IQMModel;
import cubetech.misc.Ref;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CGameState {
    public int clientframe;
    
    public boolean loading;

    public int latestSnapshotNum;	// the number of snapshots the client system has received
    public int latestSnapshotTime;	// the time from latestSnapshotNum, so we don't need to read the snapshot yet

    public Snapshot	snap;				// cg.snap->serverTime <= cg.time
    public Snapshot	nextSnap;			// cg.nextSnap->serverTime > cg.time, or NULL
    public Snapshot[]	activeSnapshots = new Snapshot[2]; // 2
    
    public float frameInterpolation;	// (float)( cg.time - cg.frame->serverTime ) / (cg.nextFrame->serverTime - cg.frame->serverTime)
    
    public int	frametime;		// cg.time - cg.oldTime
    public int	time;			// this is the time value that the client
                                                // is rendering at.
    public int	oldTime;		// time at last frame, used for missile trails and prediction checking
    //public int	physicsTime;	// either cg.snap->time or cg.nextSnap->time
    public boolean	mapRestart;			// set on a map restart to set back the weapon

    
    public ViewParams refdef;
    public String infoScreenText = "";

    // Scoreboard
    public ScoreList scores = new ScoreList();
    public boolean showScores = false; // true if scoreboard should we showing
    public int scoresRequestTime = 0;

    // development tool
    public RenderEntity testModelEntity = new RenderEntity(REType.MODEL); 
    public String testModelName;
    public boolean testGun = false;
    CVar cg_gun_x, cg_gun_y, cg_gun_z;
    
    // Demo stuff
    public boolean playingdemo;
    public PlayerState demoPlayerState = null;
    public Vector2f mouseVelocity = new Vector2f(0, 0);
    public float[] demoangles = new float[3];
    public float demofov = 90f;
    public float demofovVel = 0f;

    // client specific stuff
    public int cur_localClientNum;
    public LocalClient cur_lc;
    public PlayerState cur_ps;
    public LocalClient[] localClients = new LocalClient[4];
    public int nViewports = 0;
    public int cur_viewport = 0;
    

    public CGameState(int clientNum) {
        for (int i = 0; i < 4; i++) {
            localClients[i] = new LocalClient(clientNum, i);
        }
        cur_lc = localClients[0];
        activeSnapshots[0] = new Snapshot();
        activeSnapshots[1] = new Snapshot();
        cg_gun_x = Ref.cvars.Get("cg_gun_x", "0", EnumSet.of(CVarFlags.NONE));
        cg_gun_y = Ref.cvars.Get("cg_gun_y", "0", EnumSet.of(CVarFlags.NONE));
        cg_gun_z = Ref.cvars.Get("cg_gun_z", "0", EnumSet.of(CVarFlags.NONE));
    }

    public ICommand cg_testmodel_f = new ICommand() {
        public void RunCommand(String[] args) {
            // Clear testModelEnt
            testModelEntity = new RenderEntity(REType.MODEL);
            if(args.length < 2) {
                return;
            }

            testModelName = args[1];
            

            if(args.length == 3) {
                testModelEntity.backlerp = Float.parseFloat(args[2]);
                testModelEntity.frame = 1;
                testModelEntity.oldframe = 0;
            }
            
            IQMModel model = Ref.ResMan.loadModel(testModelName);
            

            if(model == null) {
                Ref.cgame.Print("Can't register model");
                return;
            }
            
            testModelEntity.model = model.buildFrame(0, 0, 0, null);

            Helper.VectorMA(refdef.Origin, 100, refdef.ViewAxis[0], testModelEntity.origin);
            Vector3f angles = new Vector3f(0, 180 + refdef.Angles.y, 0);
            testModelEntity.axis = Helper.AnglesToAxis(angles);
            testGun = false;
        }
    };

    public ICommand cg_testgun_f = new ICommand() {
        public void RunCommand(String[] args) {
            cg_testmodel_f.RunCommand(args);
            testGun = true;
//            testModelEntity.renderFlags |= RefFlag.FIRST_PERSON;
        }
    };

    public ICommand cg_testmodelNextFrame_f = new ICommand() {
        public void RunCommand(String[] args) {
            testModelEntity.frame++;
            Ref.cgame.Print(String.format("frame %d", testModelEntity.frame));
        }
    };

    public ICommand cg_testmodelPrevFrame_f = new ICommand() {
        public void RunCommand(String[] args) {
            testModelEntity.frame--;
            if(testModelEntity.frame < 0) testModelEntity.frame = 0;
            Ref.cgame.Print(String.format("frame %d", testModelEntity.frame));
        }
    };

    public void addTestModel() {
        if(testModelEntity == null || testModelEntity.model == null) return;

        // if testing a gun, set the origin reletive to the view origin
        if(testGun) {
            System.arraycopy(refdef.ViewAxis, 0, testModelEntity.axis, 0, 3);
            testModelEntity.origin.set(refdef.Origin);

            // allow the position to be adjusted
            Helper.VectorMA(testModelEntity.origin, cg_gun_x.fValue, refdef.ViewAxis[0], testModelEntity.origin);
            Helper.VectorMA(testModelEntity.origin, cg_gun_y.fValue, refdef.ViewAxis[1], testModelEntity.origin);
            Helper.VectorMA(testModelEntity.origin, cg_gun_z.fValue, refdef.ViewAxis[2], testModelEntity.origin);
        }

        Ref.render.addRefEntity(testModelEntity);
    }

    public static ICommand cg_SwitchWeapon_f = new ICommand() {
        public void RunCommand(String[] args) {
            if(Ref.cgame == null || Ref.cgame.cg == null) return;
            CGameState cg = Ref.cgame.cg;
            if(cg.snap == null) {
                Common.Log("cmd weapon: can't switch, no current snap");
                return;
            }
            if(args.length < 3) {
                Common.Log("usage: weapon <clientindex> <weaponindex, next or prev>");
                return;
            }
            int controllerIndex = -1;
            try {
                controllerIndex = Integer.parseInt(args[1]);
            } catch(NumberFormatException ex) {
                Common.Log("cmd weapon: Invalid integer %s", args[1]);
                return;
            }
            
            if(controllerIndex < 0 || controllerIndex >= 4 || cg.snap.lcIndex[controllerIndex] == -1) {
                Common.Log("Invalid client index %d", controllerIndex);
                return;
            }
            
            int clientIndex = 0;
            if(controllerIndex == 0) {
                clientIndex = Ref.Input.getKeyboardClient();
            } else {
                clientIndex = Ref.Input.getJoystickMapping(controllerIndex);
            }
            
            LocalClient lc = cg.localClients[clientIndex];
            PlayerState ps = lc.snapPS;
            
            Weapon current = lc.weaponSelect;
            Weapon newweap = null;
            if("next".equals(args[2])) {
                newweap = ps.stats.getWeaponNearest(current, true);
            } else if("prev".equals(args[2])) {
                newweap = ps.stats.getWeaponNearest(current, false);
            } else {
                try {
                    int num = Integer.parseInt(args[2]);
                    if(num <= 0 || num >= Weapon.values().length) return;
                    newweap = Weapon.values()[num];
                } catch(NumberFormatException ex) {
                    Common.Log("Invalid weapon index integer %s", args[2]);
                    return;
                }
            }
            
            if(newweap == null || newweap == Weapon.NONE || !ps.stats.hasWeapon(newweap)) return;

            lc.weaponSelect = newweap;
        }
    };

    /*
    ============
    CG_ProcessSnapshots

    We are trying to set up a renderable view, so determine
    what the simulated time is, and try to get snapshots
    both before and after that time if available.

    If we don't have a valid cg.snap after exiting this function,
    then a 3D game view cannot be rendered.  This should only happen
    right after the initial connection.  After cg.snap has been valid
    once, it will never turn invalid.

    Even if cg.snap is valid, cg.nextSnap may not be, if the snapshot
    hasn't arrived yet (it becomes an extrapolating situation instead
    of an interpolating one)

    ============
    */
    void ProcessSnapshots() {
        // see what the latest snapshot the client system has is
        SimpleEntry<Integer, Integer> entry = Ref.client.GetCurrentSnapshotNumber();
        
        latestSnapshotTime = entry.getValue();
        
        int n = entry.getKey();
        if(n != latestSnapshotNum) {
            if(n < latestSnapshotNum) {
                // Should never happen
                Ref.common.Error(ErrorCode.DROP, "ProcessSnapshots: n < latestSnapshotNum");
            }
            latestSnapshotNum = n;
        }

        // If we have yet to receive a snapshot, check for it.
	// Once we have gotten the first snapshot, cg.snap will
	// always have valid data for the rest of the game
        while(snap == null) {
            Snapshot newsnap = ReadNextSnapshot();
            if(newsnap == null)
                return; // We can't continue at this point

            // set our weapon selection to what
            // the playerstate is currently using
            if((newsnap.snapFlags & CLSnapshot.SF_NOT_ACTIVE) == 0)
                SetInitialSnapshot(newsnap);
        }

        // loop until we either have a valid nextSnap with a serverTime
	// greater than cg.time to interpolate towards, or we run
	// out of available snapshots
        do {
            // if we don't have a nextframe, try and read a new one in
            if(nextSnap == null) {
                Snapshot newsnap = ReadNextSnapshot();

                // if we still don't have a nextframe, we will just have to
		// extrapolate
                if(newsnap == null)
                    break;

                SetNextSnapshot(newsnap);

                // if time went backwards, we have a level restart
                if(nextSnap.serverTime < snap.serverTime) {
                    Ref.common.Error(ErrorCode.DROP, "ProcessSnapshots: Server time went backwards");
                }
            }

            // if our time is < nextFrame's, we have a nice interpolating state
            if(time >= snap.serverTime && time < nextSnap.serverTime)
                break;

            // we have passed the transition from nextFrame to frame
            TransitionSnapshot();
        } while(true);

        // assert our valid conditions upon exiting
        if(snap == null)
            Ref.common.Error(ErrorCode.DROP, "ProcessSnapshots: snap == null");
        if(time < snap.serverTime)
            time = snap.serverTime; // this can happen right after a vid_restart
        if(nextSnap != null && nextSnap.serverTime <= time)
            Ref.common.Error(ErrorCode.DROP, "ProcessSnapshots: nextSnap.serverTime <= time");
    }

    

    

    /*
    ========================
    CG_ReadNextSnapshot

    This is the only place new snapshots are requested
    This may increment cgs.processedSnapshotNum multiple
    times if the client system fails to return a
    valid snapshot.
    ========================
    */
    private Snapshot ReadNextSnapshot() {
        CGameStatic cgs = Ref.cgame.cgs;
        if(latestSnapshotNum > cgs.processedSnapshotNum + 1000) {
            Ref.cgame.Print("WARNING: ReadNextSnapshot() way out of range, " + latestSnapshotNum + " > " + cgs.processedSnapshotNum);
        }

        Snapshot dest = activeSnapshots[0];
        while(cgs.processedSnapshotNum < latestSnapshotNum) {
            // decide which of the two slots to load it into
            if(snap == activeSnapshots[0])
                dest = activeSnapshots[1]; // current snapshot is in slot 0, so use slot 1

            // try to read the snapshot from the client system
            cgs.processedSnapshotNum++;
            boolean r = Ref.client.cl.GetSnapshot(cgs.processedSnapshotNum, dest);

            // FIXME: why would trap_GetSnapshot return a snapshot with the same server time
            if(snap != null && r && dest.serverTime == snap.serverTime) {
                Common.LogDebug("Derp?");
            }

            // if it succeeded, return
            if(r) {
                Ref.cgame.lag.AddSnapshotInfo(dest);
                return dest;
            }

            // a GetSnapshot will return failure if the snapshot
            // never arrived, or  is so old that its entities
            // have been shoved off the end of the circular
            // buffer in the client system.

            // record as a dropped packet
            Ref.cgame.lag.AddSnapshotInfo(null);

            // If there are additional snapshots, continue trying to
            // read them.
        }
        return null;
    }

    /*
    ==================
    CG_SetInitialSnapshot

    This will only happen on the very first snapshot, or
    on tourney restarts.  All other times will use
    CG_TransitionSnapshot instead.

    FIXME: Also called by map_restart?
    ==================
    */
    private void SetInitialSnapshot(Snapshot newsnap) {
        snap = newsnap;
        
        for (int i = 0; i < 4; i++) {
            if(snap.lcIndex[i] != -1) {
                localClients[i].snapPS = snap.pss[snap.lcIndex[i]];
                localClients[i].clientNum = localClients[i].snapPS.clientNum;
            }
        }
        
        for (int i = 0; i < snap.numPS; i++) {
            snap.pss[i].ToEntityState(Ref.cgame.cg_entities[snap.pss[i].clientNum].currentState, false);
        }
        
        // sort out solid entities
        Ref.cgame.BuildSolidList();

        Ref.cgame.ExecuteNewServerCommands(snap.serverCommandSequence);

        Respawn(-1);

        for (int i = 0; i < snap.numEntities; i++) {
            EntityState state = snap.entities[i];
            CEntity cent = Ref.cgame.cg_entities[state.number];
            state.Clone(cent.currentState);
            cent.interpolate = false;
            cent.currentValid = true;

            cent.ResetEntity();
            cent.CheckEvents();
        }

    }

    // A respawn happened this snapshot
    private void Respawn(int clientnum) {
        // no error decay on player movement
        for (int i = 0; i < 4; i++) {
            if(clientnum == -1 || 
                    (snap.lcIndex[i] != -1 && snap.pss[snap.lcIndex[i]].clientNum != clientnum)) {
                continue;
            }
            localClients[i].thisFrameTeleport = false;
            localClients[i].weaponSelect = snap.pss[snap.lcIndex[i]].weapon;
        }
    }

    /*
    ===================
    CG_SetNextSnap

    A new snapshot has just been read in from the client system.
    ===================
    */
    private void SetNextSnapshot(Snapshot newsnap) {
        nextSnap = newsnap;
        
        for (int i = 0; i < snap.numPS; i++) {
            snap.pss[i].ToEntityState(Ref.cgame.cg_entities[snap.pss[i].clientNum].nextState, false);
            Ref.cgame.cg_entities[snap.pss[i].clientNum].interpolate = true;
        }

        // check for extrapolation errors
        for (int i= 0; i < newsnap.numEntities; i++) {
            EntityState es = newsnap.entities[i];
            CEntity cent = Ref.cgame.cg_entities[es.number];
            es.Clone(cent.nextState);

            // if this frame is a teleport, or the entity wasn't in the
            // previous frame, don't interpolate
            if(!cent.currentValid || ((cent.currentState.eFlags ^ es.eFlags) & EntityFlags.TELEPORT_BIT) > 0)
                cent.interpolate = false;
            else
                cent.interpolate = true;
        }
        
        // Check for "important" centities we stopped recieving
        Iterator<CEntity> it = Ref.cgame.centitiesWithPhysics.iterator();
        while(it.hasNext()) {
            CEntity physcent = it.next();
            if(!physcent.currentValid) {
                continue;
            }
            int j;
            for (j= 0; j < newsnap.numEntities; j++) {
                EntityState es = newsnap.entities[j];
                if(es.number == physcent.currentState.number) break;
            }
            if(j == newsnap.numEntities || !physcent.interpolate) {
                Ref.cgame.cleanPhysicsFromCEntity(physcent);
                it.remove();
            }
        }

        // if the next frame is a teleport for the playerstate, we
	// can't interpolate during demos
        //nextFrameTeleport = false;
        for (int i = 0; i < snap.numPS; i++) {
            if(newsnap.pss[i] == null) continue;
            LocalClient lc = getLocalClient(newsnap.pss[i].clientNum);
            if(lc == null) continue;
            lc.nextFrameTeleport = false;
            if(((newsnap.pss[i].eFlags ^ snap.pss[i].eFlags) & EntityFlags.TELEPORT_BIT) != 0) {
                lc.nextFrameTeleport = true;
            }
            
            // if changing follow mode, don't interpolate
            if(nextSnap.pss[i].clientNum != snap.pss[i].clientNum) lc.nextFrameTeleport = true;
            // if changing server restarts, don't interpolate
            if(((nextSnap.snapFlags ^ snap.snapFlags) & CLSnapshot.SF_SERVERCOUNT) != 0) lc.nextFrameTeleport = true;
        }

        Ref.cgame.BuildSolidList();
    }
    
    // input is client entity number
    // output is localClient index
    public int getLocalClientIndex(int clientNum) {
        for (int i = 0; i < 4; i++) {
            if(snap.lcIndex[i] != -1 && snap.pss[snap.lcIndex[i]].clientNum == clientNum) return i;
        }
        return -1;
    }
    
    // input is client entity number
    // output is localClient index
    public LocalClient getLocalClient(int clientNum) {
        for (int i = 0; i < 4; i++) {
            if(snap.lcIndex[i] != -1 && snap.pss[snap.lcIndex[i]].clientNum == clientNum) return localClients[i];
        }
        return null;
    }

    private void TransitionSnapshot() {
        if(snap == null)
            Ref.common.Error(ErrorCode.DROP, "TransitionSnapshot: snap == null");
        if(nextSnap == null)
            Ref.common.Error(ErrorCode.DROP, "TransitionSnapshot: nextSnap == null");

        // execute any server string commands before transitioning entities
        Ref.cgame.ExecuteNewServerCommands(nextSnap.serverCommandSequence);

        // clear the currentValid flag for all entities in the existing snapshot
        for (int i= 0; i < snap.numEntities; i++) {
            Ref.cgame.cg_entities[snap.entities[i].number].currentValid = false;
        }

        // move nextSnap to snap and do the transitions
        Snapshot oldframe = snap;
        snap = nextSnap;
        for (int i = 0; i < snap.numPS; i++) {
            snap.pss[i].ToEntityState(Ref.cgame.cg_entities[snap.pss[i].clientNum].currentState, false);
            Ref.cgame.cg_entities[snap.pss[i].clientNum].interpolate = false;
            localClients[snap.lcIndex[i]].snapPS = snap.pss[i];
            localClients[snap.lcIndex[i]].clientNum = snap.pss[i].clientNum;
        }
        
        for (int i= 0; i < snap.numEntities; i++) {
            CEntity cent = Ref.cgame.cg_entities[snap.entities[i].number];
            cent.TransitionEntity();
            
            // remember time of snapshot this entity was last updated in
            cent.snapshotTime = snap.serverTime;
        }

        nextSnap = null;

        // check for playerstate transition events
        if(oldframe != null) {
            PlayerState ops, ps;
            for (int i = 0; i < 4; i++) {
                if(oldframe.lcIndex[i] == -1 || snap.lcIndex[i] == -1) continue;
                
                cur_localClientNum = i;
                cur_lc = localClients[i];
                
                
                ops = oldframe.pss[oldframe.lcIndex[i]];
                ps = snap.pss[snap.lcIndex[i]];
                // Let LocalClient know about the current "base" ps
                cur_lc.snapPS = ps;
                cur_lc.clientNum = cur_lc.snapPS.clientNum;
                // teleporting checks are irrespective of prediction
                if(((ps.eFlags ^ ops.eFlags) & EntityFlags.TELEPORT_BIT) > 0)
                    cur_lc.thisFrameTeleport = true; // will be cleared by prediction code

                // if we are not doing client side movement prediction for any
                // reason, then the client events and view changes will be issued now
                if(Ref.cgame.cg_nopredict.isTrue() || playingdemo || ps.vehicle > 0) {
                    cur_lc.TransitionPlayerState(ps, ops);
                }
            }
            
        }
    }
}
