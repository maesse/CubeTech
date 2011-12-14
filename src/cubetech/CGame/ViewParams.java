package cubetech.CGame;

import cubetech.collision.CubeCollision;
import cubetech.collision.CubeMap;
import cubetech.common.CVar;
import cubetech.common.Helper;
import cubetech.gfx.Light;
import cubetech.misc.Plane;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
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
    public float FovY;
    public Matrix4f ProjectionMatrix;
    public Matrix4f viewMatrix = new Matrix4f();
    public Vector3f Angles = new Vector3f();
    public Vector3f[] ViewAxis = new Vector3f[3];

    public float xmin, xmax, ymin, ymax;
    public float w, h;
    public float farDepth;
    public float nearDepth;
    
    public ArrayList<Light> lights = new ArrayList<Light>();

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

    public void setupOthoProjection(float fovx) {
        Vector2f vidSize = Ref.glRef.GetResolution();
        float aspect = vidSize.y/vidSize.x;
        FovX = fovx;
        FovY = (FovX*aspect);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        GL11.glOrtho(0, fovx, 0, FovY, 1, -1000);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projbuffer);
        if(ProjectionMatrix == null) {
            ProjectionMatrix = new Matrix4f();
        }
        ProjectionMatrix.load(projbuffer);
        projbuffer.position(0);

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

        // convert from our coordinate system (looking down X)
	// to OpenGL's coordinate system (looking down -Z)
        viewbuffer.position(0);
        Helper.toFloatBuffer(view, viewbuffer);
        // Store for shadows



//        Helper.multMatrix(view, flipMatrix, viewbuffer);
        viewbuffer.limit(16);
        viewbuffer.position(0);
        viewMatrix.load(viewbuffer);
        viewbuffer.position(0);


        // set it for opengl
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        
        GL11.glLoadMatrix(viewbuffer);
    }

    public void SetupProjection() {
        lights.clear();
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
        setup3DProjection(fov * aspect, aspect, 64, near, far);

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
    }

    private void setup3DProjection(float fov, float aspect, float zproj, float znear, float zfar) {
        boolean useGLU = false;
        if(useGLU) {
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
        } else {
            if(ProjectionMatrix == null) ProjectionMatrix = new Matrix4f();
            else ProjectionMatrix.setZero();
            
            float fovy = fov;
            float fovx = fov / aspect;
            float _ymax = (float) (zproj * Math.tan(fovy * Math.PI / 360f));
            float _ymin = -_ymax;
            float _xmax = (float) (zproj * Math.tan(fovx * Math.PI / 360f));
            float _xmin = -_xmax;
            float _width = _xmax - _xmin;
            float _height = _ymax - _ymin;
            float stereoSep = 0f;
            float depth = zfar - znear;

            ProjectionMatrix.m00 = 2f * zproj / _width;
            ProjectionMatrix.m10 = 0;
            ProjectionMatrix.m20 = (_xmax + _xmin + 2f * stereoSep) / _width;
            ProjectionMatrix.m30 = 2f * zproj * stereoSep / _width;

            ProjectionMatrix.m01 = 0;
            ProjectionMatrix.m11 = 2f * zproj / _height;
            ProjectionMatrix.m21 = (_ymax + _ymin) / _height;
            ProjectionMatrix.m31 = 0;

            ProjectionMatrix.m02 = 0;
            ProjectionMatrix.m12 = 0;
            ProjectionMatrix.m22 = -(zfar + znear) / depth;
            ProjectionMatrix.m32 = -2f * zfar * znear / depth;

            ProjectionMatrix.m03 = 0;
            ProjectionMatrix.m13 = 0;
            ProjectionMatrix.m23 = -1;
            ProjectionMatrix.m33 = 0;

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            projbuffer.clear();
            ProjectionMatrix.store(projbuffer);
            projbuffer.flip();
            GL11.glLoadMatrix(projbuffer);
            projbuffer.position(0);
            setupFrustum(_xmin, _xmax, _ymax, zproj, stereoSep);
        }        
    }

    private void setupFrustum(float _xmin, float _xmax, float _ymax, float zproj, float stereo) {
        if(stereo == 0f && _xmin == -_xmax) {
            // symmetric case can be simplified
            float len = (float) Math.sqrt(_xmax * _xmax + zproj * zproj);
            float opplen = _xmax / len;
            float adjlen = zproj / len;

            planes[0].normal.set(ViewAxis[0]).scale(opplen);
            Helper.VectorMA(planes[0].normal, adjlen, ViewAxis[1], planes[0].normal);
            
            planes[1].normal.set(ViewAxis[0]).scale(opplen);
            Helper.VectorMA(planes[1].normal, -adjlen, ViewAxis[1], planes[1].normal);
        }

        float len = (float)Math.sqrt(_ymax * _ymax + zproj * zproj);
        float opplen = _ymax / len;
        float adjlen = zproj / len;

        planes[2].normal.set(ViewAxis[0]).scale(opplen);
        Helper.VectorMA(planes[2].normal, adjlen, ViewAxis[2], planes[2].normal);

        planes[3].normal.set(ViewAxis[0]).scale(opplen);
        Helper.VectorMA(planes[3].normal, -adjlen, ViewAxis[2], planes[3].normal);

        for (int i= 0; i < 4; i++) {
            planes[i].dist = Vector3f.dot(Origin, planes[i].normal);

        }
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
