
package cubetech.client;

import cubetech.CGame.CEntity;
import cubetech.CGame.Snapshot;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.Move;
import cubetech.common.MoveQuery;
import cubetech.common.PlayerState;
import cubetech.common.items.Weapon;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Mads
 */
public class LocalClient {
    public int localIndex; // 0-3
    public int clientNum;
    public int bobcycle;
    public float bobfracsin;
    public float xyspeed;
    public float autoAngle;

    public Weapon weaponSelect = Weapon.NONE;
    public float zoomSensitivity = 1.0f;
    
    public PlayerState	predictedPlayerState = new PlayerState();
    public CEntity predictedPlayerEntity = new CEntity();
    public boolean validPPS;				// clear until the first call to CG_PredictPlayerState
    public int predictedErrorTime;
    public Vector3f predictedError = new Vector3f();

    public int eventSequence;
    public int[] predictableEvents = new int[16]; // 16
    
    public boolean thisFrameTeleport;
    public boolean nextFrameTeleport;
    
    public boolean wasDead = true;
    
    public PlayerState snapPS = null;
    public Vector3f lastFocusPoint = null;
    
    public LocalClient(int index, int localIndex) {
        clientNum = index;
        this.localIndex = localIndex;
    }
    
    private void InterpolatePlayerState(boolean grabAngles) {

        Snapshot prev = Ref.cgame.cg.snap;
        Snapshot next = Ref.cgame.cg.nextSnap;
        
        // Copy current playerstate into the predicted playerstate
        predictedPlayerState = snapPS.Clone(predictedPlayerState);
        PlayerState out = predictedPlayerState;

        // if the next frame is a teleport, we can't lerp to it
        if(nextFrameTeleport) return;

        if(next == null || next.serverTime <= prev.serverTime) {
            return;
        }
        
        if(prev.lcIndex[localIndex] == -1 || next.lcIndex[localIndex] == -1) return;
        PlayerState prevPS = prev.pss[prev.lcIndex[localIndex]];
        PlayerState nextPS = next.pss[next.lcIndex[localIndex]];

        float f = ((float)Ref.cgame.cg.time - prev.serverTime) / ((float)next.serverTime - prev.serverTime);

        int nextbob = next.pss[next.lcIndex[localIndex]].bobcycle;
        int prevbob = prev.pss[prev.lcIndex[localIndex]].bobcycle;
        if(nextbob < prevbob) {
            nextbob += 256;
        }
        out.bobcycle = (int) (prevbob + f * (nextbob - prevbob));
        
        Vector3f delta = Vector3f.sub(nextPS.origin, prevPS.origin, null);
        Helper.VectorMA(prevPS.origin, f, delta, out.origin);
        if(!grabAngles) {
            out.viewangles = Helper.LerpAngles(prevPS.viewangles, nextPS.viewangles, null, f);
        } else {
             PlayerInput cmd = Ref.client.GetUserCommand(Ref.client.cl.cmdNumber, localIndex);
             out.UpdateViewAngle(cmd);
        }
        Vector3f.sub(nextPS.velocity, prevPS.velocity, delta);
        Helper.VectorMA(prevPS.velocity, f, delta, out.velocity);

    }
    
    public void PredictPlayerState() {
        // if this is the first frame we must guarantee
        // predictedPlayerState is valid even if there is some
        // other error condition
        if(!validPPS) {
            validPPS = true;
            if(snapPS == null) {
                int test = 2;
            }
            predictedPlayerState = snapPS.Clone(null);
        }

        if(Ref.cgame.cg.playingdemo) {
            InterpolatePlayerState(false);
            return;
        }

        // non-predicting local movement will grab the latest angles
        if(Ref.cgame.cg_nopredict.isTrue() || predictedPlayerState.vehicle > 0) {
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

        PlayerInput oldest = Ref.client.GetUserCommand(cmdNum, localIndex);
        if(oldest == null) return;
        if(oldest.serverTime > snapPS.commandTime && oldest.serverTime < Ref.cgame.cg.time) {
            Ref.cgame.Print(localIndex, "Exceeded packet_backup on commands");
            return;
        }

        // get the latest command so we can know which commands are from previous map_restarts
        PlayerInput latest = Ref.client.GetUserCommand(currentCmd, localIndex);

        // get the most recent information we have, even if
        // the server time is beyond our current cg.time,
        // because predicted player positions are going to
        // be ahead of everything else anyway
        Snapshot nextSnap = Ref.cgame.cg.nextSnap;
        if(nextSnap != null && !nextFrameTeleport && !thisFrameTeleport &&
                nextSnap.lcIndex[localIndex] != -1) {
            predictedPlayerState = nextSnap.pss[nextSnap.lcIndex[localIndex]].Clone(null);
        } else {
            predictedPlayerState = snapPS.Clone(null);
        }

        move.ps = predictedPlayerState;
        int nMove = 0;
        // run cmds
        boolean moved = false;
        for (int cmdnum= currentCmd - 63; cmdnum <= currentCmd; cmdnum++) {
            if(cmdnum <= 0)
                continue; // FIX ??

            move.cmd = Ref.client.GetUserCommand(cmdnum, localIndex);

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
                            Ref.cgame.Print(localIndex, "Prediction miss: " + len);
                        }
                        if(Ref.cgame.cg_errorDecay.iValue > 0) {
                            int t = Ref.cgame.cg.time - predictedErrorTime;
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
                        predictedErrorTime = Ref.cgame.cg.oldTime;
                    }
                }
            }

            Move.Move(move);
            moved = true;
            nMove++;
        }



        if(!moved) return;
            

//        System.out.println(""+nMove);

        // TODO: AdjustPositionForMover

        // fire events and other transition triggered things
        TransitionPlayerState(predictedPlayerState, oldPlayerState);
    }
    
    public void TransitionPlayerState(PlayerState ps, PlayerState ops) {
        // check for changing follow mode
        if(ps.clientNum != ops.clientNum)
        {
            thisFrameTeleport = true;
            // make sure we don't get any unwanted transition effects
            ops = ps;
        }
        
//        if(mapRestart) {
//            Respawn(-1);
//            mapRestart = false;
//        }
        
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
