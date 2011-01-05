/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.collision;

import cubetech.Block;
import cubetech.entities.Entity;
import cubetech.misc.Ref;
import cubetech.spatial.SpatialQuery;
import org.lwjgl.util.vector.Vector2f;
import org.openmali.FastMath;

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

    static final int RESULT_BUFFER_SIZE = 128;
    private CollisionResult[] resultBuffer = new CollisionResult[RESULT_BUFFER_SIZE];
    private int BufferOffset = 0;
    Vector2f AAxis[] = new Vector2f[2];

    public Collision() {
        // Init CollisionBuffers
        for (int i= 0; i < RESULT_BUFFER_SIZE; i++) {
            resultBuffer[i] = new CollisionResult();
        }
//        Test();
        AAxis[0] = new Vector2f(1, 0);
        AAxis[1] = new Vector2f(0, 1);

        Block testB = new Block(-2, new Vector2f(-1, -1), new Vector2f(2,2), false);
        testB.SetAngle(-(float)Math.PI*0.25f);
        Test(new Vector2f(0, 4), new Vector2f(1,1), new Vector2f(0, 2), testB, resultBuffer[0]);
    }



    

    // Get next collisionresult from the circular buffer
    private CollisionResult GetNext() {
        return resultBuffer[BufferOffset++ & RESULT_BUFFER_SIZE-1];
    }

//    public Block TestStaticPosition(Vector2f pos, Vector2f extent, int tracemask) {
//        // Trace against blocks
//        if((tracemask & MASK_WORLD) == MASK_WORLD) {
//            SpatialQuery result = Ref.spatial.Query(pos.x-extent.x-5, pos.y-extent.y-5,pos.x+extent.x+5, pos.y+extent.y+5);
//            int queryNum = result.getQueryNum();
//            Object object;
////            Vector2f v = new Vector2f(-dir.x, -dir.y);
//            float aminx = pos.x - extent.x;
//            float amaxx = pos.x + extent.x;
//            float aminy = pos.y - extent.y;
//            float amaxy = pos.y + extent.y;
////            res.frac = 1f;
//            while((object = result.ReadNext()) != null) {
//                if(object.getClass() != Block.class)
//                    continue;
//                Block block = (Block)object;
//                if(block.LastQueryNum == queryNum)
//                    continue; // duplicate
//                block.LastQueryNum = queryNum;
//
//                if(block.CustomVal != 0 || !block.Collidable)
//                    continue;
//
//                Vector2f bPos = block.getPosition();
//                Vector2f bSize = block.getSize();
//                float bmaxx = bPos.x + bSize.x;
//                float bmaxy = bPos.y + bSize.y;
//                // Early exit if input and block is overlapping
//
//                if(TestAABBAABB(aminx, aminy, amaxx, amaxy, bPos.x, bPos.y, bmaxx, bmaxy)) {
//                    return block;
//                }
//            }
//        }
//        return null;
//    }

