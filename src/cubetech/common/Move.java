package cubetech.common;

import cubetech.Game.Game;
import cubetech.collision.CollisionResult;
import cubetech.entities.Event;
import cubetech.misc.Ref;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class Move {
    private static int msec;
    private static float frametime;
//    private static float maxspeed = 500f; // max speed
    private static float accel = 8f;
    private static float friction = 4f;
    private static float spectator_friction = 2f;
    private static float stopspeed = 15f;
    private static Vector2f org_origin = new Vector2f();
    private static Vector2f org_velocity = new Vector2f();

    

    

    public class MoveType {
        public static final int NORMAL = 0;
        public static final int NOCLIP = 1;
        public static final int SPECTATOR = 2;
        public static final int DEAD = 3;
        public static final int EDITMODE = 4;
    }

    public static void Move(MoveQuery query) {
        int finaltime = query.cmd.serverTime;

        if(finaltime < query.ps.commandTime)
            return; // shouldn't happen

        if(finaltime > query.ps.commandTime + 1000)
            query.ps.commandTime = finaltime - 1000;

        // chop the move up if it is too long, to prevent framerate
	// dependent behavior
        while(query.ps.commandTime < finaltime) {
            int stepMsec = finaltime - query.ps.commandTime;
            if(stepMsec > 66)
                stepMsec = 66;
            query.cmd.serverTime = query.ps.commandTime + stepMsec;
            MoveSingle(query);
        }
    }

    private static void MoveSingle(MoveQuery query) {
        // determine the time
        msec = query.cmd.serverTime - query.ps.commandTime;
        if(msec < 1)
            msec = 1;
        if(msec > 200)
            msec = 200;
        query.ps.commandTime = query.cmd.serverTime;
        frametime = msec * 0.001f;
        DropTimers(query); // Decrement timers
        query.ps.UpdateViewAngle(query.cmd);

        if(query.ps.moveType == MoveType.DEAD)
            return; // Dead players can't move

        
        org_origin.x = query.ps.origin.x;
        org_origin.y = query.ps.origin.y;
        org_velocity.x = query.ps.velocity.x;
        org_velocity.y = query.ps.velocity.y;
        // Direction player wants to move
        Vector2f wishdir = new Vector2f();
        if(query.cmd.Left)
            wishdir.x -= 1f;
        if(query.cmd.Right)
            wishdir.x += 1f;
        if(query.cmd.Up)
            wishdir.y += 1f;
        if(query.cmd.Down)
            wishdir.y -= 1f;

        float len = (float)Math.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);
        if(len != 0f) {
            wishdir.x /= len;
            wishdir.y /= len;
        }

        wishdir.scale(Ref.cvars.Find("sv_speed").iValue);

        if(query.ps.moveType == MoveType.NORMAL)
            UpdateStepSound(query);

        Friction(query);

        WalkMove(wishdir, query);
    }

    static void Friction(MoveQuery pm) {
       float speed2 = (float)Math.sqrt(pm.ps.velocity.x * pm.ps.velocity.x + pm.ps.velocity.y * pm.ps.velocity.y);
       if(speed2 < 0.1f)
           return;

       float fric = friction;

       if(pm.ps.moveType == MoveType.SPECTATOR)
           fric = spectator_friction;

       float control = (speed2 < stopspeed ? stopspeed : speed2);
       float drop = control * fric * frametime;

       float newspeed = speed2 - drop;
       if(newspeed < 0)
           newspeed = 0;

       newspeed /= speed2;

       pm.ps.velocity.x *= newspeed;
       pm.ps.velocity.y *= newspeed;
   }

    static void WalkMove(Vector2f wishdir, MoveQuery pm) {
       // normalize
       float wishspeed = (float)Math.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);
       if(wishspeed > 0) {
           wishdir.x /= wishspeed;
           wishdir.y /= wishspeed;
       }
       int maxspeed = Ref.cvars.Find("sv_speed").iValue;
       if(wishspeed > maxspeed)
       {
           wishdir.x *= (maxspeed/wishspeed);
           wishdir.y *= (maxspeed/wishspeed);
           wishspeed = maxspeed;
       }

       float currentSpeed = Vector2f.dot(pm.ps.velocity, wishdir);
       float addSpeed = wishspeed  - currentSpeed;
       if(addSpeed > 0f)
       {
           float accelspeed = accel * frametime * wishspeed;
           if(accelspeed > addSpeed)
               accelspeed = addSpeed;

           pm.ps.velocity.x += accelspeed * wishdir.x;
           pm.ps.velocity.y += accelspeed * wishdir.y;
       }

       float speed2 = (float)Math.sqrt(pm.ps.velocity.x * pm.ps.velocity.x + pm.ps.velocity.y * pm.ps.velocity.y);
       if(speed2 < 1f)
       {
           pm.ps.velocity.x = 0f;
           pm.ps.velocity.y = 0f;
           return;
       }

       // If noclipping, just apply the velocity and be done with it
       if(pm.ps.moveType == MoveType.NOCLIP || pm.ps.moveType == MoveType.EDITMODE) {
           pm.ps.origin.x += pm.ps.velocity.x * frametime;
           pm.ps.origin.y += pm.ps.velocity.y * frametime;
           return;
       }
       
       int tries = 0;
       CollisionResult res = null;
       float timeLeft = frametime;
       do {

           float destx = pm.ps.origin.x + pm.ps.velocity.x * timeLeft;
           float desty = pm.ps.origin.y + pm.ps.velocity.y * timeLeft;

           res = pm.Trace(pm.ps.origin, new Vector2f(destx, desty), Game.PlayerMins, Game.PlayerMaxs, pm.tracemask, pm.ps.clientNum);

           // Move up
           pm.ps.origin.x += pm.ps.velocity.x * timeLeft * res.frac;
           pm.ps.origin.y += pm.ps.velocity.y * timeLeft * res.frac;

           timeLeft *= 1f-res.frac;
           tries++;
           if(res.frac != 1f) {
               // Blocked

               // Clip velocity and try to move the remaining bit
               if(res.HitAxis.x == 1.0f)
                   pm.ps.velocity.x = 0f;
               else if(res.HitAxis.y == 1.0f)
                   pm.ps.velocity.y = 0f;
               else
                   pm.ps.velocity = new Vector2f();
           }
       } while( timeLeft > 0.0f && tries < 5);
   }

    private static void UpdateStepSound(MoveQuery pm) {
        if(pm.ps.stepTime > 0)
            return; // not time yet

        float speed = pm.ps.velocity.length();

        if(speed > 70f) {
            pm.ps.stepTime = 300;
            AddEvent(pm, Event.FOOTSTEP, 0);
        }
    }

    private static void AddEvent(MoveQuery pm, int event, int eventParam) {
        pm.ps.AddPredictableEvent(event, eventParam);
    }

    private static void DropTimers(MoveQuery pm) {
        pm.ps.stepTime -= msec;
        if(pm.ps.stepTime < 0)
            pm.ps.stepTime = 0;
    }
}
