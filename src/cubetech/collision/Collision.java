package cubetech.collision;

import cubetech.Block;
import cubetech.common.Common;
import cubetech.common.Content;

import cubetech.misc.Ref;
import cubetech.spatial.SpatialQuery;
import java.util.HashMap;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Collision class. Handles OBB's and point of impact
 * @author mads
 */
public class Collision {
    public static final float EPSILON = 0.0625f;
    static final int RESULT_BUFFER_SIZE = 256;
    private CollisionResult[] resultBuffer = new CollisionResult[RESULT_BUFFER_SIZE];
    private int BufferOffset = 0;
    
//    Vector2f AAxis[] = new Vector2f[2];

    // block submodel
    private boolean boxTrace = false;
    private Vector3f box_origin = new Vector3f();
    private Vector3f box_extent = new Vector3f();

    Vector3f cmmin = new Vector3f(), cmmax = new Vector3f(), cextent = new Vector3f(), cdiff = new Vector3f()
            , cstart = new Vector3f();
    int[] cCubePosition = new int[3];

    // x+, x-, y+, y-, z+, z-
    private static Vector3f[] AABBNormals = new Vector3f[] {
        new Vector3f(1,0,0),
        new Vector3f(-1,0,0),
        new Vector3f(0,1,0),
        new Vector3f(0,-1,0),
        new Vector3f(0,0,1),
        new Vector3f(0,0,-1)
    };

