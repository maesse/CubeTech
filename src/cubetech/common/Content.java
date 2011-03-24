/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
    public static final int MASK_PLAYERSOLID = SOLID | BODY | PLAYERCLIP;
}
