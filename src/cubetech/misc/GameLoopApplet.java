/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import cubetech.GameLoop;
import cubetech.gfx.Graphics;
import cubetech.misc.Ref;
import java.awt.Canvas;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/**
 *
 * @author mads
 */
public class GameLoopApplet extends Thread {
    public boolean running = false;
    public Canvas displayParent;

    
    @Override
    public void run() {
        System.err.println("GameLoopApplet Running");
        running = true;
        
        try {
            Graphics.init(displayParent);
            Ref.InitRef();
            System.err.println("Starting GameLoop");
            GameLoop();
        } catch (Exception ex) {
            Logger.getLogger(GameLoopApplet.class.getName()).log(Level.SEVERE, null, ex);
            running = false;
        }
    }

    public void GameLoop() {
        try {
            while(running) {
                Ref.loop.RunFrame();
            }
            Display.destroy();
        } catch (Exception ex) {
            System.err.println("GameLoop() ERROR:");
            System.err.println(ex.toString());
            Logger.getLogger(GameLoopApplet.class.getName()).log(Level.SEVERE, null, ex);
            //System.exit(-1);
        }
    }

//    protected void initGL() {
//        try {
//            FloatBuffer pos = BufferUtils.createFloatBuffer(4).put(new float[] {5f, 5f, 10f, 0f});
//            pos.flip();
//            glLight(GL_LIGHT0, GL_POSITION, pos);
//            glEnable(GL_CULL_FACE);
//            glEnable(GL_LIGHTING);
//            glEnable(GL_LIGHT0);
//            glEnable(GL_DEPTH_TEST);
//
//            glMatrixMode(GL_PROJECTION);
//            System.err.println("LWJGL: " + Sys.getVersion() + " / " + LWJGLUtil.getPlatformName());
//            System.err.println("GL_VENDOR: " + glGetString(GL_VENDOR));
//            System.err.println("GL_RENDERER: " + glGetString(GL_RENDERER));
//            System.err.println("GL_VERSION: " + glGetString(GL_VERSION));
//            System.err.println();
//            System.err.println("glLoadTransposeMatrixfARB() supported: " + GLContext.getCapabilities().GL_ARB_transpose_matrix);
//            if (!GLContext.getCapabilities().GL_ARB_transpose_matrix) {
//            // --- not using extensions
//                glLoadIdentity();
//            } else {
//                // --- using extensions
//                final FloatBuffer identityTranspose = BufferUtils.createFloatBuffer(16).put(
//                new float[] { 1, 0, 0, 0, 0, 1, 0, 0,
//                0, 0, 1, 0, 0, 0, 0, 1});
//                identityTranspose.flip();
//
//                glLoadTransposeMatrixARB(identityTranspose);
//            }
//            float h = (float) displayParent.getHeight() / (float) displayParent.getWidth();
//            glFrustum(-1.0f, 1.0f, -h, h, 5.0f, 60.0f);
//            glMatrixMode(GL_MODELVIEW);
//            glLoadIdentity();
//            glTranslatef(0.0f, 0.0f, -40.0f);
//        } catch (Exception e) {
//            System.err.println(e.toString());
//            running = false;
//        }
//    }
}
