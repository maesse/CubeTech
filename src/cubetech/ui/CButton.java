package cubetech.ui;

import cubetech.gfx.CubeMaterial;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CButton extends CContainer {
    CLabel label = null;
    CubeTexture tex = null;
    ButtonEvent eventHook = null;
    boolean mouseDown = false;

    CubeMaterial normalBackground = null;
    
    public CButton(String buttonText) {
        super(new FlowLayout(false, true, true));
        init(buttonText, Align.LEFT,1);
    }

    public CButton(String buttonText, CubeTexture tex) {
        super(new FlowLayout(false, true, true));
        this.tex = tex;
        init(buttonText, Align.LEFT,2);
    }

    public CButton(String buttonText, CubeTexture tex, Align align) {
        super(new FlowLayout(false, true, true));
        this.tex = tex;
        init(buttonText, align,2);
    }

    public CButton(String buttonText, CubeTexture tex, Align align, float scale) {
        super(new FlowLayout(false, true, true));
        this.tex = tex;
        init(buttonText, align, scale);
    }

    public CButton(String buttonText, CubeTexture tex, Align align, float scale, ButtonEvent evt) {
        super(new FlowLayout(false, true, true));
        this.tex = tex;
        this.eventHook = evt;
        init(buttonText, align, scale);
    }

    public CButton(CubeTexture tex, Vector2f imageSize) {
        setMargin(8, 8, 8, 8);
        CImage img = new CImage(tex);
        img.setSize(imageSize);
        addComponent(img);
        try {
            normalBackground = CubeMaterial.Load("data/buttons.mat", true);
        } catch (Exception ex) {
            Logger.getLogger(CButton.class.getName()).log(Level.SEVERE, null, ex);
        }

        doLayout();
    }

    public CButton(CubeTexture tex, Vector2f imageSize, ButtonEvent evt) {
        setMargin(8, 8, 8, 8);
        CImage img = new CImage(tex);
        img.setSize(imageSize);
        this.eventHook = evt;
        addComponent(img);
        try {
            normalBackground = CubeMaterial.Load("data/buttons.mat", true);
        } catch (Exception ex) {
            Logger.getLogger(CButton.class.getName()).log(Level.SEVERE, null, ex);
        }

        doLayout();
    }

    public CButton(CubeTexture tex) {
        setMargin(8, 8, 8, 8);
        CImage img = new CImage(tex);
        addComponent(img);
        try {
            normalBackground = CubeMaterial.Load("data/buttons.mat", true);
        } catch (Exception ex) {
            Logger.getLogger(CButton.class.getName()).log(Level.SEVERE, null, ex);
        }

        doLayout();
    }

    public CButton(CubeMaterial mat, Vector2f imgSize, ButtonEvent evt) {
        setMargin(8, 8, 8, 8);
        CImage img = new CImage(mat);
        img.setSize(imgSize);
        img.setTexoffset(mat.getTextureOffset());
        img.setTexsize(mat.getTextureSize());
        addComponent(img);
        eventHook = evt;
        try {
            normalBackground = CubeMaterial.Load("data/buttons.mat", true);
        } catch (Exception ex) {
            Logger.getLogger(CButton.class.getName()).log(Level.SEVERE, null, ex);
        }

        doLayout();
    }

    private void init(String buttonText, Align align,float scale) {
        setMargin(8, 8, 8, 8);
        label = new CLabel(buttonText, align, scale);
//        label.setMargin(2, 2, 2, 2);
        addComponent(label);
        try {
            normalBackground = CubeMaterial.Load("data/buttons.mat", true);
        } catch (Exception ex) {
            Logger.getLogger(CButton.class.getName()).log(Level.SEVERE, null, ex);
        }

        doLayout();
    }

    public String getText() {
        return label.getText();
    }

    public void setText(String text) {
        label.setText(text);
        doLayout();
    }

    @Override
    public void MouseEvent(MouseEvent evt) {
        if(!isVisible())
            return;
        if(isMouseEnter() && evt.Button == 0 || evt.Button == 1) {
            if(evt.Pressed) {
                mouseDown = true;
            } else if(mouseDown) {
                if(eventHook != null)
                    eventHook.buttonPressed(this, evt);
                mouseDown = false;
            }
        }
        else if(!isMouseEnter())
            mouseDown = false;
    }

    // Manual button press
    public void pressButton() {
        if(eventHook != null)
            eventHook.buttonPressed(this, null);
    }

    int frame = 0;

    @Override
    public void Render(Vector2f parentPosition) {
        if(!isVisible())
            return;
        if(mouseDown && !isMouseEnter())
            mouseDown = false;
        Vector2f renderpos = new Vector2f(parentPosition);

        renderpos.x += getPosition().x;
        renderpos.y += getPosition().y;
        // Render background
        
        Vector2f siz = getSize();
        Vector4f marg = getInternalMargin();
        frame = isMouseEnter()?mouseDown?2:1:0;
//        if(tex != null) {
//        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
//        spr.Set(new Vector2f(renderpos.x, Ref.glRef.GetResolution().y - renderpos.y - siz.y),
//                new Vector2f(siz.x + marg.x + marg.z, siz.y + marg.y + marg.w), tex, null, null);
//        } else
        {
            renderNormalBg(renderpos);
        }
//        spr.SetColor(255, 255, 255, 255);
        Vector4f ma = getMargin();
        renderpos.x += marg.x + ma.x + (mouseDown?1:0);
        renderpos.y += marg.y + ma.y + (mouseDown?1:0);
        // Render all the children
        for (int i= 0; i < getComponentCount(); i++) {
            getComponent(i).Render(renderpos);
        }
    }

    private void renderNormalBg(Vector2f renderpos) {
        
        Vector2f siz = new Vector2f(getSize());
        
        Vector2f texOffset = normalBackground.getTextureOffset(frame);
        Vector2f texSize = normalBackground.getTextureSize();
        Vector4f marg = getMargin();
        siz.x += marg.x + marg.z;
        siz.y += marg.y + marg.w;

        float resY = Ref.glRef.GetResolution().y;
        Vector2f cornerSize = new Vector2f(16f/512f, 16f/512f);


        Vector2f topleftOffset = new Vector2f(texOffset.x, texOffset.y + (140f/512f));
        Vector2f leftOffset = new Vector2f(texOffset.x, texOffset.y + (16f/512f));
        Vector2f rightOffset = new Vector2f(texOffset.x + (144f/512f), texOffset.y + (16f/512f));
        Vector2f sideSize = new Vector2f(cornerSize.x, texSize.y - cornerSize.y * 2f);
        Vector2f topRightOffset = new Vector2f(texOffset.x + (144f/512f), texOffset.y + (140f/512f));
        Vector2f botRightOffset = new Vector2f(texOffset.x + (144f/512f), texOffset.y);
        Vector2f botSize = new Vector2f(texSize.x - cornerSize.x * 2f, cornerSize.y);
        Vector2f botOffset = new Vector2f(texOffset.x + (16f/512f), texOffset.y);
        Vector2f topOffset = new Vector2f(texOffset.x + (16f/512f), texOffset.y + (140f/512f));
        Vector2f centerOffset = new Vector2f(texOffset.x + cornerSize.x, texOffset.y + cornerSize.y);
        Vector2f centerSize = new Vector2f(texSize.x - cornerSize.x * 2f, texSize.y - cornerSize.y * 2f);

        // Bot left
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(renderpos.x, resY - renderpos.y - siz.y), new Vector2f(marg.x, marg.y), normalBackground.getTexture(), texOffset, cornerSize);

        // Top Left
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(renderpos.x, resY - renderpos.y - marg.y), new Vector2f(marg.x, marg.y), normalBackground.getTexture(), topleftOffset, cornerSize);

        // Top Right
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(renderpos.x + siz.x - marg.z, resY - renderpos.y - marg.y), new Vector2f(marg.x, marg.y), normalBackground.getTexture(), topRightOffset, cornerSize);

        // Bot Right
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(renderpos.x + siz.x - marg.z, resY - renderpos.y - siz.y), new Vector2f(marg.x, marg.y), normalBackground.getTexture(), botRightOffset, cornerSize);

        // Top
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(renderpos.x + marg.x, resY - renderpos.y - marg.y),
                new Vector2f(siz.x - marg.x - marg.z, marg.y), normalBackground.getTexture(), topOffset, botSize);

        // Bottom
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(renderpos.x + marg.x, resY - renderpos.y - siz.y),
                new Vector2f(siz.x - marg.x - marg.z, marg.y), normalBackground.getTexture(), botOffset, botSize);

        //  Left
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(renderpos.x, resY - renderpos.y - siz.y + marg.y),
                new Vector2f(marg.x, siz.y - marg.w - marg.y), normalBackground.getTexture(), leftOffset, sideSize);

        // Right
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(renderpos.x + siz.x - marg.z , resY - renderpos.y - siz.y + marg.y),
                new Vector2f(marg.x, siz.y - marg.w - marg.y), normalBackground.getTexture(), rightOffset, sideSize);

        // Center
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(renderpos.x + marg.x, resY - renderpos.y - siz.y + marg.y),
                new Vector2f(siz.x - marg.x - marg.z, siz.y - marg.w - marg.y), normalBackground.getTexture(), centerOffset, centerSize);

//        spr.Set(new Vector2f(renderpos.x, Ref.glRef.GetResolution().y - renderpos.y - siz.y),
//                new Vector2f(siz.x + marg.x + marg.z, siz.y + marg.y + marg.w), normalBackground.getTexture(), normalBackground.getTextureOffset(frame), normalBackground.getTextureSize());
    }
    
}
