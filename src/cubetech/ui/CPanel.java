/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.ui;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class CPanel extends CComponent {
    public CPanel(Vector2f size) {
        setSize(size);
    }

    @Override
    public void Render(Vector2f parentPosition) {
        // Doesn't render anything
    }

}
