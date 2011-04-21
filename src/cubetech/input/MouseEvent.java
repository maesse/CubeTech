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
    public int WheelDelta;

    public int[] Delta;
    public Vector2f Position;
    public MouseEvent(int button, boolean pressed, int wheelDelta, int[] delta, Vector2f pos) {
        this.Button = button;
        this.Pressed = pressed;
        this.Delta = delta;
        this.Position = pos;
        this.WheelDelta = wheelDelta;
    }
    
    public MouseEvent(int button, boolean pressed, int wheelDelta, int dx, int dy, Vector2f pos) {
        this.Button = button;
        this.Pressed = pressed;
        this.Delta = new int[] {dx, dy};
        this.Position = pos;
        this.WheelDelta = wheelDelta;
    }
}
