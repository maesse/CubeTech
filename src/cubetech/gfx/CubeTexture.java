package cubetech.gfx;
import cubetech.misc.Ref;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
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

    int minfilter;
    int magfilter;
    int wrap = GL_REPEAT;

    public int textureSlot = 0;
    

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

    public void setWrap(int value) {
        Bind();
        wrap = value;
        glTexParameteri(Target, GL_TEXTURE_WRAP_S, value);
        glTexParameteri(Target, GL_TEXTURE_WRAP_T, value);
        if(Target == GL_TEXTURE_CUBE_MAP)
            glTexParameteri(Target, GL_TEXTURE_WRAP_R, value);
    }

    public void setFiltering(boolean min, int filter) {
        Bind();
        if(min) {
            minfilter = filter;
            glTexParameteri(Target, GL_TEXTURE_MIN_FILTER, minfilter);
        }
        else {
            magfilter = filter;
            glTexParameteri(Target, GL_TEXTURE_MAG_FILTER, magfilter);
        }
    }

    public void Bind() {
        if(loaded) {
            glActiveTexture(GL_TEXTURE0+textureSlot);
            glBindTexture(Target, TextureID);
            
//            Ref.glRef.shader.setUniform(Ref.glRef.shader.GetTextureIndex(textureSlot), textureSlot);
            
            GLRef.checkError();
            
        } else {
            Ref.ResMan.getWhiteTexture().Bind();
        }
    }

    public void Unbind() {
        glActiveTexture(GL_TEXTURE0+textureSlot);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // should only be used by the ressource subsystem
    public void SetID(int id) {
        TextureID = id;
    }

    public void setAnisotropic(int i) {
        if(!Ref.glRef.caps.GL_EXT_texture_filter_anisotropic)
            return;
        Bind();
        i = i>Ref.glRef.maxAniso?Ref.glRef.maxAniso:i;
        glTexParameteri(Target, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, i);
    }
}
