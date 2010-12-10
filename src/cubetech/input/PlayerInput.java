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
public class PlayerInput {
    public Vector2f MousePos = new Vector2f(0.5f,0.5f);
    public Vector2f MouseDelta = new Vector2f();
    public int WheelDelta;
    public boolean Mouse1;
    public boolean Mouse1Diff;
    public boolean Mouse2;
    public boolean Mouse2Diff;
    public boolean Mouse3;
    public boolean Mouse3Diff;
    public boolean Up;
    public boolean Down;
    public boolean Left;
    public boolean Right;
    public boolean Jump;
}
