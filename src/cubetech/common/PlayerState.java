package cubetech.common;

import cubetech.Game.ClientPersistant;
import cubetech.Game.Game;
import cubetech.common.items.Weapon;
import cubetech.common.items.WeaponState;
import cubetech.Game.Gentity;
import cubetech.common.Move.MoveType;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.entities.Event;
import cubetech.input.Input;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import cubetech.net.NetBuffer;
import cubetech.server.SvFlags;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class PlayerState {
    public int clientNum;
    public int commandTime;
    public int[] delta_angles = new int[3];
    public int eFlags = EntityFlags.NONE;
    public int entityEventSequence;
    public int eventSequence;
    public int[] eventParams = new int[Common.MAX_PS_EVENTS];
    public Event[] events = new Event[Common.MAX_PS_EVENTS];
    public int externalEvent;
    public int externalEventParam;
    public int externalEventTime;
    public Vector3f origin = new Vector3f();
    public Vector3f velocity = new Vector3f();
    public Vector3f viewangles = new Vector3f(); // TODO: Check
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
    public Vector3f punchangle = new Vector3f();
    public int groundEntity = Common.ENTITYNUM_NONE;
    public float fallVelocity;

    public int bobcycle = 0;

    public int maptime = 0;

    public Weapon weapon = Weapon.NONE; // copied to entityState_t->weapon
    public int weaponTime;
    public WeaponState weaponState = WeaponState.READY;

    // Animation
    public int animation = 0;
    public int animTime = 0;
    public int moveDirection = 0;
    public int dmgTime = 0;

    // Duck handling
    public boolean ducking;
    public boolean ducked;
    public int ducktime;
    public boolean[] oldButtons = new boolean[30];
    public float viewheight = Game.PlayerViewHeight;

    // from game -> server, tells server what map data to send
    public ClientPersistant pers = null;
    public int vehicle; // current vehicle entity if any (0 if none)

    // Adds in the viewoffset from standing/ducking
    public Vector3f getViewOrigin() {
        Vector3f view = new Vector3f(origin);
        view.z += viewheight;
        return view;
    }

    public PlayerState() {
        delta_angles[0] = -16000;
    }

    // Wipe values
    public void Clear() {
        groundEntity = Common.ENTITYNUM_NONE;
        vehicle = 0;
        fallVelocity = 0;
        clientNum = 0;
        dmgTime = 0;
        commandTime = 0;
        delta_angles = new int[3];
        delta_angles[0] = -16000;
        eFlags = EntityFlags.NONE;
        entityEventSequence = 0;
        eventSequence = 0;
        eventParams[0] = eventParams[1] = 0;
        events[0] = events[1] = Event.NONE;
        externalEvent = 0;
        externalEventParam = 0;
        externalEventTime = 0;
        origin = new Vector3f();
        velocity = new Vector3f();
        punchangle = new Vector3f();
        viewangles = new Vector3f();
        ping = -1;
        moveType = MoveType.SPECTATOR;
        stepTime = 0;
        jumpTime = 0;
        applyPull = false;
        jumpDown = false;
        canDoubleJump = false;
        powerups = new int[NUM_POWERUPS];
        bobcycle = 0;
        maptime = 0;
        animation = 0;
        animTime = 0;
        weapon = Weapon.NONE;
        weaponTime = 0;
        weaponState = WeaponState.READY;
        stats = new PlayerStats();
        ducking = false;
        ducked = false;
        ducktime = 0;
        oldButtons = new boolean[30];
         viewheight = Game.PlayerViewHeight;
    }

//    // Use mouse position as a viewangle
//    public void UpdateViewAngle(PlayerInput cmd) {
//        // Scale from [0 -> 1] to [-1 -> 1], so 0 is center of the screen
//        viewangles.x = cmd.MousePos.x*2f - 1f;
//        viewangles.y = cmd.MousePos.y*2f - 1f;
//
//        if(viewangles.x == 0 && viewangles.y == 0)
//            viewangles.set(1, 0); // can't have zero-lenght view vectors, can we now?
//
//        Helper.Normalize(viewangles);
//    }

    public void UpdateViewAngle(PlayerInput cmd) {
        // circularly clamp the angles with deltas
        for (int i= 0; i < 3; i++) {
            int temp = cmd.angles[i] + delta_angles[i];
            if(i == Input.ANGLE_PITCH) {
                // don't let the player look up or down more than 90 degrees
                if(temp > 16000){
                    delta_angles[i] = 16000 - cmd.angles[i];
                    temp = 16000; // this is actually more like 88deg
                } else if(temp < -16000) {
                    delta_angles[i] = -16000 - cmd.angles[i];
                    temp = -16000;
                }
            }
            Helper.VectorSet(viewangles, i, Helper.Short2Angle(temp));
        }
    }

    public PlayerState Clone(PlayerState ps) {
        if(ps == null)
            ps = new PlayerState();

        ps.clientNum = clientNum;
        ps.groundEntity = groundEntity;
        ps.fallVelocity = fallVelocity;
        ps.commandTime = commandTime;
        ps.dmgTime = dmgTime;
        ps.delta_angles[0] = delta_angles[0];
        ps.delta_angles[1] = delta_angles[1];
        ps.delta_angles[2] = delta_angles[2];
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
        ps.origin = new Vector3f();
        ps.vehicle = vehicle;
        if(origin != null) {
            ps.origin.set(origin);
        }
        ps.velocity = new Vector3f();
        if(velocity != null) {
            ps.velocity.set(velocity);
        }
        ps.punchangle.set(punchangle);
        ps.ping = ping;
        ps.moveType = moveType;
        ps.stepTime = stepTime;
        ps.viewangles = new Vector3f(viewangles);
        ps.stats = stats.clone();
        ps.jumpTime = jumpTime;
        ps.applyPull = applyPull;
        ps.jumpDown = jumpDown;
        ps.canDoubleJump = canDoubleJump;
        ps.bobcycle = bobcycle;
        ps.maptime = maptime;
        ps.animTime = animTime;
        ps.animation = animation;
        ps.weapon = weapon;
        ps.weaponTime = weaponTime;
        ps.weaponState = weaponState;
        ps.pers = pers;
        ps.ducked = ducked;
        ps.ducking = ducking;
        ps.ducktime = ducktime;
        ps.viewheight = viewheight;
        System.arraycopy(powerups, 0, ps.powerups, 0, NUM_POWERUPS);
        System.arraycopy(oldButtons, 0, ps.oldButtons, 0, oldButtons.length);
        return ps;
//        }
    }

    public void ToEntityState(EntityState s, boolean snap) {
        if(moveType == MoveType.SPECTATOR)
            s.eType = EntityType.INVISIBLE;
        else
            s.eType = EntityType.PLAYER;

        // TODO animation
        s.frame = animation;
        s.number = clientNum;
        s.time = bobcycle;
        s.pos.base.set(origin);
        s.pos.type = Trajectory.INTERPOLATE;
        if(snap) {
            s.pos.base.x = (int)s.pos.base.x;
            s.pos.base.y = (int)s.pos.base.y;
            s.pos.base.z = (int)s.pos.base.z;
        }
        // set the trDelta for flag direction
        s.pos.delta.set(velocity);

        s.weapon = weapon;
        s.Angles2.y = moveDirection;

        s.apos.type = Trajectory.INTERPOLATE;
        s.contents = (stats.Health > 0 ? Content.BODY : Content.CORPSE);
        s.apos.base.set(viewangles);
        s.eFlags = eFlags;
        if(stats.Health <= 0) s.eFlags |= EntityFlags.DEAD;
        else s.eFlags &= ~EntityFlags.DEAD;
            
        if(externalEvent != 0) {
            s.evt = externalEvent;
            s.evtParams = externalEventParam;
        } else if(entityEventSequence < eventSequence) {
            if(entityEventSequence < eventSequence - Common.MAX_PS_EVENTS)
                entityEventSequence = eventSequence - Common.MAX_PS_EVENTS;

            int seq = entityEventSequence & (Common.MAX_PS_EVENTS-1);
            s.evt = events[seq].ordinal() | ((entityEventSequence & 3) << 8);
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
            int evt = events[seq].ordinal() | ((entityEventSequence & 3) << 8);
            int extEvt = externalEvent;
            externalEvent = 0;
            Gentity t = Ref.game.TempEntity(origin, evt);
            int n = t.s.number;
            ToEntityState(t.s, true);
            t.s.contents = 0;
            t.s.number = n;
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
        msg.WriteDelta(ps.delta_angles[2], delta_angles[2]);
        msg.WriteDelta(ps.eFlags, eFlags);
        msg.WriteDelta(ps.vehicle, vehicle);
        msg.WriteDelta(ps.entityEventSequence, entityEventSequence);
        msg.WriteDelta(ps.eventParams[0], eventParams[0]);
        msg.WriteDelta(ps.eventParams[1],  eventParams[1]);
        msg.WriteDelta(ps.eventSequence, eventSequence);
        msg.WriteEnum(events[0]);
        msg.WriteEnum(events[1]);
        msg.WriteDelta(ps.externalEvent, externalEvent);
        msg.WriteDelta(ps.externalEventParam, externalEventParam);
        msg.WriteDelta(ps.externalEventTime, externalEventTime);
        msg.WriteDelta(ps.origin, origin);
        msg.WriteDelta(ps.velocity, velocity);
        msg.WriteDelta(ps.punchangle, punchangle);
        msg.WriteDelta(ps.viewangles, viewangles);
        msg.WriteDelta(ps.ping, ping);
        msg.WriteDelta(ps.moveType, moveType);
        msg.WriteDelta(ps.stepTime, stepTime);
        msg.WriteDelta(ps.jumpTime, jumpTime);
        msg.WriteDelta(ps.dmgTime, dmgTime);
        msg.Write(applyPull);
        msg.Write(jumpDown);
        msg.Write(canDoubleJump);
        msg.WriteDelta(ps.animation, animation);
        msg.WriteEnum(weapon);
        msg.WriteDelta(ps.weaponTime, weaponTime);
        msg.WriteEnum(weaponState);
        msg.WriteDelta(ps.maptime, maptime);
        for (int i= 0; i < NUM_POWERUPS; i++) {
            msg.WriteDelta(ps.powerups[i], powerups[i]);
        }
        msg.WriteDelta(ps.bobcycle, bobcycle);
        stats.WriteDelta(msg, ps.stats);
        msg.Write(ducking);
        msg.Write(ducked);
        msg.WriteDelta(ps.ducktime, ducktime);
        int buttons = 0;
        for (int i= 0; i < oldButtons.length; i++) {
            if(oldButtons[i]) buttons |= (1 << i);
        }
        msg.Write(buttons);
        msg.WriteDelta(ps.viewheight, viewheight);
        msg.WriteDelta(ps.groundEntity, groundEntity);
        msg.WriteDelta(ps.fallVelocity, fallVelocity);
    }

    public void ReadDelta(NetBuffer msg, PlayerState ps) {
        if(ps == null)
            ps = nullstate;
        clientNum = msg.ReadDeltaInt(ps.clientNum);
        commandTime = msg.ReadDeltaInt(ps.commandTime);
        delta_angles[0] = msg.ReadDeltaInt(ps.delta_angles[0]);
        delta_angles[1] = msg.ReadDeltaInt(ps.delta_angles[1]);
        delta_angles[2] = msg.ReadDeltaInt(ps.delta_angles[2]);
        eFlags = msg.ReadDeltaInt(ps.eFlags);
        vehicle = msg.ReadDeltaInt(ps.vehicle);
        entityEventSequence = msg.ReadDeltaInt(ps.entityEventSequence);
        eventParams[0] = msg.ReadDeltaInt(ps.eventParams[0]);
        eventParams[1] = msg.ReadDeltaInt(ps.eventParams[1]);
        eventSequence = msg.ReadDeltaInt(ps.eventSequence);

        events[0] = msg.ReadEnum(Event.class);
        events[1] = msg.ReadEnum(Event.class);
        externalEvent = msg.ReadDeltaInt(ps.externalEvent);
        externalEventParam = msg.ReadDeltaInt(ps.externalEventParam);
        externalEventTime = msg.ReadDeltaInt(ps.externalEventTime);
        origin = msg.ReadDeltaVector(ps.origin);
        velocity = msg.ReadDeltaVector(ps.velocity);
        punchangle = msg.ReadDeltaVector(ps.punchangle);
        viewangles = msg.ReadDeltaVector(ps.viewangles);
        ping = msg.ReadDeltaInt(ps.ping);
        moveType = msg.ReadDeltaInt(ps.moveType);
        stepTime = msg.ReadDeltaInt(ps.stepTime);
        jumpTime = msg.ReadDeltaInt(ps.jumpTime);
        dmgTime = msg.ReadDeltaInt(ps.dmgTime);
        applyPull = msg.ReadBool();
        jumpDown = msg.ReadBool();
        canDoubleJump = msg.ReadBool();
        animation = msg.ReadDeltaInt(ps.animation);
        weapon = msg.ReadEnum(Weapon.class);
        weaponTime = msg.ReadDeltaInt(ps.weaponTime);
        weaponState = msg.ReadEnum(WeaponState.class);
        maptime = msg.ReadDeltaInt(ps.maptime);
        for (int i= 0; i < NUM_POWERUPS; i++) {
            powerups[i] = msg.ReadDeltaInt(ps.powerups[i]);
        }
        bobcycle = msg.ReadDeltaInt(ps.bobcycle);
        stats.ReadDelta(msg, ps.stats);
        ducking = msg.ReadBool();
        ducked = msg.ReadBool();
        ducktime = msg.ReadDeltaInt(ps.ducktime);
        int buttons = msg.ReadInt();
        for (int i= 0; i < oldButtons.length; i++) {
            oldButtons[i] = (((buttons >> i) & 1) == 1);
        }
        viewheight = msg.ReadDeltaFloat(ps.viewheight);
        groundEntity = msg.ReadDeltaInt(ps.groundEntity);
        fallVelocity = msg.ReadDeltaFloat(ps.fallVelocity);
    }

    public void AddPredictableEvent(Event event, int eventParam) {
        events[eventSequence & (Common.MAX_PS_EVENTS-1)] = event;
        eventParams[eventSequence & (Common.MAX_PS_EVENTS-1)] = eventParam;
        eventSequence++;
    }




}