    public Collision() {
        // Init CollisionBuffers
        for (int i= 0; i < RESULT_BUFFER_SIZE; i++) {
            resultBuffer[i] = new CollisionResult();
        }
//        AAxis[0] = new Vector2f(1, 0);
//        AAxis[1] = new Vector2f(0, 1);
    }
//
//    private boolean TestPosition(Vector2f pos, Vector2f extent, Block testBlock) {
//        Vector2f bCenter = testBlock.GetCenter();
//        Vector2f bAbsExtent = testBlock.getAbsExtent();
//        
//        // Start off with an Abs test
//        if(!TestAABBAABB(pos.x - extent.x, pos.y - extent.y, pos.x + extent.x, pos.y + extent.y,
//                bCenter.x - bAbsExtent.x, bCenter.y - bAbsExtent.y, bCenter.x + bAbsExtent.x, bCenter.y + bAbsExtent.y))
//            return false;
//
//        if(testBlock.getAngle() == 0f) {
//            // Block is not rotated, which means the AABB-AABB test was good enough
//            return true;
//        }
//
//        // Do a precise test
//        return TestRotatedPosition(pos, extent, testBlock);
//    }

//    // Does a full plane separation test
//    private boolean TestRotatedPosition(Vector2f center, Vector2f Extent, Block block) {
//        Vector2f Acenter = center;
//        Vector2f AExtent = Extent;
//
//        Vector2f Bcenter = block.GetCenter();
//        Vector2f BExtent = block.GetExtents();
//        Vector2f[] BAxis = block.GetAxis();
//
//        // A -  X Axis
//        float bextDot = BExtent.x * Math.abs(BAxis[0].x) + BExtent.y * Math.abs(BAxis[1].x);
//        float aextDot = AExtent.x;
//
//        float bDotPos = Bcenter.x;
//        float aDotPos = Acenter.x;
//
//        float bmin = bDotPos-bextDot;
//        float bmax = bDotPos+bextDot;
//        float amax = aDotPos+aextDot;
//        float amin = aDotPos-aextDot;
//
//        if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
//            return false;
//
//        // A - Y Axis
//        bextDot = BExtent.x * Math.abs(BAxis[0].y) + BExtent.y * Math.abs(BAxis[1].y);
//        aextDot = AExtent.y;
//        bDotPos = Bcenter.y;
//        aDotPos = Acenter.y;
//
//        bmin = bDotPos-bextDot;
//        bmax = bDotPos+bextDot;
//        amax = aDotPos+aextDot;
//        amin = aDotPos-aextDot;
//
//        if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
//            return false;
//
//
//        // Test BAxis[0]
//        bextDot = BExtent.x;
//        aextDot = AExtent.x * Math.abs(Vector2f.dot(AAxis[0], BAxis[0])) + AExtent.y * Math.abs(Vector2f.dot(AAxis[1], BAxis[0]));
//        bDotPos = Vector2f.dot(BAxis[0], Bcenter);
//        aDotPos = Vector2f.dot(BAxis[0], Acenter);
//
//        bmin = bDotPos-bextDot;
//        bmax = bDotPos+bextDot;
//        amax = aDotPos+aextDot;
//        amin = aDotPos-aextDot;
//        if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
//            return false;
//
//        // B - Y Axis
//        bextDot = BExtent.y;
//        double hags1 = Math.abs(Vector2f.dot(AAxis[0], BAxis[1]));
//        double hags2 = Math.abs(Vector2f.dot(AAxis[1], BAxis[1]));
//        aextDot = (float) (AExtent.x * hags1 + AExtent.y * hags2);
//
//        bDotPos = Vector2f.dot(BAxis[1], Bcenter);
//        aDotPos = Vector2f.dot(BAxis[1], Acenter);
//
//        bmin = bDotPos-bextDot;
//        bmax = bDotPos+bextDot;
//        amax = aDotPos+aextDot;
//        amin = aDotPos-aextDot;
//        if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
//            return false;
//
//        return true; // collision
//    }


    
    /**
     * Does a AABB -> AABB trace on the loaded cubemap
     * @param start
     * @param delta
     * @param mins
     * @param maxs
     * @return
     */
    public CollisionResult traceCubeMap(Vector3f start, Vector3f delta, Vector3f mins, Vector3f maxs, boolean server) {
        // Prepare a collision result
        CollisionResult res = GetNext();
        res.reset(start, delta, maxs);
        
        // Create a bounding volume for the move
        Vector3f.add(start, mins, cmmin);
        Vector3f.add(start, maxs, cmmax);
        if(delta.x < 0) cmmin.x += delta.x; else cmmax.x += delta.x;
        if(delta.y < 0) cmmin.y += delta.y; else cmmax.y += delta.y;
        if(delta.z < 0) cmmin.z += delta.z; else cmmax.z += delta.z;

        delta.scale(-1f);

        // query the cube map
        ChunkAreaQuery area = CubeMap.getCubesInVolume(cmmin, cmmax, server?Ref.cm.cubemap.chunks:Ref.cgame.map.chunks, server);
        //int[] cubePosition = new int[3];

        Vector3f.sub(maxs, mins, cextent);

        Vector3f.add(cextent, mins, cdiff);
        Vector3f.add(cdiff, mins, cdiff);
        cdiff.scale(0.5f);
        cextent.scale(0.5f);

//        cdiff.set(mins);
//        cdiff.scale(-1f);
//        Vector3f.sub(cdiff, maxs, cdiff);
//        cdiff.scale(-0.5f);


        Vector3f.add(start, cdiff, cstart);

        // iterate though results
        while(area.getNext(cCubePosition) != null) {
            // Test swept position
            if(TestAABBAABB(cCubePosition[0]-1, cCubePosition[1]-1, cCubePosition[2]-1,
                            cCubePosition[0] + CubeChunk.BLOCK_SIZE+1, cCubePosition[1] + CubeChunk.BLOCK_SIZE+1, cCubePosition[2] + CubeChunk.BLOCK_SIZE+1,
                            cmmin.x, cmmin.y, cmmin.z, cmmax.x, cmmax.y, cmmax.z)) {

                TestAABBCube(cstart, delta, cextent, cCubePosition, CubeChunk.BLOCK_SIZE, res);
                if(res.frac == 0f) {
                    break;
                }
            }
        }
        delta.scale(-1f);
        
        return res;
    }

