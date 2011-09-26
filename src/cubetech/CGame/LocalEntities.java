/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import cubetech.common.Common.ErrorCode;
import cubetech.misc.Ref;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class LocalEntities {
    private static Queue<LocalEntity> freeEntities = new LinkedList<LocalEntity>();
    private static LinkedList<LocalEntity> activeEntities = new LinkedList<LocalEntity>();
    private static final int MAX_LOCAL_ENTITIES = 512;
    private static boolean initialized = false;
    private static boolean allowFree = true;

    public static LocalEntity allocate() {
        if(!initialized) {
            for (int i= 0; i < MAX_LOCAL_ENTITIES; i++) {
                freeEntities.add(new LocalEntity());
            }
            initialized = true;
        }

        if(freeEntities.isEmpty()) {
            // no free entities, so free the one at the end of the chain
            // remove the oldest active entity
            activeEntities.peek().free();
        }

        LocalEntity ent = freeEntities.poll();
        ent.clear();
        activeEntities.add(ent);
        return ent;
    }

    static void free(LocalEntity ent) {
        if(!allowFree) return; // dont free entities while rendering
        
        activeEntities.remove(ent);
        freeEntities.add(ent);
    }

    public static void addLocalEntities() {
        allowFree = false;
        int time = Ref.cgame.cg.time;

        // walk the list backwards, so any new local entities generated
	// (trails, marks, etc) will be present this frame
        Iterator<LocalEntity> it = activeEntities.iterator();
        while(it.hasNext()) {
            LocalEntity le = it.next();

            if(time >= le.endTime) {
                freeEntities.add(le);
                le.clear();
                it.remove();
                continue;
            }

            if((le.Flags & LocalEntity.FLAG_TRAJECTORY) != 0) {
                applyMove(le);
            }

            switch(le.Type) {
                case LocalEntity.TYPE_SCALE_FADE:
                    addScaleFade(le);
                    break;
                case LocalEntity.TYPE_SCALE_FADE_COLOR:
                    addScaleFadeColor(le);
                    break;
                case LocalEntity.TYPE_SCALE_FADE_MOVE:
                    addScaleFadeMove(le);
                    break;
                case LocalEntity.TYPE_SCALE_DOUBLE_MOVE:
                    addDoubleMove(le);
                    break;
                case LocalEntity.TYPE_FADE:
                    addFade(le);
                    break;
                case LocalEntity.TYPE_PHYSICSOBJECT:
                    addPhysics(le);
                    break;
                case LocalEntity.TYPE_EXPLOSION:
                    addExplosion(le);
                    break;
                default:
                    Ref.common.Error(ErrorCode.DROP, "Bad LocalEntity type " + le.Type);
                    break;
            }

            // check for free events
            if(le.freeMe) {
                freeEntities.add(le);
                le.clear();
                it.remove();
                continue;
            }
        }
        allowFree = true;
    }

    private static void addExplosion(LocalEntity le) {
        RenderEntity re = le.rEntity;
//        re.frame = re.mat.getFrame(Ref.cgame.cg.time - le.startTime);
        Ref.render.addRefEntity(re);
    }



    private static void addPhysics(LocalEntity le) {
        MotionState m = le.phys_motionState;
        Transform t = new Transform();
        m.getWorldTransform(t);
        RenderEntity ent = le.rEntity;
        ent.origin.set(t.origin.x, t.origin.y, t.origin.z);
        ent.origin.scale(CGPhysics.INV_SCALE_FACTOR);
        ent.color.set(1,1,1,1);

        int activationState = le.phys_body.getActivationState();
        
        switch(activationState) {
            case 1: // active
                ent.color.set(1,0,0,1);
                break;
            case 2:
                ent.color.set(1,1,0,1);
                break;
            default:
                ent.color.set(0,0,1,1);
        }

        ent.outcolor.set(ent.color);
        ent.outcolor.scale(255);

        Ref.render.addRefEntity(ent);
    }

    private static void addScaleFadeColor(LocalEntity le) {
        RenderEntity re = le.rEntity;

        // fade / grow time
        float c = (le.endTime - Ref.cgame.cg.time) * le.lifeRate;
        float c2 = c - (1f - le.bouncefactor);
        if(c2 < 0.0) {
            re.outcolor.x = le.color2.x * 0xff;
            re.outcolor.y = le.color2.y * 0xff;
            re.outcolor.z = le.color2.z * 0xff;
            re.outcolor.w = 0xff * c * le.color2.w;
        } else {
            c2 /= le.bouncefactor;
            re.outcolor.x = 0xff * ((c2 * le.color.x) + (1f - c2) * le.color2.x);
            re.outcolor.y = 0xff * ((c2 * le.color.y) + (1f - c2) * le.color2.y);
            re.outcolor.z = 0xff * ((c2 * le.color.z) + (1f - c2) * le.color2.z);
            re.outcolor.w = 0xff * ((c2 *le.color.w) + (1f - c2) * le.color2.w * c);
        }

        le.pos.Evaluate(Ref.cgame.cg.time, re.origin);

        re.radius = le.radius * (1f - c) + 4;

        Ref.render.addRefEntity(re);
    }

    private static void applyMove(LocalEntity le) {
        le.pos.Evaluate(Ref.cgame.cg.time, le.rEntity.origin);
    }

    private static void addScaleFade(LocalEntity le) {
        // fade / grow time
        float c = (le.endTime - Ref.cgame.cg.time) * le.lifeRate;
        RenderEntity re = le.rEntity;
        re.outcolor.w = 0xff * c * re.color.w;
        re.radius = le.radius * (1f - c) + 16;

        // if the view would be "inside" the sprite, kill the sprite
	// so it doesn't add too much overdraw
//        Vector3f delta = Vector3f.sub(re.origin, Ref.cgame.cg.refdef.Origin, null);
//        float len = delta.length();
//        if(len < le.radius) {
//            le.free();
//            return;
//        }

        Ref.render.addRefEntity(re);
    }

    private static void addFade(LocalEntity le) {
        // fade / grow time
        float c = (le.endTime - Ref.cgame.cg.time) * le.lifeRate;
        RenderEntity re = le.rEntity;
        re.outcolor.w = 0xff * c * re.color.w;
        re.radius = le.radius;


        Ref.render.addRefEntity(re);
    }

    private static void addScaleFadeMove(LocalEntity le) {
        // fade / grow time
        float c = (le.endTime - Ref.cgame.cg.time) * le.lifeRate;
        RenderEntity re = le.rEntity;
        re.outcolor.w = 0xff * c * re.color.w;
        if(le.Flags != LocalEntity.FLAG_DONT_SCALE) {
            re.radius = le.radius * (1f - c) + 16;
        } else {
            re.radius = le.radius;
        }

        le.pos.Evaluate(Ref.cgame.cg.time, re.origin);

        Ref.render.addRefEntity(re);
    }

    private static void addDoubleMove(LocalEntity le) {
        // fade / grow time
        float c = (le.endTime - Ref.cgame.cg.time) * le.lifeRate;
        RenderEntity re = le.rEntity;
        re.outcolor.w = 0xff * c * re.color.w;
        if(le.Flags != LocalEntity.FLAG_DONT_SCALE) {
            re.radius = le.radius * (1f - c) + 16;
        } else {
            re.radius = le.radius;
        }

        le.angles.Evaluate(Ref.cgame.cg.time, re.oldOrigin);
        le.pos.Evaluate(Ref.cgame.cg.time, re.origin);

        Ref.render.addRefEntity(re);
    }
}
