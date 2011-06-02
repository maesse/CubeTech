package cubetech.common.items;

import cubetech.Game.Gentity;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.IThinkMethod;
import cubetech.common.ITouchMethod;
import cubetech.common.IUseMethod;
import cubetech.common.Move.MoveType;
import cubetech.common.PlayerState;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.entities.Event;
import cubetech.misc.Ref;
import cubetech.server.SvFlags;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector3f;

/**
 * Setup global itemlist for this game
 * @author mads
 */
public class ItemList {
    ArrayList<IItem> items2 = new ArrayList<IItem>();
    private WeaponItem[] weaponList = new WeaponItem[Weapon.values().length];

    public ItemList() {
        // register items
        addItem(new HealthItem());
        addItem(new RocketLauncher());
        addItem(new Cubar());
        addItem(new NullWeapon()); // just so we dont get null exceptions for Weapon_none
    }

    public IItem findItemByPickup(String pickupName) {
        for (int i= 0; i < items2.size(); i++) {
            IItem item = items2.get(i);
            if(item.getPickupName().equalsIgnoreCase(pickupName))
                return item;
        }
        return null;
    }

    public IItem findItemByClassname(String classname) {
        for (int i= 0; i < items2.size(); i++) {
            IItem item = items2.get(i);
            if(item.getClassName().equalsIgnoreCase(classname))
                return item;
        }
        return null;
    }

    public int getItemIndex(IItem item) {
        for (int i= 0; i < items2.size(); i++) {
            IItem other = items2.get(i);
            if(other == item)
                return i;
        }
        Ref.common.Error(Common.ErrorCode.DROP, "ItemList.getItemIndex(): Couldn't find item: " + item.getClassName());
        return -1;
    }

    public int getItemCount() {
        return items2.size();
    }

    public IItem getItem(int index) {
        return items2.get(index);
    }

    public WeaponItem getWeapon(Weapon w) {
        return weaponList[w.ordinal()];
    }

    private void addItem(IItem item) {
        items2.add(item);
        
        // quick weapon lookup
        if(item.getType() == ItemType.WEAPON)
        {
            WeaponItem w = ((WeaponItem)item);
            weaponList[w.getWeapon().ordinal()] = w;
        }
    }

    public IThinkMethod FinishSpawningItem = new IThinkMethod() {
        public void think(Gentity ent) {
            // At this point, a item has been attached to this ent.item
            // but it has not been placed into the world yet

            // set bounds
            ent.item.getBounds(ent.r.mins, ent.r.maxs);

            ent.s.eType = EntityType.ITEM;
            ent.s.modelindex = Ref.common.items.getItemIndex(ent.item);

            ent.r.contents = Content.TRIGGER;
            ent.use = RespawnItem;
            ent.touch = TouchItem;

            ent.SetOrigin(ent.s.origin);
            ent.Link();
        }
    };

    private IThinkMethod RespawnItemThink = new IThinkMethod() {
        public void think(Gentity ent) {
            RespawnItem.use(ent, null, null);
        }
    };
    private IUseMethod RespawnItem = new IUseMethod() {
        public void use(Gentity self, Gentity other, Gentity activator) {
            // Relink a hidden entity into the world
            self.r.contents = Content.TRIGGER;
            self.s.eFlags &= ~EntityFlags.NODRAW;
            self.r.svFlags.remove(SvFlags.NOCLIENT);
            self.Link();

            // attach an item_spawn event to the entity for a sound effect
            Ref.game.AddEvent(self, Event.ITEM_RESPAWN, 0);

            self.nextthink = 0;
        }
    };

//    private int PickupPowerup(Gentity ent, Gentity other) {
//        int quantity = 0;
//        if(ent.count != 0)
//            quantity = ent.count;
//        else
//            quantity = ent.item.getQuantity();
//
//        GameClient cl = other.getClient();
////        cl.ps.powerups[ent.item.tag] += quantity * 1000;
//        return 60;
//    }

    public ITouchMethod TouchItem = new ITouchMethod() {
        public void touch(Gentity self, Gentity other) {
            if(!other.isClient())
                return;
            if(other.getClient().isDead())
                return; // dead people cant pickup

            if(other.getClient().ps.moveType == MoveType.NOCLIP) return;

            IItem item = self.item;

            
            if(!item.canItemBeGrabbed(other.getClient().ps))
                return;

            int respawn = item.itemPicked(self, other);
            Common.LogDebug("Item touched: " + other.s.ClientNum + ", " + self.item.getClassName());

            if(respawn <= 0)
                return;

            // play the normal pickup sound
            Ref.game.AddEvent(other, Event.ITEM_PICKUP, self.s.modelindex);

            // fire item targets
            self.UseTargets(other);

            // wait of -1 will not respawn
            if(self.wait == -1) {
                self.r.svFlags.add(SvFlags.NOCLIENT);
                self.s.eFlags |= EntityFlags.NODRAW;
                self.r.contents = 0;
                self.unlinkAfterEvent = true;
                return;
            }

            // non zero wait overrides respawn time
            if(self.wait != 0) {
                respawn = self.wait;
            }

            // dropped items will not respawn
            // TODO: self.flags

            // picked up items still stay around, they just don't
            // draw anything.  This allows respawnable items
            // to be placed on movers.
            self.r.svFlags.add(SvFlags.NOCLIENT);
            self.s.eFlags |= EntityFlags.NODRAW;
            self.r.contents = 0;

            // ZOID
            // A negative respawn times means to never respawn this item (but don't
            // delete it).  This is used by items that are respawned by third party
            // events such as ctf flags
            if(respawn <= 0) {
                self.nextthink = 0;
                self.think = null;
            } else {
                self.nextthink = Ref.game.level.time + respawn * 1000;
                self.think = RespawnItemThink;
            }

            self.Link();

        }
    };

    public boolean playerTouchesItem(PlayerState ps, EntityState item, int time) {
        Vector3f origin = item.pos.Evaluate(time);

        if(ps.origin.x - origin.x > 26
                || ps.origin.x - origin.x < -26
                || ps.origin.y - origin.y > 26
                || ps.origin.y - origin.y < -26
                || ps.origin.z - origin.z > 26
                || ps.origin.z - origin.z < -26)
            return false;

        return true;
    }


}
