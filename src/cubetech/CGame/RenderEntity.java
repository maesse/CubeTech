/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.iqm.IQMModel;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class RenderEntity {
    public static final int TYPE_MODEL = 0;
    public static final int TYPE_SPRITE = 1;

    public IQMModel model = null;
    public int Type = TYPE_SPRITE;

    // most recent data
    public Vector3f[] axis; // rotation vectors
    public Vector3f origin = new Vector3f();

    public int frame;

    // previous data for frame interpolation
    public Vector3f oldOrigin;
    public int oldframe;
    public float backlerp;

    

}
