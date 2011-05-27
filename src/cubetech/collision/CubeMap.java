package cubetech.collision;

import cubetech.CGame.ViewParams;
import cubetech.common.Common;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.CubeType;
import cubetech.gfx.FrameBuffer;
import cubetech.gfx.Shader;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TerrainTextureCache;
import cubetech.iqm.IQMLoader;
import cubetech.iqm.IQMModel;
import cubetech.misc.Plane;
import cubetech.misc.Ref;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CubeMap {
    public static final int MIN_Z = -2; // how many chunks to allow to grow downward
    public static final int MAX_Z = 6;
    public static final int DEFAULT_GROW_DIST = 4;

    // Chunks
    HashMap<Long, CubeChunk> chunks = new HashMap<Long, CubeChunk>();
    IChunkGenerator chunkGen = null;
    private Queue<int[]> chunkGenQueue = new LinkedList<int[]>();

    public Color[] lightSides = new Color[6];

    // render stats
    public int nSides = 0; // sides rendered this frame
    public int nChunks = 0;

    // temp
    private Vector3f tempStart = new Vector3f();
    private Vector3f tempEnd = new Vector3f();

    public IQMModel model = null;
    FrameBuffer derp;
    CubeTexture depthTex = null;

    public CubeMap(IChunkGenerator gen, int w, int h, int d) {
        chunkGen = gen;
        

        buildLight();
        fillInitialArea(w, h, d);

        try {
            //model = IQMLoader.LoadModel("C:\\Users\\mads\\Desktop\\iqm\\mrfixit.iqm");
            model = IQMLoader.LoadModel("C:\\Users\\mads\\Documents\\Old\\Blender filer\\cubeguyTextured.iqm");
        } catch (IOException ex) {

        }

        derp = new FrameBuffer(false, true, 512, 512);
        depthTex = new CubeTexture(GL11.GL_TEXTURE_2D, derp.getTextureId(), "depth");
        

    }

    private void buildLight() {
        // Default white
        for (int i= 0; i < lightSides.length; i++) {
            lightSides[i] = (Color) Color.WHITE;
        }

        // Ambient light
        Vector3f sideColor[] = new Vector3f[6];
        sideColor[0] = new Vector3f(131, 129, 123);
        sideColor[1] = new Vector3f(156, 154, 143);
        sideColor[2] = new Vector3f(148, 144, 134);
        sideColor[3] = new Vector3f(138, 140, 136);
        sideColor[4] = new Vector3f(138, 144, 152);
        sideColor[5] = new Vector3f(145, 140, 129);

        // Add in sun
        Vector3f sunColor = new Vector3f(255, 255, 255);
        Vector3f sunDir = new Vector3f(-0.7f, 0.8f, 1.0f);
        sunDir.normalise();

        Vector3f normals[] = new Vector3f[6];
        normals[0] = new Vector3f(1, 0, 0); normals[1] = new Vector3f(-1, 0, 0);
        normals[2] = new Vector3f(0, 1, 0); normals[3] = new Vector3f(0, -1, 0);
        normals[4] = new Vector3f(0, 0, 1); normals[5] = new Vector3f(0, 0, -1);

        for (int i= 0; i < 6; i++) {
            float NdotL = Math.max(Vector3f.dot(normals[i], sunDir),0);
            Vector3f.add(sideColor[i], (Vector3f) new Vector3f(sunColor).scale(NdotL), sideColor[i]);
        }

        // Save color
        for (int i= 0; i < 6; i++) {
            int r = (int)sideColor[i].x, g = (int)sideColor[i].y, b = (int)sideColor[i].z;
            // ColorShiftLightingBytes from quake
            int max = r > g ? r : g;
            max = max > b ? max : b;

            r = r * 255 / max;
            g = g * 255 / max;
            b = b * 255 / max;

            lightSides[i] = new Color(r, g, b);
        }
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
                    generateChunk(px-halfDist, py-halfDist, pz-halfDist, true);
                }
            }
        }
    }

    private void popChunkQueue() {
        
        int[] chunkQueue = null;
        boolean chunkLoaded = false;
        while((chunkQueue = chunkGenQueue.peek()) != null && !chunkLoaded) {
            chunkGenQueue.poll();
            if(chunks.containsKey(positionToLookup(chunkQueue[0], chunkQueue[1], chunkQueue[2])))
                continue;
            CubeChunk chunk = generateChunk(chunkQueue[0], chunkQueue[1], chunkQueue[2], false);
            chunkLoaded = true;
        }
    }

    private void renderFromOrigin(int orgX, int orgY, int orgZ, int chunkDistance, boolean enqueue, Plane[] frustum) {
        // Render chunks
        for (int z= -chunkDistance; z <= chunkDistance; z++) {
            for (int y= -chunkDistance; y <= chunkDistance; y++) {
                for (int x= -chunkDistance; x <= chunkDistance; x++) {
                    Long lookup = positionToLookup(orgX + x, orgY + y, orgZ + z);
                    CubeChunk chunk = chunks.get(lookup);

                    // Chunk not generated yet, queue it..
                    if(chunk == null) {
                        if(enqueue) generateChunk(orgX + x, orgY + y, orgZ + z, true);
                        continue;
                    }

                    // do frustum culling
                    if(frustum[0] != null) {
                        Plane p = frustum[0];
                        if(p.testPoint(chunk.fcenter[0], chunk.fcenter[1], chunk.fcenter[2]) < -CubeChunk.RADIUS) {
                            continue;
                        }
                    }

                    chunk.Render();

                    nSides += chunk.nSides;
                    if(chunk.nSides > 0) nChunks++; // don't count empty chunks
                }
            }
        }
    }

    public void Render(ViewParams view) {
        popChunkQueue(); // Generate one chunk pr. frame when multiple chunks are queued

        // Set shader
        Shader shader = Ref.glRef.getShader("WorldFog");
        Ref.glRef.PushShader(shader);
        shader.setUniform("fog_factor", 1f/(view.farDepth/3f));
        shader.setUniform("fog_color", (Vector4f)new Vector4f(95,87,67,255).scale(1/255f)); // 145, 140, 129

        
        // Set rendermodes
        GL11.glDisable(GL11.GL_BLEND);
        if(!Ref.glRef.r_fill.isTrue()) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }

        // clear render stats
        nSides = 0;
        nChunks = 0;

        // chunk render distance
        int chunkDistance = (int) (view.farDepth * 0.3f / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE))+1;
        if(chunkDistance <= 0)
            chunkDistance = 1;

        // Origin in chunk coordinates
        int orgX =  (int)Math.floor(view.Origin.x / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
        int orgY =  (int)Math.floor (view.Origin.y / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
        int orgZ =  (int)Math.floor (view.Origin.z / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));

        renderFromOrigin(orgX, orgY, orgZ, chunkDistance, true, view.planes);

        // Reset shader
        Ref.glRef.PopShader();
        
        // Shadow mapping
        //Ref.glRef.PushShader(Ref.glRef.getShader("sprite"));
        
        derp.Bind();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glViewport(0, 0, 512, 512);
        GL11.glColorMask(false, false, false, false);
        renderFromOrigin(orgX, orgY, orgZ, chunkDistance, false, view.planes);
        GL11.glColorMask(true, true, true, true);
        derp.Unbind();
        GL11.glViewport(0, 0, (int)Ref.glRef.GetResolution().x, (int)Ref.glRef.GetResolution().y);
        
        //Ref.glRef.PopShader();
        Ref.glRef.srgbBuffer.Bind();

        // Clear rendermodes
        GL11.glEnable(GL11.GL_BLEND);
        if(!Ref.glRef.r_fill.isTrue()) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }
        
        

        // Render test model
//        model.animate(Ref.cgame.cg.time*25/1000f);
//        model.render(Ref.cgame.cg.predictedPlayerState.origin, (float) (-Ref.cgame.cg.predictedPlayerState.viewangles.y + 180));

//        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
//        spr.Set(0, 0, 256, depthTex);
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

    
    private CubeChunk generateChunk(int x, int y, int z, boolean queue) {
        if(z < MIN_Z || z >= MAX_Z)
            return null;

        if(queue) {
            chunkGenQueue.add(new int[] {x,y,z});
            return null;
        }
        CubeChunk chunk = chunkGen.generateChunk(this, x, y, z);
        long index = positionToLookup(x, y, z);

        Object last = chunks.put(index, chunk);

        chunk.notifyChange();
        
        if(last != null)
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
                        chunk = generateChunk(x, y, z, false);
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

    CubeChunk getChunk(int x, int y, int z, boolean create) {
        Long index = positionToLookup(x, y, z);
        
        if(!chunks.containsKey(index)) {
            if(create) {
                // Going to need to create this chunk
                generateChunk(x,y,z, false);
            } else {
                // Chunk is now created yet
                return null;
            }
        }

        return chunks.get(index);
    }
}
