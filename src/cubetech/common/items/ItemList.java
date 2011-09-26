package cubetech.common.items;

import cubetech.Game.Gentity;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.IThinkMethod;
import cubetech.common.ITouchMethod;
import cubetech.common.IUseMethod;
import cubetech.common.Move.MoveType;
import cubetech.common.PlayerState;
import cubetech.common.Trajectory;
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
        addItem(new Ak47());
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

    public Gentity dropItem(Gentity ent, IItem item, float angle, int ammo) {
        Vector3f angles = new Vector3f(ent.s.apos.base);
        angles.x = 0;
        angles.y += angle;
        Vector3f velocity = new Vector3f();
        Helper.AngleVectors(angles, velocity, null, null);
        Vector3f origin = Helper.VectorMA(ent.s.pos.base, 40f, velocity, null);
        if(ent.isClient()) {
            origin.z += ent.getClient().ps.viewheight;
        }
        velocity.scale(150f);
        velocity.z += 100 + Ref.rnd.nextFloat() * 50;

        return launchItem(item, origin, velocity, ammo);
    }

    private Gentity launchItem(IItem item, Vector3f origin, Vector3f velocity, int ammo) {
        Gentity dropped = Ref.game.Spawn();
        dropped.s.eType = EntityType.ITEM;
        dropped.s.modelindex = items2.indexOf(item);
        dropped.classname = item.getClassName();
        dropped.item = item;
        dropped.r.mins.set(-32,-32,-32);
        dropped.r.maxs.set(32,32,32);
        dropped.r.contents = Content.TRIGGER;
        dropped.touch = TouchItem;
        dropped.SetOrigin(origin);
        dropped.count = ammo;
        dropped.s.pos.type = Trajectory.GRAVITY;
        dropped.s.pos.time = Ref.game.level.time;
        dropped.s.pos.delta.set(velocity);
        dropped.think = Gentity.freeEntity;
        dropped.nextthink = Ref.game.level.time + 30000;
        //dropped.s.eFlags |= EntityFlags.BOUNCE_HALF;
        dropped.flags = Gentity.FLAG_DROPPED_ITEM;
        dropped.Link();
        return dropped;
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

            if(respawn == 0) return;
                
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
            if((self.flags & Gentity.FLAG_DROPPED_ITEM) != 0) {
                self.freeAfterEvent = true;
            }

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
