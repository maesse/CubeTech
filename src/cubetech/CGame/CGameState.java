/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;


import cubetech.common.Score;
import cubetech.client.CLSnapshot;
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
import cubetech.misc.Ref;
import java.util.AbstractMap.SimpleEntry;
import java.util.EnumSet;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CGameState {
    
    public int clientframe;
    public int clientNum;
    public boolean loading;

    public int latestSnapshotNum;	// the number of snapshots the client system has received
    public int latestSnapshotTime;	// the time from latestSnapshotNum, so we don't need to read the snapshot yet

    public Snapshot	snap;				// cg.snap->serverTime <= cg.time
    public Snapshot	nextSnap;			// cg.nextSnap->serverTime > cg.time, or NULL
    public Snapshot[]	activeSnapshots = new Snapshot[2]; // 2
    public float frameInterpolation;	// (float)( cg.time - cg.frame->serverTime ) / (cg.nextFrame->serverTime - cg.frame->serverTime)
    public boolean thisFrameTeleport;
    public boolean nextFrameTeleport;
    public int	frametime;		// cg.time - cg.oldTime
    public int	time;			// this is the time value that the client
                                                // is rendering at.
    public int	oldTime;		// time at last frame, used for missile trails and prediction checking
    public int	physicsTime;	// either cg.snap->time or cg.nextSnap->time
    public boolean	mapRestart;			// set on a map restart to set back the weapon

    public PlayerState	predictedPlayerState = new PlayerState();
    public CEntity predictedPlayerEntity = new CEntity();
    public boolean validPPS;				// clear until the first call to CG_PredictPlayerState
    public int predictedErrorTime;
    public Vector3f predictedError = new Vector3f();

    public int			eventSequence;
    public int[] predictableEvents = new int[16]; // 16
    public ViewParams refdef;
    public String infoScreenText = "";

    public ScoreList scores = new ScoreList();
    

    public boolean showScores = false; // true if scoreboard should we showing
    public int scoresRequestTime = 0;

    public float autoAngle;

    public Weapon weaponSelect = Weapon.NONE;
    public float zoomSensitivity = 1.0f;

    // development tool
    public RenderEntity testModelEntity = new RenderEntity(REType.MODEL); 
    public String testModelName;
    public boolean testGun = false;
    CVar cg_gun_x, cg_gun_y, cg_gun_z;
    public boolean playingdemo;
    public PlayerState demoPlayerState = null;
    public Vector2f mouseVelocity = new Vector2f(0, 0);
    public float[] demoangles = new float[3];
    public float demofov = 90f;
    public float demofovVel = 0f;

    public int bobcycle;
    public float bobfracsin;
    public float xyspeed;

    public CGameState(int clientnum) {
        this.clientNum = clientnum;
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
            
            testModelEntity.model = Ref.ResMan.loadModel(testModelName).buildFrame(0, 0, 0, null);

            if(testModelEntity.model == null) {
                Ref.cgame.Print("Can't register model");
                return;
            }

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
            if(cg.snap == null || args.length < 2) return;
            
            Weapon current = cg.weaponSelect;
            Weapon newweap = null;
            if("next".equals(args[1])) {
                newweap = cg.snap.ps.stats.getWeaponNearest(current, true);
            } else if("prev".equals(args[1])) {
                newweap = cg.snap.ps.stats.getWeaponNearest(current, false);
            } else {
                try {
                    int num = Integer.parseInt(args[1]);
                    if(num <= 0 || num >= Weapon.values().length) return;
                    newweap = Weapon.values()[num];
                } catch(NumberFormatException ex) {
                    return;
                }
            }
            
            if(newweap == null || newweap == Weapon.NONE || !cg.snap.ps.stats.hasWeapon(newweap)) return;

            cg.weaponSelect = newweap;
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

    private void InterpolatePlayerState(boolean grabAngles) {

        Snapshot prev = snap;
        Snapshot next = nextSnap;
        predictedPlayerState = snap.ps.Clone(predictedPlayerState);
        PlayerState out = predictedPlayerState;

        // if the next frame is a teleport, we can't lerp to it
        if(nextFrameTeleport) return;

        if(next == null || next.serverTime <= prev.serverTime) {
            return;
        }

        float f = ((float)time - prev.serverTime) / ((float)next.serverTime - prev.serverTime);

        int bob = next.ps.bobcycle;
        if(bob < prev.ps.bobcycle) {
            bob += 256;
        }
        out.bobcycle = (int) (prev.ps.bobcycle + f * (bob - prev.ps.bobcycle));
        
        Vector3f delta = Vector3f.sub(next.ps.origin, prev.ps.origin, null);
        Helper.VectorMA(prev.ps.origin, f, delta, out.origin);
        if(!grabAngles) {
            out.viewangles = Helper.LerpAngles(prev.ps.viewangles, next.ps.viewangles, null, f);
        } else {
             PlayerInput cmd = Ref.client.GetUserCommand(Ref.client.cl.cmdNumber);
             out.UpdateViewAngle(cmd);
        }
        Vector3f.sub(next.ps.velocity, prev.ps.velocity, delta);
        Helper.VectorMA(prev.ps.velocity, f, delta, out.velocity);

    }

    void PredictPlayerState() {
        // if this is the first frame we must guarantee
        // predictedPlayerState is valid even if there is some
        // other error condition
        if(!validPPS) {
            validPPS = true;
            predictedPlayerState = snap.ps.Clone(null);
        }

        if(playingdemo) {
            InterpolatePlayerState(false);
            return;
        }

        // non-predicting local movement will grab the latest angles
        if(Ref.cgame.cg_nopredict.iValue == 1 || predictedPlayerState.vehicle > 0) {
            InterpolatePlayerState(true);
            predictedError.set(0, 0, 0);
            return;
        }

        // prepare for pmove
        MoveQuery move = new MoveQuery(Ref.cgame);
        move.tracemask =  Content.MASK_PLAYERSOLID;

        // save the state before the pmove so we can detect transitions
        PlayerState oldPlayerState = predictedPlayerState;

        int currentCmd = Ref.client.cl.cmdNumber;

        // if we don't have the commands right after the snapshot, we
        // can't accurately predict a current position, so just freeze at
        // the last good position we had
        int cmdNum = currentCmd - 63;
        if(cmdNum <= 0)
            cmdNum = 1;

        PlayerInput oldest = Ref.client.GetUserCommand(cmdNum);
        if(oldest == null)
            return;
        if(oldest.serverTime > snap.ps.commandTime && oldest.serverTime < time) {
            Ref.cgame.Print("Exceeded packet_backup on commands");
            return;
        }

        // get the latest command so we can know which commands are from previous map_restarts
        PlayerInput latest = Ref.client.GetUserCommand(currentCmd);

        // get the most recent information we have, even if
        // the server time is beyond our current cg.time,
        // because predicted player positions are going to
        // be ahead of everything else anyway
        if(nextSnap != null && !nextFrameTeleport && !thisFrameTeleport) {
            predictedPlayerState = nextSnap.ps.Clone(null);
            physicsTime = nextSnap.serverTime;
        } else {
            predictedPlayerState = snap.ps.Clone(null);
            physicsTime = snap.serverTime;
        }

        move.ps = predictedPlayerState;
        int nMove = 0;
        // run cmds
        boolean moved = false;
        for (int cmdnum= currentCmd - 63; cmdnum <= currentCmd; cmdnum++) {
            if(cmdnum <= 0)
                continue; // FIX ??

            move.cmd = Ref.client.GetUserCommand(cmdnum);

            // don't do anything if the time is before the snapshot player time
            if(move.cmd.serverTime <= predictedPlayerState.commandTime)
                continue;

            // don't do anything if the command was from a previous map_restart
            if(move.cmd.serverTime > latest.serverTime)
                continue;

            // check for a prediction error from last frame
            // on a lan, this will often be the exact value
            // from the snapshot, but on a wan we will have
            // to predict several commands to get to the point
            // we want to compare
            if(predictedPlayerState.commandTime == oldPlayerState.commandTime) {
                if(thisFrameTeleport) {
                    // a teleport will not cause an error decay
                    predictedError = new Vector3f();
                    thisFrameTeleport = false;
                } else {
                    // TODO: AdjustPositionForMover()
//                    if(oldPlayerState.origin.x != predictedPlayerState.origin.x
//                            || oldPlayerState.origin.y != predictedPlayerState.origin.y)
//                        Ref.cgame.Print("Prediction error");
                    Vector3f delta = new Vector3f();
                    Vector3f.sub(oldPlayerState.origin, predictedPlayerState.origin, delta);
                    float len = delta.length();
                    if(len > 0.1f) {
                        if(Ref.cgame.cg_showmiss.isTrue()) {
                            Ref.cgame.Print("Prediction miss: " + len);
                        }
                        if(Ref.cgame.cg_errorDecay.iValue > 0) {
                            int t = time - predictedErrorTime;
                            float f = (Ref.cgame.cg_errorDecay.fValue - t) / Ref.cgame.cg_errorDecay.fValue;
                            if(f < 0)
                                f = 0;
                            predictedError.scale(f);
                            if(Float.isInfinite(predictedError.x) || Float.isNaN(predictedError.x))
                                predictedError.x = 0f;
                            if(Float.isInfinite(predictedError.y) || Float.isNaN(predictedError.y))
                                predictedError.y = 0f;
                            if(Float.isInfinite(predictedError.z) || Float.isNaN(predictedError.z))
                                predictedError.z = 0f;
                        } else predictedError = new Vector3f();
                            
                        Vector3f.add(predictedError, delta, predictedError);
                        predictedErrorTime = oldTime;
                    }
                }
            }

            Move.Move(move);
            moved = true;
            nMove++;
        }



        if(!moved)
            return;

//        System.out.println(""+nMove);

        // TODO: AdjustPositionForMover

        // fire events and other transition triggered things
        TransitionPlayerState(predictedPlayerState, oldPlayerState);
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
        snap.ps.ToEntityState(Ref.cgame.cg_entities[snap.ps.clientNum].currentState, false);
        
        // sort out solid entities
        Ref.cgame.BuildSolidList();

        Ref.cgame.ExecuteNewServerCommands(snap.serverCommandSequence);

        Respawn();

        for (int i = 0; i < snap.numEntities; i++) {
            EntityState state = snap.entities[i];
            CEntity cent = Ref.cgame.cg_entities[state.ClientNum];
            state.Clone(cent.currentState);
            cent.interpolate = false;
            cent.currentValid = true;

            cent.ResetEntity();
            cent.CheckEvents();
        }

    }

    // A respawn happened this snapshot
    private void Respawn() {
        // no error decay on player movement
        thisFrameTeleport = false;
    }

    /*
    ===================
    CG_SetNextSnap

    A new snapshot has just been read in from the client system.
    ===================
    */
    private void SetNextSnapshot(Snapshot newsnap) {
        nextSnap = newsnap;
        newsnap.ps.ToEntityState(Ref.cgame.cg_entities[newsnap.ps.clientNum].nextState, false);
        Ref.cgame.cg_entities[snap.ps.clientNum].interpolate = true;

        // check for extrapolation errors
        for (int i= 0; i < newsnap.numEntities; i++) {
            EntityState es = newsnap.entities[i];
            CEntity cent = Ref.cgame.cg_entities[es.ClientNum];
            es.Clone(cent.nextState);

            // if this frame is a teleport, or the entity wasn't in the
            // previous frame, don't interpolate
            if(!cent.currentValid || ((cent.currentState.eFlags ^ es.eFlags) & EntityFlags.TELEPORT_BIT) > 0)
                cent.interpolate = false;
            else
                cent.interpolate = true;
        }

        // if the next frame is a teleport for the playerstate, we
	// can't interpolate during demos
        if(snap != null && ((newsnap.ps.eFlags ^ snap.ps.eFlags) & EntityFlags.TELEPORT_BIT) > 0)
            nextFrameTeleport = true;
        else
            nextFrameTeleport = false;

        // if changing follow mode, don't interpolate
        if(nextSnap.ps.clientNum != snap.ps.clientNum)
            nextFrameTeleport = true;

        // if changing server restarts, don't interpolate
        if(((nextSnap.snapFlags ^ snap.snapFlags) & CLSnapshot.SF_SERVERCOUNT) >0)
            nextFrameTeleport = true;

        Ref.cgame.BuildSolidList();
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
            Ref.cgame.cg_entities[snap.entities[i].ClientNum].currentValid = false;
        }

        // move nextSnap to snap and do the transitions
        Snapshot oldframe = snap;
        snap = nextSnap;
        snap.ps.ToEntityState(Ref.cgame.cg_entities[snap.ps.clientNum].currentState, false);
        Ref.cgame.cg_entities[snap.ps.clientNum].interpolate = false;

        for (int i= 0; i < snap.numEntities; i++) {
            CEntity cent = Ref.cgame.cg_entities[snap.entities[i].ClientNum];
            cent.TransitionEntity();
            

            // remember time of snapshot this entity was last updated in
            cent.snapshotTime = snap.serverTime;
        }

        nextSnap = null;

        // check for playerstate transition events
        if(oldframe != null) {
            PlayerState ops, ps;
            ops = oldframe.ps;
            ps = snap.ps;

            // teleporting checks are irrespective of prediction
            if(((ps.eFlags ^ ops.eFlags) & EntityFlags.TELEPORT_BIT) > 0)
                thisFrameTeleport = true; // will be cleared by prediction code

            // if we are not doing client side movement prediction for any
            // reason, then the client events and view changes will be issued now
            if(Ref.cgame.cg_nopredict.iValue == 1 || playingdemo || ps.vehicle > 0) {
                TransitionPlayerState(ps, ops);
            }
        }

    }

    private void TransitionPlayerState(PlayerState ps, PlayerState ops) {
        // check for changing follow mode
        if(ps.clientNum != ops.clientNum)
        {
            thisFrameTeleport = true;
            // make sure we don't get any unwanted transition effects
            ops = ps;
        }
        
        if(mapRestart) {
            Respawn();
            mapRestart = false;
        }
        
        CheckPlayerStateEvents(ps, ops);

    }

    private void CheckPlayerStateEvents(PlayerState ps, PlayerState ops) {
        if(ps.externalEvent != 0 && ps.externalEvent != ops.externalEvent) {
            CEntity cent = Ref.cgame.cg_entities[ps.clientNum];
            cent.currentState.evt = ps.externalEvent;
            cent.currentState.evtParams = ps.externalEventParam;
            //Ref.cgame.EntityEvent(cent, cent.lerpOrigin);
            cent.Event();
        }

        CEntity cent = predictedPlayerEntity;
        // go through the predictable events buffer
        for (int i = ps.eventSequence - Common.MAX_PS_EVENTS; i < ps.eventSequence; i++) {
            // if we have a new predictable event
            if(i >= ops.eventSequence
                // or the server told us to play another event instead of a predicted event we already issued
                // or something the server told us changed our prediction causing a different event
                || (i > ops.eventSequence - Common.MAX_PS_EVENTS && ps.events[i & (Common.MAX_PS_EVENTS-1)] != ops.events[i & (Common.MAX_PS_EVENTS -1)])) {
                int evt = ps.events[i & (Common.MAX_PS_EVENTS-1)].ordinal();
                cent.currentState.evt = evt;
                cent.currentState.evtParams = ps.eventParams[i & (Common.MAX_PS_EVENTS-1)];
                //Ref.cgame.EntityEvent(cent, cent.lerpOrigin);
                cent.Event();
                predictableEvents[i & 15] = evt;
                eventSequence++;
            }
        }
    }

    

}
