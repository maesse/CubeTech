package cubetech.collision;

import cubetech.CGame.ChunkRender;
import cubetech.CGame.ViewParams;
import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.FrameBuffer;
import cubetech.gfx.GLRef;
import cubetech.gfx.Shader;
import cubetech.misc.Plane;
import cubetech.misc.Ref;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DataFormatException;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class ClientCubeMap {
    public HashMap<Long, CubeChunk> chunks = new HashMap<Long, CubeChunk>();
    public Color[] lightSides = new Color[6];

    // render stats
    public int nSides = 0; // sides rendered this frame
    public int nChunks = 0;


    public FrameBuffer derp;
    CubeTexture depthTex = null;

    Shader shader = null;
    Shader shadowShader = null;

    public ExecutorService exec = Executors.newSingleThreadExecutor();

    private static final int NUM_BUILDBUFFERS = 8;
    
    private int freeBuffers = NUM_BUILDBUFFERS;
    public ByteBuffer[] buildBuffer = new ByteBuffer[NUM_BUILDBUFFERS];
    public long[] bufferRelations = new long[NUM_BUILDBUFFERS];
    public boolean[] bufferLocked = new boolean[NUM_BUILDBUFFERS];
    public final Object bufferLock = new Object();
    private static final long NO_CHUNK = Long.MIN_VALUE;

    public int currentWrite = 0;
    public int currentRead = 0;

    public int nVBOthisFrame = 0; // vbo's updated this frame

    public void dispose() {
        exec.shutdownNow();
        exec = null;
        depthTex = null;
        if(derp != null) derp.destroy();
        for (CubeChunk cubeChunk : chunks.values()) {
            cubeChunk.destroy();
        }
        chunks.clear();
        buildBuffer = null;
    }

    //
    // Intermediate buffer handling
    //
    public boolean bufferAvailable() {
        synchronized(bufferLock) {
            return freeBuffers > 0;
        }
    }

    public void markLockedChunks() {
        //synchronized(bufferLock) {
            for (int i= 0; i < NUM_BUILDBUFFERS; i++) {
                if(bufferLocked[i]) {
                    long chunkid = bufferRelations[i];

                    if(chunkid == NO_CHUNK) {
                        Ref.common.Error(ErrorCode.FATAL, "Invalid locked chunk");
                    }

                    CubeChunk c = chunks.get(chunkid);
                    c.render.markVisible();
                }
            }
        //}
    }

    public ByteBuffer grabBuffer(long chunkid) {
        synchronized(bufferLock) {
            if(freeBuffers <= 0) return null;

            for (int i= 0; i < NUM_BUILDBUFFERS; i++) {
                // Grab the first unlocked buffer
                if(!bufferLocked[i]) {
                    // remember owner chunk
                    bufferRelations[i] = chunkid;
                    // decrement free buffer counter
                    freeBuffers--;
                    // lock this buffer
                    bufferLocked[i] = true;
                    buildBuffer[i].clear();
                    return buildBuffer[i];
                }
            }

            Ref.common.Error(ErrorCode.FATAL, "freebuffers > 0, but all are locked");
            return null;
        }
    }

    // See if we can read from this buffer
    public ByteBuffer getBuffer(long chunkId) {
        synchronized(bufferLock) {
            for (int i= 0; i < NUM_BUILDBUFFERS; i++) {
                if(bufferRelations[i] == chunkId && bufferLocked[i]) {
                    return buildBuffer[i];
                }
            }
        }
        return null;
    }

    public void releaseBuffer(long chunkid) {
        synchronized(bufferLock) {
            for (int i= 0; i < NUM_BUILDBUFFERS; i++) {
                if(bufferRelations[i] == chunkid && bufferLocked[i]) {
                    // unlock buffer
                    bufferRelations[i] = NO_CHUNK;
                    bufferLocked[i] = false;
                    freeBuffers++;
                    break;
                }
            }
        }
    }

    public ClientCubeMap() {
        for (int i= 0; i < buildBuffer.length; i++) {
            buildBuffer[i] = BufferUtils.createByteBuffer(CubeChunk.PLANE_SIZE * CubeChunk.CHUNK_SIZE /2);
        }
        shader = Ref.glRef.getShader("WorldFog");
        shader.mapTextureUniform("tex", 0);
        shader.validate();
        buildLight();
    }

    public void changedBlock(CubeChunk c, int x, int y, int z, byte type) {
        // Update Bullet physics
//        boolean removed = type == CubeType.EMPTY;
//
//        if(removed) {
//            // TODO
//            return;
//        }
//
//        // Create a transform
//        Transform boxTransform = new Transform();
//        boxTransform.setIdentity();
//
//        // Set origin to center of the block
//        int cx = c.p[0] + x * CubeChunk.BLOCK_SIZE + CubeChunk.BLOCK_SIZE/2;
//        int cy = c.p[1] + y * CubeChunk.BLOCK_SIZE + CubeChunk.BLOCK_SIZE/2;
//        int cz = c.p[2] + z * CubeChunk.BLOCK_SIZE + CubeChunk.BLOCK_SIZE/2;
//        boxTransform.origin.set(cx, cy, cz);
//
//        // Create the body
//        DefaultMotionState motionState = new DefaultMotionState(boxTransform);
//        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0, motionState, boxShape);
//        RigidBody body = new RigidBody(rbInfo);
//
//        // Add it to the world
//        world.addRigidBody(body);
    }

    

    public void unserialize(ByteBuffer buf) {
        long count = buf.getLong();
        Common.Log("Reading %d chunks from demo file", count);
        int size = buf.position();
        if(count > 0) {
            parseCubeData(buf, (int) count);
        }
        size = buf.position() - size;
        Common.Log("Loaded %d bytes of compressed map data", size);
    }

    public void serializeClientMap(FileChannel out) throws IOException {
        ByteBuffer tempBuf = ByteBuffer.allocate(8);
        tempBuf.order(ByteOrder.nativeOrder());
        // Write chunk count
        int count = chunks.size();
        tempBuf.putLong(0, count).position(0);
        out.write(tempBuf);
        Common.Log("Writing %d cube chunks to demo file" + count);
        int totalChunkSize = 0;
        for (Long key : chunks.keySet()) {
            CubeChunk chunk = chunks.get(key);

            // write chunk data
            ByteBuffer chunkData = chunk.createByteBuffer();
            totalChunkSize += chunkData.limit();
            out.write(chunkData);
        }
        Common.Log("Compressed map data: %d bytes", totalChunkSize);
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

        float ambScale = 0.8f;
        for (int i= 0; i < sideColor.length; i++) {
            sideColor[i].scale(ambScale);
        }

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

            if(max < 255) max = 255;

            r = r * 255 / max;
            g = g * 255 / max;
            b = b * 255 / max;

            lightSides[i] = new Color(r, g, b);
        }
    }

    private void renderFromOrigin(int orgX, int orgY, int orgZ, int chunkDistance, boolean enqueue, Plane[] frustum) {
        if(enqueue) {
            markLockedChunks();
        }

        nVBOthisFrame = 0;
        // Render chunks
        for (int z= -chunkDistance; z <= chunkDistance; z++) {
            for (int y= -chunkDistance; y <= chunkDistance; y++) {
                for (int x= -chunkDistance; x <= chunkDistance; x++) {
                    Long lookup = CubeMap.positionToLookup(orgX + x, orgY + y, orgZ + z);
                    CubeChunk chunk = chunks.get(lookup);

                    // Chunk not generated yet, queue it..
                    if(chunk == null) {
                       // if(enqueue) generateChunk(orgX + x, orgY + y, orgZ + z, true);
                        continue;
                    }

                    if(chunk.render == null) chunk.render = new ChunkRender(chunk);

//                    // do frustum culling
//                    if(frustum[0] != null) {
//                        Plane p = frustum[0];
//                        if(p.testPoint(chunk.fcenter[0], chunk.fcenter[1], chunk.fcenter[2]) < -CubeChunk.RADIUS) {
//                            continue;
//                        }
//                    }

                    chunk.render.Render();

                    nSides += chunk.render.sidesRendered;
                    if(chunk.render.sidesRendered > 0) nChunks++; // don't count empty chunks
                }
            }
        }
    }

    
    

    public void Render(ViewParams view) {
        if(Ref.cgame.cg.time - 1000 > lastRefresh) {
            chunkPerSecond = nChunkUpdates;
            nChunkUpdates = 0;
            lastRefresh = Ref.cgame.cg.time;
        }


        // Set shader
        CubeTexture depth = null;
        boolean shadowPass = Ref.cgame.shadowMan.isRendering();
        if(!shadowPass) {
            if(shadowShader == null) {
                shadowShader = Ref.glRef.getShader("WorldFogShadow");
                shadowShader.mapTextureUniform("tex", 0);
                shadowShader.mapTextureUniform("shadows", 1);
                shadowShader.validate();
            }
            Ref.glRef.PushShader(shadowShader);
            depth = Ref.cgame.shadowMan.getDepthTexture();
            depth.textureSlot = 1;
            depth.Bind();
            Matrix4f[] shadowmat = Ref.cgame.shadowMan.getShadowViewProjections(8);
            shadowShader.setUniform("shadowMatrix", shadowmat);
            Vector4f[] shadowDepths = Ref.cgame.shadowMan.getCascadeDepths();
            
            shadowShader.setUniform("cascadeDistances", shadowDepths);
            
//            shadowShader.setUniform("cascadeColors", Ref.cgame.shadowMan.getCascadeColors());
            shadowShader.setUniform("fog_factor", 1f/(view.farDepth));
            shadowShader.setUniform("fog_color", (Vector4f)new Vector4f(95,87,67,255).scale(1/255f)); // 145, 140, 129
            shadowShader.setUniform("shadow_bias", Ref.cvars.Find("shadow_bias").fValue);
            
            shadowShader.setUniform("pcfOffsets", Ref.cgame.shadowMan.getPCFoffsets());
            GLRef.checkError();
        } else {
            Ref.glRef.PushShader(shader);
            shader.setUniform("fog_factor", 1f/(view.farDepth/2f));
            shader.setUniform("fog_color", (Vector4f)new Vector4f(95,87,67,255).scale(1/255f)); // 145, 140, 129
        }
//        shader.mapTextureUniform("tex", 0);
        GLRef.checkError();
        

        // Set rendermodes
        GL11.glDisable(GL11.GL_BLEND);
        if(!Ref.glRef.r_fill.isTrue()) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }

        // clear render stats
        nSides = 0;
        nChunks = 0;

        // chunk render distance
        int chunkDistance = (int) (view.farDepth * 1f / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE))+1;
        if(chunkDistance <= 0)
            chunkDistance = 1;

        // Origin in chunk coordinates
        int orgX =  (int)Math.floor(view.Origin.x / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
        int orgY =  (int)Math.floor (view.Origin.y / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
        int orgZ =  (int)Math.floor (view.Origin.z / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));

        renderFromOrigin(orgX, orgY, orgZ, chunkDistance, !shadowPass, view.planes);

        if(!Ref.cgame.shadowMan.isRendering()) {
            depth.Unbind();
        }
        // Reset shader
        Ref.glRef.PopShader();

        // Clear rendermodes
        GL11.glEnable(GL11.GL_BLEND);
        if(!Ref.glRef.r_fill.isTrue()) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }
    }

    

    int nChunkUpdates = 0;
    int lastRefresh = 0;
    public int chunkPerSecond = 0;
    public void parseCubeData(ByteBuffer download, int count) {
        // need a backing array
        if(!download.hasArray()) {
            Ref.common.Error(ErrorCode.FATAL, "parseCubeDatas underlying download bytebuffer doesn't have a backing array");
        }
        byte[] src = download.array();
        download.order(ByteOrder.nativeOrder());
        int offset = download.position();
        // Handle multiple chunks in one stream
        while(offset < src.length && (count > 0 || count == -1)) {
            download.position(offset);
            int chunkSize = download.getInt();
            offset += 4; // move past the size
            
            // uncompress data
            ByteBuffer data = uncompressCubeData(src, offset, chunkSize);
            parseSingleCubeData(data);
            nChunkUpdates++;

            // move offset to next data
            offset += chunkSize;
            if(count > 0) count--;
        }
        // there might be more in this file, so ensure that buffer is positioned correctly
        if(offset < src.length) download.position(offset);

    }

    private ByteBuffer uncompressCubeData(byte[] src, int offset, int lenght) {
        byte[] dest = new byte[CubeChunk.CHUNK_SIZE + 1 + 8]; // max possible size
        int len = 0;
        try {
            len = CubeChunk.uncompressData(src, offset, lenght, dest);
        } catch (DataFormatException ex) {
            Common.Log("Got a chunkError:" + Common.getExceptionString(ex));
        }
        ByteBuffer download = ByteBuffer.wrap(dest, 0, len);
        download.order(ByteOrder.nativeOrder());
        return download;
    }

    private void parseSingleCubeData(ByteBuffer download) {
        // Check control byte
        byte control = download.get();
        if(control != 1 && control != 2) return; // problem

        // extract chunk index
        long chunkIndex = download.getLong();
        CubeChunk c = chunks.get(chunkIndex);
        if(c == null) {
            // First time we recieve data for this chunk -- create it
            int[] p = CubeMap.lookupToPosition(chunkIndex);
            c = new CubeChunk(chunks, p[0], p[1], p[2]);
            c.render = new ChunkRender(c);
            chunks.put(chunkIndex, c);
        }

        if(control == 1) {
            // get entry count

            int startVersion = download.getInt();
            byte count = download.get();
            if(download.limit() - download.position() < count * 4) {
                // Buffer doesn't contain the complete data for some reason..
                Common.Log("Got invalid delta chunk data");
                return;
            }
            for (int i= 0; i < count; i++) {
                int data = download.getInt();
                int[] delta = CubeChunk.unpackChange(data);
                // 0-2 = cube index, 3 = cube type
                byte type = (byte) delta[3];
                if(delta[3]>Byte.MAX_VALUE) {
                    int derp2 = 2;
                }
                c.setCubeType(delta[0], delta[1], delta[2], type, true);
                changedBlock(c, delta[0], delta[1], delta[2], type);
            }
        } else if(control == 2) {
            // Fill chunk
            for (int z= 0; z < CubeChunk.SIZE; z++) {
                for (int y= 0; y < CubeChunk.SIZE; y++) {
                    for (int x= 0; x < CubeChunk.SIZE; x++) {
                        byte b = download.get();
                        c.setCubeType(x, y, z, b, false);
                        changedBlock(c, x, y, z, b);
                    }
                }
            }
            // notify all neighbours
            c.render.notifyChange();
        }
    }


    
}
