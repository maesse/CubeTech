package cubetech.entities;

import cubetech.Game.Gentity;

import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.IBlockedMethod;
import cubetech.common.IThinkMethod;
import cubetech.common.ITouchMethod;
import cubetech.entities.Mover.MoverState;
import cubetech.gfx.CubeTexture;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class Func_Door implements IEntity {

    // Spawn method for func_foor
    public void init(Gentity ent) {
        // Attach mover
        ent.mover = new Mover();
        ent.blocked = Blocked_Door;

        // default speed of 100
        if(ent.speed == 0)
            ent.speed = 100;

        // default wait of 2 seconds
        if(ent.wait == 0)
            ent.wait = 2;
        ent.wait *= 1000;

        // first position at start
        ent.mover.pos1.set(ent.s.origin);
        Ref.server.SetBrushModel(ent.shEnt, 1);
//        // TODO: set movedir
//        ent.r.mins = new Vector2f(-20, -4);
//        ent.r.maxs = new Vector2f(20, 4);

        Vector3f size = new Vector3f();
        Vector3f.sub(ent.r.maxs, ent.r.mins, size);
        float distance = Vector3f.dot(ent.mover.movedir, size); // FIX FIIX
        ent.mover.pos2.set(ent.mover.pos1.x + distance * ent.mover.movedir.x,
                           ent.mover.pos1.y + distance * ent.mover.movedir.y,
                           ent.mover.pos1.z + distance * ent.mover.movedir.z);
        
        ent.mover.initMover(ent);

        ent.nextthink = Ref.game.level.time + 100; // TODO FIX
        ent.think = Think_SpawnNewDoorTrigger;
    }

    public CubeTexture getIcon() {
        return Ref.ResMan.LoadTexture("data/tool_mover.png");
    }

    public static IThinkMethod Think_SpawnNewDoorTrigger = new IThinkMethod() {
        public void think(Gentity ent) {
            // set all of the slaves as shootable
            Gentity other = null;
            for(other = ent; other != null; other = ent.mover.teamchain) {
                other.mover.takedamage = true;
            }

            Vector3f mins = new Vector3f(ent.r.absmin);
            Vector3f maxs = new Vector3f(ent.r.absmax);

            for(other = ent.mover.teamchain; other != null; other = ent.mover.teamchain) {
                Helper.AddPointToBounds(other.r.absmin, mins, maxs);
                Helper.AddPointToBounds(other.r.absmax, mins, maxs); // FIX FIX
            }

            // find the thinnest axis, which will be the one we expand
            boolean xaxis = true;
            if(ent.r.maxs.y - ent.r.mins.y < ent.r.maxs.x - ent.r.mins.x)
                xaxis = false;

            float growSize = 2;

            if(xaxis)
            {
                mins.x -= growSize;
                maxs.x += growSize;
            } else
            {
                mins.y -= growSize;
                maxs.y += growSize;
            }

            // create a trigger with this size
            Gentity trig = Ref.game.Spawn();
            trig.classname = "door_trigger";
            trig.r.mins.set(mins.x, mins.y, mins.z);
            trig.r.maxs.set(maxs.x, maxs.y, maxs.z); // FIX
            trig.parent = ent;
            trig.s.contents = Content.TRIGGER;
            trig.touch = Touch_DoorTrigger;
            // remember the thinnest axis
            trig.count = xaxis?0:1;
            trig.Link();

            ent.mover.matchTeam(ent.mover.moverState, Ref.game.level.time);
        }
    };

    public static ITouchMethod Touch_DoorTrigger = new ITouchMethod() {
        public void touch(Gentity self, Gentity other) {
            //if(other.isClient() && other.getClient())
//            System.out.println("Touched door trigger");
            if(self.parent.mover.moverState != MoverState._1TO2)
                Mover.Use_BinaryMover.use(self.parent, self, other);
        }
    };

    public static IBlockedMethod Blocked_Door = new IBlockedMethod() {
        public void blocked(Gentity self, Gentity other) {
            
        }
    };

    public String getClassName() {
        return "func_door";
    }
    
}
