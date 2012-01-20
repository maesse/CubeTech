package cubetech.input;

import java.util.EventListener;

/**
 *
 * @author mads
 */
public interface KeyEventListener extends EventListener {
    /**
     * @param evt key that caused this event
     * @return true if events should be passed on to binds subsystem
     */
    public boolean KeyPressed(KeyEvent evt);
}
