package cubetech.CGame;

import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import cubetech.common.Helper;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.CubeType;
import cubetech.gfx.GLRef.BufferTarget;
import cubetech.gfx.Shader;
import cubetech.gfx.TerrainTextureCache;
import cubetech.gfx.VBO;
import cubetech.misc.Ref;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class ChunkRender {
    private static final float VBO_RESIZE_MULTIPLIER = 1.1f;
    private static final int LAZY_TIME = 1000;
    public static final int SIZE = CubeChunk.SIZE;
    public static final int PLANE_SIZE = CubeChunk.PLANE_SIZE;
    public static final int BLOCK_SIZE = CubeChunk.BLOCK_SIZE;
    // render
    private VBO vbo = null;
    private boolean dirty = true;
    private boolean lazyDirty = false;
    private int lazyDirtyTime;
    private CubeChunk chunk;
    private boolean bufferReady = false; // if theres a sourcebuffer ready for copying to vbo
    private boolean updatingBuffer = false; // if theres an update queues
    private boolean vboReady = false; // if vbo is ready for rendering

    private int[] org = new int[3];
    private int[] chunkDelta = new int[3];

    public int sidesRendered; // set by fillbuffer
    private int Bufferindex = -1;

    public ChunkRender(CubeChunk chunk) {
        this.chunk = chunk;
    }

    public void destroy() {
        vboReady = false;
        if(vbo == null) return;
        vbo.destroy();
        vbo = null;
    }

    public void Render() {
        markVisible();

        if(chunk.nCubes > 0 && vboReady)
            renderVBO();
    }

    // notify all neighboughrs
    public void notifyChange() {
        notifyChange(0, false);
        notifyChange(0, true);
        notifyChange(1, false);
        notifyChange(1, true);
        notifyChange(2, false);
        notifyChange(2, true);
    }

    // Notify a neighbourgh chunk of a change on the edge of
    public void notifyChange(int axis, boolean pos) {
        int[] cPos = new int[] {chunk.p[0],chunk.p[1],chunk.p[2]};
        cPos[axis] += pos?1:-1;
        CubeChunk c = CubeMap.getChunk(cPos[0], cPos[1], cPos[2], false, chunk.chunks);

        // mark it dirty
        if(c != null) c.render.setDirty(true, false);
    }

    public void setDirty(boolean isDirty, boolean lazy) {
            if(!isDirty) {
                dirty = false;
                lazyDirty = false;
            } else {
                dirty = true;
                lazyDirty = lazy;
                if(lazy) {
                    lazyDirtyTime = Ref.common.frametime + LAZY_TIME;
                }
            }
    }

    private void markVisible() {
        // We're not dirty
        if(!dirty && !lazyDirty)
            return;

        // Dont want to wash too often
        if(lazyDirty && lazyDirtyTime > Ref.common.frametime)
            return; // lets be lazy
        
        if(!updateVBO()) return; // not ready yet

        // Clear dirty flags
        setDirty(false, false);
    }

    
    private void updateBufffer() {
        updatingBuffer = true;
        bufferReady = false;
        
        Ref.cgame.map.exec.submit(update);

//        Thread t = new Thread(update);
//        t.start();
    }

    private Runnable update = new Runnable() {
        public void run() {
            try {
                boolean exitSleep = false;
                int nWait = 0;
                while(!exitSleep && !Ref.cgame.map.bufferAvailable() && nWait < 5) {
                    try {
                        Thread.sleep(1000);
                        nWait++;
                    } catch (InterruptedException ex) {
                        exitSleep = true;
                        Common.LogDebug("Force exit from sleep");
                    }

                }
                ByteBuffer first = Ref.cgame.map.grabBuffer();
                Bufferindex = Ref.cgame.map.currentWrite;
                first.clear();
                fillBuffer(first);
                first.flip();
                bufferReady = true;
            } catch(BufferOverflowException ex) {
                Ref.common.Error(ErrorCode.FATAL,"CubeChunk.fillBuffer: VBO overflow" + Common.getExceptionString(ex));
            }
            updatingBuffer = false;
        }
    };

    private boolean updateVBO() {
        if(chunk.nCubes == 0) return true;

        if(!bufferReady) {
            // Start async buffer fill
            if(!updatingBuffer) {
                updateBufffer();
            }

            return false; // dont clear dirty, we need this method called
        }

        if(sidesRendered == 0) {
            bufferReady = false; // untag buffer
            Ref.cgame.map.releaseBuffer(Bufferindex);
            Bufferindex = -1;
            return true; // clear dirty
        }

        // Check if we still have the source buffer
        ByteBuffer src = Ref.cgame.map.getBuffer(Bufferindex);
        if(src == null) {
            Common.LogDebug("ByteBuffer data fell out of the circular buffer :/");
            // We fell out of the queue :/
            setDirty(true, true);
            bufferReady = false;
            sidesRendered = 0;
            return false;
        }

        // Create a VBO if we don't have one
        long startTime = System.nanoTime();
        if(vbo == null) {
            // Start off with an exact vbo size.
            vbo = new VBO(PLANE_SIZE * sidesRendered, BufferTarget.Vertex);
            // overgrow a bit when resizing
            vbo.resizeMultiplier = VBO_RESIZE_MULTIPLIER;   
        }
        
        // Copy data to VBO
        ByteBuffer buffer = vbo.map(PLANE_SIZE * sidesRendered);
        buffer.put(src);
        vbo.unmap();

        // Clear state
        vboReady = true;
        bufferReady = false;

        // Release source buffer
        Ref.cgame.map.releaseBuffer(Bufferindex);
        Bufferindex = -1;
        
        long endTime = System.nanoTime();
        float ms = (endTime - startTime)/(1000F*1000F);
        Common.LogDebug("Building Chunk VBO took %fms (%d sides)", ms, sidesRendered);
        
        return true; // clear dirty
    }

    private void padd(ByteBuffer buf) {
        buf.put((byte)0);
        buf.put((byte)0);
        buf.put((byte)0);
        buf.put((byte)0);
        buf.put((byte)0);
        buf.put((byte)0);
        buf.put((byte)0);
        buf.put((byte)0);
    }

    private static void writeColorAndAO(Color color, boolean ao1, boolean ao2, boolean ao3, ByteBuffer dest) {
        int ao = 255;
        if(!ao1 && !ao2) {
            ao = 85;
        } else if(!ao1 || !ao2) {
            ao = 127;
        } else if(!ao3) ao = 127;

        dest.put((byte)(color.getRed() / 255f * ao));
        dest.put((byte)(color.getGreen() / 255f * ao));
        dest.put((byte)(color.getBlue() / 255f * ao));
        dest.put((byte)255);
        //dest.put((byte)(color.getAlpha() / 255f * ao));
    }

    private int[] aoDir = new int[3];
    private int[] ao(int x, int y, int z) {
        aoDir[0] = x; aoDir[1] = y; aoDir[2] = z;
        return aoDir;
    }

    private void fillBuffer(ByteBuffer buffer) {
        int CHUNK_SIDE = SIZE * BLOCK_SIZE;
        int[] p = chunk.p;
        int ppx = p[0] * CHUNK_SIDE;
        int ppy = p[1] * CHUNK_SIDE;
        int ppz = p[2] * CHUNK_SIDE;
        sidesRendered = 0;
        int[] pos = new int[3];
        
        for (int z= 0; z < SIZE; z++) {
            for (int y= 0; y < SIZE; y++) {
                for (int x= 0; x < SIZE; x++) {
                    int index = CubeChunk.getIndex(x, y, z);

                    // Check VIS
                    if(chunk.blockType[index] == 0)
                        continue;

                    // Get absolute coords
                    int lx = ppx+ x * BLOCK_SIZE;
                    int ly = ppy+ y * BLOCK_SIZE;
                    int lz = ppz+ z * BLOCK_SIZE;

                    // Get texture offsets
                    byte type = chunk.blockType[index];
                    boolean multiTex = (type < 0);
                    Vector4f tx;

                    // Render thyme!
                    if(!multiTex) tx = TerrainTextureCache.getTexOffset(type);
                    else tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.TOP);

                    Color color = null;

                    pos[0] = x; pos[1] = y; pos[2] = z;
                    boolean ao1, ao2, ao3;

                    // Top: Z+
                    if(derp(pos, ao(0,0,1))) {
                        color = Ref.cgame.map.lightSides[4];
                        ao1 = derp(pos, ao(-1,0,1));
                        ao2 = derp(pos, ao(0,-1,1));
                        ao3 = derp(pos, ao(-1,-1,1));
                        buffer.putFloat(lx).putFloat(ly).putFloat(lz+BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        ao1 = derp(pos, ao(1,0,1));
                        ao2 = derp(pos, ao(0,-1,1));
                        ao3 = derp(pos, ao(1,-1,1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(             lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);

                        ao1 = derp(pos, ao(1,0,1));
                        ao2 = derp(pos, ao(0,1,1));
                        ao3 = derp(pos, ao(1,1,1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = derp(pos, ao(-1,0,1));
                        ao2 = derp(pos, ao(0,1,1));
                        ao3 = derp(pos, ao(-1,1,1));
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);

                        sidesRendered++;
                    }

                    // Bottom: Z-
                    if(derp(pos, ao(0,0,-1))) {
                    //if(visible[index*6+5]) {
                        color = Ref.cgame.map.lightSides[5];
                        if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.BOTTOM);
                        ao1 = derp(pos, ao(-1,0,-1));
                        ao2 = derp(pos, ao(0,-1,-1));
                        ao3 = derp(pos, ao(-1,-1,-1));
                        buffer.putFloat(lx).putFloat(             ly).putFloat(                 lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        ao1 = derp(pos, ao(-1,0,-1));
                        ao2 = derp(pos, ao(0,1,-1));
                        ao3 = derp(pos, ao(-1,1,-1));
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(    lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);


                        ao1 = derp(pos, ao(1,0,-1));
                        ao2 = derp(pos, ao(0,1,-1));
                        ao3 = derp(pos, ao(1,1,-1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(    lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = derp(pos, ao(1,0,-1));
                        ao2 = derp(pos, ao(0,-1,-1));
                        ao3 = derp(pos, ao(1,-1,-1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(                 lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                        sidesRendered++;
                    }

                    // Y+
                    if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.SIDE);
                    if(derp(pos, ao(0,1,0))) {
                    //if(visible[index*6+2]) {
                        color = Ref.cgame.map.lightSides[2];
                        ao1 = derp(pos, ao(-1,1,0));
                        ao2 = derp(pos, ao(0,1,-1));
                        ao3 = derp(pos, ao(-1,1,-1));
                        buffer.putFloat(lx).putFloat(             ly+ BLOCK_SIZE).putFloat(     lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        ao1 = derp(pos, ao(-1,1,0));
                        ao2 = derp(pos, ao(0,1,1));
                        ao3 = derp(pos, ao(-1,1,1));
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(    lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);

                        ao1 = derp(pos, ao(1,1,0));
                        ao2 = derp(pos, ao(0,1,1));
                        ao3 = derp(pos, ao(1,1,1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = derp(pos, ao(1,1,0));
                        ao2 = derp(pos, ao(0,1,-1));
                        ao3 = derp(pos, ao(1,1,-1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                        sidesRendered++;
                    }

                    // Y-
                    if(derp(pos, ao(0,-1,0))) {
                    //if(visible[index*6+3]) {
                        color = Ref.cgame.map.lightSides[3];
                        ao1 = derp(pos, ao(-1,-1,0));
                        ao2 = derp(pos, ao(0,-1,-1));
                        ao3 = derp(pos, ao(-1,-1,-1));
                        //ao1 = ao2 = ao3 = true;
                        buffer.putFloat(lx).putFloat(             ly).putFloat(     lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);


                        ao1 = derp(pos, ao(1,-1,0));
                        ao2 = derp(pos, ao(0,-1,-1));
                        ao3 = derp(pos, ao(1,-1,-1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);

                        ao1 = derp(pos, ao(1,-1,0));
                        ao2 = derp(pos, ao(0,-1,1));
                        ao3 = derp(pos, ao(1,-1,1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly ).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = derp(pos, ao(-1,-1,0));
                        ao2 = derp(pos, ao(0,-1,1));
                        ao3 = derp(pos, ao(-1,-1,1));
                        buffer.putFloat(lx).putFloat(             ly).putFloat(    lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);
                        sidesRendered++;
                    }

                    // X+
                    if(derp(pos, ao(1,0,0))) {
                    //if(visible[index*6]) {
                        color = Ref.cgame.map.lightSides[0];
                        ao1 = derp(pos, ao(1,-1,0));
                        ao2 = derp(pos, ao(1,0,-1));
                        ao3 = derp(pos, ao(1,-1,-1));
                        buffer.putFloat(lx+ BLOCK_SIZE).putFloat( ly).putFloat(                  lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        ao1 = derp(pos, ao(1,1,0));
                        ao2 = derp(pos, ao(1,0,-1));
                        ao3 = derp(pos, ao(1,1,-1));
                        buffer.putFloat(lx+ BLOCK_SIZE ).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);

                        ao1 = derp(pos, ao(1,1,0));
                        ao2 = derp(pos, ao(1,0,1));
                        ao3 = derp(pos, ao(1,1,1));
                        buffer.putFloat(lx+ BLOCK_SIZE ).putFloat(ly+ BLOCK_SIZE ).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = derp(pos, ao(1,-1,0));
                        ao2 = derp(pos, ao(1,0,1));
                        ao3 = derp(pos, ao(1,-1,1));
                        buffer.putFloat(lx+ BLOCK_SIZE).putFloat( ly).putFloat(                 lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);
                        sidesRendered++;
                    }

                    // X-
                    if(derp(pos, ao(-1,0,0))) {
                    //if(visible[index*6+1]) {
                        color = Ref.cgame.map.lightSides[1];
                        ao1 = derp(pos, ao(-1,-1,0));
                        ao2 = derp(pos, ao(-1,0,-1));
                        ao3 = derp(pos, ao(-1,-1,-1));
                        buffer.putFloat(lx).putFloat( ly).putFloat(                  lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        ao1 = derp(pos, ao(-1,-1,0));
                        ao2 = derp(pos, ao(-1,0,1));
                        ao3 = derp(pos, ao(-1,-1,1));
                        buffer.putFloat(lx).putFloat( ly).putFloat(                 lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);

                        ao1 = derp(pos, ao(-1,1,0));
                        ao2 = derp(pos, ao(-1,0,1));
                        ao3 = derp(pos, ao(-1,1,1));
                        buffer.putFloat(lx ).putFloat(ly+ BLOCK_SIZE ).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = derp(pos, ao(-1,1,0));
                        ao2 = derp(pos, ao(-1,0,-1));
                        ao3 = derp(pos, ao(-1,1,-1));
                        buffer.putFloat(lx ).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                        sidesRendered++;
                    }
                }
            }
        }
    }
    
    
    private boolean derp(int[] p,int[] a) {
        CubeChunk baseChunk = chunk;
        boolean gotChunkDelta = false;
        
        for (int i= 0; i < 3; i++) {
            if(p[i]+a[i] < 0) {
                chunkDelta[i] = -1;
                gotChunkDelta = true;
            }
            else if(p[i]+a[i] >= SIZE) {
                chunkDelta[i] = 1;
                gotChunkDelta = true;
            } else {
                chunkDelta[i] = 0;
            }
            org[i] = (p[i] + a[i]) & 31;
        }

        if(gotChunkDelta) {
            baseChunk = getChunkInDirection(chunkDelta);
            if(baseChunk == null) return false;
        }

        return baseChunk.blockType[org[0] | org[1] << 5 | org[2] << 10] == 0;
    }

    private CubeChunk getChunkInDirection(int[] dir) {
        return CubeMap.getChunk(chunk.p[0]+dir[0], chunk.p[1]+dir[1], chunk.p[2]+dir[2], false, chunk.chunks);
    }

    public static void preVbo() {
        int stride = 32;
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_POSITION); // position
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_POSITION, 3, GL11.GL_FLOAT, false, stride, 0);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COLOR); // color
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COLOR, 4, GL11.GL_UNSIGNED_BYTE, true, stride, 3*4);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COORDS); // coords
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS, 2, GL11.GL_FLOAT, false, stride, 4*4);
        CubeTexture tex = Ref.ResMan.LoadTexture("data/terrain.png");

        tex.setFiltering(false, GL11.GL_NEAREST);
//        tex.setAnisotropic(0);
        tex.setWrap(GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(tex.getTarget(), GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
        GL11.glTexParameteri(tex.getTarget(), GL12.GL_TEXTURE_MIN_LOD, 0);
        GL11.glTexParameteri(tex.getTarget(), GL12.GL_TEXTURE_MAX_LOD, 2);
//        Ref.ResMan.getWhiteTexture().Bind();
    }

    public static void postVbo() {
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
    }

    private void renderVBO() {
        vbo.bind();
        preVbo();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDrawArrays(GL11.GL_QUADS, 0, sidesRendered*4);
        //GL12.glDrawRangeElements(GL11.GL_QUADS, callStart, callEnd-1, callLenght, GL11.GL_UNSIGNED_INT, offset);
        GL11.glEnable(GL11.GL_BLEND);
        postVbo();
        vbo.unbind();
    }

    public void renderSingle(int x, int y, int z, int typ) {
        // ready the texture
        CubeTexture tex = Ref.ResMan.LoadTexture("data/terrain.png");
        tex.setFiltering(false, GL11.GL_NEAREST);
        tex.setWrap(GL12.GL_CLAMP_TO_EDGE);

        int[] p = chunk.p;
        //
        int CHUNK_SIDE = SIZE * BLOCK_SIZE;
        int ppx = p[0] * CHUNK_SIDE;
        int ppy = p[1] * CHUNK_SIDE;
        int ppz = p[2] * CHUNK_SIDE;

        // Get absolute coords
        int lx = ppx+ x * BLOCK_SIZE;
        int ly = ppy+ y * BLOCK_SIZE;
        int lz = ppz+ z * BLOCK_SIZE;

        // Get texture offsets
        byte type = (byte) typ;
        boolean multiTex = (type < 0);
        Vector4f tx;

        GL11.glBegin(GL11.GL_QUADS);
        col(false);

        // Render thyme!
        if(!multiTex) tx = TerrainTextureCache.getTexOffset(type);
        else tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.TOP);

        // Top: Z+
        {
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,             lz+ BLOCK_SIZE);
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,             lz+ BLOCK_SIZE);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
        }

        // Bottom: Z-
        {
            if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.BOTTOM);
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,                 lz );
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,    lz);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,    lz );
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,                 lz);
        }

        // Y+
        if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.SIDE);
        {
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly+ BLOCK_SIZE,     lz );
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,    lz+ BLOCK_SIZE);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,    lz + BLOCK_SIZE);
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly+ BLOCK_SIZE,     lz);
        }

        // Y-
        {
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,     lz );
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,     lz);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly ,    lz + BLOCK_SIZE);
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly,    lz+ BLOCK_SIZE);
        }

        // X+
        {
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx+ BLOCK_SIZE, ly,                  lz );
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx+ BLOCK_SIZE ,ly+ BLOCK_SIZE,     lz);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx+ BLOCK_SIZE ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx+ BLOCK_SIZE, ly,                 lz+ BLOCK_SIZE);
        }

        // X-
        {
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx, ly,                  lz );
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx, ly,                 lz+ BLOCK_SIZE);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx ,ly+ BLOCK_SIZE,     lz);
        }
        GL11.glEnd();
    }

    public void renderSingleWireframe(int x, int y, int z, int typ) {
        // ready the texture
        CubeTexture tex = Ref.ResMan.LoadTexture("data/terrain.png");
        tex.setFiltering(false, GL11.GL_NEAREST);
        tex.setWrap(GL12.GL_CLAMP_TO_EDGE);
        int[] p = chunk.p;

        //
        int CHUNK_SIDE = SIZE * BLOCK_SIZE;
        int ppx = p[0] * CHUNK_SIDE;
        int ppy = p[1] * CHUNK_SIDE;
        int ppz = p[2] * CHUNK_SIDE;

        // Get absolute coords
        int lx = ppx+ x * BLOCK_SIZE;
        int ly = ppy+ y * BLOCK_SIZE;
        int lz = ppz+ z * BLOCK_SIZE;

        // Get texture offsets
        byte type = (byte) typ;
        boolean multiTex = (type < 0);
        Vector4f tx;

        GL11.glBegin(GL11.GL_LINES);
        col(false);

        // Render thyme!
        if(!multiTex) tx = TerrainTextureCache.getTexOffset(type);
        else tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.TOP);

        // Top: Z+
        {
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,             lz+ BLOCK_SIZE);
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,             lz+ BLOCK_SIZE);
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,             lz+ BLOCK_SIZE);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,             lz+ BLOCK_SIZE);
        }

        // Bottom: Z-
        {
            if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.BOTTOM);
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,                 lz );
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,    lz);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,    lz );
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,                 lz);
        }

        // Y+
        if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.SIDE);
        {
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly+ BLOCK_SIZE,     lz );
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,    lz+ BLOCK_SIZE);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,    lz + BLOCK_SIZE);
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly+ BLOCK_SIZE,     lz);
        }

        // Y-
        {
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,     lz );
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,     lz);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly ,    lz + BLOCK_SIZE);
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly,    lz+ BLOCK_SIZE);
        }

        // X+
        {
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx+ BLOCK_SIZE, ly,                  lz );
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx+ BLOCK_SIZE ,ly+ BLOCK_SIZE,     lz);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx+ BLOCK_SIZE ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx+ BLOCK_SIZE, ly,                 lz+ BLOCK_SIZE);
        }

        // X-
        {
            Helper.tex(tx.x, tx.y);
            GL11.glVertex3i(lx, ly,                  lz );
            Helper.tex(tx.x, tx.w);
            GL11.glVertex3i(lx, ly,                 lz+ BLOCK_SIZE);
            Helper.tex(tx.z, tx.w);
            GL11.glVertex3i(lx ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
            Helper.tex(tx.z, tx.y);
            GL11.glVertex3i(lx ,ly+ BLOCK_SIZE,     lz);
        }
        GL11.glEnd();
    }

    private void col(boolean derp) {
        if(Ref.glRef.isShadersSupported())
            GL20.glVertexAttrib4Nub(1, derp?(byte)125:(byte)255,(byte)255,(byte)255,(byte)255);
        else
            GL11.glColor4ub(derp?(byte)125:(byte)255,(byte)255,(byte)255,(byte)255);
    }

    
}
