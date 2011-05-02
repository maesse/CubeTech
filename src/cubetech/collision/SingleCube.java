package cubetech.collision;

/**
 * Represents a single cube. Since cubes really consist of an chunk+3 bytes for position, this is easier to
 * pass around...
 * @author mads
 */
public class SingleCube {
    public CubeChunk chunk;
    public int x, y, z;

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
    }
}
