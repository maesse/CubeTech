/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.iqm.IQMAnim;

/**
 *
 * player entities need to track more information
// than any other type of entity.

// note that not every player entity is a client entity,
// because corpses after respawn are outside the normal
// client numbering range

// when changing animation, set animationTime to frameTime + lerping time
// The current lerp will finish out, then it will lerp to the new animation
 * @author mads
 */
public class LerpFrame {
    public int oldFrame;
    public int oldFrameTime; // time when ->oldFrame was exactly on

    public int frame;
    public int frametime; // time when ->frame will be exactly on

    public float backlerp;

    public float yawAngle;
    public boolean yawing;
    public float pitchAngle;
    public boolean pitching;

    public int animationNumber; // may include ANIM_TOGGLEBIT
    public IQMAnim animation;
    public int animationTime; // time when the first frame of the animation will be exact
}
