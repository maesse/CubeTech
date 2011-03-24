package cubetech.ui;

import cubetech.Block;
import cubetech.input.MouseEvent;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class StashItemUI extends CContainer {
    Block block = null;
    ButtonEvent onStashClick = null;
    public StashItemUI(Block b, ButtonEvent onStashClick) {
        block = b;
        this.onStashClick = onStashClick;
        addComponent(new CButton(block.Material, new Vector2f(64, 64), new ButtonEvent() {
            public void buttonPressed(CComponent button, cubetech.input.MouseEvent evt) {
                selectStash(evt);
            }
        }));
//        addComponent(new CButton("X"));
    }

    private void selectStash(MouseEvent evt) {
        if(onStashClick != null)
            onStashClick.buttonPressed(this, evt);
    }

    public Block getBlock() {
        return block;
    }
}
