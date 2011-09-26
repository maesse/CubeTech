package cubetech.snd;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.openal.Audio;

/**
 *
 * @author mads
 */
public class SoundLoop {
    public Vector3f origin = new Vector3f();
    public Vector3f velocity = new Vector3f();

    Audio sfx = null;
    int mergeFrame;
    boolean active;
    boolean kill;

    boolean loopAddedThisFrame;
    boolean startLoopingSound;
    Audio loophandle;
    
}