    private Vector3f tempPos = new Vector3f();
    private Vector3f tempExtent = new Vector3f();
    public void TestAABBCube(Vector3f center, Vector3f v, Vector3f Extent, int[] cubePosition, int cubeSize, CollisionResult res) {
        tempPos.set(cubePosition[0],cubePosition[1],cubePosition[2]);
        tempExtent.set(cubeSize,cubeSize,cubeSize);
        testAABBAABB(center, v, Extent, tempPos, tempExtent, res);
    }

    public void TestAABBBox(Vector3f center, Vector3f v, Vector3f Extent, CollisionResult res) {
        // Go from centered to mins + size
        Vector3f.sub(box_origin, box_extent, tempPos);
        tempExtent.set(box_extent);
        tempExtent.scale(2f);
        testAABBAABB(center, v, Extent, box_origin, box_extent, res);
    }

    public void testAABBAABB(Vector3f center, Vector3f v, Vector3f Extent, Vector3f test_position, Vector3f test_size, CollisionResult res) {
        Vector3f hitaxis = null;
        boolean startsolid = false;

        float first = 0f;
        float last = 1f;

        float bmin = test_position.x;
        float bmax = test_position.x+test_size.x;
        float amax = center.x + Extent.x;
        float amin = center.x - Extent.x;
        if(v.x < 0.0f) { // moving left
            if(bmax < amin) return; // player max is already to the left
            // player min is to the right of block
            if(amax < bmin) { float fv = (amax - bmin)/v.x; if(fv > first) { first = fv - EPSILON; hitaxis = AABBNormals[1];} }
            else
                startsolid = true;
            if(bmax > amin) last = Math.min((amin-bmax)/v.x,last);
        } else if(v.x > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax)/v.x; if(fv > first) { first = fv - EPSILON; hitaxis = AABBNormals[0];} }
            else
                startsolid = true;
            if(amax > bmin) last = Math.min((amax - bmin)/v.x,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
            else
                startsolid = true;
        }

        if(first > last)
            return;

        // A - Y Axis
        bmin = test_position.y;
        bmax = test_position.y+test_size.y;
        amax = center.y + Extent.y;
        amin = center.y - Extent.y;

        if(v.y < 0.0f) {
            if(bmax < amin) return;
            if(amax < bmin) { float fv = (amax - bmin)/v.y; if(fv > first){ first = fv - EPSILON; hitaxis = AABBNormals[3];} }
            else
                startsolid = true;
            if(bmax > amin) last = Math.min((amin-bmax)/v.y,last);
        } else if(v.y > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax)/v.y; if(fv > first) { first = fv - EPSILON; hitaxis = AABBNormals[2];} }
            else
                startsolid = true;
            if(amax > bmin) last = Math.min((amax - bmin)/v.y,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
            else
                startsolid = true;
        }

        if(first > last)
            return;

        // A - Z Axis
        bmin = test_position.z;
        bmax = test_position.z+test_size.z;
        amax = center.z + Extent.z;
        amin = center.z - Extent.z;

        if(v.z < 0.0f) {
            if(bmax < amin) return;
            if(amax < bmin) { float fv = (amax - bmin)/v.z; if(fv > first){ first = fv - EPSILON; hitaxis = AABBNormals[5];} }
            else
                startsolid = true;
            if(bmax > amin) last = Math.min((amin-bmax)/v.z,last);
        } else if(v.z > 0.0f) {
            if(bmin > amax) return;
            if(bmax < amin) { float fv = (amin - bmax)/v.z; if(fv > first) { first = fv - EPSILON; hitaxis = AABBNormals[4];} }
            else
                startsolid = true;
            if(amax > bmin) last = Math.min((amax - bmin)/v.z,last);
        } else {
            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
                return;
            else
                startsolid = true;
        }

        if(first > last)
            return;

        if(first < res.frac && hitaxis != null) {
            res.frac = first;
            if(res.frac < 0f)
                res.frac = 0f;
            res.hit = true;
            res.entitynum = Common.ENTITYNUM_WORLD;
            res.hitmask = Content.SOLID;
            res.hitAxis.set(hitaxis);
            res.startsolid = startsolid;
        } else if(startsolid && hitaxis == null) {
            res.frac = 0f;
            res.startsolid = startsolid;
            res.entitynum = Common.ENTITYNUM_WORLD;
            res.hitmask = Content.SOLID;
            res.hit = true;
        }
    }

    public CollisionResult TransformedBoxTrace(Vector3f startin, Vector3f end, Vector3f mins, Vector3f maxs, int tracemask) {
        Vector3f extent = new Vector3f();
        Vector3f.sub(maxs, mins, extent);
        extent.scale(0.5f);

        Vector3f start = new Vector3f(startin);
        Vector3f dir = new Vector3f();
        if(end != null)
            Vector3f.sub(start, end, dir);

        CollisionResult res = GetNext();
        res.reset(start, dir, extent);

//        boolean cheapTest = dir.length() == 0;

        assert(boxTrace);
        TestAABBBox(start,dir, extent , res);
        return res;
    }
