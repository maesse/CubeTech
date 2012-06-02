package cubetech.CGame;

import cubetech.collision.CollisionResult;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.gfx.Light;
import cubetech.gfx.RenderList;
import cubetech.misc.Plane;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class ViewParams {
    public FloatBuffer viewbuffer = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final FloatBuffer projbuffer = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    public static final float[] flipMatrix = new float[] {
        // convert from our coordinate system (looking down X)
	// to OpenGL's coordinate system (looking down -Z)
	0, 0, -1, 0,
	-1, 0, 0, 0,
	0, 1, 0, 0,
	0, 0, 0, 1};
    
    
    public Vector3f Origin  = new Vector3f();
    public Vector3f Angles = new Vector3f();
    
    public int ViewportX;
    public int ViewportY;
    public int ViewportWidth;
    public int ViewportHeight;
    public float FovX;
    public float FovY;
    public float farDepth;
    public float nearDepth;
    boolean forceVerticalFOVLock = false;
    
    public Matrix4f ProjectionMatrix;
    private Matrix4f WeaponProjection = null; // lazy init
    
    public Matrix4f viewMatrix = new Matrix4f();
    public Vector3f[] ViewAxis = new Vector3f[3];
    public boolean forceViewAxis = false; // when true, viewaxis has been set for us (instead of angles)
    private float[] view = new float[16];
    public Plane[] planes = new Plane[4];
    
    public ArrayList<Light> lights = new ArrayList<Light>();
    public RenderList renderList = null;
    
    public ViewParams() {
        ViewAxis[0] = new Vector3f(1,0,0);
        ViewAxis[1] = new Vector3f(0,1,0);
        ViewAxis[2] = new Vector3f(0,0,1);
        
        for (int i= 0; i < planes.length; i++) {
            planes[i] = new Plane(0, 0, 0, 0);
        }
    }
    
    public static ViewParams createFullscreenOrtho() {
        ViewParams view = new ViewParams();
        view.CalcVRect(SliceType.NONE, 0,100);
        view.setupOthoProjection(Ref.glRef.GetResolution().x);
        
//        GL11.glMatrixMode(GL11.GL_PROJECTION);
//        GL11.glLoadIdentity();
//        GL11.glOrtho(0, (int)Ref.glRef.GetResolution().x, 0, (int)Ref.glRef.GetResolution().y, 1,-1000);
//
//        GLState.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadMatrix(view.viewbuffer);
        
        return view;
    }

    public void setupOthoProjection(float fovx) {
        float aspect = (float)ViewportHeight/ViewportWidth;
        FovX = fovx;
        FovY = (FovX*aspect);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        GL11.glOrtho(0, fovx, 0, FovY, 1, -1000);
        ProjectionMatrix = readOpenGLProjection(ProjectionMatrix);

        if(!forceViewAxis) {
            ViewAxis = Helper.AnglesToAxis(Angles, ViewAxis);
        }

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
    
    
    public Matrix4f getWeaponProjection() {
        if(WeaponProjection == null) {
            float fov = Ref.cgame.cg_modelfov.fValue;
            WeaponProjection = makeProjectionMatrix(fov, fov*((float)ViewportHeight/ViewportWidth), nearDepth, farDepth, 64, WeaponProjection, true);
        }
        
        return WeaponProjection;
    }
    

    public void SetupProjection() {
        lights.clear();
        // Use fovx and fovy to set up a matrix
        
        float aspect = (float)ViewportHeight/ViewportWidth;
        if(forceVerticalFOVLock) {
            // custom HOR+ scaling
            float baseScale = 3f/4f;
            float baseXFov = Ref.cgame.cg_fov.fValue;
            float baseYFov = baseXFov * baseScale;
            
            FovY = baseYFov;
            float maxY = 120f/((float)ViewportWidth/ViewportHeight);
            if(maxY < FovY) FovY = maxY;
            FovX = (FovY * ((float)ViewportWidth/ViewportHeight));
        } else {
            FovX = Ref.cgame.cg_fov.iValue;
            FovY = (FovX*aspect);
        }

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        
        int near = Ref.cvars.Find("cg_depthnear").iValue;
        int far = Ref.cvars.Find("cg_depthfar").iValue;
        nearDepth = near;
        farDepth = far;
        
        float fov = FovX;
        if(Ref.cgame.cg.playingdemo && Ref.cgame.cg_freecam.isTrue()) {
            fov = Ref.cgame.cg.demofov;
        }

        if(!forceViewAxis) {
            ViewAxis = Helper.AnglesToAxis(Angles, ViewAxis);
        }
        
        setup3DProjection(fov, aspect, 64, near, far);
        
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
    
    public void apply() {
        // set it for opengl
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadMatrix(viewbuffer);
        
        applyOpenGLProjection(ProjectionMatrix);
        
        GL11.glViewport(ViewportX, ViewportY, ViewportWidth, ViewportHeight);
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
            projbuffer.clear();
        } else {
            ProjectionMatrix = makeProjectionMatrix(fov, fov*aspect, znear, zfar, zproj, ProjectionMatrix, true);
            applyOpenGLProjection(ProjectionMatrix);
        }
    }
    
    private static void applyOpenGLProjection(Matrix4f m) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        m.store(projbuffer);
        projbuffer.flip();
        GL11.glLoadMatrix(projbuffer);
        projbuffer.clear();
    }
    
    private static Matrix4f readOpenGLProjection(Matrix4f dest) {
        if(dest == null) dest = new Matrix4f();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projbuffer);
        dest.load(projbuffer);
        projbuffer.clear();
        return dest;
    }
    
    private Matrix4f makeProjectionMatrix(float fovx, float fovy, float znear, float zfar, float zproj, Matrix4f dest, boolean makeFrustum) {
        if(dest == null) dest = new Matrix4f();

        float _ymax = (float) (zproj * Math.tan(fovy * Math.PI / 360f));
        float _ymin = -_ymax;
        float _xmax = (float) (zproj * Math.tan(fovx * Math.PI / 360f));
        float _xmin = -_xmax;
        float _width = _xmax - _xmin;
        float _height = _ymax - _ymin;
        float stereoSep = 0f;
        float depth = zfar - znear;

        dest.m00 = 2f * zproj / _width;
        dest.m10 = 0;
        dest.m20 = (_xmax + _xmin + 2f * stereoSep) / _width;
        dest.m30 = 2f * zproj * stereoSep / _width;

        dest.m01 = 0;
        dest.m11 = 2f * zproj / _height;
        dest.m21 = (_ymax + _ymin) / _height;
        dest.m31 = 0;

        dest.m02 = 0;
        dest.m12 = 0;
        dest.m22 = -(zfar + znear) / depth;
        dest.m32 = -2f * zfar * znear / depth;

        dest.m03 = 0;
        dest.m13 = 0;
        dest.m23 = -1;
        dest.m33 = 0;
        
        if(makeFrustum) setupFrustum(_xmin, _xmax, _ymax, zproj, stereoSep);
        
        return dest;
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
    
    public enum SliceType {
        NONE,
        VERTICAL,
        HORIZONAL,
        BOTH
    }
    
    public float rectXScale = 0f;
    public float rectWidthScale = 1f;
    public float rectYScale = 0f;
    public float rectHeightScale = 1f;

    // slices= 0: fullscreen, 1: split left and right, 2: split into 4
    // sliceIndex: 0-3. 0=topleft, 1=topright, etc.. 
    // viewsize: 0-100%
    public void CalcVRect(SliceType sliceType, int sliceIndex, int viewSize) {
        int size = viewSize;
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
        
        switch(sliceType) {
            case VERTICAL:
                ViewportWidth /= 2;
                rectWidthScale = 0.5f;
                if(sliceIndex == 1) { 
                    ViewportX += ViewportWidth;
                    rectXScale = 0.5f;
                }
                break;
            case HORIZONAL:
                ViewportHeight /= 2;
                rectHeightScale = 0.5f;
                if(sliceIndex == 0) {
                    ViewportY += ViewportHeight;
                    rectYScale = 0.5f;
                }
                break;
            case BOTH:
                ViewportWidth /= 2;
                ViewportHeight /= 2;
                rectHeightScale = 0.5f;
                rectWidthScale = 0.5f;
                switch(sliceIndex) {
                    case 0:
                        ViewportY += ViewportHeight;
                        rectYScale = 0.5f;
                        break;
                    case 1:
                        ViewportY += ViewportHeight;
                        ViewportX += ViewportWidth;
                        rectYScale = 0.5f;
                        rectXScale = 0.5f;
                        break;
                    case 3:
                        ViewportX += ViewportWidth;
                        rectXScale = 0.5f;
                        break;
                }
                break;
        }
        

        GL11.glViewport(ViewportX, ViewportY, ViewportWidth, ViewportHeight);
    }

    void offsetThirdPerson() {
        // Limit pitch
        Vector3f focusAngle = new Vector3f(Angles);
        if(focusAngle.x > 90) focusAngle.x = 90; // don't go too far overhead

        // Get forward vector
        Vector3f forward = new Vector3f();
        Helper.AngleVectors(focusAngle, forward, null, null);

        // Figure out where the player is looking
        float focusDistance = 200;
        Vector3f focusPoint = Helper.VectorMA(Origin, focusDistance, forward, null);
        
        Vector3f t_forward = new Vector3f(), t_right = new Vector3f(), t_up = new Vector3f();
        Helper.AngleVectors(Angles, t_forward, t_right, t_up);

        Vector3f neworigin = new Vector3f(Origin);
        neworigin.z += 35;
        Helper.VectorMA(neworigin, 65, t_right, neworigin);
        float thirdPersonAngle = 0;
        float thirdPersonRange = 100;
        float forwardScale = (float)Math.cos(thirdPersonAngle/180.0f * Math.PI);
        float sideScale = (float)Math.sin(thirdPersonAngle/180.0f * Math.PI);
        Helper.VectorMA(neworigin, -thirdPersonRange * forwardScale, t_forward, neworigin);
        Helper.VectorMA(neworigin, -thirdPersonRange * sideScale, t_right, neworigin);
        
        // Try to figure out exactly where the weapon is aiming
        Vector3f end = Helper.VectorMA(Origin, focusDistance, t_forward, null);
        CollisionResult res = Ref.cgame.Trace(Origin, end, null, null, Content.MASK_SHOT, Ref.cgame.cg.cur_lc.clientNum);
        if(res.hit) {
            Vector3f hit = res.getPOI(null);
            
            focusPoint.set(hit);
            if(true) {
                // Debug focus point
                RenderEntity ent = Ref.render.createEntity(REType.BBOX);
                ent.flags = RenderEntity.FLAG_NOLIGHT | RenderEntity.FLAG_NOSHADOW;
                ent.origin.set(focusPoint);
                Ref.render.addRefEntity(ent);
            }
        }
        
        Origin.set(neworigin);
        
        
        float maxForceLength = 100; // apply full power at this length
        float acceleration = 10;
        
        
        if(Ref.cgame.cg.cur_lc.lastFocusPoint != null) {
            Vector3f lastFocus = new Vector3f(Ref.cgame.cg.cur_lc.lastFocusPoint);
            
            Vector3f focusDelta = Vector3f.sub(focusPoint, lastFocus,  null);
            float deltaLength = focusDelta.length();
            float maxLength = 3000;
            if(deltaLength < maxLength && deltaLength != 0.0) {
                float linForceFrac = Helper.Clamp(deltaLength/maxForceLength, 0, 1);
                float smoothFrac = Helper.SimpleSpline(linForceFrac) * 0.5f;

                Helper.VectorMA(lastFocus, smoothFrac, focusDelta, focusPoint);
            }
            Ref.cgame.cg.cur_lc.lastFocusPoint.set(focusPoint);
        } else {
            Ref.cgame.cg.cur_lc.lastFocusPoint = new Vector3f(focusPoint);
        }
        
        
        Vector3f.sub(focusPoint, Origin, focusPoint);
        
        focusPoint.normalise();
        Helper.VectorToAngles(focusPoint, Angles);
    }
    
}
