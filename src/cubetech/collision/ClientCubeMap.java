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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
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

    // temp
    private Vector3f tempStart = new Vector3f();
    private Vector3f tempEnd = new Vector3f();

    public FrameBuffer derp;
    CubeTexture depthTex = null;

    Shader shader = null;
    public ExecutorService exec = Executors.newSingleThreadExecutor();
    public ByteBuffer[] buildBuffer = new ByteBuffer[4];
    public int[] bufferRelations = new int[buildBuffer.length];
    public int currentWrite = 0;
    public int currentRead = 0;
    private static final int SANE_QUEUE = 10;

    public void dispose() {
        exec.shutdownNow();
        exec = null;
        depthTex = null;
        if(derp != null) derp.destroy();
        for (CubeChunk cubeChunk : chunks.values()) {
            cubeChunk.render.destroy();
        }
    }

    //
    // Intermediate buffer handling
    //
    public boolean bufferAvailable() {
        return currentWrite - currentRead <= buildBuffer.length || currentWrite - currentRead > SANE_QUEUE;
    }

    public ByteBuffer grabBuffer() {
        // Find oldest
        int lowest = Integer.MAX_VALUE;
        int lowestI = 0;
        for (int i= 0; i < bufferRelations.length; i++) {
            if(bufferRelations[i] < lowest) {
                lowest = bufferRelations[i];
                lowestI = i;
            }
        }

        // Take it
        currentWrite++;
        bufferRelations[lowestI] = currentWrite;
        return buildBuffer[lowestI];
    }

    public ByteBuffer getBuffer(int index) {
        for (int i= 0; i < bufferRelations.length; i++) {
            if(bufferRelations[i] == index) {
                return buildBuffer[i];
            }
        }
        return null;
    }

    public void releaseBuffer(int index) {
        for (int i= 0; i < bufferRelations.length; i++) {
            if(bufferRelations[i] == index) {
                bufferRelations[i] = 0; // make it lowest
                break;
            }
        }
        if(currentRead < index) currentRead = index;
    }

    public ClientCubeMap() {
        for (int i= 0; i < buildBuffer.length; i++) {
            buildBuffer[i] = BufferUtils.createByteBuffer(CubeChunk.PLANE_SIZE * CubeChunk.CHUNK_SIZE /2);
        }
        shader = Ref.glRef.getShader("WorldFog");
        shader.mapTextureUniform("tex", 0);
        shader.validate();
        buildLight();
//        derp = new FrameBuffer(false, true, 512, 512);
//        GLRef.checkError();
//        depthTex = new CubeTexture(GL11.GL_TEXTURE_2D, derp.getTextureId(), "depth");
//        GLRef.checkError();
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
        

        // Set shader
        Ref.glRef.PushShader(shader);
        shader.mapTextureUniform("tex", 0);
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

//        derp.Bind();
//        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
//        GL11.glViewport(0, 0, 512, 512);
//        GL11.glColorMask(false, false, false, false);
//        renderFromOrigin(orgX, orgY, orgZ, chunkDistance, false, view.planes);
//        GL11.glColorMask(true, true, true, true);
//        derp.Unbind();
//        GL11.glViewport(0, 0, (int)Ref.glRef.GetResolution().x, (int)Ref.glRef.GetResolution().y);
//
//        //Ref.glRef.PopShader();
//        Ref.glRef.srgbBuffer.Bind();

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

    public void parseCubeData(ByteBuffer download) {
        // need a backing array
        if(!download.hasArray()) {
            Ref.common.Error(ErrorCode.FATAL, "parseCubeDatas underlying download bytebuffer doesn't have a backing array");
        }
        // uncompress data
        byte[] src = download.array();
        byte[] dest = new byte[CubeChunk.CHUNK_SIZE + 1 + 8]; // max size
        int len = 0;
        try {
            len = CubeChunk.uncompressData(src, dest);
        } catch (DataFormatException ex) {
            Common.Log("Got a chunkError:" + Common.getExceptionString(ex));
        }
        download = ByteBuffer.wrap(dest, 0, len);
        download.order(ByteOrder.nativeOrder());

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
            }
//            c.render.notifyChange();
        } else if(control == 2) {
            // Fill chunk
            for (int z= 0; z < CubeChunk.SIZE; z++) {
                for (int y= 0; y < CubeChunk.SIZE; y++) {
                    for (int x= 0; x < CubeChunk.SIZE; x++) {
                        byte b = download.get();
                        c.setCubeType(x, y, z, b, false);
                    }
                }
            }
            // notify all neighbours
            c.render.notifyChange();
        }
        
        
    }


    
}
