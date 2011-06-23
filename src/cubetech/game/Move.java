/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.game;

import cubetech.Helper;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;

/**
 * Static movement helper methods.
 * @author mads
 */
public class Move {
    private static final float STOP_EPSILON = 0.0001f;

    public static void Accelerate(Vector2f velocity, Vector2f wishdir, float wishspeed, float accel) {
        // Determine veer amount
        float currentSpeed = Vector2f.dot(velocity, wishdir);

        // See how much to add
        float addSpeed = wishspeed  - currentSpeed;

        // If not adding any, done.
        if(addSpeed <= 0f)
            return;

        // Determine acceleration speed after acceleration
        float accelspeed = accel * Ref.loop.frameMsec * 0.001f * wishspeed;

        // Cap it
        if(accelspeed > addSpeed)
            accelspeed = addSpeed;

        // Adjust pmove vel.
        Helper.VectorMA(velocity, accelspeed, wishdir, velocity);
    }

    
   public static void Friction(Vector2f velocity, float friction, float stopspeed) {
       float speed2 = velocity.length();
       if(speed2 < 0.1f) {
           velocity.x = 0;
           velocity.y = 0;
           return;
       }

       // Apply ground friction
       float control = (speed2 < stopspeed ? stopspeed : speed2);
       float drop = control * friction * Ref.loop.frameMsec * 0.001f;
       

       float newspeed = speed2 - drop;
       if(newspeed < 0)
           newspeed = 0;

       newspeed /= speed2;

       velocity.scale(newspeed);
   }

    // Used for bouncing stuff off wall, etc..
    // result = in - 2*n (n*in);
    public static void ClipVelocity(Vector2f in, Vector2f out, Vector2f normal, float overbounce) {
        float dot = Vector2f.dot(normal, in);
        if(dot < 0) {
            dot *= overbounce;
        } else {
            dot /= overbounce;
        }

        out.x = in.x - normal.x * dot;
        if(out.x > -STOP_EPSILON && out.x < STOP_EPSILON) out.x = 0;
        out.y = in.y - normal.y * dot;
        if(out.y > -STOP_EPSILON && out.y < STOP_EPSILON) out.y = 0;
    }
}
