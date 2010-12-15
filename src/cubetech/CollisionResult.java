/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class CollisionResult {
    public boolean Hit;
    public float frac;
    public int hitmask;
    public Object hitObject;

    // For debugging
    public Vector2f Start;
    public Vector2f Delta;
    public Vector2f Extent;

    public void Reset(Vector2f start, Vector2f delta, Vector2f extent) {
        Hit = false;
        frac = 1.0f;
        hitmask = 0;
        hitObject = null;
        this.Start = new Vector2f(start.x, start.y);
        this.Delta = new Vector2f(delta.x, delta.y);
        this.Extent = new Vector2f(extent.x, extent.y);
    }
}
