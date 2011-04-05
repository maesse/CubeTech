package cubetech.misc;

import cubetech.common.Common;
import java.applet.Applet;
import java.awt.Canvas;
import java.util.ArrayList;

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

        ArrayList<String> args = new ArrayList<String>();
        String noSound = applet.getParameter("nosound");
        if(noSound != null)
            args.add("-nosound");
        String lowGfx = applet.getParameter("lowgfx");
        if(lowGfx != null)
            args.add("-lowgfx");

        String[] argsArray = new String[args.size()];
        args.toArray(argsArray);

        Common.Startup(displayParent, applet,argsArray);
    }

}
