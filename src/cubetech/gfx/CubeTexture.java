/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;
import static org.lwjgl.opengl.GL11.*;
/**
 *
 * @author mads
 */
public class CubeTexture {
    public int Width;
    public int Height;

    // GL
    private int TextureID;
    private int Target;

    public CubeTexture(int target, int id) {
        this.TextureID = id;
        this.Target = target;
    }

    public int GetID() {
        return TextureID;
    }

    public void Bind() {
        glBindTexture(Target, TextureID);
    }
}
