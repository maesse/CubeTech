package cubetech.common.items;

import cubetech.Game.Gentity;
import cubetech.common.PlayerState;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public interface IItem {
    public String getClassName();
    public String getPickupName();
    public ItemType getType();
    public int itemPicked(Gentity self, Gentity ent); // returns respawn time
    public boolean canItemBeGrabbed(PlayerState ps); // checks the PS for grabability
    public void getBounds(Vector3f mins, Vector3f maxs); // write item bounds to these vectors
    public String getPickupSound(); // played when picked
    public String getIconName(); // for sprites and UI
}
