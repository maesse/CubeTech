package cubetech.common.items;

import cubetech.Game.GameClient;
import cubetech.Game.Gentity;
import cubetech.common.Helper;
import cubetech.common.Move.MoveType;
import cubetech.common.PlayerState;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public abstract class WeaponItem implements IItem {

    public abstract Weapon getWeapon();
    public abstract int getAmmoQuantity();
    public abstract Gentity fireWeapon(GameClient gc);
    public abstract Gentity fireAltWeapon(GameClient gc);
    public abstract WeaponInfo getWeaponInfo();

    public abstract int getRaiseTime(); // ms it takes to raise this weapon
    public abstract int getDropTime(); // ms it takes to drop this weapon
    public abstract int getFireTime(); // ms between firing
    public abstract int getAltFireTime(); // for alternative fire

    public ItemType getType() {
        return ItemType.WEAPON;
    }

    public boolean canItemBeGrabbed(PlayerState ps) {
        if(ps.moveType == MoveType.NOCLIP) return false;
        
        return true;
    }

    public void getBounds(Vector3f mins, Vector3f maxs) {
        float size = 8;
        mins.set(-size,-size,-size);
        maxs.set(size,size,size);
    }

    public int itemPicked(Gentity self, Gentity ent) {
        int quantity = 0;
        if(self.count >= 0) { // -1 = no ammo, 0 = ask weapon, > 0 = take this amount
            if(self.count > 0) quantity = self.count;
            else quantity = getAmmoQuantity();
        }

        // add the weapon
        Weapon w = getWeapon();
        ent.getClient().ps.stats.addWeapon(w);

        ent.getClient().ps.stats.addAmmo(w, quantity);
        if(ent.getClient().ps.stats.getAmmo(w) > 200)
            ent.getClient().ps.stats.setAmmo(w, 200);

        return 10; // fix
    }

    // default pickupsound
    public String getPickupSound() {
        return "data/smallmedkit1.wav";
    }

    public String getIconName() {
        return "data/tile.png";
    }

    public static WeaponItem get(Weapon w) {
        return Ref.common.items.getWeapon(w);
    }

    public static Vector3f getMuzzlePoint(GameClient gc) {
        Vector3f p = new Vector3f(gc.s.pos.base);
        p.z += gc.ps.viewheight;

        
        return p;
    }

    public abstract void renderClientEffects();

}
