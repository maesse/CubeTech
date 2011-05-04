package cubetech.collision;

import cubetech.CGame.ViewParams;
import cubetech.common.Common;
import cubetech.gfx.CubeType;
import cubetech.gfx.TerrainTextureCache;
import cubetech.misc.Plane;
import cubetech.misc.Ref;
import java.util.HashMap;
import java.util.Random;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CubeMap {
    HashMap<Long, CubeChunk> chunks = new HashMap<Long, CubeChunk>();
    IChunkGenerator chunkGen = null;
    public static final int MIN_Z = -2; // how many chunks to allow to grow downward
    public static final int MAX_Z = 10;
    public static final int DEFAULT_GROW_DIST = 4;

    // render stats
    public int nSides = 0; // sides rendered this frame
    public int nChunks = 0;

    public CubeMap(IChunkGenerator gen, int w, int h, int d) {
        chunkGen = gen;
        generateSimpleMap(w, h, d);
    }

    public CubeMap() {
        
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
                    generateChunk(px-halfDist, py-halfDist, pz-halfDist);
                }
            }
        }
    }

    public void Render(ViewParams view) {
//        GL11.glLineWidth(2f);
        if(!Ref.glRef.r_fill.isTrue()) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }
        nSides = 0;
        nChunks = 0;
        
        int cubeDistance = (int) (view.farDepth / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
        if(cubeDistance <= 0)
            cubeDistance = 1;

        int orgX =  (int)Math.floor(view.Origin.x / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
        int orgY =  (int)Math.floor (view.Origin.y / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
        int orgZ =  (int)Math.floor (view.Origin.z / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));

        for (int z= -cubeDistance; z <= cubeDistance; z++) {
            for (int y= -cubeDistance; y <= cubeDistance; y++) {
                for (int x= -cubeDistance; x <= cubeDistance; x++) {
                    Long lookup = positionToLookup(orgX + x, orgY + y, orgZ + z);

                    CubeChunk chunk = chunks.get(lookup);
                    if(chunk == null) {
                        // todo: generate
                        continue;
                    }

                    // do frustum culling
                    if(view.planes[0] != null) {
                        Plane p = view.planes[0];

                        if(p.testPoint(chunk.fcenter[0], chunk.fcenter[1], chunk.fcenter[2]) < -CubeChunk.RADIUS) {
                            continue;
                        } else {
                            //continue;
                        }
                    }

                    chunk.Render();
                    nSides += chunk.nSides;
                    nChunks++;
                }
            }
        }

        if(!Ref.glRef.r_fill.isTrue()) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }
    }
    
    // Thanks goes out to http://www.xnawiki.com/index.php?title=Voxel_traversal
    public CubeCollision TraceRay(Vector3f start, Vector3f dir, int maxDepth) {
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
        CubeChunk chunk = chunks.get(0L);
        if(chunk != null) {
            chunk.traceTime = 5000;
            chunk.traceCount = 0;
            chunk.traceCache = new int[maxDepth];
        }
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
                chunk = chunks.get(lookup);
                ccx = chunkX; ccy = chunkY; ccz = chunkZ;
                // entering new chunk, clear its trace debug
                if(chunk != null) {
                    chunk.traceTime = 5000;
                    chunk.traceCount = 0;
                    chunk.traceCache = new int[maxDepth];
                }
            }

            // Test cube in chunk
            if(chunk != null) {
                int cubeX = x - chunkX * CubeChunk.SIZE;
                int cubeY = y - chunkY * CubeChunk.SIZE;
                int cubeZ = z - chunkZ * CubeChunk.SIZE;

                int cubeIndex = CubeChunk.getIndex(cubeX, cubeY, cubeZ);
                chunk.traceCache[chunk.traceCount++] = cubeIndex;

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

    private CubeChunk generateChunk(int x, int y, int z) {
        if(z < MIN_Z || z > MAX_Z)
            return null;
        CubeChunk chunk = chunkGen.generateChunk(this, x, y, z);
        long index = positionToLookup(x, y, z);

        Object last = chunks.put(index, chunk);
        
        if(last != null)
            throw new RuntimeException("generateSimpleMap: Chunk already existed.");
        return chunk;
    }

    private void generateSimpleMap(int w, int h, int d) {
        // Create a new 8x8x2 chunk map
        for (int z= 0; z < d; z++) {
            for (int y= 0; y < h; y++) {
                for (int x= 0; x < w; x++) {
                    generateChunk(x-w/2, y-h/2, z-d/2);
                }
            }
        }
    }

    public static Long positionToLookup(int x, int y, int z) {
        long a = bitTwiddle(x);
        long b = bitTwiddle(y)<<17;
        long c = bitTwiddle(z)<<34;
        Long res = a | b | c;
        return res;
//        return bitTwiddle(x) | (bitTwiddle(y)<<17) | (bitTwiddle(z)<<34);
    }

    // Twiddle an int down to 17bit
    private static long bitTwiddle(int input) {
        int tx = input & 0xffff;
        if(input < 0)
            tx |= 0x10000; // negative numbers get an extra bit
        return tx;
    }

    private Vector3f tempStart = new Vector3f();
    private Vector3f tempEnd = new Vector3f();
    ChunkAreaQuery getCubesInVolume(Vector3f mmin, Vector3f mmax) {
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
                    CubeChunk chunk = chunks.get(positionToLookup(x, y, z));
                    if(chunk == null) {
                        if(chunkGen == null)
                            continue; // Running as client?
                        chunk = generateChunk(x, y, z);
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
                    query.addPart(part);
                }
            }
        }
        return query;
    }

//    /**
//     *Take a SingleCube, and put a block on the highlight side of it
//     * @param cube singlecube (chunk + position) used as an origin
//     * @param side which axis to grow on and which direction (positive x = 1, negative z = -3)
//     */
//    public void putBlock(SingleCube cube, byte type) {
//        // FIgure out coords for new cube
//        int[] p = new int[] {cube.x, cube.y, cube.z};
//        int sign = (int)Math.signum(cube.highlightSide);
//        int index = (int)Math.abs(cube.highlightSide);
//        if(index <= 0 || index > 3)
//            throw new IllegalArgumentException("side needs to be betweeen -3 and 3 excl. 0");
//
//        // move point in the given direction
//        p[index-1] += sign;
//
//        CubeChunk chunk = cube.chunk;
//        if(p[index-1] < 0 || p[index-1] >= CubeChunk.SIZE) {
//            // Moving into another chunk.
//            int[] chunkPos = new int[3];
//            System.arraycopy(chunk.p, 0, chunkPos, 0, 3);
//            chunkPos[index-1] += sign; // correct the chunk index
//            // retrieve the chunk
//            Long newChunk = positionToLookup(chunkPos[0], chunkPos[1], chunkPos[2]);
//            if(!chunks.containsKey(newChunk)) {
//                // Going to need to create this chunk
//                generateChunk(chunkPos[0], chunkPos[1], chunkPos[2]);
//            }
//
//            chunk = chunks.get(newChunk);
//            p[index-1] = p[index-1] & (CubeChunk.SIZE-1); // correct the cube inde
//        }
//
//        chunk.setCubeType(p[0], p[1], p[2], type);
//    }

    CubeChunk getChunk(int x, int y, int z) {
        Long index = positionToLookup(x, y, z);
        
        if(!chunks.containsKey(index)) {
            // Going to need to create this chunk
            generateChunk(x,y,z);
        }

        return chunks.get(index);
    }
}
