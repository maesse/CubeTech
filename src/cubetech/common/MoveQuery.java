/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import cubetech.collision.CollisionResult;
import cubetech.input.PlayerInput;
import org.lwjgl.util.vector.Vector2f;

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
    public Vector2f groundNormal = null;
    public boolean onGround;
    public int blocked = 0; // 1 = step/wall, 2 = slope
    private ITrace traceImplementation;
    

    public MoveQuery(ITrace traceImpl) {
        traceImplementation = traceImpl;
    }

    public CollisionResult Trace(Vector2f start, Vector2f end, Vector2f mins, Vector2f maxs, int tracemask, int passEntityNum) {
        if(traceImplementation != null)
            return traceImplementation.Trace(start, end, mins, maxs, tracemask, passEntityNum);
        return null;
    }
}