//    public static boolean TestAABBOBB(Vector2f aabbCenter, Vector2f aabbExtent, Block b) {
//        Vector2f bCenter = b.GetCenter();
//        Vector2f b1Axis[] = new Vector2f[2];
//        b1Axis[0] = new Vector2f(1, 0);
//        b1Axis[1] = new Vector2f(0, 1);
//        Vector2f b2Axis[] = b.GetAxis();
//        Vector2f b1E = aabbExtent;
//        Vector2f b2E = b.GetExtents();
//
//
//        Vector2f kD = new Vector2f(aabbCenter.x - bCenter.x, aabbCenter.y - bCenter.y);
//
//        float aafAbsAdB[][] = new float[2][2];
//
//
//        aafAbsAdB[0][0] = Math.abs(Vector2f.dot(b1Axis[0], b2Axis[0]));
//        aafAbsAdB[0][1] = Math.abs(Vector2f.dot(b1Axis[0], b2Axis[1]));
//        float fAbsAdD = Math.abs(Vector2f.dot(b1Axis[0], kD));
//        float fRSum = b1E.x + b2E.x*aafAbsAdB[0][0] + b2E.y*aafAbsAdB[0][1];
//        if(fAbsAdD - EPSILON > fRSum)
//            return false;
//
//        aafAbsAdB[1][0] = Math.abs(Vector2f.dot(b1Axis[1], b2Axis[0]));
//        aafAbsAdB[1][1] = Math.abs(Vector2f.dot(b1Axis[1], b2Axis[1]));
//        fAbsAdD = Math.abs(Vector2f.dot(b1Axis[1], kD));
//        fRSum = b1E.y + b2E.x*aafAbsAdB[1][0] + b2E.y*aafAbsAdB[1][1];
//        if(fAbsAdD - EPSILON > fRSum)
//            return false;
//
//        fAbsAdD = Math.abs(Vector2f.dot(b2Axis[0],kD));
//        fRSum = b2E.x + b1E.x*aafAbsAdB[0][0] + b1E.y*aafAbsAdB[1][0];
//        if ( fAbsAdD - EPSILON > fRSum )
//            return false;
//
//        fAbsAdD = Math.abs(Vector2f.dot(b2Axis[1],kD));
//        fRSum = b2E.y + b1E.x*aafAbsAdB[0][1] + b1E.y*aafAbsAdB[1][1];
//        if ( fAbsAdD - EPSILON > fRSum )
//            return false;
//
//        return true;
//    }

    void Test(Vector2f center, Vector2f Extent, Vector2f v, Block block, CollisionResult res) {
        Vector2f Acenter = center;
        Vector2f AExtent = Extent;
        
        Vector2f Bcenter = block.GetCenter();
        Vector2f BExtent = block.GetExtents();
        Vector2f[] BAxis = block.GetAxis();

        Vector2f hitaxis = new Vector2f();

        float first = 0f;
        float last = 1f;

        // A -  X Axis
        float axisVel = v.x;
        
        float bextDot = BExtent.x * Math.abs(BAxis[0].x) + BExtent.y * Math.abs(BAxis[1].x);
        float aextDot = AExtent.x;

        float bDotPos = Bcenter.x;
        float aDotPos = Acenter.x;
        
        float bmin = bDotPos-bextDot;
        float bmax = bDotPos+bextDot;
        float amax = aDotPos+aextDot;
        float amin = aDotPos-aextDot;
        if(axisVel < 0.0f) {
            if(bmax < amin) return;
            if(amax < bmin) { float fv = (amax - bmin+EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = AAxis[0];} }
            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
        } else if(axisVel > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = AAxis[0];} }
            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
        }

        if(first > last)
            return;

        // A - Y Axis
        axisVel = v.y;
        bextDot = BExtent.x * Math.abs(BAxis[0].y) + BExtent.y * Math.abs(BAxis[1].y);
        aextDot = AExtent.y;
        bDotPos = Bcenter.y;
        aDotPos = Acenter.y;

        bmin = bDotPos-bextDot;
        bmax = bDotPos+bextDot;
        amax = aDotPos+aextDot;
        amin = aDotPos-aextDot;
        
        if(axisVel < 0.0f) {
            if(bmax < amin) return;
            if(amax < bmin) { float fv = (amax - bmin+EPSILON)/axisVel; if(fv > first){ first = fv; hitaxis = AAxis[1];} }
            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
        } else if(axisVel > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = AAxis[1];} }
            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
        }

        if(first > last)
            return;

        // Test BAxis[0]
        axisVel = Vector2f.dot(v, BAxis[0]);
        bextDot = BExtent.x;
        aextDot = AExtent.x * Math.abs(Vector2f.dot(AAxis[0], BAxis[0])) + AExtent.y * Math.abs(Vector2f.dot(AAxis[1], BAxis[0]));
        bDotPos = Vector2f.dot(BAxis[0], Bcenter);
        aDotPos = Vector2f.dot(BAxis[0], Acenter);

        bmin = bDotPos-bextDot;
        bmax = bDotPos+bextDot;
        amax = aDotPos+aextDot;
        amin = aDotPos-aextDot;
        if(axisVel < 0.0f) {
            if(bmax < amin) return;
            if(amax < bmin) { float fv = (amax - bmin+EPSILON)/axisVel; if(fv > first){ first = fv; hitaxis = BAxis[0];} }
            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
        } else if(axisVel > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = BAxis[0];} }
            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
        }

        if(first > last)
            return;

        // B - Y Axis
        axisVel = Vector2f.dot(v, BAxis[1]);

        bextDot = BExtent.y;
        float hags1 = Math.abs(Vector2f.dot(AAxis[0], BAxis[1]));
        float hags2 = Math.abs(Vector2f.dot(AAxis[1], BAxis[1]));
        aextDot = AExtent.x * hags1 + AExtent.y * hags2;

        bDotPos = Vector2f.dot(BAxis[1], Bcenter);
        aDotPos = Vector2f.dot(BAxis[1], Acenter);

        bmin = bDotPos-bextDot;
        bmax = bDotPos+bextDot;
        amax = aDotPos+aextDot;
        amin = aDotPos-aextDot;
        if(axisVel < 0.0f) {
            if(bmax < amin) return;
            if(amax < bmin) { float fv = (amax - bmin+EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = BAxis[1];} }
            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
        } else if(axisVel > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = BAxis[1];} }
            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
        }

        if(first > last)
            return;

        if(first - EPSILON < res.frac) {
            res.frac = first - EPSILON;
            res.Hit = true;
            res.hitObject = block;
            res.hitmask = MASK_WORLD;
            res.HitAxis = hitaxis;
        }
    }

