package cubetech.common.items;

import cubetech.Game.GameClient;
import cubetech.Game.Gentity;
import cubetech.common.Move.MoveType;
import cubetech.common.PlayerState;
import cubetech.common.PlayerStats;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class HealthItem implements IItem {
    // Ref.ResMan.LoadTexture("data/bigmed.png")
    public int quantity = 25;

    public String getClassName() {
        return "item_health";
    }

    public String getPickupName() {
        return quantity + " Health";
    }

    public int getQuantity() {
        return quantity;
    }

    public ItemType getType() {
        return ItemType.HEALTH;
    }

    public int itemPicked(Gentity self, Gentity ent) {
        PlayerStats stats = ent.getClient().ps.stats;

        stats.Health += quantity;

        int max = stats.MaxHealth * 2;
        if(stats.Health > max)
            stats.Health = max;

        return 10;
    }

    public boolean canItemBeGrabbed(PlayerState ps) {
        if(ps.stats.Health < ps.stats.MaxHealth * 2)
            return true;
        return false;
    }

    public void getBounds(Vector3f mins, Vector3f maxs) {
        float size = 8;
        mins.set(-size, -size, -size);
        maxs.set(size,size,size);
    }

    public String getPickupSound() {
        return "data/smallmedkit1.wav";
    }

    public String getIconName() {
        return "data/tile.png";
    }

}
