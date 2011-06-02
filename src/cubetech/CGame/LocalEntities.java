/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

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

            switch(le.Type) {
                case LocalEntity.TYPE_SCALE_FADE:
                    addScaleFade(le);
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

    private static void addScaleFade(LocalEntity le) {
        // fade / grow time
        float c = (le.endTime - Ref.cgame.cg.time) * le.lifeRate;
        RenderEntity re = le.rEntity;
        re.outcolor.w = 0xff * c * re.color.w;
        re.radius = le.radius * (1f - c) + 8;

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
}
