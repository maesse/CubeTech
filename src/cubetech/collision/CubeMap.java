package cubetech.collision;

import cern.colt.function.LongObjectProcedure;
import cern.colt.map.OpenLongObjectHashMap;
import cubetech.common.Common;
import cubetech.misc.Ref;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CubeMap {
    public static final int MIN_Z = -2; // how many chunks to allow to grow downward
    public static final int MAX_Z = 6;
    public static final int DEFAULT_GROW_DIST = 12;

    // Chunks
    public OpenLongObjectHashMap chunks = new OpenLongObjectHashMap();
    IChunkGenerator chunkGen = null;
    private Queue<int[]> chunkGenQueue = new LinkedList<int[]>();

    // render stats
    public int nSides = 0; // sides rendered this frame
    public int nChunks = 0;

    // temp
    private static Vector3f tempStart = new Vector3f();
    private static Vector3f tempEnd = new Vector3f();

    public CubeMap(IChunkGenerator gen, int w, int h, int d) {
        chunkGen = gen;
        fillInitialArea(w, h, d);
    }

    void destroy() {
        chunks.forEachPair(new LongObjectProcedure() {
            public boolean apply(long l, Object o) {
                ((CubeChunk)o).destroy();
                return true;
            }
        });
        chunks.clear();
        chunks = null;
        chunkGen = null;
    }

    public CubeMap() {
    }

    public void update()
    {
        // generateChunk(orgX + x, orgY + y, orgZ + z, true);
        popChunkQueue(); // Generate one chunk pr. frame when multiple chunks are queued
    }

    public long[] getVisibleChunks(Vector3f position, int dist, long[] lookups) {
        int axisSize = CubeChunk.SIZE * CubeChunk.BLOCK_SIZE;
        int x = (int)Math.floor(position.x / axisSize);
        int y = (int)Math.floor(position.y / axisSize);
        int z = (int)Math.floor(position.z / axisSize);

        if(dist < 2)
            dist = 2;
        int halfDist = dist/2;

        if(lookups == null || lookups.length < dist*dist*dist) {
            lookups = new long[dist*dist*dist];
        }
        int i=0;

        for (int pz= z; pz < z+dist; pz++) {
            for (int py= y; py < y+dist; py++) {
                for (int px= x; px < x + dist; px++) {
                    long lookup = positionToLookup(px-halfDist, py-halfDist, pz-halfDist);
                    lookups[i++] = lookup;
                }
            }
        }

        return lookups;
    }

    public void growFromPosition(Vector3f position, int dist) {
        int axisSize = CubeChunk.SIZE * CubeChunk.BLOCK_SIZE;
        int x = (int)Math.floor(position.x / axisSize);
        int y = (int)Math.floor(position.y / axisSize);
        int z = (int)Math.floor(position.z / axisSize);

        if(dist < 2)
            dist = 2;
        int halfDist = dist/2;

        for (int pz= z; pz < z+dist; pz++) {
            for (int py= y; py < y+dist; py++) {
                for (int px= x; px < x + dist; px++) {
                    // Limit Z-axis growth
                    if(pz-halfDist < MIN_Z)
                        continue;
                    if(pz-halfDist > MAX_Z)
                        continue;
                    
                    long lookup = positionToLookup(px-halfDist, py-halfDist, pz-halfDist);
                    if(chunks.containsKey(lookup))
                        continue; // chunk exists..

                    // Generate chunk here
                    generateChunk(px-halfDist, py-halfDist, pz-halfDist, true);
                }
            }
        }
    }

    private void popChunkQueue() {
        
        int[] chunkQueue = null;
        int nChunksLoaded = 0;
        boolean chunkLoaded = false;
        while((chunkQueue = chunkGenQueue.peek()) != null && nChunksLoaded < 1) {
            chunkGenQueue.poll();
            if(chunks.containsKey(positionToLookup(chunkQueue[0], chunkQueue[1], chunkQueue[2])))
                continue;
            CubeChunk chunk = generateChunk(chunkQueue[0], chunkQueue[1], chunkQueue[2], false);
            nChunksLoaded++;
        }
    }

    
    
    // Thanks goes out to http://www.xnawiki.com/index.php?title=Voxel_traversal
    public static CubeCollision TraceRay(Vector3f start, Vector3f dir, int maxDepth, OpenLongObjectHashMap chunks) {
        // NOTES:
        // * This code assumes that the ray's position and direction are in 'cell coordinates', which means
        //   that one unit equals one cell in all directions.
        // * When the ray doesn't start within the voxel grid, calculate the first position at which the
        //   ray could enter the grid. If it never enters the grid, there is nothing more to do here.
        // * Also, it is important to test when the ray exits the voxel grid when the grid isn't infinite.
        // * The Point3D structure is a simple structure having three integer fields (X, Y and Z).

        // The cell in which the ray starts.
        float dx = (start.x / CubeChunk.BLOCK_SIZE);
        float dy = (start.y / CubeChunk.BLOCK_SIZE);
        float dz = (start.z / CubeChunk.BLOCK_SIZE);
        int x = (int)Math.floor(start.x / CubeChunk.BLOCK_SIZE);
        int y = (int)Math.floor(start.y / CubeChunk.BLOCK_SIZE);
        int z = (int)Math.floor(start.z / CubeChunk.BLOCK_SIZE);

        // Determine which way we go.
        float stepX = Math.signum(dir.x);
        float stepY = Math.signum(dir.y);
        float stepZ = Math.signum(dir.z);
        
        // Calculate cell boundaries. When the step (i.e. direction sign) is positive,
        // the next boundary is AFTER our current position, meaning that we have to add 1.
        // Otherwise, it is BEFORE our current position, in which case we add nothing.
        int cx = x + (stepX > 0 ? 1 : 0);
        int cy = y + (stepY > 0 ? 1 : 0);
        int cz = z + (stepZ > 0 ? 1 : 0);

        // NOTE: For the following calculations, the result will be Single.PositiveInfinity
        // when ray.Direction.X, Y or Z equals zero, which is OK. However, when the left-hand
        // value of the division also equals zero, the result is Single.NaN, which is not OK.

        // Determine how far we can travel along the ray before we hit a voxel boundary.
        Vector3f tMax = new Vector3f(
                (cx - dx) / dir.x,
                (cy - dy) / dir.y,
                (cz - dz) / dir.z);
        if(Float.isNaN(tMax.x)) tMax.x = Float.POSITIVE_INFINITY;
        if(Float.isNaN(tMax.y)) tMax.y = Float.POSITIVE_INFINITY;
        if(Float.isNaN(tMax.z)) tMax.z = Float.POSITIVE_INFINITY;

        // Determine how far we must travel along the ray before we have crossed a gridcell.
        Vector3f delta = new Vector3f(
                stepX / dir.x,
                stepY / dir.y,
                stepZ / dir.z);
        if(Float.isNaN(delta.x)) delta.x = Float.POSITIVE_INFINITY;
        if(Float.isNaN(delta.y)) delta.y = Float.POSITIVE_INFINITY;
        if(Float.isNaN(delta.z)) delta.z = Float.POSITIVE_INFINITY;
        // For each step, determine which distance to the next voxel boundary is lowest (i.e.
        // which voxel boundary is nearest) and walk that way.
        CubeChunk chunk = (CubeChunk)chunks.get(0L);
//        if(chunk != null) {
//            chunk.traceTime = 5000;
//            chunk.traceCount = 0;
//            chunk.traceCache = new int[maxDepth];
//        }
        int ccx = 0, ccy = 0, ccz = 0; // chunkx, y, z from last iteration.
        int lastAxis = 0;
        for (int i= 0; i < maxDepth; i++) {
            // Figure out chunk
            int chunkX = (int)Math.floor(x / (float)CubeChunk.SIZE);
            int chunkY = (int)Math.floor(y / (float)CubeChunk.SIZE);
            int chunkZ = (int)Math.floor(z / (float)CubeChunk.SIZE);

            // Remember last chunk
            if(chunkX != ccx || chunkY != ccy || chunkZ != ccz) {
                long lookup = positionToLookup(chunkX, chunkY, chunkZ);
                chunk = (CubeChunk)chunks.get(lookup);
                ccx = chunkX; ccy = chunkY; ccz = chunkZ;
                // entering new chunk, clear its trace debug
//                if(chunk != null) {
//                    chunk.traceTime = 5000;
//                    chunk.traceCount = 0;
//                    chunk.traceCache = new int[maxDepth];
//                }
            }

            // Test cube in chunk
            if(chunk != null) {
                int cubeX = x - chunkX * CubeChunk.SIZE;
                int cubeY = y - chunkY * CubeChunk.SIZE;
                int cubeZ = z - chunkZ * CubeChunk.SIZE;

                int cubeIndex = CubeChunk.getIndex(cubeX, cubeY, cubeZ);
//                chunk.traceCache[chunk.traceCount++] = cubeIndex;

                if(chunk.getCubeType(cubeIndex) != 0) {
                    // Collision
                    return new CubeCollision(chunk, cubeX, cubeY, cubeZ, lastAxis*-1);
                }
            }

            // Do the next step.
            if(tMax.x < tMax.y && tMax.x < tMax.z) {
                // tMax.X is the lowest, an YZ cell boundary plane is nearest.
                x += stepX;
                tMax.x += delta.x;
                lastAxis = (int) (1 * stepX);
            } else if(tMax.y < tMax.z) {
                // tMax.Y is the lowest, an XZ cell boundary plane is nearest.
                y += stepY;
                tMax.y += delta.y;
                lastAxis = (int) (2 * stepY);
            } else {
                // tMax.Z is the lowest, an XY cell boundary plane is nearest.
                z += stepZ;
                tMax.z += delta.z;
                lastAxis = (int) (3 * stepZ);
            }
        }
        
        return null; // no collision
    }

    
    private CubeChunk generateChunk(int x, int y, int z, boolean queue) {
        if(z < MIN_Z || z >= MAX_Z)
            return null;

        if(queue) {
            chunkGenQueue.add(new int[] {x,y,z});
            return null;
        }

        long start = System.nanoTime();

        CubeChunk chunk = chunkGen.generateChunk(this, x, y, z);
        long index = positionToLookup(x, y, z);

        boolean success = chunks.put(index, chunk);

        long end = System.nanoTime();

        float time = (end-start) / 1000000f;
        if(time > 1f && Ref.common.developer.isTrue()) {
            Common.Log("Generate Chunk took %.0f ms", time);
        }

//        chunk.notifyChange();
        
        if(!success)
            throw new RuntimeException("generateSimpleMap: Chunk already existed.");
        return chunk;
    }

    private void fillInitialArea(int w, int h, int d) {
        // Create a new 8x8x2 chunk map
        for (int z= 0; z < d; z++) {
            for (int y= 0; y < h; y++) {
                for (int x= 0; x < w; x++) {
                    generateChunk(x-w/2, y-h/2, z-d/2, false);
                }
            }
        }
    }

    public static long positionToLookup(int x, int y, int z) {
        long a = bitTwiddle(x) | bitTwiddle(y)<<17 | bitTwiddle(z)<<34;
        return a;
    }

    // Twiddle an int down to 17bit
    private static long bitTwiddle(int input) {
        if(input < 0) {
            return (-input & 0xffff) | 0x10000;
        }
        return input & 0xffff;
    }

    public static int[] lookupToPosition(long lookup) {
        // decompose:
        int[] pos = new int[] {
            unTwiddle(lookup, 0),
            unTwiddle(lookup, 17),
            unTwiddle(lookup, 34)
        };
        return pos;
    }

    private static int unTwiddle(long src, int bitIndex) {
        // bitshift and mask off the stuff we care about
        
        long d = (src >> bitIndex);
        int x = (int) (d & 0xffff);
        if((d & 0x10000) == 0x10000) x = -x;
        
        return x;
    }

    
    public static ChunkAreaQuery getCubesInVolume(Vector3f mmin, Vector3f mmax, OpenLongObjectHashMap chunks, boolean server) {
        // Figure out min/max chunk positions
        int axisSize = CubeChunk.SIZE * CubeChunk.BLOCK_SIZE;
        int minx = (int)Math.floor(mmin.x / axisSize);
        int miny = (int)Math.floor(mmin.y / axisSize);
        int minz = (int)Math.floor(mmin.z / axisSize);

        int maxx = (int)Math.floor(mmax.x / axisSize)+1;
        int maxy = (int)Math.floor(mmax.y / axisSize)+1;
        int maxz = (int)Math.floor(mmax.z / axisSize)+1;

        
        ChunkAreaQuery query = new ChunkAreaQuery((maxz - minz) * (maxx - minx) * (maxy - miny));
        for (int z= minz; z < maxz; z++) {
            for (int y= miny; y < maxy; y++) {
                for (int x= minx; x < maxx; x++) {
                    CubeChunk chunk = (CubeChunk) chunks.get(positionToLookup(x, y, z));
                    if(chunk == null) {
                        if(server) {
                            chunk = Ref.cm.cubemap.generateChunk(x, y, z, false);
                        }
                        if(chunk == null)
                            continue; // Chunk denied. Probably hit Z bounds.
                    }

                    // Gather up potentially collided cubes from this chunk
                    int[] chunkMin = chunk.absmin;
                    int[] chunkMax = chunk.absmax;

                    // Cut start/end so they are withing the chunk bounds
                    tempStart.x = Math.max(chunkMin[0], mmin.x) - chunkMin[0];
                    tempStart.y = Math.max(chunkMin[1], mmin.y) - chunkMin[1];
                    tempStart.z = Math.max(chunkMin[2], mmin.z) - chunkMin[2];

                    tempEnd.x = Math.min(chunkMax[0], mmax.x) - chunkMin[0];
                    tempEnd.y = Math.min(chunkMax[1], mmax.y) - chunkMin[1];
                    tempEnd.z = Math.min(chunkMax[2], mmax.z) - chunkMin[2];

                    ChunkSpatialPart part = chunk.getCubesInVolume(tempStart, tempEnd);
                    if(part.nIndex > 0) query.addPart(part);
                    
                }
            }
        }
        return query;
    }

    public static CubeChunk getChunk(int x, int y, int z, boolean create, OpenLongObjectHashMap chunks) {
        long index = positionToLookup(x, y, z);
        
        if(!chunks.containsKey(index)) {
            if(create) {
                // Going to need to create this chunk
                Ref.cm.cubemap.generateChunk(x,y,z, false);
            } else {
                // Chunk is now created yet
                return null;
            }
        }

        return (CubeChunk)chunks.get(index);
    }

    
}
