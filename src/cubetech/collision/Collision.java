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

    private boolean TestPosition(Vector2f pos, Vector2f extent, Block testBlock) {
        Vector2f bCenter = testBlock.GetCenter();
        Vector2f bAbsExtent = testBlock.getAbsExtent();
        
        // Start off with an Abs test
        if(!TestAABBAABB(pos.x - extent.x, pos.y - extent.y, pos.x + extent.x, pos.y + extent.y,
                bCenter.x - bAbsExtent.x, bCenter.y - bAbsExtent.y, bCenter.x + bAbsExtent.x, bCenter.y + bAbsExtent.y))
            return false;

        if(testBlock.getAngle() == 0f) {
            // Block is not rotated, which means the AABB-AABB test was good enough
            return true;
        }

        // Do a precise test
        return TestRotatedPosition(pos, extent, testBlock);
    }

    // Does a full plane separation test
    private boolean TestRotatedPosition(Vector2f center, Vector2f Extent, Block block) {
        Vector2f Acenter = center;
        Vector2f AExtent = Extent;

        Vector2f Bcenter = block.GetCenter();
        Vector2f BExtent = block.GetExtents();
        Vector2f[] BAxis = block.GetAxis();

        // A -  X Axis
        float bextDot = BExtent.x * Math.abs(BAxis[0].x) + BExtent.y * Math.abs(BAxis[1].x);
        float aextDot = AExtent.x;

        float bDotPos = Bcenter.x;
        float aDotPos = Acenter.x;

        float bmin = bDotPos-bextDot;
        float bmax = bDotPos+bextDot;
        float amax = aDotPos+aextDot;
        float amin = aDotPos-aextDot;

        if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
            return false;

        // A - Y Axis
        bextDot = BExtent.x * Math.abs(BAxis[0].y) + BExtent.y * Math.abs(BAxis[1].y);
        aextDot = AExtent.y;
        bDotPos = Bcenter.y;
        aDotPos = Acenter.y;

        bmin = bDotPos-bextDot;
        bmax = bDotPos+bextDot;
        amax = aDotPos+aextDot;
        amin = aDotPos-aextDot;

        if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
            return false;


        // Test BAxis[0]
        bextDot = BExtent.x;
        aextDot = AExtent.x * Math.abs(Vector2f.dot(AAxis[0], BAxis[0])) + AExtent.y * Math.abs(Vector2f.dot(AAxis[1], BAxis[0]));
        bDotPos = Vector2f.dot(BAxis[0], Bcenter);
        aDotPos = Vector2f.dot(BAxis[0], Acenter);

        bmin = bDotPos-bextDot;
        bmax = bDotPos+bextDot;
        amax = aDotPos+aextDot;
        amin = aDotPos-aextDot;
        if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
            return false;

        // B - Y Axis
        bextDot = BExtent.y;
        double hags1 = Math.abs(Vector2f.dot(AAxis[0], BAxis[1]));
        double hags2 = Math.abs(Vector2f.dot(AAxis[1], BAxis[1]));
        aextDot = (float) (AExtent.x * hags1 + AExtent.y * hags2);

        bDotPos = Vector2f.dot(BAxis[1], Bcenter);
        aDotPos = Vector2f.dot(BAxis[1], Acenter);

        bmin = bDotPos-bextDot;
        bmax = bDotPos+bextDot;
        amax = aDotPos+aextDot;
        amin = aDotPos-aextDot;
        if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
            return false;

        return true; // collision
    }

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

        boolean cheapTest = dir.length() == 0;

        if(boxTrace) {
            if(cheapTest)
            {
                if(TestPosition(start, extent, tempCollisionBox)) {
                    // Collided
                    res.frac = 0f;
                    res.Hit = true;
                    res.HitAxis.set(0,0);
                    res.hitmask = Content.SOLID; // unknown hitmask
                    res.startsolid = true;
                }
            } else
                Test(start, extent, dir, tempCollisionBox, res);
        } else {
            // Trace a group of boxes
            // Get bounds
            BlockModel model = Ref.cm.cm.getModel(submodel);
            model.moveTo(submodelOrigin);


            for (Block block : model.blocks) {
                if(!block.Collidable)
                    continue;

                if(cheapTest) {
                    if(TestPosition(start, extent, block)) {
                        // hit block
                        res.frac = 0f;
                        res.Hit = true;
                        res.HitAxis.set(0,0);
                        res.hitmask = Content.SOLID;
                        res.startsolid = true;
                    }
                } else
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

        Vector2f hitaxis = null;

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
        double hags1 = Math.abs(Vector2f.dot(AAxis[0], BAxis[1]));
        double hags2 = Math.abs(Vector2f.dot(AAxis[1], BAxis[1]));
        aextDot = (float) (AExtent.x * hags1 + AExtent.y * hags2);

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

        if(first - EPSILON < res.frac && hitaxis != null) {
            res.frac = first - EPSILON;
            if(res.frac < 0f)
                res.frac = 0f;
            res.Hit = true;
            res.entitynum = Common.ENTITYNUM_WORLD;
            res.hitmask = Content.SOLID;
            res.HitAxis.set(hitaxis);
            res.startsolid = startsolid;
        }
    }

    // True if collision occured
    public CollisionResult TestMovement(Vector2f pos, Vector2f dir, Vector2f extent, int tracemask) {
        CollisionResult res = GetNext();
        res.Reset(pos, dir, extent);

        // Do the cheap tests if we're not trying to move
        boolean stationary = dir.length() == 0;
        
        // Trace against blocks
        if((tracemask & Content.SOLID) == Content.SOLID) {
            Vector2f v = dir;
            v.scale(-1.0f); // uhh.. yeah.. we're moving the block instead of the player
                            // cant remeber why. Probably not any good reason.
            res.frac = 1f;

            SpatialQuery result = Ref.spatial.Query(pos.x-extent.x+(dir.x>0f?0:dir.x)-5, pos.y-extent.y+(dir.y>0f?0:dir.y)-5,pos.x+extent.x+(dir.x<0f?0:dir.x)+5, pos.y+extent.y+(dir.x>0f?dir.y:0)+5);
            int queryNum = result.getQueryNum();
            Object object;
            while((object = result.ReadNext()) != null) {
                if(object.getClass() != Block.class)
                    continue; // Todo: Make everything Blocks. Will save a bit of casting and typechecking

                Block block = (Block)object;
                if(block.LastQueryNum == queryNum)
                    continue; // duplicate
                block.LastQueryNum = queryNum;

                // Ignore non-collidables and block belonging to BlockModels
                if(block.CustomVal != 0 || !block.Collidable)
                    continue;

                // Cheap test?
                if(stationary) {
                    if(TestPosition(pos, extent, block)) {
                        // blocked
                        res.frac = 0f;
                        res.startsolid = true;
                        res.hitmask = Content.SOLID; // hit world
                        res.HitAxis.set(0,0);
                        res.Hit = true;
                        break; // no reason to keep testing
                    }
                } else // Test with velocity
                    Test(pos, extent, v, block, res);
            }
            
            // Revert dir to original value
            v.scale(-1.0f);

            // Hit world
            if(res.Hit && res.HitAxis != null) {
                // FIX: Shouldn't be necesarry
//                res.HitAxis.normalise();
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


    // For debugging
    public int getBufferOffset() {
        return BufferOffset-1;
    }

    public CollisionResult[] getResultBuffer() {
        return resultBuffer;
    }

    

}
