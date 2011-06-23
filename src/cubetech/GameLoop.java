package cubetech;

import cubetech.gfx.Graphics;
import cubetech.misc.Ref;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
/**
 * Clears screen, runs a statemanager frame then draws everything. Rinse and repeat.
 * @author mads
 */
public class GameLoop {
    private long lastTime;
    private long time;

    public GameLoop() {
    }

    public void RunFrame() throws Exception {
        // Update time
        long currTime = (long)(Sys.getTime()/(long)(Sys.getTimerResolution()/1000f));
        long frameMsec = currTime - lastTime;
        lastTime = currTime;
        if(frameMsec > 300) frameMsec = 300;
        time += frameMsec;

        // Clear stuff
        Graphics.clearScreen();
        Ref.Input.Update(); // get new input
        Ref.ResMan.Update(); // load any pending textures
        Ref.SpriteMan.Reset(); // clear old sprites

        // Do the stuff
        Ref.StateMan.RunFrame((int)frameMsec);
        
        //Ref.Console.Render();

        // Draw normal
        Ref.SpriteMan.DrawNormal();

        // Draw HUD
        Graphics.setHUDProjection();
        Ref.SpriteMan.DrawHUD();

        // Display frame
        Display.update();
    }
}
