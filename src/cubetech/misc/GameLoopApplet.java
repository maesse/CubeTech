package cubetech.misc;

import cubetech.common.Common;
import java.applet.Applet;
import java.awt.Canvas;

/**
 *
 * @author mads
 */
public class GameLoopApplet extends Thread {
    public boolean running = false;
    public Canvas displayParent;
    public Applet applet = null;
    
    @Override
    public void run() {
        System.err.println("GameLoopApplet Running");
        Common.Startup(displayParent, applet);
    }

}
