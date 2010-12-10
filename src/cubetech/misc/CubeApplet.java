/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import cubetech.misc.GameLoopApplet;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;

/**
 *
 * @author mads
 */
public class CubeApplet extends Applet {
    Canvas displayParent;
    GameLoopApplet gameThread;

    public void StartLWJGL() {
        gameThread = new GameLoopApplet();
        gameThread.displayParent = displayParent;
        gameThread.start();
    }

    private void StopLWJGL() {
        gameThread.running = false;
        try {
            gameThread.join();
        } catch(InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void start() {
        System.out.println("Start");
    }

    @Override
    public void stop() {
        System.out.println("Stop");
    }

    @Override
    public void destroy() {
        remove(displayParent);
        super.destroy();
    }

    @Override
    public void init() {
        setLayout(new BorderLayout());
        try {
            displayParent = new Canvas() {
                @Override
                public final void addNotify() {
                    super.addNotify();
                    StartLWJGL();
                }
                @Override
                public final void removeNotify() {
                    StopLWJGL();
                    super.removeNotify();
                }
            };
            System.err.println("Setting displayparent size: " + getWidth() + "x" + getHeight());
            displayParent.setSize(getWidth(), getHeight());
            add(displayParent);
            displayParent.setFocusable(true);
            displayParent.requestFocus();
            displayParent.setIgnoreRepaint(true);
            setVisible(true);
        } catch (Exception e) {
            System.err.println(e.toString());
            throw new RuntimeException("Unable to create display");
        }
    }
}
