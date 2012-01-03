/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.gfx.CubeMaterial;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.PolyVert;
import cubetech.iqm.IQMModel;
import cubetech.misc.Ref;
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
    public static final int FLAG_NOLIGHT = 8; // Gets rendered after deferred pass

    public IQMModel model = null;
    public REType Type = REType.SPRITE;

    public Vector4f color = new Vector4f(255,255,255,255); // 0 - 255f
    public Vector4f outcolor = new Vector4f();

    
    // most recent data
    public Vector3f[] axis  =new Vector3f[3]; // rotation vectors
    public Vector3f origin = new Vector3f();

    public Object controllers = null;

    public int frame;

    // previous data for frame interpolation
    public Vector3f oldOrigin = new Vector3f();
    public Vector3f oldOrigin2 = new Vector3f();
    public int oldframe;
    public float backlerp;

    public float radius;
    public float shaderTime;
    public CubeMaterial mat;

    public int flags = 0;

    public PolyVert[] verts;

    public static RenderEntity addPolyToScene(PolyVert[] verts, CubeTexture tex) {
        RenderEntity ent = Ref.render.createEntity(REType.POLY);
        ent.frame = verts.length;
        ent.verts = verts;
        ent.mat = tex.asMaterial();
        ent.flags |= FLAG_NOSHADOW;
        Ref.render.addRefEntity(ent);
        return ent;
    }

    
    public boolean hasFlag(int flag) {
        return (flags & flag) != 0;
    }
    
    public RenderEntity(REType rEType) {
        Type = rEType;
        axis[0] = new Vector3f(1,0,0);
        axis[1] = new Vector3f(0,1,0);
        axis[2] = new Vector3f(0,0,1);
    }

    public void setAxis(Vector3f[] a) {
        for (int i= 0; i < 3; i++) {
            axis[i].set(a[i]);
        }

    }

    void clear() {
        model = null;
        color.set(255,255,255,255);
        outcolor.set(255,255,255,255);
        origin.set(0,0,0);
        axis[0].set(1,0,0);
        axis[1].set(0,1,0);
        axis[2].set(0,0,1);
        frame = 0;
        oldframe = 0;
        backlerp = 0;
        controllers = null;
        flags = 0;
        mat = null;
        Type = null;
        verts = null;
    }

    

}
