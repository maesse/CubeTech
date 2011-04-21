package cubetech.gfx;

/**
 *
 * @author mads
 */
public class CubeType {
    public static final byte EMPTY = 0;
    // Negative numbers indicate a cube with a texture for top, bottom and sides
    public static final byte GRASS = -1;
    // same texture on all sides
    public static final byte DIRT = 1;
    public static final byte SAND = 2;
}
