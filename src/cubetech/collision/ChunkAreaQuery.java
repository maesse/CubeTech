package cubetech.collision;

/**
 *
 * @author mads
 */
public class ChunkAreaQuery {
    // in/out
    private ChunkSpatialPart[] parts = null;
    private int index = 0;
    
    // out
    private int nCubes = 0;
    private int nRead = 0;
    private int chunkRead = 0;

    public ChunkAreaQuery(int initialSize) {
        parts = new ChunkSpatialPart[initialSize];
    }

    public boolean isEmpty() {
        return index == 0;
    }

    /**
     * Gets the next cube found in the area query.
     * @param dst the 3 integer array the cube position will be written to
     * @return returns null on EOF, else returns the
     */
    public int[] getNext(int[] dst) {
        if(dst == null)
            return dst;

        ChunkSpatialPart part = parts[chunkRead];

        if(part == null)
            return null; // Happens when nothing was contained in the area
        
        while(nRead >= part.nIndex) { // Passed end of chunk
            // Check if this was the last chunk
            if(chunkRead+1 >= index) {
                return null;
            }

            // Read next chunk
            nRead = 0;
            part = parts[++chunkRead];
        }

        // Get cube index from the chunk
        System.arraycopy(part.indexes, nRead*3, dst, 0, 3);

//        // Calculate world abs min coords
//        // Start off with the chunks absmin
//        System.arraycopy(part.chunk.absmin, 0, dst, 0, 3);
//
//        // Unpack cube position
//        int z = cubeIndex / (CubeChunk.SIZE*CubeChunk.SIZE);
//        cubeIndex -= z * CubeChunk.SIZE*CubeChunk.SIZE;
//        int y = cubeIndex / CubeChunk.SIZE;
//        cubeIndex -= y * CubeChunk.SIZE;
//        int x = cubeIndex;
//
//        // Add in local cubeposition
//        dst[0] += x * CubeChunk.BLOCK_SIZE;
//        dst[1] += y * CubeChunk.BLOCK_SIZE;
//        dst[2] += z * CubeChunk.BLOCK_SIZE;

        nRead++;
        return dst;
    }

    public void addPart(ChunkSpatialPart part) {
        parts[index++] = part;
        nCubes += part.nIndex;
    }

    int getCount() {
        return nCubes;
    }
}
