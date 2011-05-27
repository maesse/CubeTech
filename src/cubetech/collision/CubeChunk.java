package cubetech.collision;

import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.CubeType;
import cubetech.gfx.GLRef.BufferTarget;
import cubetech.gfx.TerrainTextureCache;
import cubetech.gfx.VBO;
import cubetech.misc.Ref;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CubeChunk {
    private static final float VBO_RESIZE_MULTIPLIER = 1.3f;
    private static final int LAZY_TIME = 1000;
    public static final int SIZE = 32;
    public static final int CHUNK_SIZE = SIZE*SIZE*SIZE;
    public static final int BLOCK_SIZE = 32;
    public static final float RADIUS = BLOCK_SIZE * SIZE * 1.5f;
    public static final int PLANE_SIZE = 4*32; // (vertex: 3*4, color: 4*1, tex: 2*4) * 4 points

    // block data
//    private boolean[] visible = new boolean[CHUNK_SIZE*6]; // plane vis data
    private byte[] blockType = new byte[CHUNK_SIZE];
    private byte[] packedVis = new byte[CHUNK_SIZE];
    private int[] packedAO = new int[CHUNK_SIZE];
    public int[] absmin = new int[3];
    public int[] absmax = new int[3];
    public float[] fcenter = new float[3];
    private CubeMap map = null;

    // render
    private VBO vbo = null;
    private boolean dirty = true;
    private boolean lazyDirty = false;
    private int lazyDirtyTime;
    public int nSides = 0; // Number of QUAD's that needs to be rendered

    // debug draw
    int traceTime = 0;
    int[] traceCache = null;
    int traceCount = 0;

    int[] p = new int[3]; // Position/Origin. Grows in the positive direction.
    //int px, py, pz;
    public CubeChunk(CubeMap map, int x, int y, int z) {
        this.map = map;
        p = new int[] {x,y,z};
        absmin[0] = x * SIZE * BLOCK_SIZE;
        absmin[1] = y * SIZE * BLOCK_SIZE;
        absmin[2] = z * SIZE * BLOCK_SIZE;
        absmax[0] = (x+1) * SIZE * BLOCK_SIZE;
        absmax[1] = (y+1) * SIZE * BLOCK_SIZE;
        absmax[2] = (z+1) * SIZE * BLOCK_SIZE;
        fcenter[0] = x * SIZE * BLOCK_SIZE + SIZE/2;
        fcenter[1] = y * SIZE * BLOCK_SIZE + SIZE/2;
        fcenter[2] = z * SIZE * BLOCK_SIZE + SIZE/2;
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

    private void renderDebug() {
        if(traceTime <= 0)
            return;

        traceTime -= 10;
        if(traceTime <= 0) {
            traceTime = 0;
            traceCount = 0;
        }

        for (int i= 0; i < traceCount; i++) {
            int index = traceCache[i];
            int z = index / (SIZE*SIZE);
            index -= z * SIZE*SIZE;
            int y = index / SIZE;
            index -= y * SIZE;
            int x = index;
            renderSingle(x, y, z, 1);
        }
    }

    public void setCubeType(int x, int y, int z, byte type, boolean notify) {
        int index = getIndex(x, y, z);
        setDirty(true, false);
        blockType[index] = type;

        if(!notify)
            return;

        // Check if we need to notify our neightboughr
        if(x == 0) notifyChange(0, false);
        if(x == SIZE-1) notifyChange(0, true);
        if(y == 0) notifyChange(1, false);
        if(y == SIZE-1) notifyChange(1, true);
        if(z == 0) notifyChange(2, false);
        if(z == SIZE-1) notifyChange(2, true);
    }

    // notify all neighboughrs
    void notifyChange() {
        notifyChange(0, false);
        notifyChange(0, true);
        notifyChange(1, false);
        notifyChange(1, true);
        notifyChange(2, false);
        notifyChange(2, true);
    }

    // Notify a neighbourgh chunk of a change on the edge of
    private void notifyChange(int axis, boolean pos) {
        if(Ref.cm.cubemap == null)
            return; // happens during loading.. we don't need to notify at this point
        int[] cPos = new int[] {p[0],p[1],p[2]};
        cPos[axis] += pos?1:-1;
        CubeChunk c = Ref.cm.cubemap.getChunk(cPos[0], cPos[1], cPos[2], false);

        // mark it dirty
        if(c != null) c.setDirty(true, false);
    }

    private void setDirty(boolean isDirty, boolean lazy) {
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

    public byte getCubeType(int index)
    {
        return blockType[index];
    }

    ChunkSpatialPart lastPart = null;
    ChunkSpatialPart getCubesInVolume(Vector3f start, Vector3f end) {
        // start
        int sx = (int)Math.floor(start.x / BLOCK_SIZE);
        int sy = (int)Math.floor(start.y / BLOCK_SIZE);
        int sz = (int)Math.floor(start.z / BLOCK_SIZE);
        if(sx >= SIZE) sx = SIZE-1;
        if(sy >= SIZE) sy = SIZE-1;
        if(sz >= SIZE) sz = SIZE-1;

        // end
        int ex = (int)Math.floor(end.x / BLOCK_SIZE)+1;
        int ey = (int)Math.floor(end.y / BLOCK_SIZE)+1;
        int ez = (int)Math.floor(end.z / BLOCK_SIZE)+1;
        if(ex > SIZE) ex = SIZE;
        if(ey > SIZE) ey = SIZE;
        if(ez > SIZE) ez = SIZE;
        
        ChunkSpatialPart part = null;
        int count = (ex - sx) * (ey - sy) * (ez - sz)*3;
        if(lastPart != null) {
            part = lastPart;
            part.nIndex = 0;
            if(part.indexes.length < count) {
                part.indexes = new int[count];
            }
        } else {
            part = new ChunkSpatialPart();
            part.chunk = this;
            part.indexes = new int[count];
            lastPart = part;
        }

        // Iterate though cubes
        for (int z= sz; z < ez; z++) {
            for (int y= sy; y < ey; y++) {
                for (int x= sx; x < ex; x++) {
                    int index = getIndex(x, y, z);

                    if(blockType[index] == 0)
                        continue; // no cube there

                    // Save off real-world position
                    part.indexes[part.nIndex * 3] = x * BLOCK_SIZE + absmin[0];
                    part.indexes[part.nIndex * 3 + 1] = y * BLOCK_SIZE + absmin[1];
                    part.indexes[part.nIndex * 3 + 2] = z * BLOCK_SIZE + absmin[2];
                    part.nIndex++;
                }
            }
        }
        return part;
    }

    private void markVisible() {
        if(!dirty && !lazyDirty)
            return;

        if(lazyDirty && lazyDirtyTime > Ref.common.frametime)
            return; // lets be lazy

        nSides = 0;

        CubeChunk chunkX = Ref.cm.cubemap.getChunk(p[0]-1, p[1], p[2], false);
        CubeChunk chunkX2 = Ref.cm.cubemap.getChunk(p[0]+1, p[1], p[2], false);
        CubeChunk chunkY = Ref.cm.cubemap.getChunk(p[0], p[1]-1, p[2], false);
        CubeChunk chunkY2 = Ref.cm.cubemap.getChunk(p[0], p[1]+1, p[2], false);
        CubeChunk chunkZ = Ref.cm.cubemap.getChunk(p[0], p[1], p[2]-1, false);
        CubeChunk chunkZ2 = Ref.cm.cubemap.getChunk(p[0], p[1], p[2]+1, false);
        boolean[] cubeVis = new boolean[6];
        for (int z= 0; z < SIZE; z++) {
            for (int y= 0; y < SIZE; y++) {
                for (int x= 0; x < SIZE; x++) {
                    int lookup = getIndex(x, y, z);
                    if(blockType[lookup] == 0) {
                        continue;
                    }

                    // Check all 6 sides
                    if(x < SIZE-1) {
                        cubeVis[0]  = blockType[lookup+1] == 0 && (nSides++ >= 0);
                    } else cubeVis[0] = false;
                    if(x > 0) {
                        cubeVis[1]  = blockType[lookup-1] == CubeType.EMPTY && (nSides++ >= 0);
                    } else cubeVis[1] = false;
                    if(y < SIZE-1) {
                        cubeVis[2]  = blockType[lookup+SIZE] == 0 && (nSides++ >= 0);
                    } else cubeVis[2] = false;
                    if(y > 0) {
                        cubeVis[3]  = blockType[lookup-SIZE] == 0 && (nSides++ >= 0);
                    } else cubeVis[3] = false;
                    if(z < SIZE-1) {
                        cubeVis[4] = blockType[lookup+SIZE*SIZE] == 0 && (nSides++ >= 0);
                    } else cubeVis[4] = false;
                    if(z > 0) {
                        cubeVis[5] = blockType[lookup-SIZE*SIZE] == 0 && (nSides++ >= 0);
                    } else cubeVis[5] = false;

                    // Pack up the vis
                    // TODO: implement on the chunks sides
                    packedVis[lookup] = packVis(cubeVis);

                    // Calculate AO
                    // Z++
                    boolean ao[] = new boolean[24];
                    if(z < SIZE-1) {
                        ao[0] = (x < SIZE-1) && blockType[lookup+1+SIZE*SIZE] == 0;
                        ao[1] = (x > 0) && blockType[lookup-1+SIZE*SIZE] == 0;
                        ao[2] = (y < SIZE-1) && blockType[lookup+SIZE+SIZE*SIZE] == 0;
                        ao[3] = (y > 0) && blockType[lookup-SIZE+SIZE*SIZE] == 0;
                        ao[4] = (x < SIZE-1 && y < SIZE-1) && blockType[lookup+1+SIZE+SIZE*SIZE] == 0;
                        ao[5] = (x > 0 && y < SIZE-1) && blockType[lookup-1+SIZE+SIZE*SIZE] == 0;
                        ao[6] = (y > 0 && x > 0) && blockType[lookup-1-SIZE+SIZE*SIZE] == 0;
                        ao[7] = (y > 0 && z < SIZE-1) && blockType[lookup-SIZE+1+SIZE*SIZE] == 0;
                    }
                    if(z > 0) {
                        ao[8] = (x < SIZE-1) && blockType[lookup+1-SIZE*SIZE] == 0;
                        ao[9] = (x > 0) && blockType[lookup-1-SIZE*SIZE] == 0;
                        ao[10] = (y < SIZE-1) && blockType[lookup+SIZE-SIZE*SIZE] == 0;
                        ao[11] = (y > 0) && blockType[lookup-SIZE-SIZE*SIZE] == 0;
                        ao[12] = (x < SIZE-1 && y < SIZE-1) && blockType[lookup+1+SIZE-SIZE*SIZE] == 0;
                        ao[13] = (x > 0 && y < SIZE-1) && blockType[lookup-1+SIZE-SIZE*SIZE] == 0;
                        ao[14] = (y > 0 && x > 0) && blockType[lookup-1-SIZE-SIZE*SIZE] == 0;
                        ao[15] = (y > 0 && z < SIZE-1) && blockType[lookup-SIZE+1-SIZE*SIZE] == 0;
                    }
                    ao[16] = (x < SIZE-1) && blockType[lookup+1] == 0;
                    ao[17] = (x > 0) && blockType[lookup-1] == 0;
                    ao[18] = (y < SIZE-1) && blockType[lookup+SIZE] == 0;
                    ao[19] = (y > 0) && blockType[lookup-SIZE] == 0;
                    ao[20] = (x < SIZE-1 && y < SIZE-1) && blockType[lookup+1+SIZE] == 0;
                    ao[21] = (x > 0 && y < SIZE-1) && blockType[lookup-1+SIZE] == 0;
                    ao[22] = (y > 0 && x > 0) && blockType[lookup-1-SIZE] == 0;
                    ao[23] = (y > 0 && z < SIZE-1) && blockType[lookup-SIZE+1] == 0;
                    packedAO[lookup] = packAO(ao);
                }

                
            }
        }
        for (int z= 0; z < SIZE; z++) {
            for (int y= 0; y < SIZE; y++) {
        // Handle ZY plane adjencent to another chunk
                if(chunkX != null) {
                    int lookup = getIndex(0, y, z);
                    if(blockType[lookup] != 0) {
                        boolean vis = chunkX.blockType[lookup+(SIZE-1)] == 0;
                        packVis(lookup, 1, vis);

                        if(vis) nSides++;
                    }
                }

                if(chunkX2 != null) {
                    int lookup = getIndex(SIZE-1, y, z);
                    if(blockType[lookup] != 0) {
                        boolean vis = chunkX2.blockType[lookup-(SIZE-1)] == 0;
                        packVis(lookup, 0, vis);

                        if(vis) nSides++;
                    }
                }

                // ZX plane
                if(chunkY != null) {
                    int lookup = getIndex(y, 0, z);
                    if(blockType[lookup] != 0) {
                        boolean vis = chunkY.blockType[lookup+SIZE*(SIZE-1)] == 0;

                        packVis(lookup, 3, vis);
                        if(vis) nSides++;
                    }
                }

                if(chunkY2 != null) {
                    int lookup = getIndex(y, SIZE-1, z);
                    if(blockType[lookup] != 0) {
                        boolean vis = chunkY2.blockType[lookup-SIZE*(SIZE-1)] == 0;

                        packVis(lookup, 2, vis);
                        if(vis) nSides++;
                    }
                }

                // XY plane
                if(chunkZ != null) {
                    int lookup = getIndex(y, z, 0);
                    if(blockType[lookup] != 0) {
                        boolean vis = chunkZ.blockType[lookup+SIZE*SIZE*(SIZE-1)] == 0;

                        packVis(lookup, 5, vis);
                        if(vis) nSides++;
                    }
                }

                if(chunkZ2 != null) {
                    int lookup = getIndex(y, z, SIZE-1);
                    if(blockType[lookup] != 0) {
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
    
    public static int getIndex(int x, int y, int z) {
        return x + y * SIZE + z * SIZE * SIZE;
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
        int ppx = p[0] * CHUNK_SIDE;
        int ppy = p[1] * CHUNK_SIDE;
        int ppz = p[2] * CHUNK_SIDE;

        int sidesRendered = 0;

        for (int z= 0; z < SIZE; z++) {
            for (int y= 0; y < SIZE; y++) {
                for (int x= 0; x < SIZE; x++) {
                    int index = getIndex(x, y, z);

                    // Check VIS
                    if(blockType[index] == 0)
                        continue;

                    // Get absolute coords
                    int lx = ppx+ x * BLOCK_SIZE;
                    int ly = ppy+ y * BLOCK_SIZE;
                    int lz = ppz+ z * BLOCK_SIZE;

                    // Get texture offsets
                    byte type = blockType[index];
                    boolean multiTex = (type < 0);
                    Vector4f tx;

                    // Render thyme!
                    if(!multiTex) tx = TerrainTextureCache.getTexOffset(type);
                    else tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.TOP);

                    Color color = null;

                    

                    // Top: Z+
                    boolean ao1, ao2, ao3;
                    if(unpackVis(index, 4)) {
                        color = map.lightSides[4];
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
                        color = map.lightSides[5];
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
                        color = map.lightSides[2];
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
                        color = map.lightSides[3];
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
                        color = map.lightSides[0];
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
                        color = map.lightSides[1];
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
        ARBVertexShader.glEnableVertexAttribArrayARB(0); // position
        ARBVertexShader.glVertexAttribPointerARB(0, 3, GL11.GL_FLOAT, false, stride, 0);

//
//        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        ARBVertexShader.glEnableVertexAttribArrayARB(1); // color
        ARBVertexShader.glVertexAttribPointerARB(1, 4, GL11.GL_UNSIGNED_BYTE, true, stride, 3*4);

//        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        ARBVertexShader.glEnableVertexAttribArrayARB(2); // coords
        ARBVertexShader.glVertexAttribPointerARB(2, 2, GL11.GL_FLOAT, false, stride, 4*4);
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
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,             lz+ BLOCK_SIZE);
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,             lz+ BLOCK_SIZE);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
        }

        // Bottom: Z-
        {
            if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.BOTTOM);
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,                 lz );
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,    lz);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,    lz );
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,                 lz);
        }

        // Y+
        if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.SIDE);
        {
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly+ BLOCK_SIZE,     lz );
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,    lz+ BLOCK_SIZE);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,    lz + BLOCK_SIZE);
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly+ BLOCK_SIZE,     lz);
        }

        // Y-
        {
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,     lz );
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,     lz);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly ,    lz + BLOCK_SIZE);
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly,    lz+ BLOCK_SIZE);
        }

        // X+
        {
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx+ BLOCK_SIZE, ly,                  lz );
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx+ BLOCK_SIZE ,ly+ BLOCK_SIZE,     lz);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx+ BLOCK_SIZE ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx+ BLOCK_SIZE, ly,                 lz+ BLOCK_SIZE);
        }

        // X-
        {
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx, ly,                  lz );
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx, ly,                 lz+ BLOCK_SIZE);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx ,ly+ BLOCK_SIZE,     lz);
        }
        GL11.glEnd();
    }

    public void renderSingleWireframe(int x, int y, int z, int typ) {
        // ready the texture
        CubeTexture tex = Ref.ResMan.LoadTexture("data/terrain.png");
        tex.setFiltering(false, GL11.GL_NEAREST);
        tex.setWrap(GL12.GL_CLAMP_TO_EDGE);


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
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,             lz+ BLOCK_SIZE);
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,             lz+ BLOCK_SIZE);
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,             lz+ BLOCK_SIZE);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,             lz+ BLOCK_SIZE);
        }

        // Bottom: Z-
        {
            if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.BOTTOM);
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,                 lz );
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,    lz);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,    lz );
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,                 lz);
        }

        // Y+
        if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.SIDE);
        {
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly+ BLOCK_SIZE,     lz );
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly + BLOCK_SIZE,    lz+ BLOCK_SIZE);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,    lz + BLOCK_SIZE);
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly+ BLOCK_SIZE,     lz);
        }

        // Y-
        {
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx,             ly,     lz );
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly,     lz);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx + BLOCK_SIZE,ly ,    lz + BLOCK_SIZE);
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx,             ly,    lz+ BLOCK_SIZE);
        }

        // X+
        {
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx+ BLOCK_SIZE, ly,                  lz );
            tex(tx.z, tx.y);
            GL11.glVertex3i(lx+ BLOCK_SIZE ,ly+ BLOCK_SIZE,     lz);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx+ BLOCK_SIZE ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx+ BLOCK_SIZE, ly,                 lz+ BLOCK_SIZE);
        }

        // X-
        {
            tex(tx.x, tx.y);
            GL11.glVertex3i(lx, ly,                  lz );
            tex(tx.x, tx.w);
            GL11.glVertex3i(lx, ly,                 lz+ BLOCK_SIZE);
            tex(tx.z, tx.w);
            GL11.glVertex3i(lx ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
            tex(tx.z, tx.y);
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

        int CHUNK_SIDE = SIZE * BLOCK_SIZE;
        int ppx = p[0] * CHUNK_SIDE;
        int ppy = p[1] * CHUNK_SIDE;
        int ppz = p[2] * CHUNK_SIDE;

        col(false);
        for (int z= 0; z < SIZE; z++) {
            for (int y= 0; y < SIZE; y++) {
                for (int x= 0; x < SIZE; x++) {
                    int index = getIndex(x, y, z);
                    
                    // Check VIS
                    if(blockType[index] == 0)
                        continue;

                    // Get absolute coords
                    int lx = ppx+ x * BLOCK_SIZE;
                    int ly = ppy+ y * BLOCK_SIZE;
                    int lz = ppz+ z * BLOCK_SIZE;

                    // Get texture offsets
                    byte type = blockType[index];
                    boolean multiTex = (type < 0);
                    Vector4f tx;

                    // Render thyme!
                    if(!multiTex) tx = TerrainTextureCache.getTexOffset(type);
                    else tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.TOP);
                    
                    // Top: Z+
                    if(unpackVis(index, 4)) {
                        tex(tx.x, tx.y);
                        GL11.glVertex3i(lx,             ly,             lz+ BLOCK_SIZE);
                        tex(tx.z, tx.y);
                        GL11.glVertex3i(lx + BLOCK_SIZE,ly,             lz+ BLOCK_SIZE);
                        tex(tx.z, tx.w);
                        GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
                        tex(tx.x, tx.w);
                        GL11.glVertex3i(lx,             ly + BLOCK_SIZE,lz+ BLOCK_SIZE);
                    }

                    // Bottom: Z-
                    if(unpackVis(index, 5)) {
                        if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.BOTTOM);
                        tex(tx.x, tx.y);
                        GL11.glVertex3i(lx,             ly,                 lz );
                        tex(tx.x, tx.w);
                        GL11.glVertex3i(lx,             ly + BLOCK_SIZE,    lz);
                        tex(tx.z, tx.w);
                        GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,    lz );
                        tex(tx.z, tx.y);
                        GL11.glVertex3i(lx + BLOCK_SIZE,ly,                 lz);
                    }

                    // Y+
                    if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.SIDE);
                    if(unpackVis(index, 2)) {
                        tex(tx.x, tx.y);
                        GL11.glVertex3i(lx,             ly+ BLOCK_SIZE,     lz );
                        tex(tx.x, tx.w);
                        GL11.glVertex3i(lx,             ly + BLOCK_SIZE,    lz+ BLOCK_SIZE);
                        tex(tx.z, tx.w);
                        GL11.glVertex3i(lx + BLOCK_SIZE,ly + BLOCK_SIZE,    lz + BLOCK_SIZE);
                        tex(tx.z, tx.y);
                        GL11.glVertex3i(lx + BLOCK_SIZE,ly+ BLOCK_SIZE,     lz);
                    }

                    // Y-
                    if(unpackVis(index, 3)) {
                        tex(tx.x, tx.y);
                        GL11.glVertex3i(lx,             ly,     lz );
                        tex(tx.z, tx.y);
                        GL11.glVertex3i(lx + BLOCK_SIZE,ly,     lz);
                        tex(tx.z, tx.w);
                        GL11.glVertex3i(lx + BLOCK_SIZE,ly ,    lz + BLOCK_SIZE);
                        tex(tx.x, tx.w);
                        GL11.glVertex3i(lx,             ly,    lz+ BLOCK_SIZE);
                    }

                    // X+
                    if(unpackVis(index, 0)) {
                        tex(tx.x, tx.y);
                        GL11.glVertex3i(lx+ BLOCK_SIZE, ly,                  lz );
                        tex(tx.z, tx.y);
                        GL11.glVertex3i(lx+ BLOCK_SIZE ,ly+ BLOCK_SIZE,     lz);
                        tex(tx.z, tx.w);
                        GL11.glVertex3i(lx+ BLOCK_SIZE ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
                        tex(tx.x, tx.w);
                        GL11.glVertex3i(lx+ BLOCK_SIZE, ly,                 lz+ BLOCK_SIZE);
                    }

                    // X-
                    if(unpackVis(index, 1)) {
                        tex(tx.x, tx.y);
                        GL11.glVertex3i(lx, ly,                  lz );
                        tex(tx.x, tx.w);
                        GL11.glVertex3i(lx, ly,                 lz+ BLOCK_SIZE);
                        tex(tx.z, tx.w);
                        GL11.glVertex3i(lx ,ly+ BLOCK_SIZE ,    lz + BLOCK_SIZE);
                        tex(tx.z, tx.y);
                        GL11.glVertex3i(lx ,ly+ BLOCK_SIZE,     lz);
                    }
                }
            }
        }
        GL11.glEnd();
    }

    private void tex(float x, float y) {
        if(Ref.glRef.isShadersSupported())
            GL20.glVertexAttrib2f(2, x, y);
        else
            GL11.glTexCoord2f(x, y);
    }

    private void col(boolean derp) {
        if(Ref.glRef.isShadersSupported())
            GL20.glVertexAttrib4Nub(1, derp?(byte)125:(byte)255,(byte)255,(byte)255,(byte)255);
        else
            GL11.glColor4ub(derp?(byte)125:(byte)255,(byte)255,(byte)255,(byte)255);
    }
}
