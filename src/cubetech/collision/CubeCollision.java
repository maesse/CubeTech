package cubetech.collision;

/**
 *
 * @author mads
 */
public class CubeCollision {
    // chunk the collision is contained in:
    public CubeChunk chunk;
    // cube local to chunk:
    public int x, y, z;
    public int hitAxis;

    public CubeCollision(CubeChunk chunke, int x, int y, int z, int hitAxis) {
        this.chunk = chunke;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hitAxis = hitAxis;
    }
}
