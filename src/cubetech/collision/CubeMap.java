package cubetech.collision;

import cubetech.gfx.TerrainTextureCache;
import java.util.HashMap;

/**
 *
 * @author mads
 */
public class CubeMap {
    HashMap<Long, CubeChunk> chunks = new HashMap<Long, CubeChunk>();

    public CubeMap() {
        generateSimpleMap();
    }

    public void Render() {
        for (CubeChunk cubeChunk : chunks.values()) {
            cubeChunk.Render();
        }
    }

    private void generateSimpleMap() {
        // Create a new 8x8x2 chunk map
        int width = 4;
        int height = 4;
        int depth = 1;
        for (int z= 0; z < depth; z++) {
            for (int y= 0; y < height; y++) {
                for (int x= 0; x < width; x++) {
                    if(chunks.put(positionToLookup(x, y, z), generateChunk(x, y, z))!= null) {
                        throw new RuntimeException("generateSimpleMap: Chunk already existed.");
                    }
                }
            }
        }
    }

    private CubeChunk generateChunk(int x, int y, int z) {
        CubeChunk chunk = new CubeChunk(x, y, z, z == 0);
        return chunk;
    }

    public static Long positionToLookup(int x, int y, int z) {
        return bitTwiddle(x) | (bitTwiddle(y)<<17) | (bitTwiddle(z)<<34);
    }

    // Twiddle an int down to 17bit
    private static long bitTwiddle(int input) {
        int tx = input & 0xffff;
        if(input < 0)
            tx |= 0x10000;
        return tx;
    }
}
