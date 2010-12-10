/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.input;

import org.lwjgl.util.vector.Vector2f;



/**
 *
 * @author mads
 */
public class MouseEvent  {
    public int Button;
    public boolean Pressed;

    public Vector2f Delta;
    public Vector2f Position;
    
    public MouseEvent(int button, boolean pressed, Vector2f delta, Vector2f pos) {
        this.Button = button;
        this.Pressed = pressed;
        this.Delta = delta;
        this.Position = pos;
    }
}
