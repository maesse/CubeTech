/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common.items;

import cubetech.Game.GameClient;
import cubetech.Game.Gentity;

/**
 *
 * @author mads
 */
public class NullWeapon extends WeaponItem {
    
    @Override
    public Weapon getWeapon() {
        return Weapon.NONE;
    }

    @Override
    public int getAmmoQuantity() {
        return 0;
    }

    @Override
    public Gentity fireWeapon(GameClient gc) {
        return null;
    }

    @Override
    public int getRaiseTime() {
        return 0;
    }

    @Override
    public int getDropTime() {
        return 0;
    }

    @Override
    public int getFireTime() {
        return 0;
    }

    public String getClassName() {
        return "weapon_none";
    }

    public String getPickupName() {
        return "DISREGARD ME";
    }

    @Override
    public WeaponInfo getWeaponInfo() {
        return null;
    }

    @Override
    public Gentity fireAltWeapon(GameClient gc) {
        return null;
    }

    @Override
    public int getAltFireTime() {
        return 0;
    }

}
