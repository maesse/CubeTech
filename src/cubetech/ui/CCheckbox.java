/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.ui;

import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CCheckbox extends CContainer {
    private CLabel label;
    private CImage checkboxImg;

    private boolean selected = false;

    // Texture offsets
    private Vector2f selected_offset = new Vector2f(0.5f,0.01f);
    private Vector2f unselected_offset = new Vector2f(0f,0.01f);

    private ButtonEvent eventHook = null;
    private boolean mouseDown = false;
    
    public CCheckbox(String text) {
        super(new FlowLayout(true, true, false));
        init(text, Align.LEFT, 1f);
    }

    public CCheckbox(String text, ButtonEvent evt) {
        super(new FlowLayout(true, true, false));
        init(text, Align.LEFT, 1f);
        eventHook = evt;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setText(String str) {
        label.setText(str);
    }

    public void setSelected(boolean value) {
        setSelectedDontFire(value);

        if(eventHook != null)
            eventHook.buttonPressed(this, null);
    }

    public void setSelectedDontFire(boolean value) {
         selected = value;
         if(selected)
            checkboxImg.setTexoffset(selected_offset);
        else
            checkboxImg.setTexoffset(unselected_offset);
    }

    private void init(String buttonText, Align align,float scale) {
        //setMargin(8, 8, 8, 8);
        checkboxImg = new CImage("data/textures/ui/checkbox.png");
        checkboxImg.setTexsize(new Vector2f(32f/65f, 32f/32f));
        checkboxImg.setTexoffset(unselected_offset);
        checkboxImg.setSize(new Vector2f(26, 26));
        addComponent(checkboxImg);
        label = new CLabel(buttonText, align, scale);
//        label.setMargin(2, 2, 2, 2);
        addComponent(label);
//        try {
//            normalBackground = CubeMaterial.Load("data/buttons.mat", true);
//        } catch (Exception ex) {
//            Logger.getLogger(CButton.class.getName()).log(Level.SEVERE, null, ex);
//        }

        doLayout();
    }

    @Override
    public void MouseEvent(MouseEvent evt) {
        if(isMouseEnter() && evt.Button == 0) {
            if(evt.Pressed) {
                mouseDown = true;
            } else if(mouseDown) {
                mouseDown = false;
                setSelected(!selected);
            }
        }
        else if(!isMouseEnter())
            mouseDown = false;
    }

//    // Manual button press
//    public void pressButton() {
//        if(eventHook != null)
//            eventHook.buttonPressed(this, null);
//    }

    int frame = 0;

    @Override
    public void Render(Vector2f parentPosition) {
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
//                new Vector2f(siz.x + marg.x + marg.z, siz.y + marg.y + marg.w), Ref.ResMan.getWhiteTexture(), null, null);
//        if(isMouseEnter())
//            spr.SetColor(255, 255, 255, 200);
//        } else
        {
//            renderNormalBg(renderpos);
        }
//        spr.SetColor(255, 255, 255, 255);
        Vector4f ma = getMargin();
        renderpos.x += marg.x + ma.x;
        renderpos.y += marg.y + ma.y;
        // Render all the children
        for (int i= 0; i < getComponentCount(); i++) {
            getComponent(i).Render(renderpos);
            if(i == 0) {
                renderpos.x += (mouseDown?1:0);
                renderpos.y += (mouseDown?1:0);
            }
        }
    }
}
