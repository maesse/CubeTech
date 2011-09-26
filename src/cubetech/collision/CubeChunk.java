package cubetech.collision;

import cern.colt.map.OpenLongObjectHashMap;
import cubetech.CGame.ChunkRender;
import cubetech.common.Common;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CubeChunk {
    public static final int SIZE = 32;
    public static final int CHUNK_SIZE = SIZE*SIZE*SIZE;
    public static final int BLOCK_SIZE = 32;
    public static final int NUM_VERSION = 32;
    public static final float RADIUS = BLOCK_SIZE * SIZE * 1.5f;
    public static final int PLANE_SIZE = 4*32; // (vertex: 3*4, color: 4*1, tex: 2*4) * 4 points

    // block data
    public byte[] blockType = new byte[CHUNK_SIZE];
    
    public int[] absmin = new int[3];
    public int[] absmax = new int[3];
    public float[] fcenter = new float[3];
    public OpenLongObjectHashMap chunks;
    public ChunkRender render;

    public int nCubes = 0;

    public int version = 0;
    int[] versionData = new int[32]; // remember the last 32 changes



    public int[] p = new int[3]; // Position/Origin. Grows in the positive direction.
    public CubeChunk(OpenLongObjectHashMap chunks, int x, int y, int z) {
        this.chunks = chunks;
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

    void destroy() {
        if(render != null) {
            render.destroy();
            render = null;
        }
        blockType = null;
        chunks = null;
    }

    public void setCubeType(int x, int y, int z, byte type, boolean notify) {
        int index = getIndex(x, y, z);
        if(blockType[index] == 0 && type != 0) nCubes++;
        else if(blockType[index] != 0 && type == 0) nCubes--;
        blockType[index] = type;

        if(render != null) {
            render.setDirty(true, false);
            if(!notify)
                return;
            // Check if we need to notify our neightboughr
            if(x == 0) render.notifyChange(0, false);
            if(x == SIZE-1) render.notifyChange(0, true);
            if(y == 0) render.notifyChange(1, false);
            if(y == SIZE-1) render.notifyChange(1, true);
            if(z == 0) render.notifyChange(2, false);
            if(z == SIZE-1) render.notifyChange(2, true);
        } else {

            versionData[version & (NUM_VERSION-1)] = packChange(x,y,z,type);
            version++;
        }
    }

    private static int packChange(int x, int y, int z, byte type) {
        // 5 bits for each coord, 1 byte for the type
        int res = x | (y << 5) | (z << 10) | ((type & 0xff) << 15);
        return res;
    }

    public static int[] unpackChange(int val) {
        int[] d = new int[] {
            val & 0x1f,
            (val >> 5) & 0x1f,
            (val >> 10) & 0x1f,
            (val >> 15) & 0xff
        };
        return d;
    }

    public byte getCubeType(int index) {
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
    
    public static int getIndex(int x, int y, int z) {
        return x + y * SIZE + z * SIZE * SIZE;
    }

    public ByteBuffer createByteBuffer() {
        return createByteBuffer(-1);
    }

    private static byte[] compressionBuffer1 = new byte[1 + 8 + SIZE * SIZE * SIZE];
    private static byte[] compressionBuffer2 = new byte[4 + 1 + 8 + SIZE * SIZE * SIZE];
    public ByteBuffer createByteBuffer(int start) {
        int size;

        if(start != -1 && (version - start <= 0 || version - start > 32)) {
            // Derp? fallback to full send
            start = -1;
        }
        if(start == -1) {
            // 1 control byte (start), 8 bytes for chunk lookup, size^3 bytes for chunk data.
            size = 1 + 8 + SIZE * SIZE * SIZE;
        } else {
            // 4bytes for each version entry, 2 control bytes (start/end) and 8 bytes for chunk lookup + 4 bytes version
            size = (version - start) * 4 + 2 + 8 + 4;
        }

        // Fill byte buffer
        byte[] data = compressionBuffer1;
        ByteBuffer buf = ByteBuffer.wrap(data, 0, size);
        buf.order(ByteOrder.nativeOrder());

        // Write control byte
        // 2 = full update, 1 = partial update
        buf.put(start==-1?(byte)2:(byte)1);

        // Write chunk lookup
        long lookup = CubeMap.positionToLookup(p[0], p[1], p[2]);
        buf.putLong(lookup);

        if(start==-1) {
            // write all cubes
            for (int i= 0; i < SIZE*SIZE*SIZE; i++) {
                buf.put(blockType[i]);
            }
        } else {
            buf.putInt(start);
            // write data count
            buf.put((byte)(version-start)); // end byte control
            // Write cube history
            for (int i= start; i < version; i++) {
                int index = i & (NUM_VERSION-1);
                buf.putInt(versionData[index]);
            }
        }

        // Compress data
        int compressionLevel = 4; // this seems like a good tradeoff
        byte[] dest = compressionBuffer2;
        int wrote = compressData(data, data.length, dest, compressionLevel, true);
//        Common.LogDebug("Wrote %db cubedata to client (p:%d,%d,%d)", wrote,p[0],p[1],p[2]);

        byte[] finaldest = new byte[wrote];
        System.arraycopy(compressionBuffer2, 0, finaldest, 0, wrote);
        buf = ByteBuffer.wrap(finaldest);
        buf.order(ByteOrder.nativeOrder());
        // Write lenght
        buf.position(0);
        buf.putInt(wrote-4);
        buf.position(0);
        return buf;
    }

    private void testCompression(byte[] data, byte[] dest) {
        for (int i= 0; i < 10; i++) {
            Common.LogDebug("level %d compression: %db", i, compressData(data, data.length, dest, i, true));
        }
    }

    public static int uncompressData(byte[] src, int offset, int lenght, byte[] dst) throws DataFormatException {
        Inflater unzip = new Inflater();
        unzip.setInput(src, offset, lenght);
        int len = unzip.inflate(dst);
        unzip.end();
        return len;
    }

    public static int compressData(byte[] src, int srclen, byte[] dst, int level, boolean addHeaderSpace) {
        Deflater zip = new Deflater(level);
        zip.setInput(src,0,srclen);
        zip.finish();
        int header = 0;
        if(addHeaderSpace) header = 4; // 4 bytes
        int len = zip.deflate(dst, header, dst.length-header);
        return len + header;
    }



    
}
