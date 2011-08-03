package cubetech.entities;

import cubetech.Game.Gentity;
import cubetech.collision.CollisionResult;
import cubetech.common.Common;

import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.IReachedMethod;
import cubetech.common.IThinkMethod;
import cubetech.common.IUseMethod;
import cubetech.common.Trajectory;
import cubetech.misc.Ref;
import cubetech.server.SvFlags;
import cubetech.spatial.SectorQuery;
import org.lwjgl.util.vector.Vector;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class Mover {
    MoverState moverState;
    int soundPos1;
    int soundPos2;
    int sound1to2;
    int sound2to1;
    int soundloop;
    
    Gentity activator;
    Vector3f pos1 = new Vector3f(), pos2 = new Vector3f();

    Gentity chain;
    Gentity teamchain;
    Gentity teammaster;

    boolean takedamage;
    boolean teamslave;

    Vector3f movedir = new Vector3f(1,0,0);
    
    Gentity ent;

    public enum MoverState {
        // stationary
        POS1,
        POS2,
        // currently moving
        _1TO2,
        _2TO1
    }

    public void initMover(Gentity ent) {
        this.ent = ent;

        ent.use =Use_BinaryMover;
        ent.reached = Reached_BinaryMover;
        moverState = MoverState.POS1;
//        ent.r.svFlags.add(SvFlags.USE_CURRENT_ORIGIN);
        ent.s.eType = EntityType.MOVER;
        ent.r.currentOrigin.set(pos1);
        ent.Link();

        ent.s.pos.type = Trajectory.STATIONARY;
        ent.s.pos.base.set(pos1);

        // calculate time to reach second position from speed
        Vector3f move = new Vector3f();
        Vector3f.sub(pos2, pos1, move);
        float len = Helper.Normalize(move);

        if(ent.speed == 0)
            ent.speed = 100;
        ent.s.pos.delta.x = move.x * ent.speed;
        ent.s.pos.delta.y = move.y * ent.speed;
        ent.s.pos.delta.z = move.z * ent.speed;
        ent.s.pos.duration = (int)(len * 1000 / ent.speed);
        if(ent.s.pos.duration <= 0)
            ent.s.pos.duration = 1;
    }

    private static Gentity moverPush(Gentity pusher, Vector3f move, Vector3f amove) {
        // mins/maxs are the bounds at the destination
	// totalMins / totalMaxs are the bounds for the entire move
        Vector3f totalMins = new Vector3f();
        Vector3f totalMaxs = new Vector3f();
        Vector3f mins = new Vector3f();
        Vector3f maxs = new Vector3f();
            
        if ( pusher.r.currentAngles.x != 0 || pusher.r.currentAngles.y != 0 || pusher.r.currentAngles.z != 0
		|| amove.x != 0|| amove.y != 0 || amove.z != 0 ) {
            float radius = Helper.RadiusFromBounds(pusher.r.mins, pusher.r.maxs);
            mins.set(pusher.r.currentOrigin.x + move.x - radius,
                                        pusher.r.currentOrigin.y + move.y - radius,
                                        pusher.r.currentOrigin.z + move.z - radius);
            maxs.set(pusher.r.currentOrigin.x + move.x + radius,
                                        pusher.r.currentOrigin.y + move.y + radius,
                                        pusher.r.currentOrigin.z + move.z + radius);
            
            Vector3f.sub(mins, move, totalMins);
            Vector3f.sub(maxs, move, totalMaxs);
        } else {
            mins.set(pusher.r.absmin.x + move.x, pusher.r.absmin.y + move.y, pusher.r.absmin.z + move.z);
            maxs.set(pusher.r.absmax.x + move.x, pusher.r.absmax.y + move.y, pusher.r.absmax.z + move.z);

            totalMins.set(pusher.r.absmin);
            totalMaxs.set(pusher.r.absmax);
            if(move.x > 0) totalMaxs.x += move.x;
            else totalMins.x -= move.x;
                
            if(move.y > 0) totalMaxs.y += move.y;
            else totalMins.y -= move.y;

            if(move.z > 0) totalMaxs.z += move.z;
            else totalMins.z -= move.z;

        }

        // unlink the pusher so we don't get it in the entityList
        pusher.Unlink();

        SectorQuery query = Ref.server.EntitiesInBox(totalMins, totalMaxs);
        // move the pusher to it's final position
        Vector3f.add(pusher.r.currentOrigin, move, pusher.r.currentOrigin);
        Vector3f.add(pusher.r.currentAngles, amove, pusher.r.currentAngles);
        pusher.Link();

        // see if any solid entities are inside the final position
        for (Integer integer : query.List) {
            Gentity check = Ref.game.g_entities[integer];
            // only push items and players
            if(check.s.eType != EntityType.ITEM && !check.isClient() && !check.physicsObject)
                continue;

            // see if the ent needs to be tested
            if(check.r.absmin.x >= maxs.x
                    || check.r.absmin.y >= maxs.y
                    || check.r.absmin.z >= maxs.z
                    || check.r.absmax.x <= mins.x
                    || check.r.absmax.y <= mins.y
                    || check.r.absmax.z <= mins.z)
                continue;

            // see if the ent's bbox is inside the pusher's final position
            // this does allow a fast moving object to pass through a thin entity...
            if(TestEntityPosition(check) == null)
                continue;

           // TODO: FIX continue;
            if(TryPushingEntity(check, pusher, move, amove))
                continue;
        }

        return null;
    }

    private static Gentity TestEntityPosition(Gentity ent) {
        int mask = Content.SOLID;
        if(ent.ClipMask != 0)
            mask = ent.ClipMask;

        CollisionResult res = null;
//        if(ent.isClient())
//            res = Ref.server.Trace(ent.getClient().ps.origin, ent.getClient().ps.origin, ent.r.mins, ent.r.maxs, mask, ent.s.ClientNum);
//        else
//            res = Ref.server.Trace(ent.s.pos.base, ent.s.pos.base, ent.r.mins, ent.r.maxs, mask, ent.s.ClientNum);
//
//        if(res.startsolid)
//            return Ref.game.g_entities[res.entitynum];

        return null;
    }

    private static boolean TryPushingEntity(Gentity check, Gentity pusher, Vector3f move, Vector3f amove) {
//        Vector2f org = new Vector2f();
//        if(check.isClient())
//            Vector2f.sub(check.getClient().ps.origin, pusher.r.currentOrigin, org);
//        else
//            Vector2f.sub(check.s.pos.base, pusher.r.currentOrigin, org);

        Vector3f.add(check.s.pos.base, move, check.s.pos.base);
        if(check.isClient()) {
            Vector3f.add(check.getClient().ps.origin, move, check.getClient().ps.origin);
        }

        Gentity block = TestEntityPosition(check);
        if(block == null) {
            // pushed ok
            if(check.isClient())
                check.r.currentOrigin.set(check.getClient().ps.origin);
            else
                check.r.currentOrigin.set(check.s.pos.base);
            check.Link();
            return true;
        }

        return false;
        
    }

    private void MoverTeam() {
        // make sure all team slaves can move before commiting
	// any moves or calling any think functions
	// if the move is blocked, all moved objects will be backed out
        Gentity part, obstacle = null;
        Vector3f origin = new Vector3f(), angles = new Vector3f();
        Vector3f move = new Vector3f(), amove = new Vector3f();
        for(part = ent; part != null; part = part.mover.teamchain) {
            // get current position
            part.s.pos.Evaluate(Ref.game.level.time, origin);
            part.s.apos.Evaluate(Ref.game.level.time, angles);
            Vector3f.sub(origin, part.r.currentOrigin, move);
            Vector3f.sub(angles, part.r.currentAngles, amove);
            if((obstacle = moverPush(part, move, amove)) != null) {
                break; // blocked
            }
        }

        if(part != null) {
            // go back to the previous position
            for(part = ent; part != null; part = part.mover.teamchain) {
                // get current position
                part.s.pos.time += Ref.game.level.time - Ref.game.level.previousTime;
                part.s.apos.time += Ref.game.level.time - Ref.game.level.previousTime;
                part.s.pos.Evaluate(Ref.game.level.time, part.r.currentOrigin);
                part.s.apos.Evaluate(Ref.game.level.time, part.r.currentAngles);
                part.Link();
            }

            // if the pusher has a "blocked" function, call it
            if(ent.blocked != null)
                ent.blocked.blocked(ent, obstacle);
            return;
        }

        // the move succeeded
        for(part = ent; part != null; part = part.mover.teamchain) {
            // call the reached function if time is at or past end point
            if(part.s.pos.type == Trajectory.LINEAR_STOP) {
                if(Ref.game.level.time >= part.s.pos.time + part.s.pos.duration) {
                    if(part.reached != null)
                        part.reached.reached(part);
                }
            }
        }
    }

    /**
     * All entities in a mover team will move from pos1 to pos2
     * in the same amount of time
     */
    public void matchTeam(MoverState state, int time) {
        Gentity slave;
        for(slave = ent; slave != null; slave = slave.mover.teamchain) {
            slave.mover.setMoverState(state, time);
        }
    }

    public void setMoverState(MoverState state, int time) {
        moverState = state;
        ent.s.pos.time = time;
//        System.out.println("Setting mover state: " + state + ", time: " + time);
        switch(state) {
            case POS1:
                ent.s.pos.base.set(pos1);
                ent.s.pos.type = Trajectory.STATIONARY;
                break;
            case POS2:
                ent.s.pos.base.set(pos2);
                ent.s.pos.type = Trajectory.STATIONARY;
                break;
            case _1TO2:
                ent.s.pos.base.set(pos1);
                Vector3f delta = new Vector3f();
                Vector3f.sub(pos2, pos1, delta);
                float f = 1000f / ent.s.pos.duration;
                ent.s.pos.delta.set(delta.x * f, delta.y * f, delta.z * f);
                ent.s.pos.type = Trajectory.LINEAR_STOP;
                break;
            case _2TO1:
                ent.s.pos.base.set(pos2);
                delta = new Vector3f();
                Vector3f.sub(pos1, pos2, delta);
                f = 1000f / ent.s.pos.duration;
                ent.s.pos.delta.set(delta.x * f, delta.y * f, delta.z * f);
                ent.s.pos.type = Trajectory.LINEAR_STOP;
                break;
        }
        ent.s.pos.Evaluate(Ref.game.level.time, ent.r.currentOrigin);
        ent.Link();
    }

    public void runMover() {
        if(ent.s.pos.type != Trajectory.STATIONARY || ent.s.apos.type != Trajectory.STATIONARY)
            MoverTeam();

        ent.runThink();
    }

    public static IUseMethod Use_BinaryMover = new IUseMethod() {
        public void use(Gentity self, Gentity other, Gentity activator) {
            // only the master should be used
            if(self.mover.teamslave) {
                Use_BinaryMover.use(self.mover.teammaster, other, activator);
                return;
            }

            if(self.mover.moverState == MoverState.POS1) {
                // start moving 50 msec later, becase if this was player
		// triggered, level.time hasn't been advanced yet
                self.mover.matchTeam(MoverState._1TO2, Ref.game.level.time + 50);

                // starting sound
                if(self.mover.sound1to2 != 0)
                    Ref.game.AddEvent(self, Event.GENERAL_SOUND, self.mover.sound1to2);

                return;
            }

            // if all the way up, just delay before coming down
            if(self.mover.moverState == MoverState.POS2)
            {
                self.nextthink = Ref.game.level.time + self.wait;
                return;
            }

            // only partway down before reversing
            if(self.mover.moverState == MoverState._2TO1) {
                int total = self.s.pos.duration;
                int partial = Ref.game.level.time - self.s.pos.time;
                if(partial > total)
                    partial = total;

                self.mover.matchTeam(MoverState._1TO2, Ref.game.level.time - (total - partial));

                if(self.mover.sound1to2 != 0)
                    Ref.game.AddEvent(self, Event.GENERAL_SOUND, self.mover.sound1to2);
                return;
            }

            // only partway up before reversing
            if(self.mover.moverState == MoverState._1TO2) {
                int total = self.s.pos.duration;
                int partial = Ref.game.level.time - self.s.pos.time;
                if(partial > total)
                    partial = total;

                self.mover.matchTeam(MoverState._2TO1, Ref.game.level.time - (total - partial));

                if(self.mover.sound2to1 != 0)
                    Ref.game.AddEvent(self, Event.GENERAL_SOUND, self.mover.sound2to1);
                return;
            }
        }
    };

    public static IReachedMethod Reached_BinaryMover = new IReachedMethod() {
        public void reached(Gentity self) {
            if(self.mover.moverState == MoverState._1TO2) {
                // reached pos2
                self.mover.setMoverState(MoverState.POS2, Ref.game.level.time);

                // play sound
                if(self.mover.soundPos2 != 0) {
                    Ref.game.AddEvent(self, Event.GENERAL_SOUND, self.mover.soundPos2);
                }

                // return to pos1 after a delay
                self.think = ReturnToPos1;
                self.nextthink = Ref.game.level.time + self.wait;

                // fire targets
                if(self.mover.activator == null)
                    self.mover.activator = self;

                self.UseTargets(self.mover.activator);
            } else if(self.mover.moverState == MoverState._2TO1) {
                // reached pos1
                self.mover.setMoverState(MoverState.POS1, Ref.game.level.time);

                // play sound
                if(self.mover.soundPos1 != 0) {
                    Ref.game.AddEvent(self, Event.GENERAL_SOUND, self.mover.soundPos1);
                }


            } else
                Ref.common.Error(Common.ErrorCode.DROP, "Reached_BianryMover: bad moverstate");
        }
    };

    private static IThinkMethod ReturnToPos1 = new IThinkMethod() {
        public void think(Gentity self) {
            self.mover.matchTeam(MoverState._2TO1, Ref.game.level.time);

            // play sound
            if(self.mover.sound2to1 != 0) {
                Ref.game.AddEvent(self, Event.GENERAL_SOUND, self.mover.sound2to1);
            }
        }
    };
}
