package cubetech.ui;

import cubetech.gfx.CubeMaterial;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.misc.Ref;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class CImage extends CComponent {
    //private CubeTexture tex = null;
    private CubeMaterial mat = null;
//    private Vector2f texoffset = new Vector2f();
//    private Vector2f texsize = new Vector2f(1, 1);
    
    public CImage(String path) {
        if(path.endsWith(".mat"))
            try {
            mat = CubeMaterial.Load(path, true);
        } catch (Exception ex) {
            Logger.getLogger(CImage.class.getName()).log(Level.SEVERE, null, ex);
            mat = new CubeMaterial(Ref.ResMan.LoadTexture(path));
        }
        else
            mat = new CubeMaterial(Ref.ResMan.LoadTexture(path));
        init();
    }

    public CImage(CubeTexture tex) {
        mat = new CubeMaterial(tex);
        init();
    }

    public void setMaterial(CubeMaterial mat) {
        this.mat = mat;
    }


    public CImage(CubeMaterial mat) {
        this.mat = mat;
        init();
    }

    private void init() {
        if(mat.getTexture().loaded)
            setSize(new Vector2f(mat.getTexture().Width, mat.getTexture().Height));
        else
            setSize(new Vector2f(32,32));
        
    }

    @Override
    public void Render(Vector2f parentPosition) {
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        Vector2f dst = new Vector2f(parentPosition);
        Vector2f intPos = getInternalPosition();
        dst.x += intPos.x;
        dst.y += intPos.y;
        Vector2f siz = getSize();
        dst.y = Ref.glRef.GetResolution().y - dst.y - siz.y;
        Vector2f texoffset = mat.getTextureOffset();
        Vector2f texsize = mat.getTextureSize();
        spr.Set(dst, siz, mat.getTexture(), texoffset, texsize);
    }

    public CubeTexture getTex() {
        return mat.getTexture();
    }

    public void setTex(CubeTexture tex) {
        mat.setTexture(tex);
    }

    public Vector2f getTexoffset() {
        return mat.getTextureOffset();
    }

    public void setTexoffset(Vector2f texoffset) {
        mat.setTextureOffset(texoffset);
    }

    public Vector2f getTexsize() {
        return mat.getTextureSize();
    }

    public void setTexsize(Vector2f texsize) {
        mat.setTextureSize(texsize);
    }

    public CubeMaterial getMaterial() {
        return mat;
    }

}
