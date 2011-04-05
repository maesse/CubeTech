package cubetech.common;

import cubetech.Game.Gentity;
import cubetech.common.Move.MoveType;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.net.NetBuffer;
import cubetech.server.SvFlags;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class PlayerState {
    public int clientNum;
    public int commandTime;
    public int[] delta_angles = new int[2];
    public int eFlags = EntityFlags.NONE;
    public int entityEventSequence;
    public int eventSequence;
    public int[] eventParams = new int[Common.MAX_PS_EVENTS];
    public int[] events = new int[Common.MAX_PS_EVENTS];
    public int externalEvent;
    public int externalEventParam;
    public int externalEventTime;
    public Vector2f origin = new Vector2f();
    public Vector2f velocity = new Vector2f();
    public Vector2f viewangles = new Vector2f(); // TODO: Check
    public PlayerStats stats = new PlayerStats();
    public int ping = -1;

    public int moveType = MoveType.SPECTATOR;
    public int stepTime; // counting towards 0 for a step
    public int jumpTime; // while > 0, don't apply gravity
    public boolean applyPull = false;
    public boolean jumpDown = false;
    public boolean canDoubleJump = false;

    public int[] powerups = new int[NUM_POWERUPS];
    public static int NUM_POWERUPS = 1;

    public int movetime = 0;

    public int maptime = 0;
    

    // Wipe values
    public void Clear() {
        clientNum = 0;
        commandTime = 0;
        delta_angles[0] = delta_angles[1]  = 0;
        eFlags = EntityFlags.NONE;
        entityEventSequence = 0;
        eventSequence = 0;
        eventParams[0] = eventParams[1] = 0;
        events[0] = events[1] = 0;
        externalEvent = 0;
        externalEventParam = 0;
        externalEventTime = 0;
        origin = new Vector2f();
        velocity = new Vector2f();
        viewangles = new Vector2f();
        ping = -1;
        moveType = MoveType.SPECTATOR;
        stepTime = 0;
        jumpTime = 0;
        applyPull = false;
        jumpDown = false;
        canDoubleJump = false;
        powerups = new int[NUM_POWERUPS];
        movetime = 0;
        maptime = 0;
        stats = new PlayerStats();
    }

    // Use mouse position as a viewangle
    public void UpdateViewAngle(PlayerInput cmd) {
        // Scale from [0 -> 1] to [-1 -> 1], so 0 is center of the screen
        viewangles.x = cmd.MousePos.x*2f - 1f;
        viewangles.y = cmd.MousePos.y*2f - 1f;

        if(viewangles.x == 0 && viewangles.y == 0)
            viewangles.set(1, 0); // can't have zero-lenght view vectors, can we now?

        Helper.Normalize(viewangles);
    }

    public PlayerState Clone(PlayerState ps) {
        if(ps == null)
            ps = new PlayerState();

        ps.clientNum = clientNum;
        ps.commandTime = commandTime;
        ps.delta_angles[0] = delta_angles[0];
        ps.delta_angles[1] = delta_angles[1];
        ps.eFlags = eFlags;
        ps.entityEventSequence = entityEventSequence;
        ps.eventSequence = eventSequence;
        ps.eventParams[0] = eventParams[0];
        ps.eventParams[1] = eventParams[1];
        ps.events[0] = events[0];
        ps.events[1] = events[1];
        ps.externalEvent = externalEvent;
        ps.externalEventParam = externalEventParam;
        ps.externalEventTime = externalEventTime;
        ps.origin = new Vector2f();
        if(origin != null) {

            ps.origin.x = origin.x;
            ps.origin.y = origin.y;
        }
        ps.velocity = new Vector2f();
        if(velocity != null) {
            ps.velocity.x = velocity.x;
            ps.velocity.y = velocity.y;
        }
        ps.ping = ping;
        ps.moveType = moveType;
        ps.stepTime = stepTime;
        ps.viewangles = new Vector2f(viewangles);
        ps.stats = stats.clone();
        ps.jumpTime = jumpTime;
        ps.applyPull = applyPull;
        ps.jumpDown = jumpDown;
        ps.canDoubleJump = canDoubleJump;
        ps.movetime = movetime;
        ps.maptime = maptime;
        System.arraycopy(powerups, 0, ps.powerups, 0, NUM_POWERUPS);
        return ps;
//        }
    }

//    public void ToEntityStateExtrapolate(EntityState s, boolean snap) {
//        s.eType = EntityType.PLAYER;
//        s.pos.type = Trajectory.LINEAR_STOP;
//        s.pos.base.x = origin.x;
//        s.pos.base.y = origin.y;
//        if(snap) {
//            s.pos.base.x = (int)s.pos.base.x;
//            s.pos.base.y = (int)s.pos.base.y;
//        }
//        // set the trDelta for flag direction and linear prediction
//        s.pos.delta.x = velocity.x;
//        s.pos.delta.y = velocity.y;
//        s.pos.time = commandTime;
//        s.pos.duration = 50;
//
//        s.apos.type = Trajectory.INTERPOLATE;
//        s.apos.base.x = viewangles.x;
//        s.apos.base.y = viewangles.y;
//        s.ClientNum = clientNum;
//        s.eFlags = eFlags;
//
//        if(externalEvent > 0) {
//            s.evt = externalEvent;
//            s.evtParams = externalEventParam;
//
//        } else if(entityEventSequence < eventSequence) {
//            if(entityEventSequence < eventSequence - Common.MAX_PS_EVENTS)
//                entityEventSequence = eventSequence - Common.MAX_PS_EVENTS;
//
//            int seq = entityEventSequence & (Common.MAX_PS_EVENTS-1);
//            s.evt = events[seq] | ((entityEventSequence & 3) << 8);
//            s.evtParams = eventParams[seq];
//            entityEventSequence++;
//        }
//
//
//
//    }

    public void ToEntityState(EntityState s, boolean snap) {
        if(moveType == MoveType.SPECTATOR)
            s.eType = EntityType.INVISIBLE;
        else
            s.eType = EntityType.PLAYER;
        s.ClientNum = clientNum;
        s.time = movetime;
        s.pos.base.x = origin.x;
        s.pos.base.y = origin.y;
        s.pos.type = Trajectory.INTERPOLATE;
        if(snap) {
            s.pos.base.x = (int)s.pos.base.x;
            s.pos.base.y = (int)s.pos.base.y;
        }
        // set the trDelta for flag direction
        s.pos.delta.x = velocity.x;
        s.pos.delta.y = velocity.y;

        s.apos.type = Trajectory.INTERPOLATE;
        s.apos.base.x = viewangles.x;
        s.apos.base.y = viewangles.y;
        s.eFlags = eFlags;
        if(stats.Health <= 0)
            s.eFlags |= EntityFlags.DEAD;
        else
            s.eFlags &= ~EntityFlags.DEAD;
        if(externalEvent != 0) {
            s.evt = externalEvent;
            s.evtParams = externalEventParam;
        } else if(entityEventSequence < eventSequence) {
            if(entityEventSequence < eventSequence - Common.MAX_PS_EVENTS)
                entityEventSequence = eventSequence - Common.MAX_PS_EVENTS;

            int seq = entityEventSequence & (Common.MAX_PS_EVENTS-1);
            s.evt = events[seq] | ((entityEventSequence & 3) << 8);
            s.evtParams = eventParams[seq];
            entityEventSequence++;
        }
    }

    // Predictable events is events that the owning client can predict, so we send the event to everyone else
    public void SendPendingPredictableEvents() {
        // if there are still events pending
        if(entityEventSequence < eventSequence)
        {
            // create a temporary entity for this event which is sent to everyone
            // except the client who generated the event
            int seq = entityEventSequence & (Common.MAX_PS_EVENTS-1);
            int evt = events[seq] | ((entityEventSequence & 3) << 8);
            int extEvt = externalEvent;
            externalEvent = 0;
            Gentity t = Ref.game.TempEntity(origin, evt);
            int number = t.s.ClientNum;
            ToEntityState(t.s, false);
            t.s.ClientNum = number;
            t.s.eType = EntityType.EVENTS + evt;
            t.s.eFlags |= EntityFlags.PLAYER_EVENT;
            t.s.otherEntityNum = clientNum;
            // send to everyone except the client who generated the event
            t.r.svFlags.add(SvFlags.NOTSINGLECLIENT);
            t.r.singleClient = clientNum;
            externalEvent = extEvt;
        }

    }

    private static PlayerState nullstate = new PlayerState();
    public void WriteDelta(NetBuffer msg, PlayerState ps) {
        if(ps == null) {
            ps = nullstate;
        }
        msg.WriteDelta(ps.clientNum, clientNum);
        msg.WriteDelta(ps.commandTime, commandTime);
        msg.WriteDelta(ps.delta_angles[0], delta_angles[0]);
        msg.WriteDelta(ps.delta_angles[1], delta_angles[1]);
        msg.WriteDelta(ps.eFlags, eFlags);
        msg.WriteDelta(ps.entityEventSequence, entityEventSequence);
        msg.WriteDelta(ps.eventParams[0], eventParams[0]);
        msg.WriteDelta(ps.eventParams[1],  eventParams[1]);
        msg.WriteDelta(ps.events[0], events[0]);
        msg.WriteDelta(ps.events[1], events[1]);
        msg.WriteDelta(ps.externalEvent, externalEvent);
        msg.WriteDelta(ps.externalEventParam, externalEventParam);
        msg.WriteDelta(ps.externalEventTime, externalEventTime);
        msg.WriteDelta(ps.origin, origin);
        msg.WriteDelta(ps.velocity, velocity);
        msg.WriteDelta(ps.viewangles, viewangles);
        msg.WriteDelta(ps.ping, ping);
        msg.WriteDelta(ps.moveType, moveType);
        msg.WriteDelta(ps.stepTime, stepTime);
        msg.WriteDelta(ps.jumpTime, jumpTime);
        msg.Write(applyPull);
        msg.Write(jumpDown);
        msg.Write(canDoubleJump);
        msg.WriteDelta(ps.maptime, maptime);
        for (int i= 0; i < NUM_POWERUPS; i++) {
            msg.WriteDelta(ps.powerups[i], powerups[i]);
        }
        msg.WriteDelta(ps.movetime, movetime);
        stats.WriteDelta(msg, ps.stats);

    }

    public void ReadDelta(NetBuffer msg, PlayerState ps) {
        if(ps == null)
            ps = nullstate;
        clientNum = msg.ReadDeltaInt(ps.clientNum);
        commandTime = msg.ReadDeltaInt(ps.commandTime);
        delta_angles[0] = msg.ReadDeltaInt(ps.delta_angles[0]);
        delta_angles[1] = msg.ReadDeltaInt(ps.delta_angles[1]);
        eFlags = msg.ReadDeltaInt(ps.eFlags);
        entityEventSequence = msg.ReadDeltaInt(ps.entityEventSequence);
        eventParams[0] = msg.ReadDeltaInt(ps.eventParams[0]);
        eventParams[1] = msg.ReadDeltaInt(ps. eventParams[1]);
        events[0] = msg.ReadDeltaInt(ps.events[0]);
        events[1] = msg.ReadDeltaInt(ps.events[1]);
        externalEvent = msg.ReadDeltaInt(ps.externalEvent);
        externalEventParam = msg.ReadDeltaInt(ps.externalEventParam);
        externalEventTime = msg.ReadDeltaInt(ps.externalEventTime);
        origin = msg.ReadDeltaVector(ps.origin);
        velocity = msg.ReadDeltaVector(ps.velocity);
        viewangles = msg.ReadDeltaVector(ps.viewangles);
        ping = msg.ReadDeltaInt(ps.ping);
        moveType = msg.ReadDeltaInt(ps.moveType);
        stepTime = msg.ReadDeltaInt(ps.stepTime);
        jumpTime = msg.ReadDeltaInt(ps.jumpTime);
        applyPull = msg.ReadBool();
        jumpDown = msg.ReadBool();
        canDoubleJump = msg.ReadBool();
        maptime = msg.ReadDeltaInt(ps.maptime);
        for (int i= 0; i < NUM_POWERUPS; i++) {
            powerups[i] = msg.ReadDeltaInt(ps.powerups[i]);
        }
        movetime = msg.ReadDeltaInt(ps.movetime);
        stats.ReadDelta(msg, ps.stats);
    }

    public void AddPredictableEvent(int event, int eventParam) {
        events[eventSequence & (Common.MAX_PS_EVENTS-1)] = event;
        eventParams[eventSequence & (Common.MAX_PS_EVENTS-1)] = eventParam;
        eventSequence++;
    }




}
