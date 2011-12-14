package cubetech.collision;

import cern.colt.function.LongObjectProcedure;
import cern.colt.map.OpenLongObjectHashMap;
import cubetech.CGame.ChunkRender;
import cubetech.CGame.CubeChunkDataBuilder;
import cubetech.CGame.REType;
import cubetech.CGame.RenderEntity;
import cubetech.CGame.ViewParams;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import cubetech.common.ICommand;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.GLRef;
import cubetech.gfx.Light;
import cubetech.gfx.Shader;
import cubetech.gfx.VBO;
import cubetech.misc.MutableInteger;
import cubetech.misc.Plane;
import cubetech.misc.Ref;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class ClientCubeMap {


    public OpenLongObjectHashMap chunks = new OpenLongObjectHashMap();
    public Color[] lightSides = new Color[6];

    // render stats
    public static int nSides = 0; // sides rendered this frame
    public static int nChunks = 0;
    public static int nVBOthisFrame = 0; // vbo's updated this frame

    private static Shader shader = null;
    private static Shader shadowShader = null;

    // Vertex data builder
    public CubeChunkDataBuilder builder = new CubeChunkDataBuilder();
   
    // Recieving of chunks
    int lastRefresh = 0;
    public int chunkPerSecond = 0;
    

    // For keeping track of last rendertime for chunks
    private static HashMap<CubeChunk, MutableInteger> chunksWithVBO = new HashMap<CubeChunk, MutableInteger>();
    private static VBO indexBuffer = null;

    private static CVar r_world = Ref.cvars.Get("r_world", "1", EnumSet.of(CVarFlags.CHEAT));

    public ClientCubeMap() {
        shader = Ref.glRef.getShader("WorldFog");
        buildLight();
        Ref.commands.AddCommand("cube_refresh", cmd_cube_refresh);
        indexBuffer = new VBO(32*32*32*4*6, VBO.BufferTarget.Index);
        ByteBuffer data = indexBuffer.map();
        for (int i = 0; i < 32*32*32; i++) {
            data.putInt(i * 4 + 0);
            data.putInt(i * 4 + 1);
            data.putInt(i * 4 + 2);
            data.putInt(i * 4 + 2);
            data.putInt(i * 4 + 3);
            data.putInt(i * 4 + 0);
        }
        indexBuffer.unmap();
    }

    public void dispose() {
        // Clear chunk data builder
        if(builder != null) {
            builder.dispose();
            builder = null;
        }
        if(indexBuffer != null) {
            indexBuffer.destroy();
            indexBuffer = null;
        }

        // Clear chunks
        chunks.forEachPair(new LongObjectProcedure() {
            public boolean apply(long l, Object o) {
                ((CubeChunk)o).destroy();
                return true;
            }
        });
        chunks.clear();
        chunksWithVBO.clear();
    }

    public void Render(ViewParams view) {
        if(Ref.cgame.cg.time - 1000 > lastRefresh) {
            chunkPerSecond = CubeMap.nChunkUpdates;
            CubeMap.nChunkUpdates = 0;
            lastRefresh = Ref.cgame.cg.time;
        }

        // chunk render distance
        int chunkDistance = (int) (view.farDepth * 1f / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE))+1;
        if(chunkDistance <= 0)
            chunkDistance = 1;

        // Origin in chunk coordinates
        int orgX =  (int)Math.floor (view.Origin.x / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
        int orgY =  (int)Math.floor (view.Origin.y / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
        int orgZ =  (int)Math.floor (view.Origin.z / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));

        CubeChunk[] renderList = getChunksWithinDistance(orgX, orgY, orgZ, chunkDistance);

        RenderEntity ent = Ref.render.createEntity(REType.WORLD);
        ent.controllers = renderList;

        Ref.render.addRefEntity(ent);

        if(!Ref.glRef.shadowMan.isRendering()) {
            builder.markLockedChunks(chunks); // push vertex data off to the chunks
            wipeOldChunkVBO(5000); // remove vbo's that hasn't been rendering for 5secs
        }
        
    }

    public static void renderChunkList(CubeChunk[] renderlist, ViewParams view) {
        // clear render stats
        nSides = 0;
        nChunks = 0;
        nVBOthisFrame = 0;
        if(!r_world.isTrue()) return;

        // Set shader (+ uniforms)
        boolean shadowPass = Ref.glRef.shadowMan.isRendering();
        boolean renderingShadows = !shadowPass && Ref.glRef.shadowMan.isEnabled();
        CubeTexture depth = null;
        if(shadowPass) GL11.glCullFace(GL11.GL_FRONT);
        if(Ref.glRef.deferred.isRendering()) {
            shader = Ref.glRef.getShader("WorldDeferred");
            Ref.glRef.PushShader(shader);
            shader.setUniform("far", view.farDepth);
            shader.setUniform("near", view.nearDepth);
        } else {
            if(shadowPass || !Ref.glRef.shadowMan.isEnabled()) {
                if(!Ref.glRef.shadowMan.isEnabled()) {
                    shader = Ref.glRef.getShader("WorldFog");
                    Ref.glRef.PushShader(shader);
                    shader.setUniform("fog_factor", 1f/(view.farDepth/2f));
                    shader.setUniform("fog_color", (Vector4f)new Vector4f(95,87,67,255).scale(1/255f)); // 145, 140, 129
                } else {
                    shader = Ref.glRef.getShader("World");
                    Ref.glRef.PushShader(shader);
                }
            } else {
                if(shadowShader == null) {
                    shadowShader = Ref.glRef.getShader("WorldFogShadow");
                }
                Ref.glRef.PushShader(shadowShader);
                // TODO: unhack static shadow index
                Light light = view.lights.get(0);
                depth = light.getShadowResult().getDepthTexture();
                depth.textureSlot = 1;
                depth.Bind();
                
                shadowShader.setUniform("shadowMatrix", light.getShadowResult().getShadowViewProjections(4, view.lights.get(0)));
                shadowShader.setUniform("cascadeDistances", light.getShadowResult().getCascadeDepths());
                shadowShader.setUniform("fog_factor", 1f/(view.farDepth));
                shadowShader.setUniform("fog_color", (Vector4f)new Vector4f(95,87,67,255).scale(1/255f)); // 145, 140, 129
                shadowShader.setUniform("shadow_bias", Ref.cvars.Find("shadow_bias").fValue);
                shadowShader.setUniform("shadow_factor", Ref.cvars.Find("shadow_factor").fValue);
                shadowShader.setUniform("pcfOffsets", light.getShadowResult().getPCFoffsets());
            }
        }
        GLRef.checkError();

        // Set rendermodes
        GL11.glDisable(GL11.GL_BLEND);
        if(!Ref.glRef.r_fill.isTrue()) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }

        if(!shadowPass)  {
            // Mark visible chunks before doing frustum culling,
            // as to keep chunks loaded while you have your back to them
            for (int i= 0; i < renderlist.length; i++) {
                if(renderlist[i] == null) continue;
                if(renderlist[i].render.getRenderFaces() == 0) continue;
                markChunk(renderlist[i]);
            }

            // Perform frustum culling
            cullList(renderlist, view);
        }

        // Actually render
        renderChunkList(renderlist);
        
        if(shadowPass) GL11.glCullFace(GL11.GL_BACK);

        if(renderingShadows && depth != null) {
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

    private static void cullList(CubeChunk[] renderlist, ViewParams view) {
        if(Ref.render.r_nocull.isTrue()) return;

        Vector3f center = new Vector3f();
        float radius = (float) Math.sqrt((0.5f * 32 * 32) * (0.5f * 32 * 32)) + 512f;
        for (int i= 0; i < renderlist.length; i++) {
            CubeChunk chunk = renderlist[i];
            if(chunk == null) continue;
            center.set(chunk.absmin[0] + 32 * 32 * 0.5f,
                    chunk.absmin[1] + 32 * 32 * 0.5f,
                    chunk.absmin[2] + 32 * 32 * 0.5f);


            if(cullCircle(center, radius, view)) {
                renderlist[i] = null;
            }
        }
    }

    private static boolean cullCircle(Vector3f center, float radius, ViewParams view) {
        for (int j= 0; j < 4; j++) {
            Plane p = view.planes[j];
            float dist = Vector3f.dot(center, p.normal) - p.dist;
            if(dist < -radius) return true;
        }
        return false;
    }

    private CubeChunk[] getChunksWithinDistance(int x, int y, int z, int dist) {
        ArrayList<CubeChunk> chunkList = new ArrayList<CubeChunk>();
        // Render chunks
        for (int orgz= -dist; orgz <= dist; orgz++) {
            for (int orgy= -dist; orgy <= dist; orgy++) {
                for (int orgx= -dist; orgx <= dist; orgx++) {
                    long lookup = CubeMap.positionToLookup(orgx + x, orgy + y, orgz + z);
                    CubeChunk chunk = (CubeChunk) chunks.get(lookup);
                    if(chunk == null) continue; // Chunk not generated yet
                    if(chunk.render == null) {
                        if(chunk.nCubes == 0) continue;
                        else chunk.render = new ChunkRender(chunk);
                    }
                    chunkList.add(chunk);
                }
            }
        }
        CubeChunk[] arr = new CubeChunk[chunkList.size()];
        return chunkList.toArray(arr);
    }

    private static void renderChunkList(CubeChunk[] renderList) {
        // Prepare opengl
        initOpengGLRender();
        indexBuffer.bind();

        // Render all the chunks
        for (int i= 0; i < renderList.length; i++) {
            CubeChunk chunk = renderList[i];
            if(chunk == null) continue;
            chunk.render.Render();

            // Count rendered faces
            int nFaces = chunk.render.getRenderFaces();
            nSides += nFaces;
            if(nFaces > 0) {
                nChunks++;
            }
        }
        
        ChunkRender.postVbo();
        indexBuffer.unbind();
    }

    // Marks chunks for VBO unloading
    private static void markChunk(CubeChunk chunk) {
        if(chunk.render == null || chunk.render.state != ChunkRender.State.READY) return;
        MutableInteger cached = chunksWithVBO.get(chunk);
        int time = Ref.common.frametime;
        if(cached == null) {
            chunksWithVBO.put(chunk, new MutableInteger(time));
        } else {
            cached.setValue(time);
        }
    }

    private void wipeOldChunkVBO(int timeout) {
        int minTime = Ref.common.frametime - timeout;
        Iterator<Entry<CubeChunk, MutableInteger>> it = chunksWithVBO.entrySet().iterator();
        while(it.hasNext()) {
            Entry<CubeChunk, MutableInteger> entry = it.next();
            if(entry.getValue().getValue() < minTime) {
                // too old
                if(entry.getKey().render != null) {
                    if(entry.getKey().render.state != ChunkRender.State.DIRTY)
                    // destroy render
                    //Common.Log("Releasing VBO");
                    entry.getKey().render.destroy();
                }
                it.remove();
            }
        }
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

    private static void initOpengGLRender() {
        CubeTexture tex = Ref.ResMan.LoadTexture("data/textures/terrain.png");
        tex.setFiltering(false, GL11.GL_NEAREST);
        tex.setWrap(GL12.GL_CLAMP_TO_EDGE);
        tex.setFiltering(true, GL11.GL_NEAREST_MIPMAP_LINEAR);
        GL11.glTexParameteri(tex.getTarget(), GL12.GL_TEXTURE_MIN_LOD, 0);
        GL11.glTexParameteri(tex.getTarget(), GL12.GL_TEXTURE_MAX_LOD, 2);
        
        ChunkRender.preVbo();
    }
    
    

    private ICommand cmd_cube_refresh = new ICommand() {
        public void RunCommand(String[] args) {
            // chunk render distance
            ViewParams view = Ref.cgame.cg.refdef;
            int chunkDistance = (int) (view.farDepth * 1f / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE))+1;
            if(chunkDistance <= 0)
                chunkDistance = 1;

            // Origin in chunk coordinates
            int orgX =  (int)Math.floor (view.Origin.x / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
            int orgY =  (int)Math.floor (view.Origin.y / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
            int orgZ =  (int)Math.floor (view.Origin.z / (CubeChunk.SIZE * CubeChunk.BLOCK_SIZE));
            for (int z= -chunkDistance; z <= chunkDistance; z++) {
                for (int y= -chunkDistance; y <= chunkDistance; y++) {
                    for (int x= -chunkDistance; x <= chunkDistance; x++) {
                        long lookup = CubeMap.positionToLookup(orgX + x, orgY + y, orgZ + z);
                        CubeChunk chunk = (CubeChunk) chunks.get(lookup);
                        if(chunk == null) continue;
                        chunk.render.setDirty(true, true);
                    }
                }
            }
        }
    };

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
}
