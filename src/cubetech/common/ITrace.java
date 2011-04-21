/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import cubetech.collision.CollisionResult;

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public interface ITrace {
    public CollisionResult Trace(Vector3f start, Vector3f end, Vector3f mins, Vector3f maxs, int tracemask, int passEntityNum);
}
