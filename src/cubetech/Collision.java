/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.entities.Entity;
import cubetech.misc.Ref;
import cubetech.misc.SpatialQuery;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class Collision {
    public static final float EPSILON = 0.0625f;
    public static final int MASK_WORLD = 1;
    public static final int MASK_PLAYER = 2;
    public static final int MASK_BULLETS = 8;
    public static final int MASK_BOMBAH = 4;

    public static final int MASK_ENEMIES = 4;
    public static final int MASK_ALL = 1 | 2 | 4 | 8;

    static final int RESULT_BUFFER_SIZE = 64;
    private CollisionResult[] resultBuffer = new CollisionResult[RESULT_BUFFER_SIZE];
    private int BufferOffset = 0;

    public Collision() {
        // Init CollisionBuffers
        for (int i= 0; i < RESULT_BUFFER_SIZE; i++) {
            resultBuffer[i] = new CollisionResult();
        }
    }

    // Get next collisionresult from the circular buffer
    private CollisionResult GetNext() {
        return resultBuffer[BufferOffset++ & RESULT_BUFFER_SIZE-1];
    }
    
    // True if collision occured
    public CollisionResult TestPosition(Vector2f pos, Vector2f dir, Vector2f extent, int tracemask) {
        CollisionResult res = GetNext();
        res.Reset(pos, dir, extent);

//        Vector2f dir2 = new Vector2f(dir.x, dir.y);

//        if(dir2.x > 0f)
//            dir2.x += 0.125f;
//        else if(dir2.x < 0f)
//            dir2.x -= 0.125f;
//        if(dir2.y > 0f)
//            dir2.y += 0.125f;
//        else if(dir2.y < 0f)
//            dir2.y -= 0.125f;

        if(pos.x-extent.x <= Ref.world.WorldMin.x || pos.y-extent.y <= Ref.world.WorldMin.y ||
                pos.x + extent.x >= Ref.world.WorldMax.x || pos.y + extent.y >= Ref.world.WorldMax.y)
        {
            // hit world bounds

            res.Hit = true;
            res.frac = 0f;
            res.hitmask = Collision.MASK_WORLD;
            return res;
        }
        
        // Trace against blocks
        if((tracemask & MASK_WORLD) == MASK_WORLD) {
//            Segment segment = new Segment();
//            segment.P2 = new Vector2f();
//            Segment clipSeg = new Segment();
            SpatialQuery result = Ref.spatial.Query(pos.x-extent.x-(dir.x>0f?0:dir.x)-5, pos.y-extent.y-(dir.y>0f?0:dir.y)-5,pos.x+extent.x+(dir.x<0f?0:dir.x)+5, pos.y+extent.y+(dir.x<0f?0:dir.y)+5);
            int queryNum = result.getQueryNum();
            Object object;
            Vector2f v = new Vector2f(-dir.x, -dir.y);
            float aminx = pos.x - extent.x;
            float amaxx = pos.x + extent.x;
            float aminy = pos.y - extent.y;
            float amaxy = pos.y + extent.y;
            res.frac = 1f;
            while((object = result.ReadNext()) != null) {
                if(object.getClass() != Block.class)
                    continue;
                Block block = (Block)object;
                if(block.LastQueryNum == queryNum)
                    continue; // duplicate
                block.LastQueryNum = queryNum;

                if(block.CustomVal != 0 || !block.Collidable)
                    continue;

                Vector2f bPos = block.getPosition();
                Vector2f bSize = block.getSize();
                float bmaxx = bPos.x + bSize.x;
                float bmaxy = bPos.y + bSize.y;
                // Early exit if input and block is overlapping

                
                if(TestAABBAABB(aminx, aminy, amaxx, amaxy, bPos.x, bPos.y, bmaxx, bmaxy)) {
                    res.frac = 0f;
                    res.Hit = true;
                    res.hitObject = block;
                    res.hitmask = MASK_WORLD;
                    return res;
                }
                
                float first = 0f;
                float last = 1f;

                // amax = pos.x + extent.x

                if(v.x < 0.0f) {
                    if(bmaxx < aminx ) continue;
                    if(amaxx < bPos.x) first = Math.max((amaxx - bPos.x)/v.x, first);
                    if(bmaxx > aminx ) last = Math.min((aminx - bmaxx)/v.x,last);
                }
                else if(v.x > 0.0f) {
                    if(bPos.x>amaxx ) continue;
                    if(bmaxx < aminx) first = Math.max((aminx - bmaxx)/v.x,first);
                    if(amaxx > bPos.x) last = Math.min((amaxx - bPos.x)/v.x,last);
                } else {
                    if(bPos.x > amaxx  + EPSILON || bmaxx  + EPSILON < aminx)
                        continue;
                }
                if(first > last)
                    continue;
                if(v.y < 0.0f) {
                    if(bmaxy < aminy) continue;
                    if(amaxy < bPos.y ) first = Math.max((amaxy - bPos.y)/v.y, first);
                    if(bmaxy > aminy ) last = Math.min((aminy - bmaxy)/v.y,last);
                }
                else if(v.y > 0.0f) {
                    if(bPos.y>amaxy) continue;
                    if(bmaxy < aminy ) first = Math.max((aminy - bmaxy)/v.y,first);
                    if(amaxy > bPos.y) last = Math.min((amaxy - bPos.y)/v.y,last);
                } else
                {
                    if(bPos.y > amaxy + EPSILON || bmaxy + EPSILON < aminy)
                        continue;
                }
                if(first > last + EPSILON)
                    continue;

                if(res.frac < first)
                    continue;
                
                res.frac = first;
//                if(res.frac < 0.0f)
//                    res.frac = 0.0f;


                res.frac = 0f;
                res.hitObject = block;
                res.Hit = true;
                res.hitmask = MASK_WORLD;
//                return res;
//
//               segment.P1 = pos;
//                segment.P2.x = pos.x+dir2.x;
//                segment.P2.y = pos.y+dir2.y;
//
//                if(segment.Clip(block.getPosition().x - extent.x , block.getPosition().x + block.getSize().x + extent.x, block.getPosition().y  - extent.y, block.getPosition().y+ block.getSize().y + extent.y, clipSeg)) {
//                    float xfrac = 0f;
//                    if(dir2.x != 0 || dir2.y != 0) {
//
//                        xfrac = (clipSeg.P1.x - pos.x) / dir2.x;
//                        if(dir2.y != 0) {
//                        float yfrac = (clipSeg.P1.y - pos.y) / dir2.y;
//                        if(yfrac < xfrac || dir2.x == 0f)
//                            xfrac = yfrac;
//                        }
//                       // System.out.println(""+xfrac);
//                    }
//                    res.frac = 0f;
//                    res.Hit = true;
//                    res.hitObject = block;
//                    res.hitmask = MASK_WORLD;
//                }
            }
            if(res.Hit)
                return res;

//            AreaBlocks areaB = Ref.world.GetAreaBlocks(new Vector2f(pos.x-extent.x, pos.y-extent.y), new Vector2f(pos.x+extent.x, pos.y+extent.y));
//            for (int i= 0; i < areaB.dataOffset; i++) {
//                Block block = Ref.world.Blocks[areaB.data[i]];
//                if(block == null || !block.Collidable)
//                    continue;
//
//
//            }
        }
        // Trace against player
        if((tracemask & MASK_PLAYER) == MASK_PLAYER) {
            Vector2f ppos = Ref.world.player.position;
            Vector2f pextent = new Vector2f(Ref.world.player.extent.x, Ref.world.player.extent.y);

            pextent.x += extent.x;
            pextent.y += extent.y;
            if(pos.x >= ppos.x - pextent.x && pos.x <= ppos.x + pextent.x)
                if(pos.y >= ppos.y - pextent.y && pos.y <= ppos.y + pextent.y) {
                    res.frac = 0f;
                    res.Hit = true;
                    res.hitObject = Ref.world.player;
                    res.hitmask = MASK_PLAYER;
                }
    
        }

        // Trace against entities
        for (int i= 0; i < Ref.world.Entities.size(); i++) {
            Entity ent = Ref.world.Entities.get(i);

            int type = ent.GetType();
            if((type & tracemask) == 0)
                continue;

            Vector2f entPos = ent.GetPosition();
            Vector2f entSize = ent.GetSize();
            
            if(pos.x >= entPos.x - entSize.x && pos.x <= entPos.x + entSize.x)
                if(pos.y >= entPos.y - entSize.y && pos.y <= entPos.y + entSize.y) {
                    res.frac = 0f;
                    res.Hit = true;
                    res.hitObject = ent;
                    res.hitmask = ent.GetType();
                }
        }
        

        
        return res;
    }

    public static boolean TestAABBAABB(float aminx, float aminy, float amaxx, float amaxy,
                                        float bminx, float bminy, float bmaxx, float bmaxy) {
        if(aminx > bmaxx + EPSILON || amaxx < bminx - EPSILON)
            return false;

        if(aminy > bmaxy + EPSILON || amaxy < bminy - EPSILON)
            return false;

        return true;
    }

}
