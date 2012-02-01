/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.Game;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import cubetech.collision.CollisionResult;
import cubetech.common.items.IItem;
import cubetech.common.*;
import cubetech.common.items.ItemType;
import cubetech.common.items.WeaponInfo;
import cubetech.common.items.WeaponItem;
import cubetech.entities.EntityShared;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.entities.Mover;
import cubetech.entities.SharedEntity;
import cubetech.iqm.IQMModel;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class Gentity {
    public static final int FLAG_DROPPED_ITEM = 1;
    public Gentity() {
        shEnt = new SharedEntity();
        s = shEnt.s;
        r = shEnt.r;
    }
    public SharedEntity shEnt;
    public EntityState s; // communicated by server to clients
    public EntityShared r; // shared by both the server system and game

//    public GameClient client; // NULL if not a client
    public String classname;
    public boolean inuse;
    public boolean neverfree;

    public int eventTime;
    public int freetime; // level.time when the object was freed
    public boolean freeAfterEvent;
    public boolean unlinkAfterEvent;

    public boolean physicsObject; // if true, it can be pushed by movers and fall off edges
				  // all game items are physicsObjects,
    public float physicsBounce; // 1.0 = continuous bounce, 0.0 = no bounce
    public int ClipMask; // brushes with this content value will be collided against
                        // when moving.  items and corpses do not collide against
                        // players, for instance
    
    
    
    public int nextthink;
    
    public IThinkMethod think;
    public IReachedMethod reached; // movers call this when hitting endpoint
    public IBlockedMethod blocked;
    public ITouchMethod touch;
    public IUseMethod use;
    public IPainMethod pain;
    public IDieMethod die;

    public IItem item;

    // Used by some entities
//    public int health;
    public int speed;
    public int count;

    public int flags;
    
    
    // timing
    public int wait;

    // movers
    public Mover mover;
    public String target = null;
    public String targetname = null;
    public Gentity parent;

    // rockets, nades
    public int damage;
    public int splashDamage;
    public int splashRadius;
    public MeansOfDeath meansOfDeath;
    public MeansOfDeath splashMeansOfDeath;


    public void Clear() {
        
        s.Clear();
        r.Clear();
        flags = 0;
        inuse = false;
        eventTime = 0;
        damage = 0;
        splashDamage = 0;
        splashRadius = 0;
        freeAfterEvent = false;
        unlinkAfterEvent = false;
        physicsObject = false;
        classname = "";
        neverfree = false;
        freetime = 0;
        ClipMask = 0;
        nextthink = 0;
        think = null;
        reached = null;
        blocked = null;
        touch = null;
        
        use = null;
        pain = null;
        die = null;
        item = null;
        wait = 0;
        mover = null;
        count = 0;
        speed = 0;
//        health = 0;
        target = null;
        physicsBounce = 0;

        if(isClient())
            getClient().Clear();
    }

    public void SetOrigin(Vector3f org) {
        s.pos.base.x = org.x;
        s.pos.base.y = org.y;
        s.pos.base.z = org.z;
        s.pos.type = Trajectory.STATIONARY;
        s.pos.time = 0;
        s.pos.duration = 0;
        s.pos.delta.x = s.pos.delta.y = s.pos.delta.z = 0;
        r.currentOrigin.x = org.x;
        r.currentOrigin.y = org.y;
        r.currentOrigin.z = org.z;
    }

    public void Init(int i) {
        inuse = true;
        classname = "noclass";
        s.number = i;
        r.ownernum = Common.ENTITYNUM_NONE;
    }

    void Free() {
        if(r.linked) {
            Unlink();
        }
        if(neverfree)
            return;

        // Clear entity
        Clear();
        classname = "freed";
        freetime = Ref.game.level.time;
        inuse = false;
    }

    public static IThinkMethod freeEntity = new IThinkMethod() {
        public void think(Gentity ent) {
            ent.Free();
        }
    };

    public void Link() {
        Ref.server.LinkEntity(shEnt);
    }

    public void Unlink() {
        Ref.server.UnlinkEntity(shEnt);
    }
    
    protected IQMModel getModel() {
        IQMModel model = null;
        if(r.bmodel) {
            model = Ref.server.getModel(s.modelindex);
        } else if(s.eType == EntityType.ITEM) {
            IItem item = Ref.common.items.getItem(s.modelindex);
            if(item.getType() == ItemType.WEAPON) {
                WeaponItem weaponItem = (WeaponItem)item;
                WeaponInfo wi = weaponItem.getWeaponInfo();
                if(wi != null && wi.worldModel != null) {
                    model = wi.worldModel;
                }
            } else {
                Common.Log("initPhysicsBody: non-weapon item Fixme");
            }
        }
        return model;
    }

    /**
     * @return true if this entity is an GameClient
     */
    public boolean isClient() {
        return this instanceof GameClient;
    }

    /**
     * @return the assigned GameClient, or null if not a client.
     */
    public GameClient getClient() {
        if(isClient())
            return (GameClient)this;
        return null;
    }

    /**
     * Search for (string)targetname in all entities that
     * match (string)self.target and call their .use function
     * @param activator should be set to the entity that initiated the firing.
     */
    public void UseTargets(Gentity activator) {
        if(target == null)
            return;

        Gentity t = null;
        while((t = Ref.game.Find(t, GentityFilter.TARGETNAME, target)) != null)  {
            if(t == this)
            {
                Common.Log("WARNING: Entity used self");
            }
            else {
                if(t.use != null)
                    t.use.use(t, this, activator);
            }

            if(!this.inuse) {
                Common.Log("entity was remoevd while using targets");
                return;
            }

        }
    }

    public void runThink() {
        if(nextthink <= 0)
            return;

        if(nextthink > Ref.game.level.time)
            return;

        nextthink = 0;
        if(think == null)
            Ref.common.Error(Common.ErrorCode.DROP, "NULL ent.think");
        think.think(this);
    }
    
    

    // This entity is an item, run it for this frame
    public void runItem() {
//        // TODO: FIX
//        if(s.pos.type == Trajectory.STATIONARY)
//        {
//            runThink();
//            return;
//        }
        
        {
            // get current position
            Vector3f origin = s.pos.Evaluate(Ref.game.level.time);

            // trace a line from the previous position to the current position
            int mask = ClipMask;
            if(mask == 0)
                mask = Content.MASK_PLAYERSOLID & ~Content.BODY;
            CollisionResult res = Ref.server.Trace(r.currentOrigin, origin, r.mins, r.maxs, mask, r.ownernum);
            res.getPOI(r.currentOrigin);
            if(res.startsolid) {
                res.frac = 0;
            }
            if(res.frac == 1) return;
            Gitems.bounceItem(this, res);
        }
        
        Link();

        // check think function
        runThink();
    }

//    /**
//     * "pos1", "pos2", and "speed" should be set before calling,
//     * so the movement delta can be calculated
//     */
//    public void initMover() {
//        use = Use_BinaryMover;
//    }

}
