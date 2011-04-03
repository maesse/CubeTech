package cubetech.ui;

import cubetech.gfx.TextManager.Align;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CSpinner extends CContainer {
    private int value;
    private int minValue = Integer.MIN_VALUE;
    private int maxValue = Integer.MAX_VALUE;
    int state = 0;

    private ButtonEvent eventHook = null;
    private CLabel label;
    private CImage spinImg;
    private Vector2f normal_offset = new Vector2f(0.0f,0.01f);
    private Vector2f upHover_offset = new Vector2f(32f/256f,0.01f);
    private Vector2f upDown_offset = new Vector2f(96f/256f,0.01f);
    private Vector2f downHover_offset = new Vector2f(64f/256f,0.01f);
    private Vector2f downDown_offset = new Vector2f(128f/256f,0.01f);
    private Vector2f textureSize = new Vector2f(32f/256f, 1f);

    public CSpinner(int initialValue, ButtonEvent evt) {
        super(new FlowLayout(true, true, false));
        eventHook = evt;
        value = initialValue;
        init(""+value, Align.LEFT, 1f);
    }

    public int getValue() {
        return value;
    }

    private void init(String buttonText, Align align,float scale) {
        //setMargin(8, 8, 8, 8);
        
        label = new CLabel(buttonText, align, scale);
//        label.setMargin(2, 2, 2, 2);
        addComponent(label);

        spinImg = new CImage("data/spinner.png");
        spinImg.setTexsize(textureSize);
        spinImg.setTexoffset(normal_offset);
        spinImg.setSize(new Vector2f(32, 32));
        addComponent(spinImg);
//        try {
//            normalBackground = CubeMaterial.Load("data/buttons.mat", true);
//        } catch (Exception ex) {
//            Logger.getLogger(CButton.class.getName()).log(Level.SEVERE, null, ex);
//        }

        doLayout();
    }

    @Override
    public void MouseEvent(MouseEvent evt) {
        Vector2f newpos = new Vector2f(evt.Position);
        Vector2f intPos = getInternalPosition();
        newpos.x -= intPos.x;
        newpos.y -= intPos.y;
        if(isMouseEnter()) {
            if(!spinImg.containsPoint(newpos)) {
                state = 0;
                return;
            } else {
                // What button was hit?
                newpos.y -= spinImg.getPosition().y;
                if(newpos.y >= spinImg.getSize().y/2f)
                {
                    // Hit bottom
                    if(state < 3)
                        state = 2;
                } else {
                    // Hit top
                    if(state < 3)
                        state = 1;
                }
                
            }
            
            if(evt.Button == 0) {
                if(evt.Pressed) {
                    if(state == 2)
                        state = 4;
                    else if(state == 1)
                        state = 3;
                } else if(state > 2) {
                    if(state == 3)
                    {
                        // Top button clicked
                        pressUpButton(1);
                        state = 1;
                    } else if(state == 4) {
                        // Bottom button clicked
                        pressDownButton(1);
                        state = 2;
                    }
    //                setSelected(!selected);
                }
            } else if(evt.WheelDelta != 0) {
                int count = Ref.Input.IsKeyPressed(Keyboard.KEY_LSHIFT)?10:1;
                if(evt.WheelDelta > 0)
                    pressUpButton(count);
                else if(evt.WheelDelta < 0)
                    pressDownButton(count);
            }
        }
        else if(!isMouseEnter())
            state = 0;
    }

    public void pressUpButton(int num) {
        value += num;
        label.setText(""+(value));
        if(eventHook != null)
            eventHook.buttonPressed(this, null);
    }

    public void pressDownButton(int num) {
        value -= num;
        label.setText(""+(value));
        if(eventHook != null)
            eventHook.buttonPressed(this, null);
    }

    int frame = 0;

    @Override
    public void Render(Vector2f parentPosition) {
        if(state > 0 && !isMouseEnter())
            state = 0;
        Vector2f renderpos = new Vector2f(parentPosition);

        renderpos.x += getPosition().x;
        renderpos.y += getPosition().y;
        // Render background

        Vector2f siz = getSize();
        Vector4f marg = getInternalMargin();
//        frame = isMouseEnter()?mouseDown?2:1:0;
        if(state == 0)
            spinImg.setTexoffset(normal_offset);
        else if(state == 1)
            spinImg.setTexoffset(upHover_offset);
        else if(state == 2)
            spinImg.setTexoffset(downHover_offset);
        else if(state == 3)
            spinImg.setTexoffset(upDown_offset);
        else if(state == 4)
            spinImg.setTexoffset(downDown_offset);
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
//            if(i == 0) {
//                renderpos.x += (mouseDown?1:0);
//                renderpos.y += (mouseDown?1:0);
//            }
        }
    }


}