//    boolean MovingAABBvsBlock(float aminx, float aminy, float amaxx, float amaxy, Vector2f v, Block block, CollisionResult res) {
//        Vector2f bPos = block.getPosition();
//        Vector2f bSize = block.getSize();
//        float bmaxx = bPos.x + bSize.x;
//        float bmaxy = bPos.y + bSize.y;
//        if(TestAABBAABB(aminx, aminy, amaxx, amaxy, bPos.x, bPos.y, bmaxx, bmaxy)) {
//            res.frac = 0f;
//            res.Hit = true;
//            res.hitObject = block;
//            res.hitmask = MASK_WORLD;
//            return true;
//        }
//
//        float first = 0f;
//        float last = 1f;
//
//        if(v.x < 0.0f) {
//            if(bmaxx < aminx ) return false;
//            if(amaxx < bPos.x) first = Math.max((amaxx - bPos.x+EPSILON)/(v.x), first);
//            if(bmaxx > aminx ) last = Math.min((aminx - bmaxx)/(v.x),last);
//        }
//        else if(v.x > 0.0f) {
//            if(bPos.x>amaxx ) return false;
//            if(bmaxx < aminx) first = Math.max((aminx - bmaxx-EPSILON)/(v.x),first);
//            if(amaxx > bPos.x) last = Math.min((amaxx - bPos.x)/(v.x),last);
//        } else {
//            if(bPos.x >= amaxx  + EPSILON || bmaxx  + EPSILON <= aminx)
//                return false;
//        }
//        if(first > last)
//            return false;
//
//        if(v.y < 0.0f) {
//            if(bmaxy < aminy) return false;
//            if(amaxy < bPos.y ) first = Math.max((amaxy - bPos.y+EPSILON)/(v.y), first);
//            if(bmaxy > aminy ) last = Math.min((aminy - bmaxy)/(v.y),last);
//        }
//        else if(v.y > 0.0f) {
//            if(bPos.y>amaxy) return false;
//            if(bmaxy < aminy ) first = Math.max((aminy - bmaxy-EPSILON)/(v.y),first);
//            if(amaxy > bPos.y) last = Math.min((amaxy - bPos.y)/(v.y),last);
//        } else
//        {
//            if(bPos.y >= amaxy + EPSILON || bmaxy + EPSILON <= aminy)
//                return false;
//        }
//        if(first > last)
//            return false;
//
//        if(res.frac < first)
//            return false;
//
//        if(first - EPSILON < res.frac) {
//            res.frac = first - EPSILON;
//            res.hitObject = block;
//            res.Hit = true;
//            res.hitmask = MASK_WORLD;
//        }
//        return true;
//    }
    
    // True if collision occured
    public CollisionResult TestPosition(Vector2f pos, Vector2f dir, Vector2f extent, int tracemask) {
        CollisionResult res = GetNext();
        res.Reset(pos, dir, extent);
        
        // Trace against blocks
        if((tracemask & MASK_WORLD) == MASK_WORLD) {
            Vector2f v = new Vector2f(-dir.x, -dir.y);
            res.frac = 1f;

            SpatialQuery result = Ref.spatial.Query(pos.x-extent.x-(dir.x>0f?0:dir.x)-5, pos.y-extent.y-(dir.y>0f?0:dir.y)-5,pos.x+extent.x+(dir.x<0f?0:dir.x)+5, pos.y+extent.y+(dir.x<0f?0:dir.y)+5);
            int queryNum = result.getQueryNum();
            Object object;
            while((object = result.ReadNext()) != null) {
                if(object.getClass() != Block.class)
                    continue;
                Block block = (Block)object;
                if(block.LastQueryNum == queryNum)
                    continue; // duplicate
                block.LastQueryNum = queryNum;

                if(block.CustomVal != 0 || !block.Collidable)
                    continue;

                Test(pos, extent, v, block, res);
            }

            // Hit world
            if(res.Hit) {
                float lenght = FastMath.sqrt(res.HitAxis.x * res.HitAxis.x + res.HitAxis.y * res.HitAxis.y);
                if(lenght != 0f) {
                    res.HitAxis.x /= lenght;
                    res.HitAxis.y /= lenght;
                }

                return res;
            }

//            // Check if player would be stuck at this new position
//            Vector2f newpos = new Vector2f(pos.x + dir.x, pos.y + dir.y);
//            Block collideBlock = TestStaticPosition(newpos, extent, tracemask);
//            if(collideBlock != null) {
//                res.frac = 1.0f;
//                MovingAABBvsBlock(aminx, aminy, amaxx, amaxy, v, collideBlock, res);
//                Ref.world.SetCollisionDebug();
//                return res;
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
        if(aminx >= bmaxx + EPSILON || amaxx <= bminx - EPSILON)
            return false;

        if(aminy >= bmaxy + EPSILON || amaxy <= bminy - EPSILON)
            return false;

        return true;
    }

//    public static boolean TestOBBOBB(Block b1, Block b2) {
//        Vector2f b1Center = b1.GetCenter();
//        Vector2f b2Center = b2.GetCenter();
//        Vector2f b1Axis[] = b1.GetAxis();
//        Vector2f b2Axis[] = b2.GetAxis();
//        Vector2f b1E = b1.GetExtents();
//        Vector2f b2E = b2.GetExtents();
//
//
//        Vector2f kD = new Vector2f(b1Center.x - b2Center.x, b1Center.y - b2Center.y);
//
//        float aafAbsAdB[][] = new float[2][2];
//
//
//        aafAbsAdB[0][0] = Math.abs(Vector2f.dot(b1Axis[0], b2Axis[0]));
//        aafAbsAdB[0][1] = Math.abs(Vector2f.dot(b1Axis[0], b2Axis[1]));
//        float fAbsAdD = Math.abs(Vector2f.dot(b1Axis[0], kD));
//        float fRSum = b1E.x + b2E.x*aafAbsAdB[0][0] + b2E.y*aafAbsAdB[0][1];
//        if(fAbsAdD > fRSum)
//            return false;
//
//        aafAbsAdB[1][0] = Math.abs(Vector2f.dot(b1Axis[1], b2Axis[0]));
//        aafAbsAdB[1][1] = Math.abs(Vector2f.dot(b1Axis[1], b2Axis[1]));
//        fAbsAdD = Math.abs(Vector2f.dot(b1Axis[1], kD));
//        fRSum = b1E.y + b2E.x*aafAbsAdB[1][0] + b2E.y*aafAbsAdB[1][1];
//        if(fAbsAdD > fRSum)
//            return false;
//
//        fAbsAdD = Math.abs(Vector2f.dot(b2Axis[0],kD));
//        fRSum = b2E.x + b1E.x*aafAbsAdB[0][0] + b1E.y*aafAbsAdB[1][0];
//        if ( fAbsAdD > fRSum )
//            return false;
//
//        fAbsAdD = Math.abs(Vector2f.dot(b2Axis[1],kD));
//        fRSum = b2E.y + b1E.x*aafAbsAdB[0][1] + b1E.y*aafAbsAdB[1][1];
//        if ( fAbsAdD > fRSum )
//            return false;
//
//        return true;
//
//
//    }

    // For debugging
    public int getBufferOffset() {
        return BufferOffset-1;
    }

    public CollisionResult[] getResultBuffer() {
        return resultBuffer;
    }

}
