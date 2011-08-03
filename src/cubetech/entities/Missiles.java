/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.entities;

import cubetech.Game.Gentity;
import cubetech.collision.CollisionResult;
import cubetech.common.IThinkMethod;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class Missiles {
    public static IThinkMethod ExplodeMissile = new IThinkMethod() {
        public void think(Gentity ent) {
            Vector3f origin = ent.s.pos.Evaluate(Ref.game.level.time);
            ent.SetOrigin(origin);

            // we don't have a valid direction, so just point straight up
            Vector3f dir = new Vector3f(0, 0, 1);

            ent.s.eType = EntityType.GENERAL;
            Ref.game.AddEvent(ent, Event.MISSILE_MISS, 0);
            ent.freeAfterEvent = true;
            ent.Link();
        }
    };

    public static void runMissile(Gentity ent) {
        // get current position
        Vector3f origin = ent.s.pos.Evaluate(Ref.game.level.time);

        // ignore interactions with the missile owner
        int passEnt = ent.r.ownernum;

        // trace a line from the previous position to the current position
        CollisionResult res = Ref.server.Trace(ent.r.currentOrigin, origin, ent.r.mins, ent.r.maxs, passEnt, ent.ClipMask);
//        res.getPOI(ent.r.currentOrigin);

        Ref.server.LinkEntity(ent.shEnt);

        if(res.frac != 1) {
            missileImpact(ent, res);
            if(ent.s.eType != EntityType.MISSILE) return; // exploded
        } else {
            ent.r.currentOrigin.set(origin);
        }

        // check think function after bouncing
        ent.runThink();
    }

    private static void missileImpact(Gentity ent, CollisionResult res) {
        Ref.game.AddEvent(ent, Event.MISSILE_MISS, 0);

        ent.freeAfterEvent = true;
        // change over to a normal entity right at the point of impact
        ent.s.eType = EntityType.GENERAL;
        Vector3f endPos = res.getPOI(null);
        ent.SetOrigin(endPos);

        Gentity other = Ref.game.g_entities[res.entitynum];
        // Splash damage
        if(ent.splashDamage > 0) {
            Ref.game.radiusDamage(endPos, ent.parent, ent.splashDamage, ent.splashRadius, other, ent.splashMeansOfDeath);
        }

        ent.Link();
    }
}
