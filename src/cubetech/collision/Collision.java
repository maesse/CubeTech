package cubetech.collision;

import cubetech.Block;
import cubetech.common.Common;
import cubetech.common.Content;

import cubetech.misc.Ref;
import cubetech.spatial.SpatialQuery;
import org.lwjgl.util.vector.Vector2f;

/**
 * Collision class. Handles OBB's and point of impact
 * @author mads
 */
public class Collision {
    public static final float EPSILON = 0.0625f;


    static final int RESULT_BUFFER_SIZE = 256;
    private CollisionResult[] resultBuffer = new CollisionResult[RESULT_BUFFER_SIZE];
    private int BufferOffset = 0;
    Vector2f AAxis[] = new Vector2f[2];
    Block tempCollisionBox = new Block(-1, new Vector2f(), new Vector2f(1, 1), false);

    public Collision() {
        // Init CollisionBuffers
        for (int i= 0; i < RESULT_BUFFER_SIZE; i++) {
            resultBuffer[i] = new CollisionResult();
        }
//        Test();
        AAxis[0] = new Vector2f(1, 0);
        AAxis[1] = new Vector2f(0, 1);

        //Block testB = new Block(-2, new Vector2f(-1, -1), new Vector2f(2,2), false);
        //testB.SetAngle(-(float)Math.PI*0.25f);
        //Test(new Vector2f(0, 4), new Vector2f(1,1), new Vector2f(0, 2), testB, resultBuffer[0]);
    }

    // Get next collisionresult from the circular buffer
    private CollisionResult GetNext() {
        return resultBuffer[BufferOffset++ & RESULT_BUFFER_SIZE-1];
    }

    /**
     *
     * @param mins 
     * @param maxs
     * @param origin
     */
    public void SetBoxModel(Vector2f extent, Vector2f origin) {
        tempCollisionBox.SetCentered(origin, extent);
        boxTrace = true; // next BoxTrace will use the boxmodel
    }

    public void SetSubModel(int index, Vector2f origin) {
        submodelOrigin = origin;
        submodel = index;
        boxTrace = false; // next boxTrace wont use the boxmodel
    }

    private boolean boxTrace = false;
    private int submodel = 0;
    private Vector2f submodelOrigin = null;

    public CollisionResult TransformedBoxTrace(Vector2f startin, Vector2f end, Vector2f mins, Vector2f maxs, int tracemask) {
        Vector2f extent = new Vector2f();
        Vector2f.sub(maxs, mins, extent);
        extent.x /= 2f;
        extent.y /= 2f;

        Vector2f start = new Vector2f(startin);
        Vector2f.sub(maxs, extent, start);
        Vector2f.add(start, startin, start);

        Vector2f dir = new Vector2f();
        if(end != null)
            Vector2f.sub(start, end, dir);

        CollisionResult res = GetNext();
        res.Reset(start, dir, extent);

        if(boxTrace)
            Test(start, extent, dir, tempCollisionBox, res);
        else {
            // Trace a group of boxes
            // Get bounds
            BlockModel model = Ref.cm.cm.getModel(submodel);
            model.moveTo(submodelOrigin);

            // position - originToCenter = centerBounds -> position movement vector
//            Vector2f centerToPosition = new Vector2f();
//            Vector2f.sub(submodelOrigin, model.center, centerToPosition);

            for (Block block : model.blocks) {
//                Vector2f newposition = new Vector2f();
//                Vector2f.add(block.getPosition(), centerToPosition, newposition);
//                block.SetPosition(newposition);
                if(!block.Collidable)
                    continue;
                Test(start, extent, dir, block, res);
                if(res.frac == 0f)
                    break;
            }
        }

        return res;
    }

