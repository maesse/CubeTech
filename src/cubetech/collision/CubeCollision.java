package cubetech.collision;

import cubetech.misc.Plane;

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

    public Plane getHitPlane() {
        float dx = 0, dy = 0, dz = 0, d = 0;
        switch((int)Math.abs(hitAxis)) {
            case 1: // x
                dx = 1;
                d = chunk.p[0] * CubeChunk.SIZE * CubeChunk.BLOCK_SIZE + (x) * CubeChunk.BLOCK_SIZE;
                if(hitAxis < 0) {
                    dx *= -1f;
                    d *= -1f;
                } else {
                    d += CubeChunk.BLOCK_SIZE;
                }
                break;
            case 2: // y
                dy = 1;

                d = chunk.p[1] * CubeChunk.SIZE * CubeChunk.BLOCK_SIZE + (y) * CubeChunk.BLOCK_SIZE;
                if(hitAxis < 0)  {
                    dy *= -1f;
                    d *= -1f;
                } else {
                    d += CubeChunk.BLOCK_SIZE;
                }
                break;
            case 3: // z
                dz = 1;
                d = chunk.p[2] * CubeChunk.SIZE * CubeChunk.BLOCK_SIZE + (z) * CubeChunk.BLOCK_SIZE;
                if(hitAxis < 0)  {
                    dz *= -1f;
                    d *= -1f;
                } else {
                    d += CubeChunk.BLOCK_SIZE;
                }
                break;
        }
        

        Plane p = new Plane(dx, dy, dz, d);
        return p;
    }
}
