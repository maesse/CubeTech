package cubetech.ui;

import cubetech.input.MouseEvent;

/**
 *
 * @author mads
 */
public interface IMenu {
    public void Update(int msec); // Called when active
    public boolean IsFullscreen(); // menus decide if they need to be fullscreen
    public void Show(); // Called when set actiev
    public void GotMouseEvent(MouseEvent evt);
}
