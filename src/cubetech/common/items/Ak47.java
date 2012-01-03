package cubetech.common.items;

import cubetech.Game.GameClient;
import cubetech.Game.Gentity;
import cubetech.collision.CollisionResult;
import cubetech.collision.CubeCollision;
import cubetech.collision.CubeMap;
import cubetech.collision.SingleCube;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.MeansOfDeath;
import cubetech.entities.Event;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class Ak47 extends WeaponItem {
    WeaponInfo wi = new WeaponInfo();

    public Ak47() {
        wi.viewModel = Ref.ResMan.loadModel("data/weapons/ak/ak47_vmodel.iqm");
        wi.fireSound = "data/weapons/ak/ak47-1.wav";
        wi.flashTexture = "data/textures/muzzleflashX.png";
        wi.explodeSound = "data/sounds/ric5.wav";
        wi.worldModel = Ref.ResMan.loadModel("data/weapons/ak/ak47.iqm");
    }

    @Override
    public Weapon getWeapon() {
        return Weapon.AK47;
    }
    
    

    @Override
    public int getAmmoQuantity() {
        return 30;
    }

    @Override
    public Gentity fireWeapon(GameClient gc) {
        Vector3f forward = new Vector3f();
        Vector3f right = new Vector3f();
        Vector3f up = new Vector3f();
        Helper.AngleVectors(gc.ps.viewangles, forward, right, up);

        // Handle recoil
        gc.extraWeaponTime += getFireTime()*2;
        int maxRecoilTime = 3*getFireTime();
        if(gc.extraWeaponTime > maxRecoilTime) {
            gc.extraWeaponTime = maxRecoilTime;
        }
        float recoilFrac = gc.extraWeaponTime / maxRecoilTime;
        float recoilFactor = 100;

        // Extra recoil from velocity
        float vel = Math.min(gc.ps.velocity.length() / 200, 1);
        float velFactor = 100;


        float spread = 50 + recoilFrac * recoilFactor + vel * velFactor;
        float r = (float) (Ref.rnd.nextFloat() * Math.PI * 2.0f);
        float u = (float) (Math.sin(r) * ((Ref.rnd.nextFloat() - 0.5f) * 2f) * spread);
        r = (float) (Math.sin(r) * ((Ref.rnd.nextFloat() - 0.5f) * 2f) * spread);
        gc.ps.punchangle.x += Ref.rnd.nextFloat() * 2f;
        Vector3f muzzle = getMuzzlePoint(gc);
        Vector3f end = Helper.VectorMA(muzzle, 8192, forward, null);
        Helper.VectorMA(end, r, right, end);
        Helper.VectorMA(end, u, up, end);

        int passent = gc.s.ClientNum;
        Vector3f delta = Vector3f.sub(end, muzzle, null);
        Vector3f dir = new Vector3f(delta);
        dir.normalise();

        CollisionResult res = Ref.server.Trace(muzzle, end, null, null, Content.MASK_SHOT, passent);
        if(res.frac == 1) {
            return null;
        }

        Vector3f hit = Helper.VectorMA(muzzle, res.frac, delta, null);
        Vector3f hitAxis = res.hitAxis;
        Gentity ent = Ref.game.g_entities[res.entitynum];
        
        // send bullet impact
        Gentity tent = null;
        if(ent.isClient()) {
            tent = Ref.game.TempEntity(hit, Event.BULLET_HIT_FLESH.ordinal());
            tent.s.evtParams = ent.s.ClientNum;

            Ref.game.damage(ent, null, gc, null, hit, 17, 0, MeansOfDeath.AK47);
        } else {
            tent = Ref.game.TempEntity(hit, Event.BULLET_HIT_WALL.ordinal());
            tent.s.evtParams = Helper.normalToInt(hitAxis);
        }
        tent.s.otherEntityNum = gc.s.ClientNum;
        return null;
    }

    public static Vector3f getMuzzlePoint(GameClient gc) {
        Vector3f p = new Vector3f(gc.s.pos.base);
        p.z += gc.ps.viewheight;
        return p;
    }

    @Override
    public Gentity fireAltWeapon(GameClient gc) {
        return null;
    }

    @Override
    public WeaponInfo getWeaponInfo() {
        return wi;
    }

    @Override
    public int getRaiseTime() {
        return 300;
    }

    @Override
    public int getDropTime() {
        return 300;
    }

    @Override
    public int getFireTime() {
        return 100;
    }

    @Override
    public int getAltFireTime() {
        return 1;
    }

    @Override
    public void renderClientEffects() {
        
    }

    public String getClassName() {
        return "weapon_ak47";
    }

    public String getPickupName() {
        return "Ak47";
    }

}
