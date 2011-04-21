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
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CubeChunk {
    public static final int SIZE = 8;
    public static final int CHUNK_SIZE = SIZE*SIZE*SIZE;
    public static final int BLOCK_SIZE = 32;
    private static final int PLANE_SIZE = 4*32; // (vertex: 3*4, color: 4*1, tex: 2*4) * 4 points

    private boolean[] visBlock = new boolean[CHUNK_SIZE]; // pre-vis data
    private short[] visBlock2 = new short[CHUNK_SIZE];
    private boolean[] visible = new boolean[CHUNK_SIZE*6];
    private int nVis = 0;
    private byte[] blockType = new byte[CHUNK_SIZE];
    private VBO vbo = null;
    private boolean dirty = true;

    int px, py, pz; // Position/Origin. Grows in the positive direction.
    public CubeChunk(int x, int y, int z, boolean fill) {
        px = x;
        py = y;
        pz = z;
        if(fill) {
            for (int i= 0; i < CHUNK_SIZE; i++) {
                if(Ref.rnd.nextBoolean() || Ref.rnd.nextBoolean() || Ref.rnd.nextBoolean())
                    continue;
                visBlock[i] = true;
                blockType[i] = CubeType.GRASS;
            }
        }
    }

    public void Render() {
        //RenderSimple();
        markVisible();
        
        renderVBO();
    }

    private static final int MAX_VIS_RELATIONS = 128;
    boolean[] visRelations = new boolean[MAX_VIS_RELATIONS];
    int visIndex = 1;
    private void preVIS() {
        // Clear pre-vis data
        for (int i= 0; i < visBlock.length; i++) {
            visBlock[i] = false;
        }

        rels.clear();
        
        visIndex = 1;
        for (int z= 0; z < SIZE; z++) {
            for (int y= 0; y < SIZE; y++) {
                for (int x= 0; x < SIZE; x++) {
                    int lookup = getIndex(x, y, z);
                    if(blockType[lookup] != 0) {
                        visBlock[lookup] = true;
                        visBlock2[lookup] = -1;
                        continue;
                    }

                    boolean boundary = (x == 0 || y == 0 || z == 0);
                    if(boundary) {
                        // Set 0, don't check
                        visBlock2[lookup] = 0;
                        continue;
                    }

                    boolean endBoundary = x == SIZE-1 || y == SIZE-1 || z == SIZE-1;
                    
                    // X-
                    short left = -1;
                    if(x>0 && !visBlock[lookup-1]) {
                        left = visBlock2[lookup-1];
                    }
                    short down = -1;
                    if(y>0 && !visBlock[lookup-SIZE]) {
                        down = visBlock2[lookup-SIZE];
                    }
                    short back = -1;
                    if(z>0 && !visBlock[lookup-SIZE*SIZE]) {
                        back = visBlock2[lookup-SIZE*SIZE];
                    }

                    int nSolid = back == -1?1:0;
                    nSolid += down == -1?1:0;
                    nSolid += left == -1?1:0;
                    if(nSolid == 3) {
                        if(endBoundary) {
                            visBlock2[lookup] = 0;
                            continue;
                        }
                        // Unknown area!
                        visBlock2[lookup] = (short) visIndex++;
                        continue;
                    }

                    if(nSolid == 2) {
                        if(back != -1) {
                            visBlock2[lookup] = visBlock2[lookup-SIZE*SIZE];
                        } else if(down != -1) {
                            visBlock2[lookup] = visBlock2[lookup-SIZE];
                        } else if(left != -1) {
                            visBlock2[lookup] = visBlock2[lookup-1];
                        }
                        if(endBoundary) {
                            // Save 0->vis
                            rel(0, visBlock2[lookup]);
                        }
                        continue;
                    }

                    if(nSolid == 0 && left == back && left == down) {
                        // All side are the same type
                        visBlock2[lookup] = left;
                        if(endBoundary) {
                            rel(0,left);
                        }
                        continue;
                    }

                    if(nSolid == 0) {
                        // 3 sides, different
                        int max = back > left ? back : left;
                        max = down > max? down:max;
                        int min = left < down? left:down;
                        min = back < min ? back : min;

                        boolean third = back != left && back != down && left != down;
                        visBlock2[lookup] =(short) min;

                        // Save min->max
                        rel(min, max);
                        if(endBoundary) {
                            rel(0,min);
                        }

                        if(third) {
                            int thirdRel = -1;
                            if(back > min && back < max) thirdRel = back;// save min -> back
                            else if(left > min && left < max) thirdRel = left;// save min -> left
                            else if(down > min && down < max) thirdRel = down;// save min -< down
                            rel(min, thirdRel);
                        }
                    } else if(nSolid == 1) {
                        // 2 sides
                        int max = back > left ? back : left;
                        max = down > max? down:max;
                        int min;
                        if(back == -1) {
                            min = left < down? left: down;
                        } else if(left == -1) {
                            min = back < down? back:down;
                        } else {
                            min = back < left? back:left;
                        }

                        if(min == max)
                        {
                            // Theyre the same
                            visBlock2[lookup] =(short) min;
                            if(endBoundary) {
                                rel(0, min);
                            }
                            continue;
                        }

                        visBlock2[lookup] =(short) min;
                        
                        // Save min -> max lookup
                        rel(min, max);
                        if(endBoundary) {
                            rel(0,min);
                        }
                    }
                }
            }
        }
        visRelations = new boolean[visIndex];


    }
    private class RelItem implements Comparable<RelItem> {
        int min, max;
        public RelItem(int min, int max) {
            this.min = min; this.max = max;
        }

        public int compareTo(RelItem o) {
            if(min == o.min) return 0;
            if(min < o.min) return -1;
            return 1;
        }
    }
    ArrayList<RelItem> rels = new ArrayList<RelItem>();
    private void rel(int min, int max) {
        if(max == -1) {
            int test = 2;
        }
        rels.add(new RelItem(min, max));
    }

    private void postVIS() {
        Collections.sort(rels);
        int culledRels = 0;
        for (RelItem relItem : rels) {
            int min = relItem.min;
            if(min != 0 && !visRelations[min]) {
                culledRels++;
                continue; // Got something to cull!
            }

            visRelations[relItem.max] = true;
        }

        int culledBlocks = 0;
        for (int i= 0; i < SIZE*SIZE*SIZE; i++) {
            if(visBlock2[i] > 0 && !visRelations[visBlock2[i]]) {
                culledBlocks++;
                visBlock[i] = true;
            }
        }
        Common.Log(""+culledBlocks);
    }

    private void markVisible() {
        if(!dirty)
            return;

        preVIS();
        postVIS();
        nVis = 0;
        for (int z= 0; z < SIZE; z++) {
            for (int y= 0; y < SIZE; y++) {
                for (int x= 0; x < SIZE; x++) {
                    int lookup = getIndex(x, y, z);
                    if(blockType[lookup] == 0) {
                        continue;
                    }

                    // Check all 6 sides
                    visible[lookup*6] = (x == SIZE-1 || !visBlock[lookup+1]) && (nVis++ >= 0);
                    visible[lookup*6+1] = (x == 0 || !visBlock[lookup-1]) && (nVis++ >= 0);
                    visible[lookup*6+2] = (y == SIZE-1 || !visBlock[lookup+SIZE]) && (nVis++ >= 0);
                    visible[lookup*6+3] = (y == 0 || !visBlock[lookup-SIZE]) && (nVis++ >= 0);
                    visible[lookup*6+4] = (z == SIZE-1 || !visBlock[lookup+SIZE*SIZE]) && (nVis++ >= 0);
                    visible[lookup*6+5] = (z == 0 || !visBlock[lookup-SIZE*SIZE]) && (nVis++ >= 0);
                }
            }
        }
        updateVBO();
        dirty = false;
    }
    
    private int getIndex(int x, int y, int z) {
        return x + y * SIZE + z * SIZE * SIZE;
    }

    private void updateVBO() {
        if(vbo == null)
            vbo = new VBO(PLANE_SIZE * SIZE*SIZE*SIZE*6, BufferTarget.Vertex);

        ByteBuffer buffer = vbo.map();

        try {
            fillBuffer(buffer);
        } catch(BufferOverflowException ex) {
            Ref.common.Error(ErrorCode.FATAL,"CubeChunk.fillBuffer: VBO overflow");

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

    private void fillBuffer(ByteBuffer buffer) {
        int CHUNK_SIDE = SIZE * BLOCK_SIZE;
        int ppx = px * CHUNK_SIDE;
        int ppy = py * CHUNK_SIDE;
        int ppz = pz * CHUNK_SIDE;

        Color white = (Color) Color.WHITE;

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
                    if(visible[index*6+4]) {

                        buffer.putFloat(lx).putFloat(ly).putFloat(lz+BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(             lz+ BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);

                        
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(lz+ BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);
                        
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(lz+ BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);
                    }

                    // Bottom: Z-
                    if(visible[index*6+5]) {
                        if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.BOTTOM);

                        
                        buffer.putFloat(lx).putFloat(             ly).putFloat(                 lz );
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);
                        
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(    lz);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);
                        
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(    lz );
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);
                        
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(                 lz);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                    }

                    // Y+
                    if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.SIDE);
                    if(visible[index*6+2]) {
                        
                        buffer.putFloat(lx).putFloat(             ly+ BLOCK_SIZE).putFloat(     lz );
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);
                        
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(    lz+ BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);
                        
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(    lz + BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);
                        
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                    }

                    // Y-
                    if(visible[index*6+3]) {
                        
                        buffer.putFloat(lx).putFloat(             ly).putFloat(     lz );
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);

                        
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(     lz);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                        
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly ).putFloat(    lz + BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);
                        
                        buffer.putFloat(lx).putFloat(             ly).putFloat(    lz+ BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);
                    }

                    // X+
                    if(visible[index*6]) {
                        
                        buffer.putFloat(lx+ BLOCK_SIZE).putFloat( ly).putFloat(                  lz );
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);
                        
                        buffer.putFloat(lx+ BLOCK_SIZE ).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                        
                        buffer.putFloat(lx+ BLOCK_SIZE ).putFloat(ly+ BLOCK_SIZE ).putFloat(    lz + BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);
                        
                        buffer.putFloat(lx+ BLOCK_SIZE).putFloat( ly).putFloat(                 lz+ BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);
                    }

                    // X-
                    if(visible[index*6+1]) {
                        
                        buffer.putFloat(lx).putFloat( ly).putFloat(                  lz );
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        padd(buffer);
                        
                        buffer.putFloat(lx).putFloat( ly).putFloat(                 lz+ BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        padd(buffer);
                        
                        buffer.putFloat(lx ).putFloat(ly+ BLOCK_SIZE ).putFloat(    lz + BLOCK_SIZE);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        padd(buffer);
                        
                        buffer.putFloat(lx ).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        white.writeRGBA(buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        padd(buffer);
                    }
                }
            }
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
        GL11.glDrawArrays(GL11.GL_QUADS, 0, nVis*4);
        //GL12.glDrawRangeElements(GL11.GL_QUADS, callStart, callEnd-1, callLenght, GL11.GL_UNSIGNED_INT, offset);
        GL11.glEnable(GL11.GL_BLEND);
        postVbo();
        vbo.unbind();
    }

    private void RenderSimple() {
        markVisible();
        
        CubeTexture tex = Ref.ResMan.LoadTexture("data/terrain.png");
        tex.setFiltering(false, GL11.GL_NEAREST);
        tex.setWrap(GL12.GL_CLAMP_TO_EDGE);
        
        GL11.glBegin(GL11.GL_QUADS);

        int CHUNK_SIDE = SIZE * BLOCK_SIZE;
        int ppx = px * CHUNK_SIDE;
        int ppy = py * CHUNK_SIDE;
        int ppz = pz * CHUNK_SIDE;

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
                    if(visible[index*6+4]) {
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
                    if(visible[index*6+5]) {
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
                    if(visible[index*6+2]) {
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
                    if(visible[index*6+3]) {
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
                    if(visible[index*6]) {
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
                    if(visible[index*6+1]) {
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
