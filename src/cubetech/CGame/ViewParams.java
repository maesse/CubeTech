package cubetech.CGame;

import cubetech.common.Helper;
import cubetech.misc.Plane;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.omg.PortableServer.POA;

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
    public Matrix ProjectionMatrix;
    public Vector3f Angles = new Vector3f();
    public Vector3f[] ViewAxis = new Vector3f[3];

    public float xmin, xmax, ymin, ymax;
    public float w, h;
    public float farDepth;

    public Plane[] planes = new Plane[6];

    private FloatBuffer viewbuffer = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private FloatBuffer projbuffer = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();

    public ViewParams() {
        for (int i= 0; i < 3; i++) {
            ViewAxis[i] = new Vector3f();
        }

    }

    public void SetupProjection() {
        // Use fovx and fovy to set up a matrix
        Vector2f vidSize = Ref.glRef.GetResolution();
        float aspect = vidSize.y/vidSize.x;

        float maxfov = Ref.cvars.Find("camera_maxfov").fValue;
        float minfov = Ref.cgame.cg_fov.fValue;
        float maxspeed = Ref.cvars.Find("camera_maxspeed").fValue;
        float currentspeed = Ref.cgame.speed;

        if(currentspeed > maxspeed)
            currentspeed = maxspeed;

        float speedFrac = currentspeed / maxspeed;
        float fovadd = (maxfov-minfov) * speedFrac;

        float fovChangeLimiter =  Ref.cvars.Find("camera_zoomspeed").fValue * (Ref.client.frame_msec / 1000f);
        
        float fovChange = (minfov + fovadd) - Ref.cgame.lastFov;
        float fovChangeFrac = Math.abs(fovChange) / 10f;
        fovChangeLimiter += fovChangeLimiter * fovChangeFrac;
        boolean neg = fovChange < 0;
        if(fovChangeLimiter < Math.abs(fovChange)) {
            fovChange = fovChangeLimiter;
        if(neg)
            fovChange *= -1f;
        }
        

        
        FovX = (int)(Ref.cgame.lastFov + fovChange);

        Ref.cgame.lastFov = Ref.cgame.lastFov + fovChange;
        if(Ref.cgame.cg_editmode.iValue == 1)
            FovX = Ref.cgame.cg_fov.iValue;
        FovY = (int)(FovX*aspect);
//        this.VisibleSize = new Vector2f(width, width * aspect);
//        DefaultSize = new Vector2f(VisibleSize.x, VisibleSize.y);
//        this.Position = position;
//        if(this.Position.y < 0f)
//            this.Position.y = 0;
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        int near = Ref.cvars.Find("cg_depthnear").iValue;
        int far = Ref.cvars.Find("cg_depthfar").iValue;
//        if(Ref.cvars.Find("cg_editmode").iValue == 1) {
//            near = Ref.cvars.Find("edit_nearlayer").iValue;
//            far = Ref.cvars.Find("edit_farlayer").iValue;
//        }
        if(Ref.cgame.cg_viewmode.iValue == 1)
            setup3DProjection(Ref.cgame.cg_fov.fValue * aspect, aspect, near, far);
        else if(Ref.cgame.cg_editmode.iValue == 0) {
            float left = Ref.cvars.Find("camera_hplayerpos").fValue;

            float vleft = Ref.cvars.Find("camera_vplayerpos").fValue;
            float verticalVelocity = Ref.cgame.cg.predictedPlayerState.velocity.y;
            float vsnapmin = Ref.cvars.Find("camera_vsnapmin").fValue;
            float absVel = Math.abs(verticalVelocity);
//            if(absVel > vsnapmin) {
                float vsnapmax = Ref.cvars.Find("camera_vsnapmax").fValue;
                float frac = 0f;
                if(absVel > vsnapmax)
                    frac = 1f;
                else
                    frac = (absVel-vsnapmin)/(vsnapmax-vsnapmin);

                if(absVel < vsnapmin)
                    frac = 0.2f;

                if(frac < 0)
                    frac = 0;
                if(frac > 1)
                    frac = 1;

                float maxChange;
                if(verticalVelocity > 0) {
                    maxChange = vleft - 0.1f;
            }
                else
                    maxChange = 0.8f - vleft;
                
                if(maxChange > 1)
                    maxChange = 1;
                if(maxChange < 0)
                    maxChange = 0;

                float change = -1f;
                if(verticalVelocity > 0f) {
                    change = 0.5f;
                    if(Ref.cgame.lastVleft < vleft)
                        change = 1f;
                }

                change *= maxChange * frac;

//                float vChangeLimiter = 1f * 0.01f;
//                float changeFromLastFrame = (vleft + change) - Ref.cgame.lastVleft;
//                neg = changeFromLastFrame < 0f;
//                if(vChangeLimiter < Math.abs(changeFromLastFrame)) {
//                    changeFromLastFrame = vChangeLimiter;
//
//                    if(neg)
//                        changeFromLastFrame *= -1f;
//                }

                //
                vleft =  (vleft + change);

                float delta = vleft - Ref.cgame.lastVleft;
                float maxVChange = frac * 0.5f* (Ref.client.frame_msec/1000f);
                if(Math.abs(delta) > maxVChange) {
                    if(delta > 0f)
                        delta = maxVChange;
                    else
                        delta = -maxVChange;
                }
                //vleft = Ref.cgame.lastVleft + changeFromLastFrame;
                Ref.cgame.lastVleft += delta;
                vleft = Ref.cgame.lastVleft;
                //Ref.cgame.lastVleft = vleft;
                //vleft += change;

                

//            }
                xmin = -FovX*left;
                xmax = FovX*(1f-left);
                ymin = -FovX * aspect * (1f - vleft);
                ymax = FovX * aspect * vleft;
                w = xmax-xmin;
                h = ymax - ymin;
            GL11.glOrtho(-FovX*left, FovX*(1f-left), -FovX*aspect*(1f-vleft), FovX*aspect*vleft, near,far);

        } else {
            GL11.glOrtho(-FovX*0.5f, FovX*0.5f, -FovX*aspect*0.5f, FovX*aspect*0.5f, near,far);
        }

        ViewAxis = Helper.AnglesToAxis(Angles);
//        viewbuffer.put(ViewAxis[1].x); viewbuffer.put(ViewAxis[2].x); viewbuffer.put(ViewAxis[0].x); viewbuffer.put(Origin.x);
//        viewbuffer.put(ViewAxis[1].y); viewbuffer.put(ViewAxis[2].y); viewbuffer.put(ViewAxis[0].y); viewbuffer.put(Origin.y);
//        viewbuffer.put(ViewAxis[1].z);viewbuffer.put(ViewAxis[2].z);viewbuffer.put(ViewAxis[0].z); viewbuffer.put(0);
//        viewbuffer.put(0); viewbuffer.put(0); viewbuffer.put(0); viewbuffer.put(1);


        int derp = 0;
        
        

//        ViewAxis[1].set(-1,0,0);
//        ViewAxis[2].set(0,1,0);
//        ViewAxis[0].set(0,0,1);

        Vector3f org = new Vector3f(-Origin.x, -Origin.y, -Origin.z);
        ViewAxis[0].scale(-1);
        ViewAxis[1].scale(-1);
        
        viewbuffer.put(ViewAxis[1].x); viewbuffer.put(ViewAxis[2].x); viewbuffer.put(ViewAxis[0].x); viewbuffer.put(0);
        viewbuffer.put(ViewAxis[1].y); viewbuffer.put(ViewAxis[2].y); viewbuffer.put(ViewAxis[0].y); viewbuffer.put(0);
        viewbuffer.put(ViewAxis[1].z); viewbuffer.put(ViewAxis[2].z); viewbuffer.put(ViewAxis[0].z); viewbuffer.put(0);
        viewbuffer.put(Vector3f.dot(ViewAxis[1], org)); viewbuffer.put(Vector3f.dot(ViewAxis[2], org)); viewbuffer.put(Vector3f.dot(ViewAxis[0], org));
        viewbuffer.put(1);

//        viewbuffer.put(ViewAxis[1].x); viewbuffer.put(ViewAxis[1].y); viewbuffer.put(ViewAxis[1].z); // right
//        viewbuffer.put(0);
//        viewbuffer.put(ViewAxis[2].x); viewbuffer.put(ViewAxis[2].y); viewbuffer.put(ViewAxis[2].z); // up
//        viewbuffer.put(0);
//        viewbuffer.put(ViewAxis[0].x); viewbuffer.put(ViewAxis[0].y); viewbuffer.put(ViewAxis[0].z); // forward
//        viewbuffer.put(0);
//        //viewbuffer.put(0);viewbuffer.put(0);viewbuffer.put(000);viewbuffer.put(1);
//        viewbuffer.put(Origin.x-400); viewbuffer.put(Origin.y-200); viewbuffer.put(200); viewbuffer.put(1f);
        viewbuffer.flip();
//        GL11.glTranslatef(-Origin.x, -Origin.y, 1);
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        
        GL11.glLoadMatrix(viewbuffer);

        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projbuffer);
