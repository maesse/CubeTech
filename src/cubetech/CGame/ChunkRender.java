/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
    private static final float VBO_RESIZE_MULTIPLIER = 1.3f;
    private static final int LAZY_TIME = 1000;
    public static final int SIZE = CubeChunk.SIZE;
    public static final int PLANE_SIZE = CubeChunk.PLANE_SIZE;
    public static final int BLOCK_SIZE = CubeChunk.BLOCK_SIZE;
    // render
    private VBO vbo = null;
    private boolean dirty = true;
    private boolean lazyDirty = false;
    private int lazyDirtyTime;
    public int nSides = 0; // Number of QUAD's that needs to be rendered
    private byte[] packedVis = new byte[CubeChunk.CHUNK_SIZE];
    private int[] packedAO = new int[CubeChunk.CHUNK_SIZE];

    private CubeChunk chunk;

    public ChunkRender(CubeChunk chunk) {
        this.chunk = chunk;
    }

    private static byte packVis(boolean[] vals) {
        byte out = 0;
        for (int i= 0; i < vals.length; i++) {
            if(!vals[i])
                continue;
            out |= 1 << i;
        }
        return out;
    }

    private static int packAO(boolean[] vals) {
        int out = 0;
        for (int i= 0; i < vals.length; i++) {
            if(!vals[i])
                continue;
            out |= 1 << i;
        }
        return out;
    }

    // Takes a packVis and sets the bit with the given value
    private void packAO(int index, int bit, boolean value) {
        if(value)
            packedAO[index] |= 1 << bit;
        else
            packedAO[index] &= ~(1 << bit);
    }

    // Takes a packVis and sets the bit with the given value
    private void packVis(int index, int bit, boolean value) {
        if(value)
            packedVis[index] |= 1 << bit;
        else
            packedVis[index] &= ~(1 << bit);
    }

    private boolean unpackVis(int index, int bit) {
        return (packedVis[index] & (1 << bit)) != 0;
    }

    private boolean unpackAO(int index, int bit) {
        return (packedAO[index] & (1 << bit)) != 0;
    }

    public void Render() {
        //RenderSimple();
        markVisible();

        if(nSides > 0)
            renderVBO();

//        renderDebug();
    }

