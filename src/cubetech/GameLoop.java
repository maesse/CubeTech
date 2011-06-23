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
    public int frameMsec; // msec since last frame

    public GameLoop() {
    }

    public void RunFrame() throws Exception {
        // Update time
        long currTime = (long)(Sys.getTime()/(long)(Sys.getTimerResolution()/1000f));
        frameMsec = (int) (currTime - lastTime);
        lastTime = currTime;
        if(frameMsec > 300) frameMsec = 300;
        time += frameMsec;

        // Clear stuff
        Graphics.clearScreen();
        Ref.Input.Update(); // get new input
        Ref.ResMan.Update(); // load any pending textures
        Ref.SpriteMan.Reset(); // clear old sprites

        // Do the stuff
        Ref.StateMan.RunFrame(frameMsec);
        
        //Ref.Console.Render();

        // Draw normal
        Graphics.getCamera().applyCameraPosition();
        Ref.SpriteMan.DrawNormal();

        // Draw HUD
        Graphics.setHUDProjection();
        Ref.SpriteMan.DrawHUD();

        // Display frame
        Display.update(false);

        if(Display.isCloseRequested()) {
            Display.destroy();
            throw new RuntimeException("exit");
        }
    }
}
