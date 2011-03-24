package cubetech.common;

import cubetech.misc.Ref;
import cubetech.net.NetBuffer;
import org.lwjgl.util.vector.Vector2f;

/**
 * Stores a position + the velocity at that time + the time this position
 * was recorded + a Trajectory Type.
 * @author mads
 */
public class Trajectory {
    // Flags
    public static final int STATIONARY = 0;
    public static final int INTERPOLATE = 1;
    public static final int LINEAR = 2;
    public static final int LINEAR_STOP = 3;
    public static final int SINE = 4;
    public static final int GRAVITY = 5;

    public int type; // Should be one of the flags defined
    public int time;
    public int duration;
    public Vector2f base = new Vector2f();
    public Vector2f delta  = new Vector2f();
    
    // Just a wrapper, returns a new vector instead of reusing
    public Vector2f Evaluate(int time) {
        Vector2f result = new Vector2f();
        Evaluate(time, result);
        return result;
    }

    public void Evaluate(int attime, Vector2f dest) {
        switch(type) {
            case STATIONARY:
            case INTERPOLATE:
                dest.x = base.x;
                dest.y = base.y;
                break;
            case LINEAR:
                float deltaTime = (attime - time) * 0.001f;
                dest.x = base.x + deltaTime * delta.x;
                dest.y = base.y + deltaTime * delta.y;
                break;
            case SINE:
                deltaTime = (attime - time) / duration;
                float phase = (float)Math.sin(deltaTime * Math.PI * 2f);
                dest.x = base.x + phase * delta.x;
                dest.y = base.y + phase * delta.y;
                break;
            case LINEAR_STOP:
                if(attime > time + duration)
                    attime = time + duration;
                deltaTime = (attime - time) * 0.001f;
                if(deltaTime < 0)
                    deltaTime = 0;
                dest.x = base.x + deltaTime * delta.x;
                dest.y = base.y + deltaTime * delta.y;
                break;
            case GRAVITY:
                deltaTime = (attime - time) * 0.001f;
                dest.x = base.x + deltaTime * delta.x;
                dest.y = base.y + deltaTime * delta.y;
                dest.y -= 0.5f * Common.DEFAULT_GRAVITY * deltaTime * deltaTime; // FIXME: local gravity...
                break;
            default:
                Ref.common.Error(Common.ErrorCode.DROP, "Trajectory.Evaluate(): Unknown type " + type);
                break;
        }
    }

    // Compare two trajectories
    public boolean IsEqual(Trajectory pos) {
        if(type == pos.type && time == pos.time
                && duration == pos.duration && Helper.Equals(base, pos.base)
                && Helper.Equals(delta, pos.delta))
            return true;
        return false;
    }

    public void ReadDelta(NetBuffer buf, Trajectory from) {
        base = buf.ReadDeltaVector(from.base);
        delta = buf.ReadDeltaVector(from.delta);
        type = buf.ReadDeltaInt(from.type);
        time = buf.ReadDeltaInt(from.time);
        duration = buf.ReadDeltaInt(from.duration);
    }

    // b is the old
    public void WriteDelta(NetBuffer buf, Trajectory b) {
        buf.WriteDelta(b.base, base);
        buf.WriteDelta(b.delta, delta);
        buf.WriteDelta(b.type, type);
        buf.WriteDelta(b.time, time);
        buf.WriteDelta(b.duration, duration);
    }
}
