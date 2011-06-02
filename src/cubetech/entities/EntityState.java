package cubetech.entities;

import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.common.PlayerState;
import cubetech.common.Trajectory;
import cubetech.common.items.Weapon;
import cubetech.net.NetBuffer;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 * // entityState_t is the information conveyed from the server
// in an update message about entities that the client will
// need to render in some way
// Different eTypes may use the information in different ways
// The messages are delta compressed, so it doesn't really matter if
// the structure size is fairly large
 * @author mads
 */
public class EntityState {
    public static final int SOLID_BMODEL = Integer.MAX_VALUE;
    public int ClientNum = -1; // entity id

    public int eType = EntityType.GENERAL; // entityType_t
    public int eFlags = EntityFlags.NONE;

    public Trajectory apos = new Trajectory(); // for calculating position
    public Trajectory pos = new Trajectory(); // for calculating angles
    public int solid; // packed half-size (y<<8) | x

    // events.. 
    public int evt;  // impulse events -- muzzle flashes, footsteps, etc
    public int evtParams;
    
    public int otherEntityNum; // shotgun sources, etc
    public int modelindex = -1;

    public Weapon weapon;
    
    // not used atm
//    public int[] powerups = new int[PlayerState.NUM_POWERUPS];
    public int frame; 
    public Vector3f origin = new Vector3f();
    public Vector3f Angles = new Vector3f();
    public int time;

    public void Clear() {
        Angles = new Vector3f();
        origin = new Vector3f();
        evt = 0;
        evtParams = 0;
        eType = EntityType.GENERAL;
        eFlags = EntityFlags.NONE;
        frame = 0;
        time = 0;
        pos = new Trajectory();
        apos = new Trajectory();
        ClientNum = -1;
        solid = 0;
        otherEntityNum = 0;
        modelindex = -1;
        weapon = null;
    }

    private boolean IsEqual(EntityState s) {
        if(ClientNum == s.ClientNum && time == s.time
                && evtParams == s.evtParams && evt == s.evt
                && pos.IsEqual(s.pos) && apos.IsEqual(s.apos)
                && Helper.Equals(s.Angles, Angles) && Helper.Equals(origin, s.origin)
                && eType == s.eType && eFlags == s.eFlags
                && frame == s.frame && otherEntityNum == s.otherEntityNum && solid == s.solid
                && modelindex == s.modelindex && s.weapon == weapon)
            return true;
        return false;
    }

    public static void WriteDeltaRemoveEntity(NetBuffer buf, EntityState toRemove) {
        buf.Write(toRemove.ClientNum);
        buf.Write(true); // remove
    }

    public void ReadDeltaEntity(NetBuffer buf, EntityState from, int newnum) {
        if(buf.ReadBool()) {
            // Remoev
            ClientNum = Common.ENTITYNUM_NONE;
            Common.LogDebug("Removed entity");
            return;
        }

        if(!buf.ReadBool()) {
            // No delta
            if(from == null)
                Clear();
            else
                from.Clone(this);
            ClientNum = newnum;
            return;
        }


        ClientNum = newnum;
        Angles = buf.ReadDeltaVector(from.Angles);
        origin = buf.ReadDeltaVector(from.origin);
        evt = buf.ReadDeltaInt(from.evt);
        evtParams = buf.ReadDeltaInt(from.evtParams);
        eType = buf.ReadDeltaInt(from.eType);
        eFlags = buf.ReadDeltaInt(from.eFlags);
        frame = buf.ReadDeltaInt(from.frame);
        time = buf.ReadDeltaInt(from.time);
        pos.ReadDelta(buf, from.pos);
        apos.ReadDelta(buf, from.apos);
        otherEntityNum = buf.ReadDeltaInt(from.otherEntityNum);
        solid = buf.ReadDeltaInt(from.solid);
        modelindex = buf.ReadDeltaInt(from.modelindex);
        weapon = buf.ReadEnum(Weapon.class);
    }

    public void WriteDeltaEntity(NetBuffer buf, EntityState b, boolean force) {
        boolean same = IsEqual(b);
        if(same) {
            if(!force)
                return;

            buf.Write(ClientNum);
            buf.Write(false); // not removed
            buf.Write(false); // no delta
            return;
        }

        buf.Write(ClientNum);
        buf.Write(false); // not removed
        buf.Write(true); // got delta

        buf.WriteDelta(b.Angles, Angles);
        buf.WriteDelta(b.origin, origin);
        buf.WriteDelta(b.evt, evt);
        buf.WriteDelta(b.evtParams, evtParams);
        buf.WriteDelta(b.eType, eType);
        buf.WriteDelta(b.eFlags, eFlags);
        buf.WriteDelta(b.frame, frame);
        buf.WriteDelta(b.time, time);
        pos.WriteDelta(buf, b.pos);
        apos.WriteDelta(buf, b.apos);
        buf.WriteDelta(b.otherEntityNum, otherEntityNum);
        buf.WriteDelta(b.solid, solid);
        buf.WriteDelta(b.modelindex, modelindex);
        buf.WriteEnum(weapon);
    }

    public EntityState Clone(EntityState st) {
        st.Angles = new Vector3f(Angles);
        st.origin = new Vector3f(origin);
        st.evt = evt;
        st.evtParams = evtParams;
        st.eType = eType;
        st.eFlags = eFlags;
        st.frame = frame;
        st.time = time;
        st.pos = new Trajectory();
        st.pos.base = new Vector3f(pos.base);
        st.pos.delta = new Vector3f(pos.delta);
        st.pos.type = pos.type;
        st.pos.time = pos.time;
        st.pos.duration = pos.duration;
        st.apos = new Trajectory();
        st.apos.base = new Vector3f(apos.base);
        st.apos.delta = new Vector3f(apos.delta);
        st.apos.type = apos.type;
        st.apos.time = apos.time;
        st.apos.duration = apos.duration;
        st.ClientNum = ClientNum;
        st.otherEntityNum = otherEntityNum;
        st.solid = solid;
        st.modelindex = modelindex;
        st.weapon = weapon;
        return st;
    }

}


