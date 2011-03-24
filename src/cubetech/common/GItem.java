package cubetech.common;

import cubetech.Block;
import cubetech.gfx.CubeTexture;

/**
 *
 * @author mads
 */
public class GItem {
    public static final int ITEM_RADIUS = 8;
    public enum Type {
        BAD,
        WEAPON,
        AMMO,
        ARMOR,
        HEALTH,
        POWERUP,
        HOLDABLE,
        PERSISTANT_POWERUP,
        TEAM // team items, such as flags
    }

    public GItem(String classname, String pickupSound, Block model, String name,
            int quantity, Type itemType, int tag, CubeTexture icon) {
        this.classname = classname;
        this.pickupSound = pickupSound;
        this.worldModel = model;
        this.pickupName = name;
        this.quantity = quantity;
        this.type = itemType;
        this.tag = tag;
        this.icon = icon;
    }

    public String classname;
    public String pickupSound;
    public String pickupName;
    public Block worldModel;
    public int quantity;
    public Type type;
    public int tag; // Maybe shouldn't be here
    public CubeTexture icon;

    
}
