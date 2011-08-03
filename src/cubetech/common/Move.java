package cubetech.common;

import cubetech.common.items.Weapon;
import cubetech.common.items.WeaponState;
import cubetech.Game.Game;
import cubetech.collision.CollisionResult;
import cubetech.common.items.WeaponItem;
import cubetech.entities.Event;
import cubetech.input.Input;
import cubetech.input.PlayerInput;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;


import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class Move {
    // Constants loaded
    private static final float STOP_EPSILON = 0.1f;
    private static float acceleration = 8f;
    private static float airaccelerate = 10;
    private static float friction = 4f;
    private static float stopspeed = 15f;
    private static float gravity;
    private static int speed = 100;
    private static float jumpvel = 100;
    private static float stepheight = 10;

    private static final float MIN_WALK_NORMAL = 0.7f;
    private static final float OVERCLIP = 1.0001f;

    private static int msec;
    private static float frametime;
    public class MoveType {
        public static final int NORMAL = 0;
        public static final int NOCLIP = 1;
        public static final int SPECTATOR = 2;
        public static final int DEAD = 3;
        public static final int EDITMODE = 4;
    }

    private static void ContinueAnim(Animations animation, MoveQuery q) {
        ContinueAnim(animation, q, false);
    }
    private static void ContinueAnim(Animations animation, MoveQuery q, boolean forceSame) {
        if((q.ps.animation & ~128) == animation.ordinal() && !forceSame) return;

        if(q.ps.animTime > 0) return; // a priority animation is playing

        if(q.ps.moveType == MoveType.DEAD) return;

        // Set animation + togglebit
        q.ps.animation = ((q.ps.animation & 128) ^ 128) | animation.ordinal();
    }

    private static void setMovementDir(MoveQuery query) {
        if(query.cmd.Forward || query.cmd.Back || query.cmd.Right || query.cmd.Left) {
            if (!query.cmd.Right && !query.cmd.Left && query.cmd.Forward ) {
                query.ps.moveDirection = 0;
            } else if ( query.cmd.Left && query.cmd.Forward) {
                query.ps.moveDirection = 1;
            } else if ( query.cmd.Left && !query.cmd.Back && !query.cmd.Forward ) {
                query.ps.moveDirection = 2;
            } else if ( query.cmd.Left && query.cmd.Back ) {
                query.ps.moveDirection = 3;
            } else if ( !query.cmd.Right && !query.cmd.Left && query.cmd.Back ) {
                query.ps.moveDirection = 4;
            } else if ( query.cmd.Right && query.cmd.Back ) {
                query.ps.moveDirection = 5;
            } else if ( query.cmd.Right && !query.cmd.Back && !query.cmd.Forward ) {
                query.ps.moveDirection = 6;
            } else if ( query.cmd.Right && query.cmd.Forward) {
                query.ps.moveDirection = 7;
            }
        } else {
            // if they aren't actively going directly sideways,
            // change the animation to the diagonal so they
            // don't stop too crooked
            if(query.ps.moveDirection == 2) {
                query.ps.moveDirection = 1;
            } else if(query.ps.moveDirection == 6) {
                query.ps.moveDirection = 7;
            }
        }
    }

    public static void Move(MoveQuery query) {


        int finaltime = query.cmd.serverTime;

        if(finaltime < query.ps.commandTime)
            return; // shouldn't happen

        SecTag s = Profiler.EnterSection(Sec.MOVE);

        if(finaltime > query.ps.commandTime + 1000)
            query.ps.commandTime = finaltime - 1000;

        try {
            gravity = Ref.cvars.Find("sv_gravity").fValue;
            speed = Ref.cvars.Find("sv_speed").iValue;
            jumpvel = Ref.cvars.Find("sv_jumpvel").fValue;
            acceleration = Ref.cvars.Find("sv_acceleration").fValue;
            airaccelerate = Ref.cvars.Find("sv_airaccelerate").fValue;
            friction = Ref.cvars.Find("sv_friction").fValue;
            stopspeed = Ref.cvars.Find("sv_stopspeed").fValue;
            stepheight = Ref.cvars.Find("sv_stepheight").fValue;
        } catch(Exception ex) {
            Common.LogDebug("Missing Move variables: %s", Common.getExceptionString(ex));
        }

        Vector3f start = new Vector3f(query.ps.origin);
        // chop the move up if it is too long, to prevent framerate
	// dependent behavior
        int totalMsec = finaltime - query.ps.commandTime;
        while(query.ps.commandTime < finaltime) {
            int stepMsec = finaltime - query.ps.commandTime;
            if(stepMsec > 66)
                stepMsec = 66;
            query.cmd.serverTime = query.ps.commandTime + stepMsec;
            MoveSingle(query);
            query.cropped = false;
        }
        Vector3f end = new Vector3f(query.ps.origin);

        Vector3f.sub(start, end, start);
        start.y *= 0.5f; // don't let y velocity impact move animation too much
        float movelen = start.length() * 15;
        query.ps.movetime += (int)movelen;

        s.ExitSection();
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

        query.ps.UpdateViewAngle(query.cmd);

        Helper.AngleVectors(query.ps.viewangles, query.forward, query.right, query.up);


        if(query.ps.moveType == MoveType.DEAD)
            return; // Dead players can't move

        if(query.ps.moveType == MoveType.NOCLIP) {
            NoclipMove(query);
            DropTimers(query); // Decrement timers
            return;
        }
        
        // Direction player wants to move
        Vector3f wishdir = new Vector3f();
        // Handle horizontal wish direction
        if(query.cmd.Left)
            wishdir.y -= 1f;
        if(query.cmd.Right)
            wishdir.y += 1f;
        if(query.cmd.Forward)
            wishdir.x += 1f;
        if(query.cmd.Back)
            wishdir.x -= 1f;

        wishdir.scale(speed);
        float len = wishdir.length();
        if(len > speed) {
            wishdir.scale(speed/len);
        }
        query.wishdir = wishdir;

        groundTrace(query);
        DropTimers(query); // Decrement timers

        setMovementDir(query);

        // handle ducking
        handleDuck(query);

        if(query.onGround) {
            // Walk
            WalkMove(wishdir, query);
            
        } else {
            AirMove(wishdir, query);
        }

//        groundTrace(query);

        if(!query.onGround) {
            query.ps.velocity.z -= gravity * frametime * 0.5f;
        }

        // animate
        if(query.onGround) {
            if(query.ps.velocity.x * query.ps.velocity.x + query.ps.velocity.y * query.ps.velocity.y > 10f)  {
                if(query.ps.ducked) {
                    ContinueAnim(Animations.CROUCH_WALK, query);
                } else {
                    ContinueAnim(Animations.WALK, query);
                }
            }
            else if(query.ps.velocity.z == 0f) {
                if(query.ps.ducked) {
                    ContinueAnim(Animations.CROUCH_IDLE, query);
                } else {
                    ContinueAnim(Animations.IDLE, query);
                }
            }
        }

        // weapon
        weapons(query);
    }

    static void handleDuck(MoveQuery pm) {
        boolean duckPressed = !pm.ps.oldButtons[2] && pm.cmd.buttons[2];
        boolean duckReleased = pm.ps.oldButtons[2] && !pm.cmd.buttons[2];
        System.arraycopy(pm.cmd.buttons, 0, pm.ps.oldButtons, 0, pm.ps.oldButtons.length);
        

        if(pm.ps.stats.Health <= 0) {
            // Dead
            if(pm.ps.ducked) {
                finishUnduck(pm);
            }
            return;
        }

        if(pm.ps.ducked && !pm.cropped) {
            pm.wishdir.scale(0.5f);
            pm.cropped = true;
        }

        // Holding duck, in process of ducking or fully ducked?
        if(pm.cmd.buttons[2] || pm.ps.ducking || pm.ps.ducked) {
            // holding duck
            if(pm.cmd.buttons[2]) { 
                // Just pressed duck, and not fully ducked?
                if(duckPressed && !pm.ps.ducked) {
                    pm.ps.ducking = true;
                    pm.ps.ducktime = 1000;
                }

                float duckms = Math.max(0, 1000 - pm.ps.ducktime);
                float ducks = duckms / 1000f;

                // doing a duck movement? (ie. not fully ducked?)
                if(pm.ps.ducking) {
                    // Finish ducking immediately if duck time is over or not on ground
                    if(ducks > 0.4f || !pm.onGround || pm.ps.ducked) {
                        finishDuck(pm);
                    } else {
                        // Calc parametric time
                        float duckFrac = Helper.SimpleSpline(ducks/0.4f);
                        setDuckedEyeOffset(pm, duckFrac);
                    }
                }
            } else {
                if(duckReleased && pm.ps.ducked) {
                    // start a unduck
                    pm.ps.ducktime = 1000;
                    pm.ps.ducking = true;
                }

                float duckms = Math.max(0, 1000 - pm.ps.ducktime);
                float ducks = duckms / 1000f;

                // try to unduck
                if(canUnduck(pm)) {
                    if(pm.ps.ducked || pm.ps.ducking) {
                        // Finish ducking immediately if duck time is over or not on ground
                        if(ducks > 0.2f || !pm.onGround) {
                            finishUnduck(pm);
                        } else {
                            // Calc parametric time
                            float duckFrac = Helper.SimpleSpline(1.0f - (ducks / 0.2f));
                            setDuckedEyeOffset(pm, duckFrac);
                        }
                    }
                } else {
                    // Still under something where we can't unduck, so make sure we reset this timer so
                    //  that we'll unduck once we exit the tunnel, etc.
                    pm.ps.ducktime = 1000;
                }
            }
        }
    }

    static boolean canUnduck(MoveQuery pm) {
        Vector3f start = new Vector3f(pm.ps.origin);
        if(!pm.onGround) {
            // If in air an letting go of croush, make sure we can offset origin to make
                //  up for uncrouching
            Vector3f standingHull = Vector3f.sub(Game.PlayerMaxs, Game.PlayerMins, null);
            Vector3f duckedHull = Vector3f.sub(Game.PlayerDuckedMaxs, Game.PlayerDuckedMins, null);

            Vector3f.sub(standingHull, duckedHull, duckedHull);
            duckedHull.scale(-0.5f);
            Vector3f.add(start, duckedHull, start);
        }

        boolean saveDuck = pm.ps.ducked;
        pm.ps.ducked = false;
        CollisionResult res = TracePlayerBBox(pm, pm.ps.origin, start, Content.MASK_PLAYERSOLID);
        pm.ps.ducked = saveDuck;
        if(!res.startsolid && res.frac == 1f) {
            return true;
        }
        return false;
    }

    static void setDuckedEyeOffset(MoveQuery pm, float frac) {
        float duckedView = Game.PlayerDuckedHeight;
        float standingView = Game.PlayerViewHeight;

        pm.ps.viewheight = (int)((duckedView * frac) + (standingView * (1f-frac)));
    }

    static void finishDuck(MoveQuery pm) {
        boolean wasDucked = pm.ps.ducked;
        pm.ps.ducked = true;
        pm.ps.ducking = false;
        pm.ps.viewheight = (int) Game.PlayerDuckedHeight;

        if(pm.onGround) {

        } else if(!wasDucked) {
            Vector3f standingHull = Vector3f.sub(Game.PlayerMaxs, Game.PlayerMins, null);
            Vector3f duckedHull = Vector3f.sub(Game.PlayerDuckedMaxs, Game.PlayerDuckedMins, null);

            Vector3f.sub(standingHull, duckedHull, duckedHull);
            duckedHull.scale(0.5f);
            Vector3f.add(pm.ps.origin, duckedHull, pm.ps.origin);
        }
        groundTrace(pm);
    }

    static void finishUnduck(MoveQuery pm) {
        if(!pm.onGround) {
            // If in air an letting go of croush, make sure we can offset origin to make
                //  up for uncrouching
            Vector3f standingHull = Vector3f.sub(Game.PlayerMaxs, Game.PlayerMins, null);
            Vector3f duckedHull = Vector3f.sub(Game.PlayerDuckedMaxs, Game.PlayerDuckedMins, null);

            Vector3f.sub(standingHull, duckedHull, duckedHull);
            duckedHull.scale(-0.5f);
            Vector3f.add(pm.ps.origin, duckedHull, pm.ps.origin);
        }

        pm.ps.ducked = false;
        pm.ps.ducking = false;
        pm.ps.ducktime = 0;
        pm.ps.viewheight = (int) Game.PlayerViewHeight;

        groundTrace(pm);
    }

    static void beginWeaponChange(MoveQuery pm, Weapon newWeapon) {
        if(!pm.ps.stats.hasWeapon(newWeapon)) return;

        if(pm.ps.weaponState == WeaponState.DROPPING) return;

        AddEvent(pm, Event.CHANGE_WEAPON, 0);
        pm.ps.weaponState = WeaponState.DROPPING;
        pm.ps.weaponTime += Ref.common.items.getWeapon(pm.ps.weapon).getDropTime();
        // start animation
    }

    static void finishWeaponChange(MoveQuery pm) {
        Weapon w = pm.cmd.weapon;

        // maybe fix (make stats.getFirstWeapon())
        if(!pm.ps.stats.hasWeapon(w)) w = Weapon.NONE;
        pm.ps.weapon = w;
        pm.ps.weaponState = WeaponState.RAISING;
        pm.ps.weaponTime += Ref.common.items.getWeapon(w).getRaiseTime();
    }

    static void weapons(MoveQuery pm) {
        if(pm.ps.stats.Health <= 0) return; // dead

        // make weapon function
        if(pm.ps.weaponTime > 0) {
            pm.ps.weaponTime -= msec;
        }

        // check for weapon change
	// can't change if weapon is firing, but can change
	// again if lowering or raising
        if(pm.ps.weaponTime <= 0 || pm.ps.weaponState != WeaponState.FIRING) {
            if(pm.ps.weapon != pm.cmd.weapon) {
                beginWeaponChange(pm, pm.cmd.weapon);
            }
        }

        if(pm.ps.weaponTime > 0) return; // not yet..

        // change weapon if time
        if(pm.ps.weaponState == WeaponState.DROPPING) {
            finishWeaponChange(pm);
            return;
        }

        if(pm.ps.weaponState == WeaponState.RAISING) {
            pm.ps.weaponState = WeaponState.READY;
            return;
        }

        // check for fire
        boolean primary = pm.cmd.isButtonDown(0);
        boolean secondary = pm.cmd.isButtonDown(1);
        if(!primary && !secondary) {
            pm.ps.weaponTime = 0;
            pm.ps.weaponState = WeaponState.READY;
            return;
        }

        if(pm.ps.weapon == Weapon.NONE) return; // firing blanks :O

        pm.ps.weaponState = WeaponState.FIRING;

        // FIX: Handle ammo for primary & secondary
        // check for out of ammo
        if(pm.ps.stats.getAmmo(pm.ps.weapon) == 0) {
            AddEvent(pm, Event.NO_AMMO, 0);
            pm.ps.weaponTime += 300;
            return;
        }

        WeaponItem w = Ref.common.items.getWeapon(pm.ps.weapon);

        // take an ammo away if not infinite
        pm.ps.stats.addAmmo(pm.ps.weapon, -1);

        // fire weapon
        if(primary) {
            // primary fire
            AddEvent(pm, Event.FIRE_WEAPON, 0);
            pm.ps.weaponTime += w.getFireTime();
        } else if(secondary) {
            // Secondary fire
            AddEvent(pm, Event.FIRE_WEAPON_ALT, 0);
            pm.ps.weaponTime += w.getAltFireTime();
        }
    }
//    static void Jump(MoveQuery pm) {
//        pm.ps.jumpTime = jumpmsec;
//        pm.ps.velocity.y = jumpvel;
//        if(pm.onGround)
//            AddEvent(pm, Event.FOOTSTEP, 0);
//        AddEvent(pm, Event.JUMP, 0);
//
//    }

    static boolean groundTrace(MoveQuery pm) {
        Vector3f end = new Vector3f(pm.ps.origin);
        float delta = -1f;

        
        if(pm.ps.velocity.z < 0) {
            delta += pm.ps.velocity.z * frametime;
        }
        end.z += delta;
        //end.z = 870;


        CollisionResult trace = TracePlayerBBox(pm, pm.ps.origin, end, pm.tracemask);


        if(trace.frac == 1f)
        {
            groundTraceMissed(pm);
            pm.onGround = false;
            return false;
        }
        pm.groundNormal = trace.hitAxis;
        
        // check if getting thrown off the ground
        if(pm.ps.velocity.z > 0 && Vector3f.dot(pm.ps.velocity, pm.groundNormal) > 10) {
            Common.LogDebug("Kickoff");
            pm.onGround = false;
            pm.groundNormal = null;
            return false;
        }

        // move down
        if(trace.frac != 0f) {
            pm.ps.origin.z += delta * trace.frac;
        }

        // slopes that are too steep will not be considered onground
        if(pm.groundNormal.z < MIN_WALK_NORMAL) {
            pm.onGround = false;
            pm.groundNormal = null;
            return false;
        }

        pm.onGround = true;

//        if(pm.ps.groundEntityNum == Common.ENTITYNUM_NONE) {
//            // just hit the ground
//            CrashLand(query);
//
//
//        }
        return true;
    }

    static void groundTraceMissed(MoveQuery pm) {
        pm.onGround = false;
        pm.groundNormal = null;
        
    }

    private static Vector3f fric_vel = new Vector3f();
    static void Friction(MoveQuery pm) {
       fric_vel.set(pm.ps.velocity);

       // ignore slope movement
       if(pm.onGround) {    
           fric_vel.z = 0;
       }

       float speed2 = fric_vel.length();
       if(speed2 < 0.1f) {
           pm.ps.velocity.x = 0;
           pm.ps.velocity.y = 0;
           return;
       }

       // Apply ground friction
       float drop = 0;
       if(pm.onGround) {
           float control = (speed2 < stopspeed ? stopspeed : speed2);
           drop = control * friction * frametime;
       }

       float newspeed = speed2 - drop;
       if(newspeed < 0)
           newspeed = 0;

       newspeed /= speed2;

       pm.ps.velocity.scale(newspeed);
   }
    
    static void FrictionSpecial(MoveQuery pm) {
        if(!pm.cmd.Left)
            return;
        
       float speed2 = (float)Math.sqrt(pm.ps.velocity.x * pm.ps.velocity.x);
       if(speed2 < 0.1f)
           return;

       float fric = friction;

       float control = speed2;
       float drop = control * fric * frametime;

       float newspeed = speed2 - drop;
       if(newspeed < 0)
           newspeed = 0;

       newspeed /= speed2;

       pm.ps.velocity.x *= newspeed;
//       pm.ps.velocity.y *= newspeed;
   }

    static void AirAccelerate(MoveQuery pm, Vector3f wishdir, float wishspeed, float accel) {
        // Cap speed
        if(wishspeed > 30)
            wishspeed = 30;

        Accelerate(pm, wishdir, wishspeed, accel);
    }

    static void Accelerate(MoveQuery pm, Vector3f wishdir, float wishspeed, float accel) {
        // Determine veer amount
        float currentSpeed = Vector3f.dot(pm.ps.velocity, wishdir);

        // See how much to add
        float addSpeed = wishspeed  - currentSpeed;

        // If not adding any, done.
        if(addSpeed <= 0f)
            return;
       
        // Determine acceleration speed after acceleration
        float accelspeed = accel * frametime * wishspeed;

        // Cap it
        if(accelspeed > addSpeed)
            accelspeed = addSpeed;

        // Adjust pmove vel.
        Helper.VectorMA(pm.ps.velocity, accelspeed, wishdir, pm.ps.velocity);
    }

    static boolean checkJump(MoveQuery pm) {
        if(!pm.cmd.Up)
            return false;

        pm.onGround =false;
        pm.groundNormal = null;
        pm.ps.velocity.z = jumpvel;
        
        ContinueAnim(Animations.JUMP, pm, true);
        return true;
    }

    static void AirMove(Vector3f move, MoveQuery pm) {
        pm.ps.velocity.z -= gravity * frametime * 0.5f;
        
        pm.forward.z = 0;
        pm.right.z = 0;
        Helper.Normalize(pm.forward);
        Helper.Normalize(pm.right);

        Vector3f wishvel = new Vector3f();
        wishvel.x = pm.forward.x * move.x + pm.right.x * move.y;
        wishvel.y = pm.forward.y * move.x + pm.right.y * move.y;

        Vector3f wishdir = new Vector3f(wishvel);
        float wishspeed = Helper.Normalize(wishdir);
        //wishspeed = speed;
        if(wishspeed > speed) {
            wishvel.scale(speed / wishspeed);
            wishspeed = speed;
        }

        AirAccelerate(pm, wishdir, wishspeed, airaccelerate);

        SlideMove(pm);
    }

    static void WalkMove(Vector3f move, MoveQuery pm) {
        

        // Check for jump
        if(checkJump(pm)) {
            AirMove(move, pm);
            return;
        }

        // Add Gravity
        

        // Apply friction
        if(pm.onGround) {
            pm.ps.velocity.z = 0f;
        }
        Friction(pm);

        // Project down to flat plane
        pm.forward.z = 0;
        pm.right.z = 0;

        // project the forward and right directions onto the ground plane
        ClipVelocity(pm.forward, pm.forward, pm.groundNormal, OVERCLIP);
        ClipVelocity(pm.right, pm.right, pm.groundNormal, OVERCLIP);

        Helper.Normalize(pm.forward);
        Helper.Normalize(pm.right);

        Vector3f wishvel = new Vector3f();
        wishvel.x = pm.forward.x * move.x + pm.right.x * move.y;
        wishvel.y = pm.forward.y * move.x + pm.right.y * move.y;
        wishvel.z = pm.forward.z * move.x + pm.right.z * move.y;

        Vector3f wishdir = new Vector3f(wishvel);
        float wishspeed = Helper.Normalize(wishdir);
        

        Accelerate(pm, wishdir, wishspeed, acceleration);

        float vel = pm.ps.velocity.length();

        // slide along the ground plane
        ClipVelocity(pm.ps.velocity, pm.ps.velocity, pm.groundNormal, OVERCLIP);


        // don't decrease velocity when going up or down a slope
        Helper.Normalize(pm.ps.velocity);
        pm.ps.velocity.scale(vel);

        // don't do anything if standing still
        if(pm.ps.velocity.x == 0 && pm.ps.velocity.y == 0 && pm.ps.velocity.z == 0) {
            return;
        }

        StepSlideMove(pm, false);


   }

    private static void StepSlideMove(MoveQuery pm, boolean gravity) {
        if(SlideMove(pm) == 0) {
            return; // we got exactly where we wanted to go first try
        }
    }

    private static int SlideMove(MoveQuery pm) {
        int bumpcount = 4;
        Vector3f orgVel = new Vector3f(pm.ps.velocity);
        Vector3f priVel = new Vector3f(pm.ps.velocity);
        Vector3f[] planes = new Vector3f[5];
        Vector3f newVel = new Vector3f();
        int numplanes = 0;
        int blocked = 0;

        float allFrac = 0;
        float timeLeft = frametime;
        Vector3f end = new Vector3f();
        for (int bump= 0; bump < bumpcount; bump++) {
            if(pm.ps.velocity.lengthSquared() == 0) {
                break;
            }

            // Assume we can move all the way from the current origin to the
            //  end point.
            end.set(pm.ps.velocity);
            end.scale(timeLeft);
            Vector3f.add(end, pm.ps.origin, end);

            // See if we can make it from origin to end point.
            CollisionResult res = TracePlayerBBox(pm, pm.ps.origin, end, pm.tracemask);
            allFrac += res.frac;

            // If we started in a solid object, or we were in solid space
            //  the whole way, zero out our velocity and return that we
            //  are blocked by floor and wall.
            // FIX

            // If we moved some portion of the total distance, then
            //  copy the end position into the pmove->origin and
            //  zero the plane counter.
            if(res.frac > 0) {
                // actually covered some distance
                Helper.VectorMA(res.start, res.frac, res.delta, pm.ps.origin);
                orgVel.set(pm.ps.velocity);
                numplanes = 0;
            }            

            // If we covered the entire distance, we are done
            //  and can return.
            if(res.frac == 1f)
                break;
            
            // If the plane we hit has a high z component in the normal, then
            //  it's probably a floor
            if(res.hitAxis.z > MIN_WALK_NORMAL)
                blocked |= 1; // floor
            if(res.hitAxis.z == 0)
                blocked |= 2; // wall/step

            timeLeft -= timeLeft * res.frac;

            // Did we run out of planes to clip against?
            if(numplanes >= 5) {
                pm.ps.velocity.set(0,0,0);
                break;
            }

            // Set up next clipping plane
            planes[numplanes++] = res.hitAxis;

            // modify original_velocity so it parallels all of the clip planes
            if(numplanes == 1 && !pm.onGround) {
                for (int i= 0; i < numplanes; i++) {
                    if(planes[i].z > MIN_WALK_NORMAL) {
                        // floor or slope
                        ClipVelocity(orgVel, newVel, planes[i], 1);
                        orgVel.set(newVel);
                    } else {
                        ClipVelocity(orgVel, newVel, planes[i], 1);
                    }
                }
                pm.ps.velocity.set(newVel);
                orgVel.set(newVel);
            } else {
                int i;
                for (i= 0; i < numplanes; i++) {
                    ClipVelocity(orgVel, pm.ps.velocity, planes[i], 1);
                    int j;
                    for (j= 0; j < numplanes; j++) {
                        if(j != i) {
                            // Are we now moving against this plane?
                            if(Vector3f.dot(pm.ps.velocity, planes[j]) < 0f)
                                break; // not ok
                        }
                    }
                    if(j == numplanes) // Didn't have to clip, so we're ok
                        break;
                }

                // Did we go all the way through plane set
                if(i != numplanes) {
                    // go along this plane
                    // pmove->velocity is set in clipping call, no need to set again.
                } else {
                    // go along the crease
                    if(numplanes != 2) {
                        pm.ps.velocity.set(0,0,0);
                        break;
                    }
                    Vector3f dir = Vector3f.cross(planes[0], planes[1], null);
                    float d = Vector3f.dot(dir, pm.ps.velocity);
                    pm.ps.velocity.set(dir).scale(d);
                }

                //
                // if original velocity is against the original velocity, stop dead
                // to avoid tiny occilations in sloping corners
                //
                float vdiff = Vector3f.dot(pm.ps.velocity, priVel);
                if(vdiff <= 0f) {
                    pm.ps.velocity.set(0,0,0);
                    break;
                }
            }
        }

        if(allFrac == 0) {
            pm.ps.velocity.set(0,0,0);
        }

        return blocked;
    }

    // try a series of up, forward, down moves
   private static boolean TryStepMove(MoveQuery pm) {
       // Up
       Vector3f up = new Vector3f(pm.ps.origin);
       up.y += stepheight;
       CollisionResult res = pm.Trace(pm.ps.origin, up, Game.PlayerMins, Game.PlayerMaxs, pm.tracemask, pm.ps.clientNum);
       if(res.frac != 1f) {
           return false;
       }

       // Forward
       pm.ps.origin.y += stepheight;
       up.set(pm.ps.velocity);
       up.scale(frametime);
       Vector3f.add(pm.ps.origin, up, up);

       res = pm.Trace(pm.ps.origin, up, Game.PlayerMins, Game.PlayerMaxs, pm.tracemask, pm.ps.clientNum);
       if(res.frac != 1f) {
           return false;
       }

       // Press down
       up.set(pm.ps.velocity);
       up.scale(frametime);
       Vector3f.add(pm.ps.origin, up, pm.ps.origin);
       up.set(pm.ps.origin);
       up.y -= stepheight;
       res = pm.Trace(pm.ps.origin, up, Game.PlayerMins, Game.PlayerMaxs, pm.tracemask, pm.ps.clientNum);
       if(res.frac == 1f) {
           return false;
       }

       pm.ps.origin.y -= res.frac * stepheight;

       return true; // success
   }

   private static void NoclipMove(MoveQuery pm) {
        // friction
        float spd = pm.ps.velocity.length();
        if(spd < 1) {
            pm.ps.velocity.set(0,0,0);
        } else {
            stopspeed = 1;
            float fric = friction * 0.35f;
            float control = spd < stopspeed ? stopspeed : spd;
            float drop = control * fric * frametime;

            float newspeed = spd - drop;
            if(newspeed < 0)
                newspeed = 0;
            newspeed /= spd;
            pm.ps.velocity.scale(newspeed);
        }

        // accelerate
        float forwardmove = 0, sidemove = 0;
        // Handle horizontal wish direction
        if(pm.cmd.Left)
            sidemove -= 1f;
        if(pm.cmd.Right)
            sidemove += 1f;
        if(pm.cmd.Forward)
            forwardmove += 1f;
        if(pm.cmd.Back)
            forwardmove -= 1f;

        Vector3f wishvel = new Vector3f();
        wishvel.x = pm.forward.x * forwardmove + pm.right.x * sidemove;
        wishvel.y = pm.forward.y * forwardmove + pm.right.y * sidemove;
        wishvel.z = pm.forward.z * forwardmove + pm.right.z * sidemove;

        Helper.Normalize(wishvel);

        Accelerate(pm, wishvel, speed*2, acceleration*0.35f);

        Vector3f delta = new Vector3f(pm.ps.velocity);
        delta.scale(frametime);
        Vector3f.add(delta, pm.ps.origin, delta);
//        if(pm.Trace(pm.ps.origin, delta, new Vector3f(-5,-5,-5), new Vector3f(5,5,5), 0,0).hit)
//            return;

        Helper.VectorMA(pm.ps.origin, frametime, pm.ps.velocity, pm.ps.origin);
    }

    private static CollisionResult TracePlayerBBox(MoveQuery pm, Vector3f origin, Vector3f end, int tracemask) {
        return pm.Trace(origin, end, // select bounding by duck-state
                pm.ps.ducked ? Game.PlayerDuckedMins : Game.PlayerMins,
                pm.ps.ducked ? Game.PlayerDuckedMaxs : Game.PlayerMaxs,
                tracemask, pm.ps.clientNum);
    }

    // result = in - 2*n (n*in);
    private static void ClipVelocity(Vector3f in, Vector3f out, Vector3f normal, float overbounce) {
        float dot = Vector3f.dot(normal, in);
        if(dot < 0) {
            dot *= overbounce;
        } else {
            dot /= overbounce;
        }

        out.x = in.x - normal.x * dot;
        if(out.x > -STOP_EPSILON && out.x < STOP_EPSILON) out.x = 0;
        out.y = in.y - normal.y * dot;
        if(out.y > -STOP_EPSILON && out.y < STOP_EPSILON) out.y = 0;
        out.z = in.z - normal.z * dot;
        if(out.z > -STOP_EPSILON && out.z < STOP_EPSILON) out.z = 0;
    }

    private static void UpdateStepSound(MoveQuery pm) {
        if(pm.onGround)
                pm.ps.stepTime -= (int)(Math.abs(pm.ps.velocity.x)*frametime*200);

        if(pm.ps.stepTime > 0) {
            return; // not time yet
        }

//        float speed = pm.ps.velocity.length();

        if(pm.onGround) {
            pm.ps.stepTime = 6000;
            AddEvent(pm, Event.FOOTSTEP, 0);
        }
    }

    private static void AddEvent(MoveQuery pm, Event event, int eventParam) {
        pm.ps.AddPredictableEvent(event, eventParam);
    }

    private static void DropTimers(MoveQuery pm) {
//        pm.ps.stepTime -= msec;
        pm.ps.jumpTime -= msec;
        pm.ps.ducktime -= msec;
        if(pm.ps.stepTime < 0)
            pm.ps.stepTime = 0;
        if(pm.ps.jumpTime < 0)
            pm.ps.jumpTime = 0;
        if(pm.ps.ducktime < 0)
            pm.ps.ducktime = 0;

    }
}
