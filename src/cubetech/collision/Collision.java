package cubetech.collision;


import cubetech.common.Common;
import cubetech.common.Content;

import cubetech.misc.Ref;


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
    
    // block submodel
    private boolean boxTrace = false;
    private Vector3f box_origin = new Vector3f();
    private Vector3f box_extent = new Vector3f();

    Vector3f cmmin = new Vector3f(), cmmax = new Vector3f(), cextent = new Vector3f(), cdiff = new Vector3f()
            , cstart = new Vector3f();
    int[] cCubePosition = new int[3];

    private Vector3f tempPos = new Vector3f();
    private Vector3f tempExtent = new Vector3f();

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
    }
    
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

        cmmin.x -= 0.1f; cmmin.y -= 0.1f; cmmin.z -= 0.1f;
        cmmax.x += 0.1f; cmmax.y += 0.1f; cmmax.z += 0.1f;

        // query the cube map
        ChunkAreaQuery area = CubeMap.getCubesInVolume(cmmin, cmmax, server?Ref.cm.cubemap.chunks:Ref.cgame.map.chunks, server);
        if(area.isEmpty()) return res;

        cmmin.x += 0.1f; cmmin.y += 0.1f; cmmin.z += 0.1f;
        cmmax.x -= 0.1f; cmmax.y -= 0.1f; cmmax.z -= 0.1f;
        
        delta.scale(-1f);
        Vector3f.sub(maxs, mins, cextent);
        Vector3f.add(cextent, mins, cdiff);
        Vector3f.add(cdiff, mins, cdiff);
        cdiff.scale(0.5f);
        cextent.scale(0.5f);
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
        testAABBAABB(center, v, Extent, tempPos, tempExtent, res);
    }
    
    private class SATState {
        boolean startsolid;
        Vector3f hitaxis;
        float first;
        float last;
        
        void clear() {
            startsolid = false;
            hitaxis = null;
            first = -0.00000001f;
            last = 1;
        }
    }
    private SATState satState = new SATState();

    private boolean separateAxis(SATState state, float amin, float amax, float bmin, float bmax, float v, int axisOffset) {
        if(v < 0) { // moving left
            if(bmax < amin) return false; // player max is already to the left
            // player min is to the right of block
            if(amax  < bmin) {
                float fv = (amax - (bmin - EPSILON))/v;
                if(fv  > state.first) {
                    state.first = fv;
                    state.hitaxis = AABBNormals[axisOffset+1];
                }
            }
            else {
                state.startsolid = true;
            }
            if(bmax > amin) state.last = Math.min((amin-bmax)/v,state.last);
        } else if(v > 0) {
            if(bmin > amax) return false;
            if(bmax < amin) {
                float fv = (amin - (bmax + EPSILON))/v;
                if(fv   > state.first) {
                    state.first = fv ;
                    state.hitaxis = AABBNormals[axisOffset];
                }
            }
            else {
                state.startsolid = true;
            }
            if(amax > bmin) state.last = Math.min((amax - bmin)/v,state.last);
        } else {
            if(bmin >= amax + EPSILON || bmax  + EPSILON <= amin) return  false;
            else state.startsolid = true;
        }

        if(state.first > state.last)
            return  false;

        return true; // continue
    }

    public void testAABBAABB(Vector3f center, Vector3f v, Vector3f Extent, Vector3f test_position, Vector3f test_size, CollisionResult res) {
        satState.clear();


        float bmin = test_position.x;
        float bmax = test_position.x+test_size.x;
        float amax = center.x + Extent.x;
        float amin = center.x - Extent.x;
        if(!separateAxis(satState, amin, amax, bmin, bmax, v.x, 0)) return;

        // A - Y Axis
        bmin = test_position.y;
        bmax = test_position.y+test_size.y;
        amax = center.y + Extent.y;
        amin = center.y - Extent.y;

        if(!separateAxis(satState, amin, amax, bmin, bmax, v.y, 2)) return;

        // A - Z Axis
        bmin = test_position.z;
        bmax = test_position.z+test_size.z;
        amax = center.z + Extent.z;
        amin = center.z - Extent.z;

        if(!separateAxis(satState, amin, amax, bmin, bmax, v.z, 4)) return;

        if(satState.first > 0 && center.x == -60.0625f) {
            int test =2 ;
        }

//        if(v.z < 0.0f) {
//            if(bmax < amin) return;
//            if(amax < bmin) { float fv = (amax - bmin)/v.z; if(fv > first){ first = fv - EPSILON; hitaxis = AABBNormals[5];} }
//            else
//                startsolid = true;
//            if(bmax > amin) last = Math.min((amin-bmax)/v.z,last);
//        } else if(v.z > 0.0f) {
//            if(bmin > amax) return;
//            if(bmax < amin) { float fv = (amin - bmax)/v.z; if(fv > first) { first = fv- EPSILON; hitaxis = AABBNormals[4];} }
//            else
//                startsolid = true;
//            if(amax > bmin) last = Math.min((amax - bmin)/v.z,last);
//        } else {
//            if(bmin >= amax  + EPSILON || bmax  + EPSILON <= amin)
//                return;
//            else
//                startsolid = true;
//        }
//
//        if(first > last)
//            return;

        if(satState.first < res.frac && satState.hitaxis != null) {
            res.frac = satState.first;
            if(res.frac < 0f)
                res.frac = 0f;
            res.hit = true;
            res.entitynum = Common.ENTITYNUM_WORLD;
            res.hitmask = Content.SOLID;
            res.hitAxis.set(satState.hitaxis);
            res.startsolid = false;
        } else if(satState.startsolid && satState.hitaxis == null) {
            res.frac = 0f;
            res.startsolid = satState.startsolid;
            res.entitynum = Common.ENTITYNUM_WORLD;
            res.hitmask = Content.SOLID;
            res.hit = true;
        }
    }

    public CollisionResult TransformedBoxTrace(Vector3f startin, Vector3f end, Vector3f mins, Vector3f maxs, int tracemask) {
        Vector3f extent = new Vector3f();
        Vector3f.sub(maxs, mins, extent);
        extent.scale(0.5f);

        Vector3f diff = Vector3f.add(maxs, mins, null);
        diff.scale(0.5f);

        Vector3f start = Vector3f.add(startin, diff, null);
        Vector3f dir = new Vector3f();
        if(end != null)
            Vector3f.sub(startin, end, dir);

        CollisionResult res = GetNext();
        res.reset(start, dir, extent);
        
        TestAABBBox(start,dir, extent , res);
        return res;
    }

    private class Ray {
        Vector3f o = new Vector3f();
        Vector3f d = new Vector3f();
        Vector3f tempMin = new Vector3f();
        Vector3f tempMax = new Vector3f();
    }

    private Ray r = new Ray();

    public float TestAABBRay(Vector3f origin, Vector3f dir, 
            Vector3f aabb_org, Vector3f aabb_mins, Vector3f aabb_maxs,
            Vector3f hitaxis)
    {
        r.o = origin;
        r.d = dir;
        Vector3f p1 = Vector3f.add(aabb_org, aabb_mins, r.tempMin);
        Vector3f p2 = Vector3f.add(aabb_org, aabb_maxs, r.tempMax);
        float t1, t2, tmp;
        float tfar = Float.POSITIVE_INFINITY;
        float tnear = Float.NEGATIVE_INFINITY;
        int axis = 0;
        // check X slab
        if (r.d.x == 0) {
            if (r.o.x > p2.x || r.o.x < p1.x) {
                return Float.POSITIVE_INFINITY; // ray is parallel to the planes & outside slab
            }
        } else {
            tmp = 1.0f / r.d.x;
            t1 = (p1.x - r.o.x) * tmp;
            t2 = (p2.x - r.o.x) * tmp;
            if (t1 > t2) {
                float c = t1; t1 = t2; t2 = c;
            }
            if (t1 > tnear) {
                axis = 1;
                tnear = t1;
            }
            if (t2 < tfar) {
                tfar = t2;
            }
            if (tnear > tfar || tfar < 0.0) {
                return Float.POSITIVE_INFINITY; // ray missed box or box is behind ray
            }
        }
        // check Y slab
        if (r.d.y == 0) {
            if (r.o.y > p2.y || r.o.y < p1.y) {
                return Float.POSITIVE_INFINITY; // ray is parallel to the planes & outside slab
            }
        } else {
            tmp = 1.0f / r.d.y;
            t1 = (p1.y - r.o.y) * tmp;
            t2 = (p2.y - r.o.y) * tmp;
            if (t1 > t2) {
                float c = t1; t1 = t2; t2 = c;
            }
            if (t1 > tnear) {
                tnear = t1;
                axis = 2;
            }
            if (t2 < tfar) {
                tfar = t2;
            }
            if (tnear > tfar || tfar < 0) {
                return Float.POSITIVE_INFINITY; // ray missed box or box is behind ray
            }
        }
        // check Z slab
        if (r.d.z == 0) {
            if (r.o.z > p2.z || r.o.z < p1.z) {
                return Float.POSITIVE_INFINITY; // ray is parallel to the planes & outside slab
            }
        } else {
            tmp = 1.0f / r.d.z;
            t1 = (p1.z - r.o.z) * tmp;
            t2 = (p2.z - r.o.z) * tmp;
            if (t1 > t2) {
                float c = t1; t1 = t2; t2 = c;
            }
            if (t1 > tnear) {
                tnear = t1;
                axis = 3;
            }
            if (t2 < tfar) {
                tfar = t2;
            }
            if (tnear > tfar || tfar < 0) {
                return Float.POSITIVE_INFINITY; // ray missed box or box is behind ray
            }
        }
        switch(axis) {
            case 1:
                hitaxis.set(r.d.x>0?1:0,0,0);
                break;
            case 2:
                hitaxis.set(0,r.d.y>0?1:0,0);
                break;
            case 3:
                hitaxis.set(0,0,r.d.z>0?1:0);
                break;
        }
        if (tnear > 0) {
            return tnear;
        } else {
            return tfar;
        }
    }

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
    public CollisionResult GetNext() {
        return resultBuffer[BufferOffset++ & RESULT_BUFFER_SIZE-1];
    }

    public void SetBoxModel(Vector3f mins, Vector3f maxs, Vector3f origin) {
        Vector3f.add(mins, maxs, box_extent);
        box_extent.scale(0.5f);
        // get mins and maxs offset for centering
        Vector3f.add(origin, box_extent, box_origin);

        // grab the extents
        Vector3f.sub(maxs, mins, box_extent);
        box_extent.scale(0.5f);
        boxTrace = true; // next BoxTrace will use the boxmodel
    }

    public void SetBoxModel(Vector3f extent, Vector3f origin) {
        Vector3f.sub(origin, extent, box_origin);
        box_extent.set(extent);
        boxTrace = true; // next BoxTrace will use the boxmodel
    }

    public void SetSubModel(int index, Vector3f origin) {
        boxTrace = true;
        
        
    }
}
