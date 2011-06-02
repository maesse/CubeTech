package cubetech.common;

import cubetech.common.items.Weapon;
import cubetech.net.NetBuffer;

/**
 *
 * @author mads
 */
public class PlayerStats {
    public int Health;
    public int MaxHealth = 100; // not networked..

    // bit field of weapons player holds
    private int weapons = 0; // Start with Weapon.NONE
    // ammo for up to 16 weapons
    private int[] ammo = new int[16];

    public void addWeapon(Weapon w) {
        int tag = w.ordinal();
        weapons |= (1 << tag);
    }

    public boolean hasWeapon(Weapon w) {
        if(w == Weapon.NONE) return true;
        int tag = w.ordinal();
        return ((weapons & (1 << tag)) != 0);
    }

    public void addAmmo(Weapon weapon, int quantity) {
        // don't change ammo when infinite
        if(ammo[weapon.ordinal()] != -1) ammo[weapon.ordinal()] += quantity;
    }

    public int getAmmo(Weapon weapon) {
        return ammo[weapon.ordinal()];
    }

    public void setAmmo(Weapon weapon, int quant) {
        ammo[weapon.ordinal()] = quant;
    }

    // Serialize
    public void WriteDelta(NetBuffer msg, PlayerStats old) {
        msg.WriteDelta(old.Health, Health);
        msg.WriteDelta(old.weapons, weapons);
        for (int i= 0; i < ammo.length; i++) {
            msg.WriteByte(ammo[i] & 0xff);
        }
    }

    // Deserialize
    public void ReadDelta(NetBuffer msg, PlayerStats old) {
        Health = msg.ReadDeltaInt(old.Health);
        weapons = msg.ReadDeltaInt(old.weapons);
        for (int i= 0; i < ammo.length; i++) {
            ammo[i] = msg.ReadByte() & 0xff;
        }
    }

    @Override
    public PlayerStats clone() {
        PlayerStats ps = new PlayerStats();
        ps.Health = Health;
        ps.weapons = weapons;
        System.arraycopy(ammo, 0, ps.ammo, 0, ammo.length);
        return ps;
    }



    
}
