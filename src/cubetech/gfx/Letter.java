/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class Letter {
    public Vector2f Offset;
    public Vector2f Size;

    public Letter(Vector2f offset, Vector2f size) {
        this.Offset = offset;
        this.Size = size;
    }
}
