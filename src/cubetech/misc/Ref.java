package cubetech.misc;

import cubetech.snd.SoundManager;
import cubetech.gfx.GLRef;
import cubetech.CGame.CGame;
import cubetech.CGame.Render;
import cubetech.Game.Game;
import cubetech.common.Common;
import cubetech.net.Net;
import cubetech.spatial.Spatial;
import cubetech.collision.Collision;

import cubetech.client.Client;
import cubetech.collision.ClipMap;
import cubetech.common.CVars;
import cubetech.common.Commands;
import cubetech.gfx.ResourceManager;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.TextManager;
import cubetech.input.Input;
import cubetech.server.Server;
//import cubetech.state.StateManager;
import cubetech.ui.UI;
import java.util.Random;
import javax.swing.UIManager;

/**
 * Just seems nicer than letting all this be singletons
 * @author mads
 */
public class Ref {
    public static ResourceManager ResMan;
    public static SpriteManager SpriteMan;
    public static TextManager textMan;
    public static Input Input;
    public static Console Console;
//    public static StateManager StateMan;
    public static Random rnd;
    public static Common common;
    public static Net net;
    public static Collision collision;

    public static SoundManager soundMan;
    public static Spatial spatial;
    public static Client client;
    public static Server server;
    public static CVars cvars;
    public static Commands commands;
    public static Game game;
    public static CGame cgame;
    public static GLRef glRef;
    public static UI ui;
    public static ClipMap cm;

    public static Render render;

    // Initializes classes that there need to be only one off
    public static void InitRef(boolean isApplet, boolean noSound) throws Exception {
        // Set default platform look and feel
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        
        Log.Init(isApplet); // Re-route System.out through the Log system
        Ref.SpriteMan = new SpriteManager(); // sprite factory/manager/renderer
        Ref.ResMan = new ResourceManager(); // Loads and caches textures
        
        Ref.rnd = new Random(); // just nice to have a single random object allocated
        
        Ref.textMan = new TextManager(); // manages text-rendering
        Ref.common = new Common(); // common subsystem. runs everything
        Ref.commands = new Commands(); // command subsystem, a string communication system
        Ref.Input = new Input(); // input subsystem
        Ref.cvars = new CVars(); // init cvar subsystem
        Ref.Input.initialize();
        Ref.net = new Net(); // init network, creates sockets for client and server
        Ref.collision = new Collision();
        Ref.soundMan = new SoundManager(); // loads, manages and plays sounds
        if(!noSound)
            Ref.soundMan.initialize(12); // Init 12 channels
        Ref.spatial = new Spatial(); // spatial query subsystem
//        Ref.StateMan = new StateManager(); // old game system
        Ref.glRef = new GLRef(); // OpenGL/Window manager
        Ref.client = new Client(); // Client subsystem
//        Ref.server = new Server(); // Server subsystem

        Ref.Console = new Console(); // Set up the console
        Ref.ui = new UI();
        Ref.cm = new ClipMap();
        
    }
}
