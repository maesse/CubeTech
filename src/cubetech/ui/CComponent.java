package cubetech.ui;

import cubetech.input.MouseEvent;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public abstract class CComponent {
    private Vector2f position = new Vector2f(); // positions are relative to parent
    private Vector2f size = new Vector2f();
    // x = x, y=y, z=rx, w=by
    private Vector4f margin = new Vector4f(); // this gets added to the size
    CContainer parent = null;
    private boolean mouseEnter = false;
    public Object tag = null;
    private boolean visible = true;

    public abstract void Render(Vector2f parentPosition);

    public void setVisible(boolean val) {
        visible = val;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setPosition(Vector2f pos) {
        position.set(pos);
    }
    public void setSize(Vector2f siz) {
        size.set(siz);
    }
    public final void setSize2(Vector2f siz) {
        size.set(siz);
    }
    public Vector2f getSize() {
        return size;
    }
    public Vector2f getInternalPosition() {
        return new Vector2f(position.x + margin.x, position.y + margin.y);
    }
    public Vector2f getPosition() {
        return position;
    }
    // The actual final wanted size of the component
    public Vector2f getLayoutSize() {
        return new Vector2f(size.x + margin.x + margin.z, size.y + margin.y + margin.w);
    }

    public Vector4f getMargin() {
        return margin;
    }

    public void MouseEnter() {
        mouseEnter = true;
        onMouseEnter();
    }
    public void MouseExit() {
        mouseEnter = false;
        onMouseExit();
    }

    // Returns true if the point is contained in the control
    public boolean containsPoint(Vector2f point) {
        return (point.x >= position.x && point.x < position.x + size.x + margin.x + margin.z
                && point.y >= position.y && point.y < position.y + size.y + margin.y + margin.w);
    }

    public void MouseEvent(MouseEvent evt) {}
    public void onMouseExit() {}
    public void onMouseEnter() {}

    public void setMargin(float left, float top, float right, float bottom) {
        margin.x = left;
        margin.y = top;
        margin.z = right;
        margin.w = bottom;
    }

    public boolean isMouseEnter() {
        return mouseEnter;
    }

    public void setMouseEnter(boolean mouseEnter) {
        this.mouseEnter = mouseEnter;
    }

    public void setParent(CContainer parent) {
        this.parent = parent;
    }
    public CContainer getParent() {
        return parent;
    }

}