    Vector2f derpAxis = new Vector2f();
    void Test(Vector2f center, Vector2f Extent, Vector2f v, Block block, CollisionResult res) {
        Vector2f Acenter = center;
        Vector2f AExtent = Extent;
        
        Vector2f Bcenter = block.GetCenter();
        Vector2f BExtent = block.GetExtents();
        Vector2f[] BAxis = block.GetAxis();

        Vector2f hitaxis = derpAxis;

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

        boolean startsolid = false;

        if(axisVel < 0.0f) { // moving left
            if(bmax < amin) return; // player max is already to the left
            // player min is to the right of block
            if(amax < bmin) { float fv = (amax - bmin+EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = AAxis[0];} }
            else
                startsolid = true;
            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
        } else if(axisVel > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = AAxis[0];} }
            else
                startsolid = true;
            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
            else
                startsolid = true;
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
            else
                startsolid = true;
            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
        } else if(axisVel > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = AAxis[1];} }
            else
                startsolid = true;
            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
            else
                startsolid = true;
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
            else
                startsolid = true;
            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
        } else if(axisVel > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = BAxis[0];} }
            else
                startsolid = true;
            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
            else
                startsolid = true;
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
            else
                startsolid = true;
            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
        } else if(axisVel > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = BAxis[1];} }
            else
                startsolid = true;
            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
            else
                startsolid = true;
        }

        if(first > last)
            return;

        if(first - EPSILON < res.frac) {
            res.frac = first - EPSILON;
            if(res.frac < 0f)
                res.frac = 0f;
            res.Hit = true;
            res.entitynum = Common.ENTITYNUM_WORLD;
            res.hitmask = Content.SOLID;
            res.HitAxis = hitaxis;
            res.startsolid = startsolid;
        }
    }

    // True if collision occured
    public CollisionResult TestPosition(Vector2f pos, Vector2f dir, Vector2f extent, int tracemask) {
        CollisionResult res = GetNext();
        res.Reset(pos, dir, extent);
        
        // Trace against blocks
        if((tracemask & Content.SOLID) == Content.SOLID) {
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
                float lenght = (float) Math.sqrt(res.HitAxis.x * res.HitAxis.x + res.HitAxis.y * res.HitAxis.y);
                if(lenght != 0f) {
                    res.HitAxis.x /= lenght;
                    res.HitAxis.y /= lenght;
                }

                return res;
            }

        }
        
//        // Trace against player
//        if((tracemask & MASK_PLAYER) == MASK_PLAYER) {
//            Vector2f ppos = Ref.world.player.position;
//            Vector2f pextent = new Vector2f(Ref.world.player.extent.x, Ref.world.player.extent.y);
//
//            pextent.x += extent.x;
//            pextent.y += extent.y;
//            if(pos.x >= ppos.x - pextent.x && pos.x <= ppos.x + pextent.x)
//                if(pos.y >= ppos.y - pextent.y && pos.y <= ppos.y + pextent.y) {
//                    res.frac = 0f;
//                    res.Hit = true;
//                    res.entitynum = Common.ENTITYNUM_NONE;
//                    res.hitmask = MASK_PLAYER;
//                }
//        }
//
//        // Trace against entities
//        for (int i= 0; i < Ref.world.Entities.size(); i++) {
//            Entity ent = Ref.world.Entities.get(i);
//
//            int type = ent.GetType();
//            if((type & tracemask) == 0)
//                continue;
//
//            Vector2f entPos = ent.GetPosition();
//            Vector2f entSize = ent.GetSize();
//
//            if(pos.x >= entPos.x - entSize.x && pos.x <= entPos.x + entSize.x)
//                if(pos.y >= entPos.y - entSize.y && pos.y <= entPos.y + entSize.y) {
//                    res.frac = 0f;
//                    res.Hit = true;
//                    res.hitObject = ent;
//                    res.hitmask = ent.GetType();
//                }
//        }
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


    // For debugging
    public int getBufferOffset() {
        return BufferOffset-1;
    }

    public CollisionResult[] getResultBuffer() {
        return resultBuffer;
    }

    

}
