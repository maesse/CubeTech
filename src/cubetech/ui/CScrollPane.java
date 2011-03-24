package cubetech.ui;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CScrollPane extends CContainer {
    private boolean vert = true;
    private boolean hori = true;
    float scollbarHeight = 50;
    private float vertPositon = 0f;
    CubeTexture slider;

    public CScrollPane(Direction dir) {
        super(new FlowLayout(false, false, true));
        slider = Ref.ResMan.LoadTexture("data/slider.png");
        setResizeToChildren(dir);
        vert = true;
        hori = true;
        if(dir == Direction.VERTICAL) {
            setInternalMargin(new Vector4f(0, 0, 20, 0));
            hori = false;
        }
        else if(dir == Direction.HORIZONTAL) {
            setInternalMargin(new Vector4f(0, 0, 0, 32));
            vert = false;
        }
        else if(dir == Direction.BOTH)
            setInternalMargin(new Vector4f(0, 0, 32, 32));
    }

    boolean mouseOverVertical = false;
    boolean mouseDown = false;

    @Override
    public void onMouseExit() {
        mouseDown = false;
    }

    @Override
    public void MouseEvent(MouseEvent evt) {
        // Check against scrollbar
        float scollableHeight = getSize().y - scollbarHeight;
        Vector2f componentSize = getComponent(0).getSize();
        if(getComponentCount() > 1)
            throw new RuntimeException("DERRRPPPhh only one child component per scrollpane!");
        Vector2f viewSize = getInternalSize();
        if(evt.Button == 0)
            mouseDown = evt.Pressed;
        if(!isMouseEnter())
            mouseDown = false;

        float maxScroll = componentSize.y - viewSize.y;
        if(maxScroll < 0)
            maxScroll = 0;
        float scollFrac = vertPositon / maxScroll;
        if(maxScroll == 0)
            scollFrac = 0f;
//        System.out.println(evt.Position);
        Vector2f intPos = getInternalPosition();
        Vector2f sliderPosition = new Vector2f( intPos.x + getSize().x  - getInternalMargin().z,
                     (getInternalPosition().y    ));
        if(evt.Position.x >= sliderPosition.x && evt.Position.x <= sliderPosition.x + getInternalMargin().z &&
                evt.Position.y >= sliderPosition.y && evt.Position.y <= sliderPosition.y + getSize().y) {
            mouseOverVertical = true;
            if(mouseDown) {
                
                // Scroll to button press
                float sliderClickFrac = (evt.Position.y - (getPosition().y  + getMargin().y)) - 25; // in pixels
                if(sliderClickFrac < 0)
                    sliderClickFrac = 0;
//                System.out.println(""+sliderClickFrac + " / " + getSize().y + " - max: " + maxScroll);
                sliderClickFrac /= getSize().y - scollbarHeight; // now fraction
                vertPositon = maxScroll * sliderClickFrac;  // back to pixel
            }
        } else
            mouseOverVertical = false;

        Vector2f relative = new Vector2f(evt.Position);
        relative.y += vertPositon ;
        if(containsPoint(evt.Position)) {
            if(!isMouseEnter())
                MouseEnter();
//            System.out.println("evt: " + evt.Position);

            // Offset position for children
            Vector2f relativePosition = new Vector2f(evt.Position);
            relativePosition.x -= getInternalPosition().x;
            relativePosition.y -= getInternalPosition().y;
            relativePosition.y += vertPositon;
            for (int i= 0; i < getComponentCount(); i++) {
                CComponent comp = getComponent(i);
                boolean hit = comp.containsPoint(relativePosition);
                if(hit != comp.isMouseEnter()) {
                    if(hit)
                        comp.MouseEnter();
                    else
                        comp.MouseExit();
                }
                if(hit) {
                    MouseEvent evt2 = new MouseEvent(evt.Button, evt.Pressed, evt.WheelDelta, evt.Delta, relativePosition);
                    comp.MouseEvent(evt2);

                }

            }
        } else if(isMouseEnter()) {
            MouseExit();
        }
    }

    @Override
    public void Render(Vector2f parentPosition) {
        Vector2f renderpos = new Vector2f(parentPosition);

        Vector2f internalPosition = getInternalPosition();
        renderpos.x += internalPosition.x;
        renderpos.y += internalPosition.y;

        if(getBackground() != null) {
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(renderpos.x, Ref.glRef.GetResolution().y - (renderpos.y + getSize().y)), getSize(), getBackground(), null, null);
            spr.SetColor(255,255,255,20);
        }

        Vector2f absPos = new Vector2f(renderpos);

        Vector4f childrenPosition = getInternalMargin();
        renderpos.x += childrenPosition.x;
        renderpos.y += childrenPosition.y;

        // Render all the children
        Vector2f componentSize = getComponent(0).getSize();
        Vector2f viewSize = getInternalSize();

        float maxScroll = componentSize.y - viewSize.y;
        if(maxScroll < 0)
            maxScroll = 0;
        if(vertPositon > maxScroll)
            vertPositon = maxScroll;
        
        Ref.SpriteMan.GetSprite(Type.HUD).SetSpecial(GL11.GL_SCISSOR_BOX, (int)(renderpos.x + childrenPosition.x), (int)Ref.glRef.GetResolution().y - (int)(renderpos.y + viewSize.y + childrenPosition.y), (int)(viewSize.x), (int)(viewSize.y));
        Ref.SpriteMan.GetSprite(Type.HUD).SetSpecial(GL11.GL_SCISSOR_TEST, true);
        renderpos.y -= vertPositon;
        for (int i= 0; i < getComponentCount(); i++) {
            getComponent(i).Render(renderpos);
        }
        Ref.SpriteMan.GetSprite(Type.HUD).SetSpecial(GL11.GL_SCISSOR_TEST, false);

        if(vert) {
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(absPos.x + getSize().x - getInternalMargin().z/2f - 3 ,  Ref.glRef.GetResolution().y - (getPosition().y + parentPosition.y + getMargin().y + getSize().y - 25)), new Vector2f(6, getSize().y - 50), null, null, null);
            spr.SetColor(9, 10, 12, 255);
            spr = Ref.SpriteMan.GetSprite(Type.HUD);
            
            float scollableHeight = getSize().y - scollbarHeight;
            float scollFrac = vertPositon / maxScroll;
            if(maxScroll == 0)
                scollFrac = 0f;
            Vector2f sliderPosition = new Vector2f(absPos.x + getSize().x - getInternalMargin().z ,
                    Ref.glRef.GetResolution().y - (getPosition().y + parentPosition.y + getMargin().y + scollbarHeight + scollableHeight * scollFrac));
            spr.Set(sliderPosition, new Vector2f(getInternalMargin().z, scollbarHeight), slider, null, null);
            if(mouseOverVertical && isMouseEnter())
                spr.SetColor(180, 180, 180, 255);
            else
                spr.SetColor(100, 100, 100, 255);
        
        }
    }
}
