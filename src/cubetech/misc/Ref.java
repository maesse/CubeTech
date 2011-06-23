/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import cubetech.spatial.Spatial;
import cubetech.collision.Collision;
import cubetech.GameLoop;

import cubetech.gfx.ResourceManager;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.TextManager;
import cubetech.input.Input;
import cubetech.state.StateManager;
import java.io.IOException;
import java.util.Random;
import org.lwjgl.LWJGLException;

/**
 *
 * @author mads
 */
public class Ref {
    public static ResourceManager ResMan;
    public static SpriteManager SpriteMan;
    public static GameLoop loop;
    public static TextManager textMan;
    public static Input Input;
    public static Console Console;
    public static StateManager StateMan;
    public static Random rnd;
    public static Collision collision;
    public static SoundManager soundMan;
    public static Spatial spatial;
    

    // Initializes classes that there need to be only one off
    public static void InitRef() throws IOException, Exception {
        Ref.ResMan = new ResourceManager();
        Ref.loop = new GameLoop();
        Ref.SpriteMan = new SpriteManager();
        
        Ref.Input = new Input();
        Ref.rnd = new Random();
        try {
            Ref.Input.Init();
        } catch (LWJGLException ex) {
            System.out.println(ex);
            System.exit(-1);
        }
        Ref.Console = new Console();
        Ref.textMan = new TextManager();
        Ref.textMan.Init();
        Ref.StateMan = new StateManager();
        Ref.collision = new Collision();
        Ref.soundMan = new SoundManager();
        Ref.soundMan.initialize(12);
        Ref.spatial = new Spatial();
    }
}
