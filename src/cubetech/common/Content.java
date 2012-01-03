
package cubetech.common;

/**
 *
 * @author mads
 */
public  class Content {
    public static final int SOLID = 1;
    public static final int BODY = 2;
    public static final int TRIGGER = 4;
    public static final int PLAYERCLIP = 8;
    public static final int CORPSE = 16;
    public static final int MASK_PLAYERSOLID = SOLID | BODY | PLAYERCLIP;
    public static final int MASK_SHOT = SOLID | BODY | PLAYERCLIP;
    public static final int PHYSICS = 32;
    
}
