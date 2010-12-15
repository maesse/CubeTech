/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.state.HagserState;
import java.util.Random;
import cubetech.gfx.ResourceManager;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.TextManager;
import cubetech.misc.Ref;
import cubetech.input.Input;
import cubetech.misc.Spatial;
import cubetech.state.StateManager;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.DisplayMode;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;
/**
 *
 * @author mads
 */
public class GameLoop {
    public boolean running = true;
    CubeTexture tex;
    CubeTexture cursor;
    public DisplayMode mode;
    long lastTime;
    public long time;

    // Initializes classes that there need to be only one off
    public static void InitRef() {
        Ref.loop = new GameLoop();
        Ref.SpriteMan = new SpriteManager();
        Ref.ResMan = new ResourceManager();
        Ref.Input = new Input();
        Ref.rnd = new Random();
        try {
            Ref.Input.Init();
        } catch (LWJGLException ex) {
            Logger.getLogger(GameLoop.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        Ref.Console = new Console();
        try {
            Ref.textMan = new TextManager();
        } catch (IOException ex) {
            Logger.getLogger(GameLoop.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        Ref.StateMan = new StateManager();
        Ref.net = new Net();
        Ref.common = new Common();
        Ref.collision = new Collision();
        Ref.soundMan = new SoundManager();
        Ref.soundMan.initialize(12);
        Ref.spatial = new Spatial();
    }

    
    public void Init() {
        tex = (CubeTexture)(Ref.ResMan.LoadResource("data/test.png").Data);
        cursor = (CubeTexture)(Ref.ResMan.LoadResource("data/cursor.png").Data);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mode = Display.getDisplayMode();
        Ref.common.Init();
    }
    
    public void RunFrame() throws Exception {
        long currTime = (long)(Sys.getTime()/(long)(Sys.getTimerResolution()/1000f));
        long frameMsec = currTime - lastTime;
        lastTime = currTime;
        if(frameMsec > 300)
            frameMsec = 300;
       time += frameMsec;

       Ref.Input.Update();


       

//       GL11.glMatrixMode(GL11.GL_PROJECTION);
//       GL11.glLoadIdentity();
//       GL11.glOrtho(0, 1, 0, 1, 1,-1);

        // Get ready for new frame
       glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
       Ref.SpriteMan.Reset();

       Ref.StateMan.RunFrame((int)frameMsec);
        Ref.SpriteMan.DrawNormal();


//        Ref.SpriteMan.Reset();
        
//        Ref.textMan.Render();
       Ref.Console.Render();
       
       
        Ref.textMan.Render();
        Ref.SpriteMan.DrawNormal();
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
       GL11.glLoadIdentity();
       GL11.glOrtho(0, 1, 0, 1, 1,-1);
        Ref.SpriteMan.DrawHUD();
        World world = ((HagserState)Ref.StateMan.GetGameState("hagser")).world;


       // Display frame
//       Display.sync(60);
       Display.update();
    }
}


