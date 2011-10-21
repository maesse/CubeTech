package cubetech.ui;

import cubetech.collision.Collision;
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
    float scrollbarSize = 50;
    private float vertPositon = 0f;
    boolean mouseOverVertical = false;
    boolean mouseDown = false;

    private float horiPosition = 0f;
    private boolean mouseOverHorizontal = false;
    CubeTexture slider;
    CubeTexture vslider;

    public CScrollPane(Direction dir) {
        super(new FlowLayout(false, false, true));
        slider = Ref.ResMan.LoadTexture("data/textures/ui/slider.png");
        vslider = Ref.ResMan.LoadTexture("data/textures/ui/vslider.png");
        setResizeToChildren(dir);
        vert = true;
        hori = true;
        if(dir == Direction.VERTICAL) {
            setInternalMargin(new Vector4f(0, 0, 20, 0));
            hori = false;
        }
        else if(dir == Direction.HORIZONTAL) {
            setInternalMargin(new Vector4f(0, 0, 0, 20));
            vert = false;
        }
        else if(dir == Direction.BOTH)
            setInternalMargin(new Vector4f(0, 0, 20, 20));
    }

    

    @Override
    public void onMouseExit() {
        mouseDown = false;
    }

    @Override
    public void MouseEvent(MouseEvent evt) {
        // Check against scrollbar
        float scollableHeight = getSize().y - scrollbarSize;
        Vector2f componentSize = getComponent(0).getSize();
        if(getComponentCount() > 1)
            throw new RuntimeException("DERRRPPPhh only one child component per scrollpane!");
        Vector2f viewSize = getInternalSize();
        if(evt.Button == 0)
            mouseDown = evt.Pressed;
        if(!isMouseEnter())
            mouseDown = false;

        // Figure out how much can be scrolled
        Vector2f vertOverflow = new Vector2f(componentSize.x - viewSize.x,componentSize.y - viewSize.y);
        if(vertOverflow.x < 0)
            vertOverflow.x = 0;
        if(vertOverflow.y < 0)
            vertOverflow.y = 0;
        
        Vector2f intPos = getInternalPosition();
        Vector2f intSize = getInternalSize();
        if(vert) {
            // Check slider bounds
            Vector2f sliderPosition = new Vector2f( intPos.x + intSize.x, intPos.y);
            Vector2f sliderEnd = new Vector2f(getPosition().x + getSize().x, getPosition().y + getSize().y);
            if(Collision.TestPointAABB(evt.Position, sliderPosition.x, sliderPosition.y, sliderEnd.x, sliderEnd.y)) {
                mouseOverVertical = true;
                if(mouseDown) {
                    // Scroll to button press
                    float sliderClickFrac = (evt.Position.y - intPos.y) - scrollbarSize/2; // in pixels
                    if(sliderClickFrac < 0)
                        sliderClickFrac = 0;
    //                System.out.println(""+sliderClickFrac + " / " + getSize().y + " - max: " + maxScroll);
                    sliderClickFrac /= intSize.y - scrollbarSize; // now fraction
                    vertPositon = vertOverflow.y * sliderClickFrac;  // back to pixel
                }
            } else
                mouseOverVertical = false;
        }

        if(hori) {
            // Check slider bounds
            Vector2f sliderPosition = new Vector2f( intPos.x , intPos.y+ intSize.y);
            Vector2f sliderEnd = new Vector2f(getPosition().x + getSize().x, getPosition().y + getSize().y);
            if(Collision.TestPointAABB(evt.Position, sliderPosition.x, sliderPosition.y, sliderEnd.x, sliderEnd.y)) {
                mouseOverHorizontal = true;
                if(mouseDown) {
                    // Scroll to button press
                    float sliderClickFrac = (evt.Position.x - intPos.x) - scrollbarSize/2; // in pixels
                    if(sliderClickFrac < 0)
                        sliderClickFrac = 0;
    //                System.out.println(""+sliderClickFrac + " / " + getSize().y + " - max: " + maxScroll);
                    sliderClickFrac /= intSize.x - scrollbarSize; // now fraction
                    horiPosition = vertOverflow.x * sliderClickFrac;  // back to pixel
                }
            } else
                mouseOverHorizontal = false;
        }

        Vector2f relative = new Vector2f(evt.Position);
        relative.y += vertPositon ;
        relative.x += horiPosition ;
        if(containsPoint(evt.Position)) {
            if(!isMouseEnter())
                MouseEnter();
//            System.out.println("evt: " + evt.Position);

            // Offset position for children
            Vector2f relativePosition = new Vector2f(evt.Position);
            relativePosition.x -= getInternalPosition().x;
            relativePosition.y -= getInternalPosition().y;
            relativePosition.y += vertPositon;
            relativePosition.x += horiPosition;
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
            spr.SetColor(255,255,255,255);
        }

        Vector2f absPos = new Vector2f(renderpos);

        Vector4f childrenPosition = getInternalMargin();
        renderpos.x += childrenPosition.x;
        renderpos.y += childrenPosition.y;

        // Render all the children
        Vector2f componentSize = getComponent(0).getSize();
        Vector2f viewSize = getInternalSize();

        Vector2f maxScroll = new Vector2f(componentSize.x - viewSize.x,componentSize.y - viewSize.y);
        if(maxScroll.y < 0)
            maxScroll.y = 0;
        if(maxScroll.x < 0)
            maxScroll.x = 0;
        if(vertPositon > maxScroll.y)
            vertPositon = maxScroll.y;
        if(horiPosition > maxScroll.x)
            horiPosition = maxScroll.x;
        
        Ref.SpriteMan.GetSprite(Type.HUD).SetSpecial(GL11.GL_SCISSOR_BOX, (int)(renderpos.x + childrenPosition.x), (int)Ref.glRef.GetResolution().y - (int)(renderpos.y + viewSize.y + childrenPosition.y), (int)(viewSize.x), (int)(viewSize.y));
        Ref.SpriteMan.GetSprite(Type.HUD).SetSpecial(GL11.GL_SCISSOR_TEST, true);
        renderpos.y -= vertPositon;
        renderpos.x -= horiPosition;
        for (int i= 0; i < getComponentCount(); i++) {
            getComponent(i).Render(renderpos);
        }
        Ref.SpriteMan.GetSprite(Type.HUD).SetSpecial(GL11.GL_SCISSOR_TEST, false);

        if(vert) {
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(absPos.x + getSize().x - getInternalMargin().z/2f - 3 ,  Ref.glRef.GetResolution().y - (getPosition().y + parentPosition.y + getMargin().y + getSize().y - 25)), new Vector2f(6, getSize().y - 50), null, null, null);
            spr.SetColor(9, 10, 12, 255);
            spr = Ref.SpriteMan.GetSprite(Type.HUD);
            
            float scollableHeight = getSize().y - scrollbarSize;
            float scollFrac = vertPositon / maxScroll.y;
            if(maxScroll.y <= 0)
                scollFrac = 0f;
            Vector2f sliderPosition = new Vector2f(absPos.x + getSize().x - getInternalMargin().z ,
                    Ref.glRef.GetResolution().y - (getPosition().y + parentPosition.y + getMargin().y + scrollbarSize + scollableHeight * scollFrac));
            spr.Set(sliderPosition, new Vector2f(getInternalMargin().z, scrollbarSize), slider, null, null);
            if(mouseOverVertical && isMouseEnter())
                spr.SetColor(180, 180, 180, 255);
            else
                spr.SetColor(120, 120, 120, 255);
        
        }

        if(hori) {
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f( (getPosition().x + parentPosition.x + getMargin().x + scrollbarSize/2),
                    Ref.glRef.GetResolution().y - (absPos.y + getInternalSize().y + getInternalMargin().w/2f + 3)),
                    new Vector2f(getSize().x - scrollbarSize,6), null, null, null);
            spr.SetColor(9, 10, 12, 255);
            spr = Ref.SpriteMan.GetSprite(Type.HUD);

            float scollableHeight = getSize().x - scrollbarSize;
            float scollFrac = horiPosition / maxScroll.x;
            if(maxScroll.x <= 0)
                scollFrac = 0f;
            Vector2f sliderPosition = new Vector2f((getPosition().x + parentPosition.x + getMargin().x + scollableHeight * scollFrac) ,
                    Ref.glRef.GetResolution().y - (absPos.y + getSize().y + getInternalMargin().y));
            spr.Set(sliderPosition, new Vector2f(scrollbarSize,getInternalMargin().w), vslider, null, null);
            if(mouseOverHorizontal && isMouseEnter())
                spr.SetColor(180, 180, 180, 255);
            else
                spr.SetColor(120, 120, 120, 255);
        }
    }
}
