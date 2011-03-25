/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;
import org.lwjgl.opengl.GL20;
import cubetech.misc.Ref;
import org.lwjgl.opengl.GL13;
import static org.lwjgl.opengl.GL11.*;
/**
 *
 * @author mads
 */
public class CubeTexture {
    public int Width;
    public int Height;
    public String name;
    public boolean loaded;

    // GL
    private int TextureID;
    private int Target;
    

    public CubeTexture(int target, int id, String name) {
        this.TextureID = id;
        this.Target = target;
        this.name = name;
    }

    public int GetID() {
        return TextureID;
    }

    public boolean needSort() {
        return true;
    }

    public void Bind() {
        if(loaded) {
//            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            glBindTexture(Target, TextureID);
            GLRef.checkError();
//            GL20.glUniform1i(Ref.glRef.shader.GetTextureIndex(), TextureID);
        }
    }

    public static void Unbind() {
//        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // should only be used by the ressource subsystem
    public void SetID(int id) {
        TextureID = id;
    }
}
