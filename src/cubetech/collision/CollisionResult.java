package cubetech.collision;

import cubetech.common.Common;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CollisionResult {
    public boolean hit;
    public float frac;
    public int hitmask;
    public int entitynum;
    public boolean startsolid;

    // For debugging
    public Vector3f start = new Vector3f();
    public Vector3f delta = new Vector3f();
    public Vector3f extent = new Vector3f();
    public Vector3f hitAxis = new Vector3f();

    public void reset(Vector2f start, Vector2f delta, Vector2f extent) {
        hit = false;
        frac = 1.0f;
        hitmask = 0;
        startsolid = false;
        entitynum = Common.ENTITYNUM_NONE;
        this.start.set(start.x, start.y,0);
        this.delta.set(delta.x, delta.y,0);
        this.extent.set(extent.x, extent.y,0);
        hitAxis.set(0,0);
    }

    public void reset(Vector3f start, Vector3f delta, Vector3f extent) {
        hit = false;
        frac = 1;
        hitmask = 0;
        startsolid = false;
        entitynum = Common.ENTITYNUM_NONE;
        this.start.set(start);
        this.delta.set(delta);
        if(extent != null) this.extent.set(extent); // can be null for ray tracing
    }

    public Vector3f getPOI(Vector3f dest) {
        if(dest == null) dest = new Vector3f(delta);
        else dest.set(delta);
        dest.scale(frac);
        
        Vector3f.add(dest, start, dest);
        return dest;
    }
}
