package cubetech.common.items;

import cubetech.Game.GameClient;
import cubetech.Game.Gentity;
import cubetech.collision.CubeCollision;
import cubetech.collision.CubeMap;
import cubetech.collision.SingleCube;
import cubetech.common.Helper;
import cubetech.gfx.CubeType;
import cubetech.misc.Ref;
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
        CubeCollision col = CubeMap.TraceRay(gc.ps.origin, dir, 6, Ref.cm.cubemap.chunks);
        if(col == null) return;
        SingleCube cube = new SingleCube(col);

        if(cube.highlightSide != 0) {
            if(put)cube.getHightlightside().putBlock(CubeType.GRASS);
            else cube.removeBlock();
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
        return 500;
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
    
}
