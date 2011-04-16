package cubetech.ui;

import cubetech.gfx.CubeMaterial;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CContainer extends CComponent {
    private ILayoutManager layout;
    private ArrayList<CComponent> components = new ArrayList<CComponent>();
    private Direction resizeToChildren = Direction.BOTH; // Resize to fit components
    private Vector4f internalMargin = new Vector4f(); // margin on the insize of the container
    private CubeTexture background = null;
    private boolean inLayout = false;
    public int border = 0;

    public void removeComponents() {
        for (CComponent cComponent : components) {
            cComponent.setParent(null);
        }
        components.clear();
    }

    public enum Direction {
        NONE,
        VERTICAL,
        HORIZONTAL,
        BOTH
    }

    public CContainer() {
        layout = new FlowLayout(false, true, false);
    }

    public CContainer(ILayoutManager layout) {
        this.layout = layout;
    }

    @Override
    public void setSize(Vector2f siz) {
        setSize2(siz);
        doLayout();
    }
    
    public void doLayout() {
        if(inLayout)
            return;
        inLayout = true;
        layout.layoutComponents(this);
        CContainer par = getParent();
        if(par != null)
            par.doLayout();
        inLayout = false;
    }

    public void addComponent(CComponent comp) {
        components.add(comp);
        comp.setParent(this);
    }

    public void removeComponent(CComponent comp) {
        components.remove(comp);
        comp.setParent(null);
    }

    public CComponent getComponent(int index) {
        return components.get(index);
    }

    public int getComponentCount() {
        return components.size();
    }

    @Override
    public void Render(Vector2f parentPosition) {
        Vector2f renderpos = new Vector2f(parentPosition);

        Vector2f intPos = getInternalPosition();
        renderpos.x += intPos.x;
        renderpos.y += intPos.y;

        if(border > 0) {
            
            Vector2f topleft = new Vector2f(renderpos.x, Ref.glRef.GetResolution().y - (renderpos.y + getSize().y));
            Vector2f botright = new Vector2f(renderpos.x + getSize().x+1, Ref.glRef.GetResolution().y - (renderpos.y));
            
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.setLine(new Vector2f(topleft), new Vector2f(topleft.x, botright.y), border);
            spr.SetColor(0,0,0,255);
            spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.setLine(new Vector2f(topleft), new Vector2f(botright.x, topleft.y), border);
            spr.SetColor(0,0,0,255);
            spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.setLine(new Vector2f(botright), new Vector2f(botright.x, topleft.y), border);
            spr.SetColor(0,0,0,255);
            spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.setLine(new Vector2f(botright), new Vector2f(topleft.x, botright.y), border);
            spr.SetColor(0,0,0,255);
        }

        if(background != null) {
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(renderpos.x, Ref.glRef.GetResolution().y - (renderpos.y + getSize().y)), getSize(), background, null, null);
            spr.SetColor(255,255,255,255);
        }

        RenderImplementation(renderpos);

        renderpos.x += internalMargin.x ;
        renderpos.y += internalMargin.y ;
        // Render all the children
        for (int i= 0; i < components.size(); i++) {
            components.get(i).Render(renderpos);
        }
    }

    public void RenderImplementation(Vector2f pos) {
        
    }

    public ILayoutManager getLayout() {
        return layout;
    }

    public void setLayout(ILayoutManager layout) {
        this.layout = layout;
    }

    public Direction isResizeToChildren() {
        return resizeToChildren;
    }

    public void setResizeToChildren(Direction resizeToChildren) {
        this.resizeToChildren = resizeToChildren;
    }

    public Vector4f getInternalMargin() {
        return internalMargin;
    }

    public void setInternalMargin(Vector4f internalMargin) {
        this.internalMargin = internalMargin;
    }

    @Override
    public void onMouseEnter() {
        setMouseEnter(true);
//        System.out.println("Container: Enter");
    }

    @Override
    public void onMouseExit() {
//        System.out.println("Container: Exit");
        setMouseEnter(false);
        // Send mouseExit to all sub controls that have mouseover
        for (int i= 0; i < getComponentCount(); i++) {
            CComponent comp = getComponent(i);
            if(comp.isMouseEnter()) {
                comp.MouseExit();
            }
        }
    }

    @Override
    public void MouseEvent(MouseEvent evt) {
        if(containsPoint(evt.Position)) {
            if(!isMouseEnter())
                MouseEnter();
//            System.out.println("evt: " + evt.Position);

            // Offset position for children
            Vector2f relativePosition = new Vector2f(evt.Position);
            relativePosition.x -= getInternalPosition().x;
            relativePosition.y -= getInternalPosition().y;
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

    // Returns the size of the usable component area
    public Vector2f getInternalSize() {
        // Offset size for margin
        Vector2f containerSize = new Vector2f(getSize());
        containerSize.x -= internalMargin.x + internalMargin.z;
        containerSize.y -= internalMargin.y + internalMargin.w;
        return containerSize;
    }

    // Resizes the component so it can contain this size
    public void setInternalSize(Vector2f newsize, boolean doLayout) {
        Vector2f temp = new Vector2f(newsize);
        // Append the margin to the size
        temp.x += internalMargin.x + internalMargin.z;
        temp.y += internalMargin.y + internalMargin.w;
        if(doLayout)
            setSize(temp);
        else
            setSize2(temp);
    }

    public CubeTexture getBackground() {
        return background;
    }

    public void setBackground(CubeTexture background) {
        this.background = background;
    }

    @Override
    public boolean containsPoint(Vector2f point) {
        Vector2f position = getPosition();
        Vector2f size = getLayoutSize();
        return (point.x >= position.x && point.x < position.x + size.x
                && point.y >= position.y && point.y < position.y + size.y );
    }
}
