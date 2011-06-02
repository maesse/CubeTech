package cubetech.collision;

import cubetech.gfx.CubeType;
import cubetech.misc.Ref;

/**
 * Represents a single cube. Since cubes really consist of an chunk+3 bytes for position, this is easier to
 * pass around...
 * @author mads
 */
public class SingleCube {
    public CubeChunk chunk;
    public int x, y, z;

    public int highlightSide = 0;

    public SingleCube(CubeChunk chunk, int x, int y, int z) {
        this.chunk = chunk;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SingleCube(CubeCollision col) {
        chunk = col.chunk;
        x = col.x;
        y = col.y;
        z = col.z;
        highlightSide = col.hitAxis;
    }

    /**
     * Changes the cubetype in the chunk to the given value.
     * Can also be 0/CubeType.EMPTY..
     * @param type
     */
    public void putBlock(byte type) {
        if(chunk != null)
            chunk.setCubeType(x, y, z, type, true);
    }

    /**
     * Removes the block from the chunk (makes it invisible/non-collidable)
     */
    public void removeBlock() {
        chunk.setCubeType(x,y,z, CubeType.EMPTY, true);
    }

    /**
     * Creates a new SingleCube by moving one step in the direction
     * specified by the side parameter
     * @param side x = 1, y = 2, z = 3 - multiply by -1 for negative direction.
     * @return
     */
    public SingleCube getHightlightside(int side) {
        // FIgure out coords for new cube
        int[] p = new int[] {x, y, z};
        int sign = (int)Math.signum(side);
        int index = (int)Math.abs(side);
        if(index <= 0 || index > 3)
            return null;

        // move point in the given direction
        p[index-1] += sign;
        CubeChunk nChunk = chunk;

        // Check if new cube is in another chunk
        if(p[index-1] < 0 || p[index-1] >= CubeChunk.SIZE) {
            // Copy original chunk position
            int[] chunkPos = new int[3];
            System.arraycopy(chunk.p, 0, chunkPos, 0, 3);

             // Use index & sign to get the right chunk
            chunkPos[index-1] += sign;
            
            // retrieve the chunk
            nChunk = CubeMap.getChunk(chunkPos[0], chunkPos[1], chunkPos[2], false, Ref.cgame.map.chunks);

            // correct the cube position for the new chunk
            p[index-1] = p[index-1] & (CubeChunk.SIZE-1); 
        }

        // return a singlecube describing the position of the new cube
        return new SingleCube(nChunk, p[0], p[1], p[2]);
    }

    public SingleCube getHightlightside() {
        return getHightlightside(highlightSide);
    }
}
