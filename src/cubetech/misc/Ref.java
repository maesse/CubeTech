/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import cubetech.Collision;
import cubetech.Common;
import cubetech.Console;
import cubetech.GameLoop;
import cubetech.Net;
import cubetech.SoundManager;
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
    public static Common common;
    public static Net net;
    public static Collision collision;
    public static World world;
    public static SoundManager soundMan;
}
