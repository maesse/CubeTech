/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import cubetech.spatial.Spatial;
import cubetech.collision.Collision;
import cubetech.GameLoop;
import cubetech.World;
import cubetech.gfx.ResourceManager;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.TextManager;
import cubetech.input.Input;
import cubetech.state.StateManager;
import java.util.Random;

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
    public static World world;
    public static SoundManager soundMan;
    public static Spatial spatial;
}
