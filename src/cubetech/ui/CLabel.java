package cubetech.ui;

import cubetech.common.Helper;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public final class CLabel extends CComponent {
    private String str = "";
    private float Scale = 1f;
    private Vector2f textSize = new Vector2f();
    private Align align = Align.LEFT;

    public CLabel(String str) {
        setText(str);
    }

    public CLabel(String str, Align align, float scale) {
        this.align = align;
        this.Scale = scale;
        setText(str);
    }

    public void setText(String str) {
        if(str != null && str.equals(this.str))
            return; // no change
        this.str = str;
        Vector2f size = getStringSize();
        if(Helper.Equals(size, textSize))
            return; // no size change
        textSize = size;
        setSize(textSize);
        CContainer par = getParent();
        if(par != null)
            par.doLayout();
    }

    public float getScale() {
        return Scale;
    }

    public void setScale(float Scale) {
        this.Scale = Scale;
    }

    // Calculate the pixel size of the string
    private Vector2f getStringSize() {
        return Ref.textMan.GetStringSize(str, null, null, Scale, Type.HUD);
    }
    
    @Override
    public void Render(Vector2f parentPosition) {
        // Get the render position in pixels
        Vector2f renderposition = getInternalPosition(); // takes margin into account
        renderposition.x += parentPosition.x;
        renderposition.y += parentPosition.y;

//        renderposition.y = Ref.glRef.GetResolution().y - renderposition.y;
//        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
//        spr.Set(new Vector2f(renderposition.x, Ref.glRef.GetResolution().y - renderposition.y - getSize().y), getSize(), null, null, null);
//        spr.SetColor(0, 255, 0, 100);

        if(align == Align.CENTER) {
            renderposition.x += (getSize().x - textSize.x) / 2f;
        } else if(align == Align.RIGHT) {
            renderposition.x += getSize().x - textSize.x;
        }
        Ref.textMan.AddText(renderposition, str, Align.LEFT, null, getSize(), Type.HUD, Scale);
    }

    public String getText() {
        return str;
    }

}