//        projbuffer.flip();
        Matrix4f prov = (Matrix4f) new Matrix4f().load(projbuffer);
        Matrix4f view = (Matrix4f) new Matrix4f().load(viewbuffer);
        viewbuffer.position(0);
        projbuffer.position(0);

        Matrix4f mvp = Matrix4f.mul(view, prov, null);
        
        // create the near plane
        Vector3f derps = new Vector3f(ViewAxis[0]);
        derps.scale(-1f);
        float d = Vector3f.dot(derps, Origin);

        planes[0] = new Plane(derps.x, derps.y, derps.z, d).normalize();
        //planes[0] = new Plane(mvp.m20 + mvp.m30, mvp.m21 + mvp.m31, mvp.m22 + mvp.m32, mvp.m23 + mvp.m33).normalize();
        //planes[0] = new Plane(mvp.m02 + mvp.m03, mvp.m12 + mvp.m13, mvp.m32 + mvp.m33, mvp.m22 + mvp.m23).normalize();

        // normalize the plane
        
//        float z = 1;
//        if(Ref.cgame.cg_viewmode.iValue == 1) {
//            // First person view
//            z = ((float)Math.sin(Ref.client.realtime / 250f) + 1) * 2f + FovX;
//            GLU.gluLookAt(0, 0, 0, ViewAxis[0].x, ViewAxis[0].y, ViewAxis[0].z, ViewAxis[1].x, ViewAxis[1].y, ViewAxis[1].z);
//        } else {
//            GLU.gluLookAt(Origin.x, Origin.y, z, Origin.x, Origin.y, 0, 0, 1, 0);
//        }
//        GL11.glLoadIdentity();

