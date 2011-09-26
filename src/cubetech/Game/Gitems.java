package cubetech.Game;

import cubetech.collision.CollisionResult;
import cubetech.common.Helper;
import cubetech.common.Trajectory;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector3f;

/**
 * @author mads
 */
public class Gitems {
    static void bounceItem(Gentity ent, CollisionResult res) {
        LevelLocal level = Ref.game.level;
        // reflect the velocity on the trace plane
        int hitTime = (int) (level.previousTime + (level.time - level.previousTime) * res.frac);
        Vector3f velocity = new Vector3f();
        ent.s.pos.EvaluateDelta(hitTime, velocity);
        float dot = Vector3f.dot(velocity, res.hitAxis);
        Helper.VectorMA(velocity, -2*dot, res.hitAxis, ent.s.pos.delta);

        // cut the velocity to keep from bouncing forever
        ent.s.pos.delta.scale(ent.physicsBounce);

        // check for stop
        if(res.hitAxis.z > 0 && ent.s.pos.delta.z < 40) {
            // TODO
            ent.s.pos.type = Trajectory.STATIONARY;
            Vector3f end = new Vector3f();
            res.getPOI(end);
            end.z += 1f;
            ent.SetOrigin(end);
            return;

        }

        Vector3f.add(ent.r.currentOrigin, res.hitAxis, ent.r.currentOrigin);
        ent.s.pos.base.set(ent.r.currentOrigin);
        ent.s.pos.time = level.time;
    }
}
