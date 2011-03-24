package cubetech.collision;

import cubetech.common.Common;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class CollisionResult {
    public boolean Hit;
    public float frac;
    public int hitmask;
    public int entitynum;
    public boolean startsolid;

    // For debugging
    public Vector2f Start;
    public Vector2f Delta;
    public Vector2f Extent;
    public Vector2f HitAxis = new Vector2f();

    public void Reset(Vector2f start, Vector2f delta, Vector2f extent) {
        Hit = false;
        frac = 1.0f;
        hitmask = 0;
        startsolid = false;
        entitynum = Common.ENTITYNUM_NONE;
        this.Start = new Vector2f(start.x, start.y);
        this.Delta = new Vector2f(delta.x, delta.y);
        this.Extent = new Vector2f(extent.x, extent.y);
        HitAxis = new Vector2f();
        
    }
}
