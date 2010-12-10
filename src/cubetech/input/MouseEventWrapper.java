/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.input;

import java.util.EventObject;

/**
 *
 * @author mads
 */
public class MouseEventWrapper extends EventObject {
    public MouseEventWrapper(MouseEvent source) {
        super(source);
    }
}
