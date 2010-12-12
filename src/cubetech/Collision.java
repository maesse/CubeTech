/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.entities.Entity;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class Collision {

    public static final int MASK_WORLD = 1;
    public static final int MASK_PLAYER = 2;
    public static final int MASK_BULLETS = 8;
    public static final int MASK_BOMBAH = 4;

    public static final int MASK_ENEMIES = 4;
    public static final int MASK_ALL = 1 | 2 | 4 | 8;
    
    // True if collision occured
    public CollisionResult TestPosition(Vector2f pos, Vector2f dir, Vector2f extent, int tracemask) {
        CollisionResult res = new CollisionResult();

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
            Segment segment = new Segment();
            segment.P2 = new Vector2f();
            Segment clipSeg = new Segment();
            AreaBlocks areaB = Ref.world.GetAreaBlocks(new Vector2f(pos.x-extent.x, pos.y-extent.y), new Vector2f(pos.x+extent.x, pos.y+extent.y));
            for (int i= 0; i < areaB.dataOffset; i++) {
                Block block = Ref.world.Blocks[areaB.data[i]];
                if(block == null || !block.Collidable)
                    continue;

                segment.P1 = pos;
                segment.P2.x = pos.x+dir.x;
                segment.P2.y = pos.y+dir.y;

                if(segment.Clip(block.Position.x - extent.x , block.Position.x + block.Size.x + extent.x, block.Position.y  - extent.y, block.Position.y+ block.Size.y + extent.y, clipSeg)) {
                    float xfrac = 0f;
                    if(dir.x != 0 || dir.y != 0) {

                        xfrac = (clipSeg.P1.x - pos.x) / dir.x;
                        if(dir.y != 0) {
                        float yfrac = (clipSeg.P1.y - pos.y) / dir.y;
                        if(yfrac < xfrac || dir.x == 0f)
                            xfrac = yfrac;
                        }
                       // System.out.println(""+xfrac);
                    }
                    res.frac = xfrac;
                    res.Hit = true;
                    res.hitObject = block;
                    res.hitmask = MASK_WORLD;
                }
            }
        }
        // Trace against player
        if((tracemask & MASK_PLAYER) == MASK_PLAYER) {
            Vector2f ppos = Ref.world.player.position;
            Vector2f pextent = Ref.world.player.extent;

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

}
