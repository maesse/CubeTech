package cubetech.entities;

import cubetech.common.Animations;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.common.PlayerState;
import cubetech.common.Trajectory;
import cubetech.common.items.Weapon;
import cubetech.net.NetBuffer;
import org.lwjgl.util.vector.Quaternion;
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
    public int number = -1; // entity id

    
    
    public int eType = EntityType.GENERAL; // entityType_t
    public int eFlags = EntityFlags.NONE;

    public Trajectory apos = new Trajectory(); // for calculating angles
    public Trajectory pos = new Trajectory(); // for calculating position
    public int solid; // packed half-size (y<<8) | x

    // events.. 
    public int evt;  // impulse events -- muzzle flashes, footsteps, etc
    public int evtParams;
    
    public int otherEntityNum; // shotgun sources, etc
    public int modelindex = -1;

    public Weapon weapon;
    
    public int contents; // CONTENTS_TRIGGER, CONTENTS_SOLID, CONTENTS_BODY, etc
                         // a non-solid entity should set to 0
    
    
    // not used atm
//    public int[] powerups = new int[PlayerState.NUM_POWERUPS];
    public int frame; 
    public Vector3f origin = new Vector3f();
    public Vector3f Angles = new Vector3f();
    public Vector3f Angles2 = new Vector3f();
    public int time;

    public Animations frameAsAnimation() {
        int animFrame = frame & ~128;
        if(animFrame < 0 || animFrame >= Animations.values().length) {
            return null;
        }
        Animations anim = Animations.values()[animFrame];
        return anim;
    }

    public void Clear() {
        Angles = new Vector3f();
        Angles2 = new Vector3f();
        origin = new Vector3f();
        contents = 0;
        evt = 0;
        evtParams = 0;
        eType = EntityType.GENERAL;
        eFlags = EntityFlags.NONE;
        frame = 0;
        time = 0;
        pos = new Trajectory();
        apos = new Trajectory();
        number = -1;
        solid = 0;
        otherEntityNum = 0;
        modelindex = -1;
        weapon = null;
    }

    private boolean IsEqual(EntityState s) {
        if(number == s.number && time == s.time
                && evtParams == s.evtParams && evt == s.evt
                && pos.IsEqual(s.pos) && apos.IsEqual(s.apos)
                && Helper.Equals(s.Angles, Angles) && Helper.Equals(Angles2, s.Angles2) && Helper.Equals(origin, s.origin)
                && eType == s.eType && eFlags == s.eFlags
                && frame == s.frame && otherEntityNum == s.otherEntityNum && solid == s.solid
                && modelindex == s.modelindex && s.weapon == weapon && contents == s.contents)
            return true;
        return false;
    }

    public static void WriteDeltaRemoveEntity(NetBuffer buf, EntityState toRemove) {
        buf.WriteShort(toRemove.number);
        buf.Write(true); // remove
    }

    public void ReadDeltaEntity(NetBuffer buf, EntityState from, int newnum) {
        if(buf.ReadBool()) {
            // Remoev
            number = Common.ENTITYNUM_NONE;
            //Common.LogDebug("Removed entity");
            return;
        }

        if(!buf.ReadBool()) {
            // No delta
            if(from == null)
                Clear();
            else
                from.Clone(this);
            number = newnum;
            return;
        }


        number = newnum;
        Angles = buf.ReadDeltaVector(from.Angles);
        Angles2 = buf.ReadDeltaVector(from.Angles2);
        origin = buf.ReadDeltaVector(from.origin);
        evt = buf.ReadDeltaShort((short)from.evt);
        evtParams = buf.ReadDeltaInt(from.evtParams);
        eType = buf.ReadDeltaShort((short)from.eType);
        eFlags = buf.ReadByte();
        frame = buf.ReadDeltaShort((short)from.frame);
        time = buf.ReadDeltaShort((short)from.time);
        pos.ReadDelta(buf, from.pos);
        apos.ReadDelta(buf, from.apos);
        otherEntityNum = buf.ReadDeltaShort((short)from.otherEntityNum);
        solid = buf.ReadDeltaInt(from.solid);
        contents = buf.ReadDeltaInt(from.contents);
        modelindex = buf.ReadDeltaShort((short)from.modelindex);
        weapon = buf.ReadEnum(Weapon.class);
    }

    public void WriteDeltaEntity(NetBuffer buf, EntityState b, boolean force) {
        boolean same = IsEqual(b);
        if(same) {
            if(!force)
                return;

            buf.WriteShort(number);
            buf.Write(false); // not removed
            buf.Write(false); // no delta
            return;
        }

        buf.WriteShort(number);
        buf.Write(false); // not removed
        buf.Write(true); // got delta

        buf.WriteDelta(b.Angles, Angles); // 13b
        buf.WriteDelta(b.Angles2, Angles2);

        buf.WriteDelta(b.origin, origin);
        buf.WriteDelta((short)b.evt, (short)evt);
        buf.WriteDelta(b.evtParams, evtParams);
        buf.WriteDelta((short)b.eType, (short)eType);
        buf.WriteByte(eFlags);
        buf.WriteDelta((short)b.frame, (short)frame);
        buf.WriteDelta((short)b.time, (short)time);
        pos.WriteDelta(buf, b.pos);
        apos.WriteDelta(buf, b.apos);
        buf.WriteDelta((short)b.otherEntityNum, (short)otherEntityNum);
        buf.WriteDelta(b.solid, solid);
        buf.WriteDelta(b.contents, contents);
        buf.WriteDelta((short)b.modelindex, (short)modelindex);
        buf.WriteEnum(weapon);
    }

    public EntityState Clone(EntityState st) {
        st.Angles.set(Angles);
        st.Angles2.set(Angles2);
        st.origin.set(origin);
        st.evt = evt;
        st.evtParams = evtParams;
        st.eType = eType;
        st.eFlags = eFlags;
        st.frame = frame;
        st.time = time;
        st.pos = new Trajectory();
        st.pos.base.set(pos.base);
        st.pos.delta.set(pos.delta);
        st.pos.type = pos.type;
        st.pos.time = pos.time;
        st.pos.duration = pos.duration;
        st.apos = new Trajectory();
        st.apos.base.set(apos.base);
        st.apos.delta.set(apos.delta);
        st.apos.quater.set(apos.quater);
        st.apos.type = apos.type;
        st.apos.time = apos.time;
        st.contents = contents;
        st.apos.duration = apos.duration;
        st.number = number;
        st.otherEntityNum = otherEntityNum;
        st.solid = solid;
        st.modelindex = modelindex;
        st.weapon = weapon;
        return st;
    }

}


