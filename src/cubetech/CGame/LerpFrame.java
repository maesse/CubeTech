/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.iqm.IQMAnim;

/**
 *
 * @author mads
 */
public class LerpFrame {
    public int oldFrame;
    public int oldFrameTime; // time when ->oldFrame was exactly on

    public int frame;
    public int frametime; // time when ->frame will be exactly on

    public float backlerp;

    public int animationNumber; // may include ANIM_TOGGLEBIT
    public IQMAnim animation;
    public int animationTime; // time when the first frame of the animation will be exact
}
