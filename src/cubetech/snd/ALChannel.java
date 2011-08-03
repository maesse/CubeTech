/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.snd;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.openal.Audio;

/**
 *
 * @author mads
 */
public class ALChannel {
    public int entNum;
    public SoundChannel entChannel;
    public Vector3f origin = new Vector3f();  // only use if fixed_origin is set
    public boolean fixed_origin;

    public int allocTime;
    public Audio sfx;
    public boolean dirty; // notifies the soundstore that a channel should be started or stopped.

    public boolean localsound;
    public boolean loop;
    

    // OpenAL side:
    public boolean source_playing; // is source currently playing?
    public int chanIndex = -1; // source index
    

    public ALChannel(int chanIndex) {
        this.chanIndex = chanIndex;
    }
}
