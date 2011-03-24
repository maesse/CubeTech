/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import cubetech.collision.CollisionResult;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public interface ITrace {
    public CollisionResult Trace(Vector2f start, Vector2f end, Vector2f mins, Vector2f maxs, int tracemask, int passEntityNum);
}
