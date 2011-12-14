package cubetech.CGame;

import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import cubetech.common.Helper;
import cubetech.gfx.TerrainTextureCache;
import cubetech.misc.Ref;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CubeChunkDataWorker extends Thread {
    private static int BLOCK_SIZE = CubeChunk.BLOCK_SIZE;
    CubeChunkDataBuilder builder;
    boolean working = false;
    boolean exit = false;

    // Current working set:
    CubeChunkDataBuilder.WorkerJob job = null;
    CubeChunk chunk = null;
    int sidesRendered;

    // temp fields
    private int[] chunkDelta = new int[3];
    private int[] org = new int[3];
    
    Vector2f[] encodedNormals = new Vector2f[6];
    

    public CubeChunkDataWorker(CubeChunkDataBuilder builder) {
        this.builder = builder;
        
        for (int i = 0; i < encodedNormals.length; i++) {
            encodedNormals[i] = new Vector2f();
            int axis = i / 2;
            boolean neg = i % 2 == 1;
            Vector3f normal = new Vector3f();
            Helper.VectorSet(normal, axis, 1f);
            if(neg) normal.scale(-1f);
            encodeNormalSphereMap(normal, encodedNormals[i]);
            
        }
        
    }
    
    // Using SphereMap Transform
    private void encodeNormalSphereMap(Vector3f normal, Vector2f dest) {
        float d = (float)Math.sqrt(normal.z * 8f + 8f);
        dest.set(normal.x/d + 0.5f, normal.y/d + 0.5f);
    }
    
    // Optimized for viewspace
    private void encodeNormal(Vector3f normal, Vector2f dest) {
        dest.x = normal.x;
        dest.y = normal.y;
        Helper.Normalize(dest);
        dest.scale((float)Math.sqrt(-normal.z*0.5f + 0.5f));
        dest.scale(0.5f);
        dest.x += 0.5f;
        dest.y += 0.5f;
    }

    public void initWorker(CubeChunk chunk) {
        this.chunk = chunk;
        sidesRendered = 0;
    }

    @Override
    public void run() {
        while(!exit) {
            if(builder.hasWork()) {
                job = builder.getWork();
                chunk = job.chunk;
                chunk.render.state = ChunkRender.State.BUILDING;
                sidesRendered = 0;
                runOnce();
                job.visibleSides = sidesRendered;
                builder.finishedWork(job);
            } else {
                // No work left, chill out
                try {
                    synchronized(this) {
                        wait();
                    }
                } catch (InterruptedException ex) {
                    // Got work? :D
                }
            }
        }
        
    }

    private void runOnce() {
        working = true;
        // Wait for a free buffer
        while(!exit && !builder.bufferAvailable()) {
            try {
                Thread.sleep(100);
//                Common.LogDebug("[Builder] Waiting for buffer...");
            } catch (InterruptedException ex) {
            }
        }

        if(exit) return; // woken up by exit request

        // Grab a buffer
        long chunkid = CubeMap.positionToLookup(chunk.p[0], chunk.p[1], chunk.p[2]);
        ByteBuffer data = builder.grabBuffer(chunkid);
        if(data == null) { // shouldn't happen
            Common.Log("[Builder] Couldn't get a buffer");
            working = false;
            return;
        }

        try { // Create vertex data
            fillBuffer(data);
        } catch(BufferOverflowException ex) {
            Ref.common.Error(ErrorCode.FATAL,"CubeChunk.fillBuffer: VBO overflow" + Common.getExceptionString(ex));
        }

        // Check buffer
        data.flip();
        if(data.limit() != CubeChunk.PLANE_SIZE * sidesRendered) {
            Ref.common.Error(ErrorCode.FATAL, String.format(
                    "[Builder] Buffer.limit (%d) is unexpected. Should be (%d)"
                    , data.limit(), CubeChunk.PLANE_SIZE * sidesRendered));
        }

//        if(sidesRendered == 0) {
//            builder.releaseBuffer(chunkid);
//        } else
        {
            builder.workerFinish(chunkid);
        }
        working = false;
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
    
    private void writeNormal(int index, ByteBuffer buf) {
        buf.putFloat(encodedNormals[index].x);
        buf.putFloat(encodedNormals[index].y);
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
        int CHUNK_SIDE = CubeChunk.SIZE * CubeChunk.BLOCK_SIZE;
        int[] p = chunk.p;
        int ppx = p[0] * CHUNK_SIDE;
        int ppy = p[1] * CHUNK_SIDE;
        int ppz = p[2] * CHUNK_SIDE;
        int tempSidesRendered = 0;
        int[] pos = new int[3];

        for (int z= 0; z < CubeChunk.SIZE; z++) {
            for (int y= 0; y < CubeChunk.SIZE; y++) {
                for (int x= 0; x < CubeChunk.SIZE; x++) {
                    int index = CubeChunk.getIndex(x, y, z);

                    // Check VIS
                    if(chunk.blockType[index] == 0)
                        continue;

                    // Get absolute coords
                    int lx = ppx+ x * CubeChunk.BLOCK_SIZE;
                    int ly = ppy+ y * CubeChunk.BLOCK_SIZE;
                    int lz = ppz+ z * CubeChunk.BLOCK_SIZE;

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
                    if(derp2(pos, ao(0,0,1))) {
                        color = Ref.cgame.map.lightSides[4];
                        ao1 = derp(pos, ao(-1,0,1));
                        ao2 = derp(pos, ao(0,-1,1));
                        ao3 = derp(pos, ao(-1,-1,1));
                        buffer.putFloat(lx).putFloat(ly).putFloat(lz+BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        writeNormal(4, buffer);

                        ao1 = derp(pos, ao(1,0,1));
                        ao2 = derp(pos, ao(0,-1,1));
                        ao3 = derp(pos, ao(1,-1,1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(             lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        writeNormal(4, buffer);

                        ao1 = derp(pos, ao(1,0,1));
                        ao2 = derp(pos, ao(0,1,1));
                        ao3 = derp(pos, ao(1,1,1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        writeNormal(4, buffer);

                        ao1 = derp(pos, ao(-1,0,1));
                        ao2 = derp(pos, ao(0,1,1));
                        ao3 = derp(pos, ao(-1,1,1));
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        writeNormal(4, buffer);

                        tempSidesRendered++;
                    }

                    // Bottom: Z-
                    if(derp2(pos, ao(0,0,-1))) {
                    //if(visible[index*6+5]) {
                        color = Ref.cgame.map.lightSides[5];
                        if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.BOTTOM);
                        ao1 = derp(pos, ao(-1,0,-1));
                        ao2 = derp(pos, ao(0,-1,-1));
                        ao3 = derp(pos, ao(-1,-1,-1));
                        buffer.putFloat(lx).putFloat(             ly).putFloat(                 lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        writeNormal(5, buffer);

                        ao1 = derp(pos, ao(-1,0,-1));
                        ao2 = derp(pos, ao(0,1,-1));
                        ao3 = derp(pos, ao(-1,1,-1));
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(    lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        writeNormal(5, buffer);


                        ao1 = derp(pos, ao(1,0,-1));
                        ao2 = derp(pos, ao(0,1,-1));
                        ao3 = derp(pos, ao(1,1,-1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(    lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        writeNormal(5, buffer);

                        ao1 = derp(pos, ao(1,0,-1));
                        ao2 = derp(pos, ao(0,-1,-1));
                        ao3 = derp(pos, ao(1,-1,-1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(                 lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        writeNormal(5, buffer);
                        tempSidesRendered++;
                    }

                    // Y+
                    if(multiTex) tx = TerrainTextureCache.getSide(type, TerrainTextureCache.Side.SIDE);
                    if(derp2(pos, ao(0,1,0))) {
                    //if(visible[index*6+2]) {
                        color = Ref.cgame.map.lightSides[2];
                        ao1 = derp(pos, ao(-1,1,0));
                        ao2 = derp(pos, ao(0,1,-1));
                        ao3 = derp(pos, ao(-1,1,-1));
                        buffer.putFloat(lx).putFloat(             ly+ BLOCK_SIZE).putFloat(     lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        writeNormal(2, buffer);

                        ao1 = derp(pos, ao(-1,1,0));
                        ao2 = derp(pos, ao(0,1,1));
                        ao3 = derp(pos, ao(-1,1,1));
                        buffer.putFloat(lx).putFloat(             ly + BLOCK_SIZE).putFloat(    lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        writeNormal(2, buffer);

                        ao1 = derp(pos, ao(1,1,0));
                        ao2 = derp(pos, ao(0,1,1));
                        ao3 = derp(pos, ao(1,1,1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly + BLOCK_SIZE).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        writeNormal(2, buffer);

                        ao1 = derp(pos, ao(1,1,0));
                        ao2 = derp(pos, ao(0,1,-1));
                        ao3 = derp(pos, ao(1,1,-1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        writeNormal(2, buffer);
                        tempSidesRendered++;
                    }

                    // Y-
                    if(derp2(pos, ao(0,-1,0))) {
                    //if(visible[index*6+3]) {
                        color = Ref.cgame.map.lightSides[3];
                        ao1 = derp(pos, ao(-1,-1,0));
                        ao2 = derp(pos, ao(0,-1,-1));
                        ao3 = derp(pos, ao(-1,-1,-1));
                        //ao1 = ao2 = ao3 = true;
                        buffer.putFloat(lx).putFloat(             ly).putFloat(     lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        writeNormal(3, buffer);


                        ao1 = derp(pos, ao(1,-1,0));
                        ao2 = derp(pos, ao(0,-1,-1));
                        ao3 = derp(pos, ao(1,-1,-1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        writeNormal(3, buffer);

                        ao1 = derp(pos, ao(1,-1,0));
                        ao2 = derp(pos, ao(0,-1,1));
                        ao3 = derp(pos, ao(1,-1,1));
                        buffer.putFloat(lx + BLOCK_SIZE).putFloat(ly ).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        writeNormal(3, buffer);

                        ao1 = derp(pos, ao(-1,-1,0));
                        ao2 = derp(pos, ao(0,-1,1));
                        ao3 = derp(pos, ao(-1,-1,1));
                        buffer.putFloat(lx).putFloat(             ly).putFloat(    lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        writeNormal(3, buffer);
                        tempSidesRendered++;
                    }

                    // X+
                    if(derp2(pos, ao(1,0,0))) {
                    //if(visible[index*6]) {
                        color = Ref.cgame.map.lightSides[0];
                        ao1 = derp(pos, ao(1,-1,0));
                        ao2 = derp(pos, ao(1,0,-1));
                        ao3 = derp(pos, ao(1,-1,-1));
                        buffer.putFloat(lx+ BLOCK_SIZE).putFloat( ly).putFloat(                  lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        writeNormal(0, buffer);

                        ao1 = derp(pos, ao(1,1,0));
                        ao2 = derp(pos, ao(1,0,-1));
                        ao3 = derp(pos, ao(1,1,-1));
                        buffer.putFloat(lx+ BLOCK_SIZE ).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        writeNormal(0, buffer);

                        ao1 = derp(pos, ao(1,1,0));
                        ao2 = derp(pos, ao(1,0,1));
                        ao3 = derp(pos, ao(1,1,1));
                        buffer.putFloat(lx+ BLOCK_SIZE ).putFloat(ly+ BLOCK_SIZE ).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        writeNormal(0, buffer);

                        ao1 = derp(pos, ao(1,-1,0));
                        ao2 = derp(pos, ao(1,0,1));
                        ao3 = derp(pos, ao(1,-1,1));
                        buffer.putFloat(lx+ BLOCK_SIZE).putFloat( ly).putFloat(                 lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        writeNormal(0, buffer);
                        tempSidesRendered++;
                    }

                    // X-
                    if(derp2(pos, ao(-1,0,0))) {
                    //if(visible[index*6+1]) {
                        color = Ref.cgame.map.lightSides[1];
                        ao1 = derp(pos, ao(-1,-1,0));
                        ao2 = derp(pos, ao(-1,0,-1));
                        ao3 = derp(pos, ao(-1,-1,-1));
                        buffer.putFloat(lx).putFloat( ly).putFloat(                  lz );
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.y);
                        writeNormal(1, buffer);

                        ao1 = derp(pos, ao(-1,-1,0));
                        ao2 = derp(pos, ao(-1,0,1));
                        ao3 = derp(pos, ao(-1,-1,1));
                        buffer.putFloat(lx).putFloat( ly).putFloat(                 lz+ BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.x).putFloat(tx.w);
                        writeNormal(1, buffer);

                        ao1 = derp(pos, ao(-1,1,0));
                        ao2 = derp(pos, ao(-1,0,1));
                        ao3 = derp(pos, ao(-1,1,1));
                        buffer.putFloat(lx ).putFloat(ly+ BLOCK_SIZE ).putFloat(    lz + BLOCK_SIZE);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.w);
                        writeNormal(1, buffer);

                        ao1 = derp(pos, ao(-1,1,0));
                        ao2 = derp(pos, ao(-1,0,-1));
                        ao3 = derp(pos, ao(-1,1,-1));
                        buffer.putFloat(lx ).putFloat(ly+ BLOCK_SIZE).putFloat(     lz);
                        writeColorAndAO(color, ao1, ao2, ao3, buffer);
                        buffer.putFloat(tx.z).putFloat(tx.y);
                        writeNormal(1, buffer);
                        tempSidesRendered++;
                    }
                }
            }
        }
        sidesRendered = tempSidesRendered;
    }

    private boolean derp2(int[] p,int[] a) {
        return derp3(p, a, false);
    }
    private boolean derp(int[] p,int[] a) {
        return derp3(p, a, true);
    }

    private boolean derp3(int[] p,int[] a, boolean onNoChunk) {
        CubeChunk baseChunk = chunk;
        boolean gotChunkDelta = false;

        for (int i= 0; i < 3; i++) {
            if(p[i]+a[i] < 0) {
                chunkDelta[i] = -1;
                gotChunkDelta = true;
            }
            else if(p[i]+a[i] >= CubeChunk.SIZE) {
                chunkDelta[i] = 1;
                gotChunkDelta = true;
            } else {
                chunkDelta[i] = 0;
            }
            org[i] = (p[i] + a[i]) & 31;
        }

        if(gotChunkDelta) {
            // todo: pass neighbour chunks to worker, because this may crash
            baseChunk = getChunkInDirection(chunkDelta);
            if(baseChunk == null) return onNoChunk;
        }

        return baseChunk.blockType[org[0] | org[1] << 5 | org[2] << 10] == 0;
    }

    private CubeChunk getChunkInDirection(int[] dir) {
        int index = (dir[2]+1) * 3 * 3 + (dir[1]+1) * 3 + (dir[0]+1);
        return job.neighbors[index];
    }

}
