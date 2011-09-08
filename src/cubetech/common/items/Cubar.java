package cubetech.common.items;

import cubetech.CGame.REType;
import cubetech.CGame.RenderEntity;
import cubetech.Game.GameClient;
import cubetech.Game.Gentity;
import cubetech.collision.CollisionResult;
import cubetech.collision.CubeCollision;
import cubetech.collision.CubeMap;
import cubetech.collision.SingleCube;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.gfx.CubeType;
import cubetech.misc.Ref;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class Cubar extends WeaponItem {
    WeaponInfo wi = new WeaponInfo();

    public Cubar() {
        
    }

    @Override
    public Weapon getWeapon() {
        return Weapon.CUBAR;
    }

    @Override
    public int getAmmoQuantity() {
        return -1;
    }

    @Override
    public Gentity fireWeapon(GameClient gc) {
        putOrRemoveBlock(gc, true);
        
        return null;
    }

    private void putOrRemoveBlock(GameClient gc, boolean put) {
        Vector3f dir = new Vector3f(gc.getForwardVector());
        Helper.Normalize(dir);
        CubeCollision col = CubeMap.TraceRay(gc.ps.getViewOrigin(), dir, 8, Ref.cm.cubemap.chunks);
        if(col == null) return;
        SingleCube cube = new SingleCube(col);

        if(cube.highlightSide != 0) {
            if(!put) {
                // removing is easy
                cube.removeBlock();
            } else {
                // placing stuff is less easy
                SingleCube destination = cube.getHightlightside(true);
                Vector3f origin = destination.getOrigin();
                Vector3f maxs = destination.getSize();

                // Gotta shrink it a little, or it will collide because of epsilon
                origin.x += 1; origin.y += 1; origin.z += 1;
                maxs.x -= 2; maxs.y -= 2; maxs.z -= 2;

                // Check if cube can be placed without blocking anything
                CollisionResult res = Ref.server.Trace(origin, origin, null, maxs, Content.SOLID, Common.ENTITYNUM_NONE);

                // We clear?
                boolean ok = !res.startsolid && !res.hit;
                if(ok) {
                    // Alright, now try with the normal size
                    origin = destination.getOrigin();
                    maxs = destination.getSize();
                    res = Ref.server.Trace(origin, origin, null, maxs, Content.MASK_PLAYERSOLID & ~Content.SOLID, Common.ENTITYNUM_NONE);
                    ok = (!res.startsolid && !res.hit);
                }

                if(ok) {
                    destination.putBlock(CubeType.GRASS);
                } else {
                    gc.SayTo("Server", "Cube placement blocked");
                }
            }
        }
    }

    @Override
    public WeaponInfo getWeaponInfo() {
        return wi;
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
        return 250;
    }

    public String getClassName() {
        return "weapon_cubar";
    }

    public String getPickupName() {
        return "Cubar";
    }

    @Override
    public Gentity fireAltWeapon(GameClient gc) {
        putOrRemoveBlock(gc, false);
        return null;
    }

    @Override
    public int getAltFireTime() {
        return getFireTime();
    }

    @Override
    public void renderClientEffects() {
        // Highlight block
        SingleCube cube = null;
        if(Ref.cgame.map != null) {
            Vector3f dir = Ref.cgame.cg.refdef.ViewAxis[0];
            Vector3f origin = Ref.cgame.cg.predictedPlayerState.getViewOrigin();
            CubeCollision col = CubeMap.TraceRay(origin, dir, 8, Ref.cgame.map.chunks);
            if(col != null) {
                 cube = new SingleCube(col);
            }
        }
        if(cube != null) {
            RenderEntity ent = Ref.render.createEntity(REType.BBOX);
            ent.origin.set(cube.getOrigin());
            ent.oldOrigin.set(cube.getSize());
            Ref.render.addRefEntity(ent);
        }
    }
    
}