//    private void renderDebug() {
//        if(traceTime <= 0)
//            return;
//
//        traceTime -= 10;
//        if(traceTime <= 0) {
//            traceTime = 0;
//            traceCount = 0;
//        }
//
//        for (int i= 0; i < traceCount; i++) {
//            int index = traceCache[i];
//            int z = index / (SIZE*SIZE);
//            index -= z * SIZE*SIZE;
//            int y = index / SIZE;
//            index -= y * SIZE;
//            int x = index;
//            renderSingle(x, y, z, 1);
//        }
//    }

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
        if(!dirty && !lazyDirty)
            return;

        if(lazyDirty && lazyDirtyTime > Ref.common.frametime)
            return; // lets be lazy

        nSides = 0;

        int[] p = chunk.p;
        CubeChunk chunkX = CubeMap.getChunk(p[0]-1, p[1], p[2], false, chunk.chunks);
        CubeChunk chunkX2 = CubeMap.getChunk(p[0]+1, p[1], p[2], false, chunk.chunks);
        CubeChunk chunkY = CubeMap.getChunk(p[0], p[1]-1, p[2], false, chunk.chunks);
        CubeChunk chunkY2 = CubeMap.getChunk(p[0], p[1]+1, p[2], false, chunk.chunks);
        CubeChunk chunkZ = CubeMap.getChunk(p[0], p[1], p[2]-1, false, chunk.chunks);
        CubeChunk chunkZ2 = CubeMap.getChunk(p[0], p[1], p[2]+1, false, chunk.chunks);
        boolean[] cubeVis = new boolean[6];
        for (int z= 0; z < CubeChunk.SIZE; z++) {
            for (int y= 0; y < CubeChunk.SIZE; y++) {
                for (int x= 0; x < CubeChunk.SIZE; x++) {
                    int lookup = CubeChunk.getIndex(x, y, z);
                    if(chunk.blockType[lookup] == 0) {
                        continue;
                    }

                    // Check all 6 sides
                    if(x < CubeChunk.SIZE-1) {
                        cubeVis[0]  = chunk.blockType[lookup+1] == 0 && (nSides++ >= 0);
                    } else cubeVis[0] = false;
                    if(x > 0) {
                        cubeVis[1]  = chunk.blockType[lookup-1] == CubeType.EMPTY && (nSides++ >= 0);
                    } else cubeVis[1] = false;
                    if(y < CubeChunk.SIZE-1) {
                        cubeVis[2]  = chunk.blockType[lookup+CubeChunk.SIZE] == 0 && (nSides++ >= 0);
                    } else cubeVis[2] = false;
                    if(y > 0) {
                        cubeVis[3]  = chunk.blockType[lookup-CubeChunk.SIZE] == 0 && (nSides++ >= 0);
                    } else cubeVis[3] = false;
                    if(z < CubeChunk.SIZE-1) {
                        cubeVis[4] = chunk.blockType[lookup+CubeChunk.SIZE*CubeChunk.SIZE] == 0 && (nSides++ >= 0);
                    } else cubeVis[4] = false;
                    if(z > 0) {
                        cubeVis[5] = chunk.blockType[lookup-CubeChunk.SIZE*CubeChunk.SIZE] == 0 && (nSides++ >= 0);
                    } else cubeVis[5] = false;

                    // Pack up the vis
                    // TODO: implement on the chunks sides
                    packedVis[lookup] = packVis(cubeVis);

                    // Calculate AO
                    // Z++
                    boolean ao[] = new boolean[24];
                    if(z < SIZE-1) {
                        ao[0] = (x < SIZE-1) && chunk.blockType[lookup+1+SIZE*SIZE] == 0;
                        ao[1] = (x > 0) && chunk.blockType[lookup-1+SIZE*SIZE] == 0;
                        ao[2] = (y < SIZE-1) && chunk.blockType[lookup+SIZE+SIZE*SIZE] == 0;
                        ao[3] = (y > 0) && chunk.blockType[lookup-SIZE+SIZE*SIZE] == 0;
                        ao[4] = (x < SIZE-1 && y < SIZE-1) && chunk.blockType[lookup+1+SIZE+SIZE*SIZE] == 0;
                        ao[5] = (x > 0 && y < SIZE-1) && chunk.blockType[lookup-1+SIZE+SIZE*SIZE] == 0;
                        ao[6] = (y > 0 && x > 0) && chunk.blockType[lookup-1-SIZE+SIZE*SIZE] == 0;
                        ao[7] = (y > 0 && z < SIZE-1) && chunk.blockType[lookup-SIZE+1+SIZE*SIZE] == 0;
                    }
                    if(z > 0) {
                        ao[8] = (x < SIZE-1) && chunk.blockType[lookup+1-SIZE*SIZE] == 0;
                        ao[9] = (x > 0) && chunk.blockType[lookup-1-SIZE*SIZE] == 0;
                        ao[10] = (y < SIZE-1) && chunk.blockType[lookup+SIZE-SIZE*SIZE] == 0;
                        ao[11] = (y > 0) && chunk.blockType[lookup-SIZE-SIZE*SIZE] == 0;
                        ao[12] = (x < SIZE-1 && y < SIZE-1) && chunk.blockType[lookup+1+SIZE-SIZE*SIZE] == 0;
                        ao[13] = (x > 0 && y < SIZE-1) && chunk.blockType[lookup-1+SIZE-SIZE*SIZE] == 0;
                        ao[14] = (y > 0 && x > 0) && chunk.blockType[lookup-1-SIZE-SIZE*SIZE] == 0;
                        ao[15] = (y > 0 && z < SIZE-1) && chunk.blockType[lookup-SIZE+1-SIZE*SIZE] == 0;
                    }
                    ao[16] = (x < SIZE-1) && chunk.blockType[lookup+1] == 0;
                    ao[17] = (x > 0) && chunk.blockType[lookup-1] == 0;
                    ao[18] = (y < SIZE-1) && chunk.blockType[lookup+SIZE] == 0;
                    ao[19] = (y > 0) && chunk.blockType[lookup-SIZE] == 0;
                    ao[20] = (x < SIZE-1 && y < SIZE-1) && chunk.blockType[lookup+1+SIZE] == 0;
                    ao[21] = (x > 0 && y < SIZE-1) && chunk.blockType[lookup-1+SIZE] == 0;
                    ao[22] = (y > 0 && x > 0) && chunk.blockType[lookup-1-SIZE] == 0;
                    ao[23] = (y > 0 && z < SIZE-1) && chunk.blockType[lookup-SIZE+1] == 0;
                    packedAO[lookup] = packAO(ao);
                }


            }
        }
        for (int z= 0; z < SIZE; z++) {
            for (int y= 0; y < SIZE; y++) {
        // Handle ZY plane adjencent to another chunk
                if(chunkX != null) {
                    int lookup = CubeChunk.getIndex(0, y, z);
                    if(chunk.blockType[lookup] != 0) {
                        boolean vis = chunkX.blockType[lookup+(SIZE-1)] == 0;
                        packVis(lookup, 1, vis);

                        if(vis) nSides++;
                    }
                }

                if(chunkX2 != null) {
                    int lookup = CubeChunk.getIndex(SIZE-1, y, z);
                    if(chunk.blockType[lookup] != 0) {
                        boolean vis = chunkX2.blockType[lookup-(SIZE-1)] == 0;
                        packVis(lookup, 0, vis);

                        if(vis) nSides++;
                    }
                }

                // ZX plane
                if(chunkY != null) {
                    int lookup = CubeChunk.getIndex(y, 0, z);
                    if(chunk.blockType[lookup] != 0) {
                        boolean vis = chunkY.blockType[lookup+SIZE*(SIZE-1)] == 0;

                        packVis(lookup, 3, vis);
                        if(vis) nSides++;
                    }
                }

                if(chunkY2 != null) {
                    int lookup = CubeChunk.getIndex(y, SIZE-1, z);
                    if(chunk.blockType[lookup] != 0) {
                        boolean vis = chunkY2.blockType[lookup-SIZE*(SIZE-1)] == 0;

                        packVis(lookup, 2, vis);
                        if(vis) nSides++;
                    }
                }

                // XY plane
                if(chunkZ != null) {
                    int lookup = CubeChunk.getIndex(y, z, 0);
                    if(chunk.blockType[lookup] != 0) {
                        boolean vis = chunkZ.blockType[lookup+SIZE*SIZE*(SIZE-1)] == 0;

                        packVis(lookup, 5, vis);
                        if(vis) nSides++;
                    }
                }

                if(chunkZ2 != null) {
                    int lookup = CubeChunk.getIndex(y, z, SIZE-1);
                    if(chunk.blockType[lookup] != 0) {
                        boolean vis = chunkZ2.blockType[lookup-SIZE*SIZE*(SIZE-1)] == 0;

                        packVis(lookup, 4, vis);
                        if(vis) nSides++;
                    }
                }
            }
        }

        updateVBO();
        setDirty(false, false);
    }

    private void updateVBO() {
        if(nSides == 0) {
            // TODO: Release VBO
            return;
        }
        if(vbo == null) {
            // Start off with an exact vbo size.
            vbo = new VBO(PLANE_SIZE * nSides, BufferTarget.Vertex);
            // overgrow a bit when resizing
            vbo.resizeMultiplier = VBO_RESIZE_MULTIPLIER;
        }

        ByteBuffer buffer = vbo.map(PLANE_SIZE * nSides);

        try {
            fillBuffer(buffer);
        } catch(BufferOverflowException ex) {
            Ref.common.Error(ErrorCode.FATAL,"CubeChunk.fillBuffer: VBO overflow" + Common.getExceptionString(ex));

        }

        vbo.unmap();
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

    private void fillBuffer(ByteBuffer buffer) {
        int CHUNK_SIDE = SIZE * BLOCK_SIZE;
        int[] p = chunk.p;
        int ppx = p[0] * CHUNK_SIDE;
        int ppy = p[1] * CHUNK_SIDE;
        int ppz = p[2] * CHUNK_SIDE;

        int sidesRendered = 0;

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



                    // Top: Z+
                    boolean ao1, ao2, ao3;
                    if(unpackVis(index, 4)) {
                        color = Ref.cgame.map.lightSides[4];
                        ao1 = unpackAO(index, 1);
                        ao2 = unpackAO(index, 3);
                        ao3 = unpackAO(index, 6);
                        buffer.putFloat(lx).putFloat(ly).putFloat(lz+BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        //color.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        ao1 = unpackAO(index, 0);
                        ao2 = unpackAO(index, 3);
                        ao3 = unpackAO(index, 7);
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(             lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        //color.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);

                        ao1 = unpackAO(index, 0);
                        ao2 = unpackAO(index, 2);
                        ao3 = unpackAO(index, 4);
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        //color.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = unpackAO(index, 1);
                        ao2 = unpackAO(index, 2);
                        ao3 = unpackAO(index, 5);
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        //color.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);

                        sidesRendered++;
                    }

                    // Bottom: Z-
                    if(unpackVis(index, 5)) {
                    //if(visible[index*6+5]) {
                        color = Ref.cgame.map.lightSides[5];
                        if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.BOTTOM);

                        ao1 = unpackAO(index, 1+8);
                        ao2 = unpackAO(index, 3+8);
                        ao3 = unpackAO(index, 6+8);
                        buffer.putFloat(lx).putFloat(             ly).putFloat(                 lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        ao1 = unpackAO(index, 1+8);
                        ao2 = unpackAO(index, 2+8);
                        ao3 = unpackAO(index, 5+8);
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(    lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);


                        ao1 = unpackAO(index, 0+8);
                        ao2 = unpackAO(index, 2+8);
                        ao3 = unpackAO(index, 4+8);
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(    lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = unpackAO(index, 0+8);
                        ao2 = unpackAO(index, 3+8);
                        ao3 = unpackAO(index, 7+8);
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(                 lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                        sidesRendered++;
                    }

                    // Y+
                    if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.SIDE);
                    if(unpackVis(index, 2)) {
                    //if(visible[index*6+2]) {
                        color = Ref.cgame.map.lightSides[2];
                        ao3 = unpackAO(index, 5+8);
                        ao1 = unpackAO(index, 2+8);
                        ao2 = unpackAO(index, 5+8+8);
                        buffer.putFloat(lx).putFloat(             ly+ BLOCK_SIZE).putFloat(     lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        ao1 = unpackAO(index, 2);
                        ao2 = unpackAO(index, 5+8+8);
                        ao3 = unpackAO(index, 5);
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(    lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);

                        ao1 = unpackAO(index, 4+8+8);
                        ao2 = unpackAO(index, 2);
                        ao3 = unpackAO(index, 4);
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = unpackAO(index, 4+8+8);
                        ao2 = unpackAO(index, 2+8);
                        ao3 = unpackAO(index, 4+8);
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                        sidesRendered++;
                    }

                    // Y-
                    if(unpackVis(index, 3)) {
                    //if(visible[index*6+3]) {
                        color = Ref.cgame.map.lightSides[3];
                        ao1 = unpackAO(index, 22);
                        ao2 = unpackAO(index, 11);
                        ao3 = unpackAO(index, 14);
                        //ao1 = ao2 = ao3 = true;
                        buffer.putFloat(lx).putFloat(             ly).putFloat(     lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);


                        ao1 = unpackAO(index, 23);
                        ao2 = unpackAO(index, 11);
                        ao3 = unpackAO(index, 15);
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);

                        ao1 = unpackAO(index, 23);
                        ao2 = unpackAO(index, 3);
                        ao3 = unpackAO(index, 7);
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly ).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = unpackAO(index, 22);
                        ao2 = unpackAO(index, 3);
                        ao3 = unpackAO(index, 6);
                        buffer.putFloat(lx).putFloat(             ly).putFloat(    lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);
                        sidesRendered++;
                    }

                    // X+
                    if(unpackVis(index, 0)) {
                    //if(visible[index*6]) {
                        color = Ref.cgame.map.lightSides[0];
                        ao1 = unpackAO(index, 8);
                        ao2 = unpackAO(index, 23);
                        ao3 = unpackAO(index, 15);
                        buffer.putFloat(lx+ BLOCK_SIZE).putFloat( ly).putFloat(                  lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        ao1 = unpackAO(index, 20);
                        ao2 = unpackAO(index, 8);
                        ao3 = unpackAO(index, 12);
                        buffer.putFloat(lx+ BLOCK_SIZE ).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);

                        ao2 = unpackAO(index, 0);
                        ao1 = unpackAO(index, 20);
                        ao3 = unpackAO(index, 4);
                        buffer.putFloat(lx+ BLOCK_SIZE ).putFloat(ly+ BLOCK_SIZE ).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = unpackAO(index, 0);
                        ao2 = unpackAO(index, 23);
                        ao3 = unpackAO(index, 7);
                        buffer.putFloat(lx+ BLOCK_SIZE).putFloat( ly).putFloat(                 lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);
                        sidesRendered++;
                    }

                    // X-
                    if(unpackVis(index, 1)) {
                    //if(visible[index*6+1]) {
                        color = Ref.cgame.map.lightSides[1];
                        ao1 = unpackAO(index, 22);
                        ao2 = unpackAO(index, 9);
                        ao3 = unpackAO(index, 14);
                        buffer.putFloat(lx).putFloat( ly).putFloat(                  lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        ao1 = unpackAO(index, 22);
                        ao2 = unpackAO(index, 1);
                        ao3 = unpackAO(index, 6);
                        buffer.putFloat(lx).putFloat( ly).putFloat(                 lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);

                        ao1 = unpackAO(index, 21);
                        ao2 = unpackAO(index, 1);
                        ao3 = unpackAO(index, 5);
                        buffer.putFloat(lx ).putFloat(ly+ BLOCK_SIZE ).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);

                        ao1 = unpackAO(index, 21);
                        ao2 = unpackAO(index, 9);
                        ao3 = unpackAO(index, 13);
                        buffer.putFloat(lx ).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                        sidesRendered++;
                    }
                }
            }
        }

        if(sidesRendered != nSides) {
            int test = 2;
        }
    }

    public static void preVbo() {
        int stride = 32;
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_POSITION); // position
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_POSITION, 3, GL11.GL_FLOAT, false, stride, 0);

