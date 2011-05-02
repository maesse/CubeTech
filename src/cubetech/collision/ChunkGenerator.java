package cubetech.collision;

import cubetech.gfx.CubeType;
import java.util.Random;

/**
 *
 * @author mads
 */
public class ChunkGenerator implements IChunkGenerator {
    long seed = 0;
    Random rng = null;

    public void setSeed(long seed) {
        this.seed = seed;
        rng = new Random(seed);
    }

    public CubeChunk generateChunk(CubeMap map, int x, int y, int z) {
        CubeChunk chunk = new CubeChunk(map, x, y, z);
        if(z <= 0) {
            for (int i= 0; i < CubeChunk.CHUNK_SIZE; i++) {
                chunk.setCubeType(i, CubeType.GRASS);
            }
        }
        return chunk;
    }
    
}
