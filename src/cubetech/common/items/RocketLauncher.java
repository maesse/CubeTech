/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common.items;

import cubetech.CGame.CEntity;
import cubetech.CGame.CGameRender;
import cubetech.CGame.IMethodCentity;
import cubetech.CGame.LocalEntity;
import cubetech.Game.Game;
import cubetech.Game.GameClient;
import cubetech.Game.Gentity;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.Trajectory;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.entities.Missiles;
import cubetech.misc.Ref;
import cubetech.server.SvFlags;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class RocketLauncher extends WeaponItem {
    private WeaponInfo wi = new WeaponInfo();
    public RocketLauncher() {
        wi.missileModel = Ref.ResMan.loadModel("data/rocket.iqm");
        wi.missileTrailFunc = missileTrailFunc;
    }

    IMethodCentity missileTrailFunc = new IMethodCentity() {
        public void run(CEntity ent) {
            EntityState es = ent.currentState;
            int startTime = ent.trailTime;
            int step = 50;
            int t = step * ((startTime + step) / step);


            Vector3f origin = es.pos.Evaluate(Ref.cgame.cg.time);
            if(es.pos.type == Trajectory.STATIONARY) {
                ent.trailTime = Ref.cgame.cg.time;
                return;
            }

            Vector3f lastPos = es.pos.Evaluate(ent.trailTime);
            ent.trailTime = Ref.cgame.cg.time;

            Vector3f dir = new Vector3f(es.pos.delta);
            dir.normalise();

            for (;t <= ent.trailTime; t += step) {
                es.pos.Evaluate(t, lastPos);
                Helper.VectorMA(lastPos, -14f, dir, lastPos);
                LocalEntity le = LocalEntity.smokePuff(lastPos, null, 64, 1, 1, 1, 0.33f, 2000, t, 0, 0, Ref.ResMan.LoadTexture("data/smokepuff.png").asMaterial());
                // use the optimized local entity add
                le.Type = LocalEntity.TYPE_SCALE_FADE;
                
            }
        }
    };
    
    @Override
    public Weapon getWeapon() {
        return Weapon.ROCKETLAUNCHER;
    }

    @Override
    public int getAmmoQuantity() {
        return 10;
    }

    @Override
    public Gentity fireWeapon(GameClient gc) {
        Vector3f dir = gc.getForwardVector();
        Vector3f start = getMuzzlePoint(gc);

        dir.normalise();

        Game g = Ref.game;
        Gentity r = g.Spawn();
        r.classname = "rocket";
        r.nextthink = g.level.time + 15000;
        r.think = Missiles.ExplodeMissile;
        r.s.eType = EntityType.MISSILE;
        r.s.weapon = getWeapon();
        r.r.ownernum = gc.s.ClientNum;
        r.parent = gc;
        r.ClipMask = Content.MASK_SHOT;

        r.s.pos.type = Trajectory.LINEAR;
        r.s.pos.time = g.level.time - 50; // move a bit on first frame
        r.s.pos.base.set(start);
        r.s.pos.delta.set(dir);
        r.s.pos.delta.scale(800);
        r.r.currentOrigin.set(start);

        return r;
    }

    public String getClassName() {
        return "weapon_rocketlauncher";
    }

    public String getPickupName() {
        return "Rocket Launcher";
    }

    @Override
    public int getRaiseTime() {
        return 250;
    }

    @Override
    public int getDropTime() {
        return 200;
    }

    @Override
    public int getFireTime() {
        return 800;
    }

    @Override
    public WeaponInfo getWeaponInfo() {
        return wi;
    }

    @Override
    public Gentity fireAltWeapon(GameClient gc) {
        // no alternative fire
        return null;
    }

    @Override
    public int getAltFireTime() {
        return 0; // doesn't do anything
    }



   

}