//
//        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COLOR); // color
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COLOR, 4, GL11.GL_UNSIGNED_BYTE, true, stride, 3*4);

//        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COORDS); // coords
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS, 2, GL11.GL_FLOAT, false, stride, 4*4);
        CubeTexture tex = Ref.ResMan.LoadTexture("data/terrain.png");
        tex.setFiltering(false, GL11.GL_NEAREST);
        tex.setAnisotropic(4);
        tex.setWrap(GL12.GL_CLAMP_TO_EDGE);
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
        GL11.glDrawArrays(GL11.GL_QUADS, 0, nSides*4);
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

    private void RenderSimple() {
        markVisible();

        CubeTexture tex = Ref.ResMan.LoadTexture("data/terrain.png");
        tex.setFiltering(false, GL11.GL_NEAREST);
        tex.setWrap(GL12.GL_CLAMP_TO_EDGE);

        GL11.glBegin(GL11.GL_QUADS);
        int[] p = chunk.p;

        int CHUNK_SIDE = SIZE * BLOCK_SIZE;
        int ppx = p[0] * CHUNK_SIDE;
        int ppy = p[1] * CHUNK_SIDE;
        int ppz = p[2] * CHUNK_SIDE;

        col(false);
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

                    // Top: Z+
                    if(unpackVis(index, 4)) {
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
                    if(unpackVis(index, 5)) {
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
                    if(unpackVis(index, 2)) {
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
                    if(unpackVis(index, 3)) {
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
                    if(unpackVis(index, 0)) {
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
                    if(unpackVis(index, 1)) {
                        Helper.tex(tx.x, tx.y);
                        GL11.glVertex3i(lx, ly,                  lz );
                        Helper.tex(tx.x, tx.w);
                        GL11.glVertex3i(lx, ly,                 lz+ BLOCK_SIZE);
                        Helper.tex(tx.z, tx.w);
                        GL11.glVertex3i(lx ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
                        Helper.tex(tx.z, tx.y);
                        GL11.glVertex3i(lx ,ly+ BLOCK_SIZE,     lz);
                    }
                }
            }
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