//        GL11.glRotatef((float)Math.PI/2f, 0, 1, 0);
//        GL11.glRotatef(Ref.game.level.time/100f, 0, 0, 1);
    }

    private void setup3DProjection(float fov, float aspect, float znear, float zfar) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(fov, 1f/aspect, znear, zfar);
        farDepth = zfar;

        return;
//        float xymax = znear * (float)Math.tan(fov * (Math.PI/360f));
//        float ymin = -xymax;
//        float xmin = -xymax;
//
//        float width = xymax - xmin;
//        float height = xymax - ymin;
//
//        float depth = zfar - znear;
//        float q = -(zfar + znear) / depth;
//        float qn = -2 * (zfar * znear) / depth;
//
//        float w = 2 * znear / width;
//        w = w / aspect;
//        float h = 2 * znear / height;
//
//
//        FloatBuffer m = ByteBuffer.allocateDirect(16*4).asFloatBuffer();
//        m.put(w);
//        m.put(0);
//        m.put(0);
//        m.put( 0);
//
//        m.put( 0);
//        m.put( h);
//        m.put(0);
//       m.put(0);
//
//        m.put(0);
//        m.put(0);
//        m.put(q);
//        m.put(-1);
//
//       m.put(0);
//        m.put(0);
//       m.put(qn);
//        m.put(0);
//
//        m.flip();
//        GL11.glLoadMatrix(m);
        
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
    
}