//
//
//    void Test(Vector2f center, Vector2f Extent, Vector2f v, Block block, CollisionResult res) {
//
//
//        Vector2f Acenter = center;
//        Vector2f AExtent = Extent;
//
//        Vector2f Bcenter = block.GetCenter();
//        Vector2f BExtent = block.GetExtents();
//        Vector2f[] BAxis = block.GetAxis();
//
//        Vector2f hitaxis = null;
//
//        float first = 0f;
//        float last = 1f;
//
//        // A -  X Axis
//        float axisVel = v.x;
//
//        float bextDot = BExtent.x * Math.abs(BAxis[0].x) + BExtent.y * Math.abs(BAxis[1].x);
//        float aextDot = AExtent.x;
//
//        float bDotPos = Bcenter.x;
//        float aDotPos = Acenter.x;
//
//        float bmin = bDotPos-bextDot;
//        float bmax = bDotPos+bextDot;
//        float amax = aDotPos+aextDot;
//        float amin = aDotPos-aextDot;
//
//        boolean startsolid = false;
//
//        if(axisVel < 0.0f) { // moving left
//            if(bmax < amin) return; // player max is already to the left
//            // player min is to the right of block
//            if(amax < bmin) { float fv = (amax - bmin+EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = AAxis[0];} }
//            else
//                startsolid = true;
//            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
//        } else if(axisVel > 0.0f) {
//            if(bmin > amax) return;
//            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = AAxis[0];} }
//            else
//                startsolid = true;
//            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
//        } else {
//            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
//                return;
//            else
//                startsolid = true;
//        }
//
//        if(first > last)
//            return;
//
//        // A - Y Axis
//        axisVel = v.y;
//        bextDot = BExtent.x * Math.abs(BAxis[0].y) + BExtent.y * Math.abs(BAxis[1].y);
//        aextDot = AExtent.y;
//        bDotPos = Bcenter.y;
//        aDotPos = Acenter.y;
//
//        bmin = bDotPos-bextDot;
//        bmax = bDotPos+bextDot;
//        amax = aDotPos+aextDot;
//        amin = aDotPos-aextDot;
//
//        if(axisVel < 0.0f) {
//            if(bmax < amin) return;
//            if(amax < bmin) { float fv = (amax - bmin+EPSILON)/axisVel; if(fv > first){ first = fv; hitaxis = AAxis[1];} }
//            else
//                startsolid = true;
//            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
//        } else if(axisVel > 0.0f) {
//            if(bmin > amax) return;
//            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = AAxis[1];} }
//            else
//                startsolid = true;
//            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
//        } else {
//            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
//                return;
//            else
//                startsolid = true;
//        }
//
//        if(first > last)
//            return;
//
//        // Test BAxis[0]
//        axisVel = Vector2f.dot(v, BAxis[0]);
//        bextDot = BExtent.x;
//        aextDot = AExtent.x * Math.abs(Vector2f.dot(AAxis[0], BAxis[0])) + AExtent.y * Math.abs(Vector2f.dot(AAxis[1], BAxis[0]));
//        bDotPos = Vector2f.dot(BAxis[0], Bcenter);
//        aDotPos = Vector2f.dot(BAxis[0], Acenter);
//
//        bmin = bDotPos-bextDot;
//        bmax = bDotPos+bextDot;
//        amax = aDotPos+aextDot;
//        amin = aDotPos-aextDot;
//        if(axisVel < 0.0f) {
//            if(bmax < amin) return;
//            if(amax < bmin) { float fv = (amax - bmin+EPSILON)/axisVel; if(fv > first){ first = fv; hitaxis = BAxis[0];} }
//            else
//                startsolid = true;
//            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
//        } else if(axisVel > 0.0f) {
//            if(bmin > amax) return;
//            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = BAxis[0];} }
//            else
//                startsolid = true;
//            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
//        } else {
//            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
//                return;
//            else
//                startsolid = true;
//        }
//
//        if(first > last)
//            return;
//
//        // B - Y Axis
//        axisVel = Vector2f.dot(v, BAxis[1]);
//
//        bextDot = BExtent.y;
//        double hags1 = Math.abs(Vector2f.dot(AAxis[0], BAxis[1]));
//        double hags2 = Math.abs(Vector2f.dot(AAxis[1], BAxis[1]));
//        aextDot = (float) (AExtent.x * hags1 + AExtent.y * hags2);
//
//        bDotPos = Vector2f.dot(BAxis[1], Bcenter);
//        aDotPos = Vector2f.dot(BAxis[1], Acenter);
//
//        bmin = bDotPos-bextDot;
//        bmax = bDotPos+bextDot;
//        amax = aDotPos+aextDot;
//        amin = aDotPos-aextDot;
//        if(axisVel < 0.0f) {
//            if(bmax < amin) return;
//            if(amax < bmin) { float fv = (amax - bmin+EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = BAxis[1];} }
//            else
//                startsolid = true;
//            if(bmax > amin) last = Math.min((amin-bmax)/axisVel,last);
//        } else if(axisVel > 0.0f) {
//            if(bmin > amax) return;
//            if(bmax < amin) { float fv = (amin - bmax - EPSILON)/axisVel; if(fv > first) { first = fv; hitaxis = BAxis[1];} }
//            else
//                startsolid = true;
//            if(amax > bmin) last = Math.min((amax - bmin)/axisVel,last);
//        } else {
//            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
//                return;
//            else
//                startsolid = true;
//        }
//
//        if(first > last)
//            return;
//
//        if(first - EPSILON < res.frac && hitaxis != null) {
//            res.frac = first - EPSILON;
//            if(res.frac < 0f)
//                res.frac = 0f;
//            res.hit = true;
//            res.entitynum = Common.ENTITYNUM_WORLD;
//            res.hitmask = Content.SOLID;
//            res.hitAxis.set(hitaxis.x, hitaxis.y,0);
//            res.startsolid = startsolid;
//        }
//    }
//
//    // True if collision occured
//    public CollisionResult TestMovement(Vector2f pos, Vector2f dir, Vector2f extent, int tracemask) {
//        CollisionResult res = GetNext();
//        res.reset(pos, dir, extent);
//
//        // Do the cheap tests if we're not trying to move
//        boolean stationary = dir.length() == 0;
//
//        // Trace against blocks
//        if((tracemask & Content.SOLID) == Content.SOLID) {
//            Vector2f v = dir;
//            v.scale(-1.0f); // uhh.. yeah.. we're moving the block instead of the player
//                            // cant remeber why. Probably not any good reason.
//            res.frac = 1f;
//
//            SpatialQuery result = Ref.spatial.Query(pos.x-extent.x+(dir.x>0f?0:dir.x)-5, pos.y-extent.y+(dir.y>0f?0:dir.y)-5,pos.x+extent.x+(dir.x<0f?0:dir.x)+5, pos.y+extent.y+(dir.x>0f?dir.y:0)+5);
//            int queryNum = result.getQueryNum();
//            Object object;
//            while((object = result.ReadNext()) != null) {
//                if(object.getClass() != Block.class)
//                    continue; // Todo: Make everything Blocks. Will save a bit of casting and typechecking
//
//                Block block = (Block)object;
//                if(block.LastQueryNum == queryNum)
//                    continue; // duplicate
//                block.LastQueryNum = queryNum;
//
//                // Ignore non-collidables and block belonging to BlockModels
//                if(block.CustomVal != 0 || !block.Collidable)
//                    continue;
//
//                // Cheap test?
//                if(stationary) {
//                    if(TestPosition(pos, extent, block)) {
//                        // blocked
//                        res.frac = 0f;
//                        res.startsolid = true;
//                        res.hitmask = Content.SOLID; // hit world
//                        res.hitAxis.set(0,0);
//                        res.hit = true;
//                        break; // no reason to keep testing
//                    }
//                } else // Test with velocity
//                    Test(pos, extent, v, block, res);
//            }
//
//            // Revert dir to original value
//            v.scale(-1.0f);
//
//            // Hit world
//            if(res.hit && res.hitAxis != null) {
//                // FIX: Shouldn't be necesarry
////                res.HitAxis.normalise();
//            }
//        }
//
//        return res;
//    }

    /**
     * Returns true on collision
     * @param Position The point to test
     * @param x x-min
     * @param y y-min
     * @param x0 x-max
     * @param y0 y-max
     * @return
     */
    public static boolean TestPointAABB(Vector2f Position, float x, float y, float x0, float y0) {
        return (Position.x >= x && Position.x <= x0
                && Position.y >= y && Position.y <= y0);
    }

    public static boolean TestAABBAABB(float aminx, float aminy, float amaxx, float amaxy,
                                        float bminx, float bminy, float bmaxx, float bmaxy) {
        if(aminx >= bmaxx + EPSILON || amaxx <= bminx - EPSILON)
            return false;

        if(aminy >= bmaxy + EPSILON || amaxy <= bminy - EPSILON)
            return false;

        return true;
    }

    public static boolean TestAABBAABB(float aminx, float aminy, float aminz, float amaxx, float amaxy, float amaxz,
                                        float bminx, float bminy, float bminz, float bmaxx, float bmaxy, float bmaxz) {
        if(aminx >= bmaxx + EPSILON || amaxx <= bminx - EPSILON)
            return false;

        if(aminy >= bmaxy + EPSILON || amaxy <= bminy - EPSILON)
            return false;

        if(aminz >= bmaxz + EPSILON || amaxz <= bminz - EPSILON)
            return false;

        return true;
    }

    /**
     * Tests if the distance between two points are whithin the radius
     * @param p1
     * @param p2
     * @param radius
     * @return true on collision
     */
    public static boolean TestPointPointRadius(Vector2f p1, Vector2f p2, float radius) {
        Vector2f delta = new Vector2f();
        Vector2f.sub(p1, p2, delta);
        return radius*radius >= delta.lengthSquared();
    }


    // For debugging
    public int getBufferOffset() {
        return BufferOffset-1;
    }

    public CollisionResult[] getResultBuffer() {
        return resultBuffer;
    }
    // Get next collisionresult from the circular buffer
    private CollisionResult GetNext() {
        return resultBuffer[BufferOffset++ & RESULT_BUFFER_SIZE-1];
    }

    public void SetBoxModel(Vector3f mins, Vector3f maxs, Vector3f origin) {
        // get mins and maxs offset for centering
        Vector3f.add(origin, mins, box_origin);

        // grab the extents
        Vector3f.sub(maxs, mins, box_extent);
        boxTrace = true; // next BoxTrace will use the boxmodel
    }

    public void SetBoxModel(Vector3f extent, Vector3f origin) {
        Vector3f.sub(origin, extent, box_origin);
        box_extent.set(extent).scale(2f);
        boxTrace = true; // next BoxTrace will use the boxmodel
    }

    public void SetSubModel(int index, Vector3f origin) {
        boxTrace = false; // next boxTrace wont use the boxmodel
        throw new IllegalArgumentException();
        
    }
    

}
