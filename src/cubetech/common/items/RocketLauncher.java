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
import cubetech.common.MeansOfDeath;
import cubetech.common.Trajectory;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.entities.Missiles;
import cubetech.gfx.CubeMaterial;
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
        wi.missileModel = Ref.ResMan.loadModel("data/weapons/rocketlauncher/rocket.iqm");
        wi.missileTrailFunc = missileTrailFunc;
        wi.viewModel = Ref.ResMan.loadModel("data/weapons/rocketlauncher/rocket_vmodel.iqm");
        wi.fireSound = "data/weapons/rocketlauncher/TF2RL_Fire.wav";
        wi.missileSound = "data/weapons/rocketlauncher/TF2RL_Loop.wav";
        wi.trailRadius = 16f;
        wi.explodeSound = "data/sounds/80401__steveygos93__explosion2.wav";
        wi.trailTime = 1500f;
        wi.worldModel = Ref.ResMan.loadModel("data/weapons/rocketlauncher/rocket_vmodel.iqm");
    }

    IMethodCentity missileTrailFunc = new IMethodCentity() {
        public void run(CEntity ent) {
            EntityState es = ent.currentState;
            int startTime = ent.trailTime;
            int step = 10;
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
            Vector3f rndOffset = new Vector3f();
            CubeMaterial mat = Ref.ResMan.LoadTexture("data/textures/smokepuff.png").asMaterial();
            for (;t <= ent.trailTime; t += step) {
                es.pos.Evaluate(t, lastPos);
                Helper.VectorMA(lastPos, -16f, dir, lastPos);
                rndOffset.set(Ref.rnd.nextFloat()-0.5f,Ref.rnd.nextFloat()-0.5f,Ref.rnd.nextFloat()-0.5f);
                rndOffset.scale(4f);
                Vector3f.add(rndOffset, lastPos, rndOffset);
                float colorRnd = (Ref.rnd.nextFloat()-0.5f)*0.3f;
                float colorRnd2 = (Ref.rnd.nextFloat()-0.5f)*0.2f;
                LocalEntity le = LocalEntity.colorFadedSmokePuff(rndOffset, null, wi.trailRadius,
                        0.8f+colorRnd2, 0.4f+colorRnd2, 0.2f+colorRnd2, 0.4f,
                        wi.trailTime, t, 0, mat, 
                        0.6f+colorRnd, 0.6f+colorRnd, 0.6f+colorRnd, 0.4f, 0.2f);
//                LocalEntity le = LocalEntity.smokePuff(rndOffset, null, wi.trailRadius,
//                        0.6f+colorRnd, 0.6f+colorRnd, 0.6f+colorRnd, 0.6f,
//                        wi.trailTime, t, 0, 0, );
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
        // Add rocketlauncher offset
        // extent along forward vector
        Vector3f forward = new Vector3f();
        Vector3f right = new Vector3f();
        Vector3f up = new Vector3f();
        Helper.AngleVectors(gc.ps.viewangles, forward, right, up);
        Helper.VectorMA(start, 14f, forward, start);
        Helper.VectorMA(start, 7f, right, start);
        Helper.VectorMA(start, -4f, up, start);

        dir.normalise();

        Game g = Ref.game;
        Gentity r = g.Spawn();
        r.r.mins = wi.missileModel.getMins();
        r.r.maxs = wi.missileModel.getMaxs();
        r.classname = "rocket";
        r.nextthink = g.level.time + 15000;
        r.think = Missiles.ExplodeMissile;
        r.s.eType = EntityType.MISSILE;
        r.s.weapon = getWeapon();
        r.r.ownernum = gc.s.ClientNum;
        r.parent = gc;
        r.splashDamage = 100;
        r.splashRadius = 130;
        r.meansOfDeath = MeansOfDeath.ROCKET;
        r.splashMeansOfDeath = MeansOfDeath.ROCKET_SPLASH;
        r.damage = 100;
        r.ClipMask = Content.MASK_SHOT;
        r.s.pos.type = Trajectory.LINEAR;
        r.s.pos.time = g.level.time - 10; // move a bit on first frame
        r.s.pos.base.set(start);
        r.s.pos.delta.set(dir);
        r.s.pos.delta.scale(600);
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

    @Override
    public void renderClientEffects() {
        
    }



   

}
