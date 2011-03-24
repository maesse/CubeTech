package cubetech.CGame;

import cubetech.common.Common;
import cubetech.common.GItem;
import cubetech.common.Helper;
import cubetech.common.Trajectory;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.entities.Event;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;

/**
 * centity have a direct corespondence with gentity_t in the game, but
 * only the entityState is directly communicated to the cgame
 * @author mads
 */
public class CEntity {
    public EntityState currentState = new EntityState(); // from cg.frame
    public EntityState nextState = new EntityState(); // from cg.nextFrame, if available
    public boolean interpolate; // true if next is valid to interpolate to
    public boolean currentValid; // true if cg.frame holds this entity

    public int previousEvent;
    public int snapshotTime; // last time this entity was found in a snapshot
    //public PlayerEntity pe;

    public int errorTime; // decay the error from this time
    public Vector2f errorOrigin = new Vector2f();
    public Vector2f errorAngles = new Vector2f();

    public boolean extrapolated; // false if origin / angles is an interpolation
    public Vector2f rawOrigin = new Vector2f();
    public Vector2f rawAngles = new Vector2f();

    // exact interpolated position of entity on this frame
    public Vector2f lerpOrigin = new Vector2f();
    public Vector2f lerpAngles = new Vector2f();

    void ResetEntity() {
        // if the previous snapshot this entity was updated in is at least
	// an event window back in time then we can reset the previous event
        if(snapshotTime < Ref.cgame.cg.time - Common.EVENT_VALID_MSEC)
            previousEvent = 0;
        lerpOrigin = new Vector2f(currentState.origin);
        lerpAngles = new Vector2f(currentState.Angles);
        if(currentState.eType == EntityType.PLAYER)
            ResetPlayerEntity();
    }

    // A player just came into view or teleported, so reset all animation info
    private void ResetPlayerEntity() {
        errorTime = -9999;
        extrapolated =false;

        currentState.pos.Evaluate(Ref.cgame.cg.time, lerpOrigin);
        currentState.apos.Evaluate(Ref.cgame.cg.time, lerpAngles);

        Helper.VectorCopy(lerpOrigin, rawOrigin);
        Helper.VectorCopy(lerpAngles, rawAngles);
    }

    void CheckEvents() {
        // check for event-only entities
        if(currentState.eType > EntityType.EVENTS) {
            if(previousEvent > 0)
                return; // already fired

            // if this is a player event set the entity number of the client entity number
            if((currentState.eFlags & EntityFlags.PLAYER_EVENT) > 0)
                currentState.ClientNum = currentState.otherEntityNum;

            previousEvent = 1;
            currentState.evt = currentState.eType - EntityType.EVENTS;
        } else {
            // check for events riding with another entity
            if(currentState.evt == previousEvent)
                return;
            previousEvent = currentState.evt;
            if((currentState.evt & ~Common.EV_EVENT_BITS) == 0)
                return;
        }

        // calculate the position at exactly the frame time
        currentState.pos.Evaluate(Ref.cgame.cg.snap.serverTime, lerpOrigin);
        Ref.soundMan.SetEntityPosition(currentState.ClientNum, lerpOrigin, currentState.pos.delta);
        Event();
        //Ref.cgame.EntityEvent(this, lerpOrigin);
    }

    /*
    ===============
    CG_TransitionEntity

    cent->nextState is moved to cent->currentState and events are fired
    ===============
    */
    void TransitionEntity() {
        currentState = nextState;
        nextState = new EntityState();
        currentValid = true;

        // reset if the entity wasn't in the last frame or was teleported
        if(!interpolate)
            ResetEntity();

        // clear the next state.  if will be set by the next CG_SetNextSnap
        interpolate = false;

        CheckEvents();
    }

    void CalcLerpPosition() {
        // if this player does not want to see extrapolated players
        if(Ref.cgame.cg_smoothclients.iValue == 0)
        {
            // make sure the clients use TR_INTERPOLATE
            if(currentState.ClientNum < 64) {
                currentState.pos.type = Trajectory.INTERPOLATE;
                nextState.pos.type = Trajectory.INTERPOLATE;
            }
        }

        if(interpolate && currentState.pos.type == Trajectory.INTERPOLATE) {
            InterpolateEntityPosition();
            return;
        }

        // first see if we can interpolate between two snaps for
	// linear extrapolated clients
        if(interpolate && currentState.pos.type == Trajectory.LINEAR_STOP && currentState.ClientNum  < 64)
        {
            InterpolateEntityPosition();
            return;
        }

        // just use the current frame and evaluate as best we can
        currentState.pos.Evaluate(Ref.cgame.cg.time, lerpOrigin);
        currentState.apos.Evaluate(Ref.cgame.cg.time, lerpAngles);

        // adjust for riding a mover if it wasn't rolled into the predicted
        // player state
        if(this != Ref.cgame.cg.predictedPlayerEntity) {
            // AdjustForMover...
        }
    }

    private void InterpolateEntityPosition() {
        // it would be an internal error to find an entity that interpolates without
        // a snapshot ahead of the current one
        if(Ref.cgame.cg.nextSnap == null)
            Ref.common.Error(Common.ErrorCode.DROP, "InterpolateEntityPosition: cg.nextSnap == null");

        float f = Ref.cgame.cg.frameInterpolation;

        // this will linearize a sine or parabolic curve, but it is important
	// to not extrapolate player positions if more recent data is available
        Vector2f current = new Vector2f();
        Vector2f next = new Vector2f();
        currentState.pos.Evaluate(Ref.cgame.cg.snap.serverTime, current);
        nextState.pos.Evaluate(Ref.cgame.cg.nextSnap.serverTime, next);

        float diff = f * (next.x - current.x);
        lerpOrigin.x = current.x + diff;
        diff = f * (next.y - current.y);
        lerpOrigin.y = current.y + diff;

        currentState.apos.Evaluate(Ref.cgame.cg.snap.serverTime, current);
        nextState.apos.Evaluate(Ref.cgame.cg.nextSnap.serverTime, next);

        lerpAngles.x = current.x + f * (next.x - current.x);
        lerpAngles.y = current.y + f * (next.y - current.y);
    }

    public void Event() {
        int event = currentState.evt & ~Common.EV_EVENT_BITS;

        if(event == 0) {
            System.out.println("Zero event");
            return;
        }

        switch(event) {
            case Event.FOOTSTEP:
                Ref.soundMan.playEntityEffect(currentState.ClientNum, Ref.cgame.cgs.media.s_footStep, 1f);
                break;
            case Event.ITEM_PICKUP:
                int index = currentState.evtParams;
                if(index < 0 || index >= Ref.common.items.getItemCount()) {
                    System.out.println("Invalid ITEM_PICKUP GItem index: " + index);
                    break;
                }
                GItem item = Ref.common.items.getItem(index);
                Ref.soundMan.playEntityEffect(currentState.ClientNum, Ref.soundMan.AddWavSound(item.pickupSound), 1.0f);
                break;
            case Event.ITEM_RESPAWN:
                Ref.soundMan.playEntityEffect(currentState.ClientNum, Ref.cgame.cgs.media.s_itemRespawn, 1.0f);
                break;
            default:
                System.out.println("Unknown event: " + event);
                break;
        }
    }

    void Effects() {
        Ref.soundMan.SetEntityPosition(currentState.ClientNum, lerpOrigin, currentState.pos.delta);


    }


}
