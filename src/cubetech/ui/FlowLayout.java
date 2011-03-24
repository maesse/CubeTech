package cubetech.ui;

import cubetech.ui.CContainer.Direction;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class FlowLayout implements ILayoutManager {
    private boolean horizontal, centered, stretchChildren;
    
    public FlowLayout(boolean horizontal, boolean centered, boolean stretchChildren) {
        this.horizontal = horizontal;
        this.centered = centered;
        this.stretchChildren = stretchChildren;
    }

    public void layoutComponents(CContainer container) {
        while(true) {
            // Calculate would-be size
            Vector2f wouldBeTotal = new Vector2f();
            for (int i= 0; i < container.getComponentCount(); i++) {
                CComponent comp = container.getComponent(i);

                // Figure out how big the component is
                Vector2f compSize = comp.getLayoutSize();

                // Add to the total
                if(horizontal) {
                    // Add to the width
                    wouldBeTotal.x += compSize.x;
                    // keep the tallest component size as the total
                    wouldBeTotal.y = Math.max(compSize.y, wouldBeTotal.y);
                } else {
                    // Add to the height
                    wouldBeTotal.y += compSize.y;
                    // keep the widest component size as the total
                    wouldBeTotal.x = Math.max(compSize.x, wouldBeTotal.x);
                }
            }

            // Resize container if necessary
            Vector2f containerSize = container.getInternalSize(); // Get internal size
            boolean componentsFit = (wouldBeTotal.x <= containerSize.x && wouldBeTotal.y <= containerSize.y);
            Direction contResizeSetting = container.isResizeToChildren();
            if(contResizeSetting != Direction.NONE && !componentsFit) {
                if(contResizeSetting == Direction.BOTH) {
                    container.setInternalSize(wouldBeTotal, false);
                    containerSize = container.getInternalSize(); // Get internal size again
                } else if(contResizeSetting == Direction.VERTICAL) {
                    container.setInternalSize(new Vector2f(container.getInternalSize().x, wouldBeTotal.y), false);
                    containerSize = container.getInternalSize(); // Get internal size again
                } else {
                    container.setInternalSize(new Vector2f(wouldBeTotal.x, container.getInternalSize().y), false);
                    containerSize = container.getInternalSize(); // Get internal size again
                }
            }

            // Now layout the components
            Vector4f containerMargin = container.getInternalMargin();
            Vector2f currentPosition = new Vector2f(containerMargin.x, containerMargin.y);
            int compCount = container.getComponentCount();
            for (int i= 0; i < compCount; i++) {
                CComponent comp = container.getComponent(i);

                // Calculate new position
                Vector2f destination = new Vector2f(currentPosition);
                Vector2f compSize = comp.getLayoutSize();

    //            if(compCount == 1 && stretchChildren) {
    //                comp.setSize(containerSize);
    //                comp.setPosition(destination);
    //                continue;
    //            }

                if(horizontal) {
                    if(stretchChildren)
                    {
                        // Stretch height
                        Vector4f marg = comp.getMargin();
                        comp.setSize(new Vector2f(compSize.x - marg.x - marg.z, containerSize.y - marg.y - marg.w));
                    } else if(centered) {
                        // center on y axis
                        destination.y += (containerSize.y - compSize.y) / 2f;
                    }
                } else {
                    if(stretchChildren) {
                        // Stretch width
                        Vector4f marg = comp.getMargin();

                        comp.setSize(new Vector2f(containerSize.x - marg.x - marg.z , compSize.y - marg.y - marg.w ));
                    } else if(centered) {
                        // center on x axis
                        destination.x += (containerSize.x - compSize.x) / 2f;
                    }
                }


                // Apply position
    //            destination.x += comp.getMargin().x;
    //            destination.y += comp.getMargin().y;
                comp.setPosition(destination);

                // Move current position
                if(horizontal) {
                    currentPosition.x += compSize.x;
                } else {
                    currentPosition.y += compSize.y;
                }
            }
            break;
        }
    }

    public boolean isCentered() {
        return centered;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }
    
}
