/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.CGame.Marks.PolyVert;
import cubetech.gfx.CubeMaterial;
import cubetech.iqm.IQMModel;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class RenderEntity {
    public static final int TYPE_MODEL = 0;
    public static final int TYPE_SPRITE = 1;
    public static final int TYPE_BEAM = 2;

    public static final int FLAG_SPRITE_AXIS = 1; // use axis[0] and axis[1] as up & right vectors
    public static final int FLAG_NOSHADOW = 2;
    public static final int FLAG_GPUSKINNED = 4;

    public IQMModel model = null;
    public REType Type = REType.SPRITE;

    public Vector4f color = new Vector4f(255,255,255,255); // 0 - 255f
    public Vector4f outcolor = new Vector4f();

    
    // most recent data
    public Vector3f[] axis  =new Vector3f[3]; // rotation vectors
    public Vector3f origin = new Vector3f();

    public int frame;

    // previous data for frame interpolation
    public Vector3f oldOrigin = new Vector3f();
    public int oldframe;
    public float backlerp;

    public float radius;
    public float shaderTime;
    public CubeMaterial mat;

    public int flags = 0;

    public PolyVert[] verts;

    
    public RenderEntity(REType rEType) {
        Type = rEType;
        axis[0] = new Vector3f(1,0,0);
        axis[1] = new Vector3f(0,1,0);
        axis[2] = new Vector3f(0,0,1);
    }

    void clear() {
        model = null;
        color.set(255,255,255,255);
        axis[0].set(1,0,0);
        axis[1].set(0,1,0);
        axis[2].set(0,0,1);
        frame = 0;
        flags = 0;
        mat = null;
        Type = null;
        verts = null;
    }

    

}
