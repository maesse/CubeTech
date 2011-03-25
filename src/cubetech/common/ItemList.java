package cubetech.common;

import cubetech.Game.Gentity;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.entities.Event;
import cubetech.misc.Ref;
import cubetech.server.SvFlags;
import java.util.ArrayList;
import java.util.HashMap;
import org.lwjgl.util.vector.Vector2f;

/**
 * Setup global itemlist for this game
 * @author mads
 */
public class ItemList {
    ArrayList<GItem> items2 = new ArrayList<GItem>();
    HashMap<String, GItem> items = new HashMap<String, GItem>();

    public ItemList() {

        addItem(new GItem("item_health", "data/smallmedkit1.wav", null, "25 Health", 25, GItem.Type.HEALTH,0, Ref.ResMan.LoadTexture("data/bigmed.png")));

    }

    public GItem findItemByPickup(String pickupName) {
        GItem item = items.get(pickupName.toLowerCase());
        return item;
    }

    public GItem findItemByClassname(String classname) {
        for (int i= 0; i < items2.size(); i++) {
            GItem item = items2.get(i);
            if(item.classname.equalsIgnoreCase(classname))
                return item;
        }
        return null;
    }

    public int getItemIndex(GItem item) {
        for (int i= 0; i < items2.size(); i++) {
            GItem other = items2.get(i);
            if(other == item)
                return i;
        }
        Ref.common.Error(Common.ErrorCode.DROP, "ItemList.getItemIndex(): Couldn't find item: " + item.classname);
        return -1;
    }

    public int getItemCount() {
        return items2.size();
    }

    public boolean canItemBeGrabbed(EntityState ent, PlayerState ps) {
        if(ent.modelindex < 0 || ent.modelindex > items.size())
            Ref.common.Error(Common.ErrorCode.DROP, "ItemList.canItemBeGrabbed(): index out of range: " + ent.modelindex);

        if(ps.moveType == Move.MoveType.NOCLIP || ps.moveType == Move.MoveType.EDITMODE)
            return false; // noclip & editmode blocks items from being grabbed

        GItem item = getItem(ent.modelindex);
        switch(item.type) {
            case HEALTH:
                if(ps.stats.Health < ps.stats.MaxHealth * 2)
                    return true;
                return false;
            // TODO: REMOVE
            default:
                return true;
        }

        //return false;
    }

    public GItem getItem(int index) {
        return items2.get(index);
    }

    private void addItem(GItem item) {

        items.put(item.pickupName.toLowerCase(), item);
        items2.add(item);
    }

    public IThinkMethod FinishSpawningItem = new IThinkMethod() {
        public void think(Gentity ent) {

            // set bounds
            ent.r.mins.set(-GItem.ITEM_RADIUS, -GItem.ITEM_RADIUS);
            ent.r.maxs.set(GItem.ITEM_RADIUS, GItem.ITEM_RADIUS);

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
            self.r.contents = Content.TRIGGER;
            self.s.eFlags &= ~EntityFlags.NODRAW;
            self.r.svFlags.remove(SvFlags.NOCLIENT);
            self.Link();

            Ref.game.AddEvent(self, Event.ITEM_RESPAWN, 0);


            self.nextthink = 0;
        }
    };


    private int PickupHealth(Gentity ent, Gentity other) {
        PlayerStats stats = other.getClient().ps.stats;

        int quantity = ent.item.quantity;
        stats.Health += quantity;

        int max = stats.MaxHealth * 2;
        if(stats.Health > max)
            stats.Health = max;

        return 10;
    }

    private ITouchMethod TouchItem = new ITouchMethod() {
        public void touch(Gentity self, Gentity other) {
            if(!other.isClient())
                return;
            if(other.getClient().ps.stats.Health <= 0)
                return; // dead people cant pickup

            if(!Ref.common.items.canItemBeGrabbed(self.s, other.getClient().ps))
                return;

            Common.LogDebug("Item touched: " + other.s.ClientNum + ", " + self.item.classname);
            int respawn = 0;
            switch(self.item.type) {
                case HEALTH:
                    respawn = PickupHealth(self, other);
                    break;
                default:
                    Common.LogDebug("need to implement " + self.item.type);
                    break;
            }

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
        Vector2f origin = item.pos.Evaluate(time);

        if(ps.origin.x - origin.x > 26
                || ps.origin.x - origin.x < -26
                || ps.origin.y - origin.y > 26
                || ps.origin.y - origin.y < -26)
            return false;

        return true;
    }
}
