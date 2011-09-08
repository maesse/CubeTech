package cubetech.CGame;

import cubetech.collision.CubeCollision;
import cubetech.collision.CubeMap;
import cubetech.common.CVar;
import cubetech.common.Helper;
import cubetech.misc.Plane;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class ViewParams {
    public Vector3f Origin  = new Vector3f();
    public int ViewportX;
    public int ViewportY;
    public int ViewportWidth;
    public int ViewportHeight;
    public float FovX;
    public int FovY;
    public Matrix4f ProjectionMatrix;
    public Matrix4f viewMatrix = new Matrix4f();
    public Vector3f Angles = new Vector3f();
    public Vector3f[] ViewAxis = new Vector3f[3];

    public float xmin, xmax, ymin, ymax;
    public float w, h;
    public float farDepth;
    public float nearDepth;

    public Plane[] planes = new Plane[6];
    private float[] view = new float[16];

    public FloatBuffer viewbuffer = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private FloatBuffer projbuffer = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    public static final float[] flipMatrix = new float[] {
        // convert from our coordinate system (looking down X)
	// to OpenGL's coordinate system (looking down -Z)
	0, 0, -1, 0,
	-1, 0, 0, 0,
	0, 1, 0, 0,
	0, 0, 0, 1};
    public static final Matrix4f flipMatrix2 = new Matrix4f();

    public ViewParams() {
        Helper.toFloatBuffer(flipMatrix, viewbuffer);
        flipMatrix2.load(viewbuffer);
        viewbuffer.clear();
        for (int i= 0; i < 3; i++) {
            ViewAxis[i] = new Vector3f();
        }
        for (int i= 0; i < planes.length; i++) {
            planes[i] = new Plane(0, 0, 0, 0);
        }
    }

    public void SetupProjection() {
        // Use fovx and fovy to set up a matrix
        Vector2f vidSize = Ref.glRef.GetResolution();
        float aspect = vidSize.y/vidSize.x;
        FovX = Ref.cgame.cg_fov.iValue;
        FovY = (int)(FovX*aspect);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        int near = Ref.cvars.Find("cg_depthnear").iValue;
        int far = Ref.cvars.Find("cg_depthfar").iValue;
        nearDepth = near;
        farDepth = far;
        float fov = Ref.cgame.cg_fov.fValue;
        if(Ref.cgame.cg.playingdemo && Ref.cgame.cg_freecam.isTrue()) {
            fov = Ref.cgame.cg.demofov;
        }
        setup3DProjection(fov * aspect, aspect, near, far);
        
        
//        Angles.x *= -1f;
//        Angles.y *= -1f;
        ViewAxis = Helper.AnglesToAxis(Angles, ViewAxis);
        
        view[0] = ViewAxis[0].x;
        view[4] = ViewAxis[0].y;
        view[8] = ViewAxis[0].z;
        view[12] = -Origin.x * view[0] + -Origin.y * view[4] + -Origin.z * view[8];

        view[1] = ViewAxis[1].x;
        view[5] = ViewAxis[1].y;
        view[9] = ViewAxis[1].z;
        view[13] = -Origin.x * view[1] + -Origin.y * view[5] + -Origin.z * view[9];

        view[2] = ViewAxis[2].x;
        view[6] = ViewAxis[2].y;
        view[10] = ViewAxis[2].z;
        view[14] = -Origin.x * view[2] + -Origin.y * view[6] + -Origin.z * view[10];

        view[3] = view[7] = view[11] = 0;
        view[15] = 1;

        //Vector3f org = new Vector3f(-Origin.x, -Origin.y, -Origin.z);
//        ViewAxis[0].scale(-1);
//        ViewAxis[1].scale(-1);
        
//        viewbuffer.put(ViewAxis[0].x); viewbuffer.put(ViewAxis[1].x); viewbuffer.put(ViewAxis[2].x); viewbuffer.put(0);
//        viewbuffer.put(ViewAxis[0].y); viewbuffer.put(ViewAxis[1].y); viewbuffer.put(ViewAxis[2].y); viewbuffer.put(0);
//        viewbuffer.put(ViewAxis[0].z); viewbuffer.put(ViewAxis[1].z); viewbuffer.put(ViewAxis[2].z); viewbuffer.put(0);
//        viewbuffer.put(-org.x * ViewAxis[0].x + -org.y * ViewAxis[0].y + -org.z * ViewAxis[0].z);
//        viewbuffer.put(-org.x * ViewAxis[1].x + -org.y * ViewAxis[1].y + -org.z * ViewAxis[1].z);
//        viewbuffer.put(-org.x * ViewAxis[2].x + -org.y * ViewAxis[2].y + -org.z * ViewAxis[2].z);
//        viewbuffer.put(1);
//
//        viewbuffer.flip();
        
        // convert from our coordinate system (looking down X)
	// to OpenGL's coordinate system (looking down -Z)
        viewbuffer.position(0);
        Helper.toFloatBuffer(view, viewbuffer);
        // Store for shadows
        

        
        Helper.multMatrix(view, flipMatrix, viewbuffer);
        viewbuffer.limit(16);
        viewbuffer.position(0);
        viewMatrix.load(viewbuffer);
        viewbuffer.position(0);
        

        // set it for opengl
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        
        GL11.glLoadMatrix(viewbuffer);

//        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projbuffer);
//        Matrix4f prov = (Matrix4f) new Matrix4f().load(projbuffer);
        
//        projbuffer.position(0);
//
//        Matrix4f mvp = Matrix4f.mul(view, prov, null);
        
        // create the near plane
        Vector3f derps = new Vector3f(ViewAxis[0]);
        derps.scale(-1f);
        float d = Vector3f.dot(derps, Origin);

        planes[0].set(derps.x, derps.y, derps.z, d);
        planes[0].normalize();
    }

    private void setup3DProjection(float fov, float aspect, float znear, float zfar) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(fov, 1f/aspect, znear, zfar);
        farDepth = zfar;
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projbuffer);
        if(ProjectionMatrix == null) {
            ProjectionMatrix = new Matrix4f();
        }
        ProjectionMatrix.load(projbuffer);
        projbuffer.position(0);
        return;
        
    }

    public void CalcVRect() {
        int size = Ref.cgame.cg_viewsize.iValue;
        if(size < 30) {
            size = 30;
            Ref.cvars.Set2("cg_viewsize", "30", true);
        } else if(size > 100) {
            size = 100;
            Ref.cvars.Set2("cg_viewsize", "100", true);
        }

        Vector2f vidSize = Ref.glRef.GetResolution();
        ViewportWidth = (int) (vidSize.x * size / 100);
        ViewportHeight = (int) (vidSize.y * size / 100);
        ViewportX = (int) ((vidSize.x - ViewportWidth) / 2);
        ViewportY = (int) ((vidSize.y - ViewportHeight) / 2);

        GL11.glViewport(ViewportX, ViewportY, ViewportWidth, ViewportHeight);
    }

    void offsetThirdPerson() {
        Vector3f focusAngle = new Vector3f(Angles);
        if(focusAngle.x > 75) focusAngle.x = 75;

        Vector3f forward = new Vector3f();
        Helper.AngleVectors(focusAngle, forward, null, null);
//        forward.scale(-1f);

        float focusDistance = 100;
        Vector3f focusPoint = Helper.VectorMA(Origin, focusDistance, forward, null);
        Vector3f view = new Vector3f(Origin);
        view.z += 8;
        Angles.x *= 0.5f;

        Vector3f t_forward = new Vector3f(), t_right = new Vector3f(), t_up = new Vector3f();
        Helper.AngleVectors(Angles, t_forward, t_right, t_up);
        t_forward.set(forward);
        t_forward.scale(-1f);

        CubeCollision col = CubeMap.TraceRay(view, t_forward, 8, Ref.cgame.map.chunks);
        t_forward.normalise();
        float len = 150;
        if(col != null) {
            Vector3f start = new Vector3f(view);
            Vector3f delta = Helper.VectorMA(view, len, t_forward, null);
            Vector3f.sub(delta, start, delta);
            delta = new Vector3f(t_forward);
            delta.scale(len);

            Plane p = col.getHitPlane();
            float frac = p.findIntersection(start, delta);
            frac -= 0.07f; if( frac < 0) frac = 0f;
            len *= frac;
        }
        Helper.VectorMA(view, len, t_forward, view);

        Origin.set(view);
        Vector3f.sub(focusPoint, Origin, focusPoint);
        float focusDist = (float) Math.sqrt(focusPoint.x * focusPoint.x + focusPoint.y * focusPoint.y);
        if(focusDist < 1) focusDist = 1;
        Angles.x = (float) (-180f / Math.PI * Math.atan2(focusPoint.z, focusDist));
    }
    
}
