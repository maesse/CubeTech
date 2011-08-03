/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import cubetech.collision.CollisionResult;
import cubetech.input.PlayerInput;

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class MoveQuery {
    // state (in/out)
    public PlayerState ps;

    // command (in)
    public PlayerInput cmd;
    public int tracemask;

    // results (out)
    //public float maxSpeed;
    // TODO: Add list for touched entities
    public Vector3f groundNormal = null;
    public boolean onGround;
    public int blocked = 0; // 1 = step/wall, 2 = slope
    private ITrace traceImplementation;
    Vector3f forward = new Vector3f();
    Vector3f up = new Vector3f();
    Vector3f right = new Vector3f();
    boolean cropped;
    Vector3f wishdir;

    public MoveQuery(ITrace traceImpl) {
        traceImplementation = traceImpl;
    }

    public CollisionResult Trace(Vector3f start, Vector3f end, Vector3f mins, Vector3f maxs, int tracemask, int passEntityNum) {
        if(traceImplementation != null)
            return traceImplementation.Trace(start, end, mins, maxs, tracemask, passEntityNum);
        return null;
    }
}
