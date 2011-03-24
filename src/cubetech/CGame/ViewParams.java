package cubetech.CGame;

import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
class ViewParams {
    public Vector2f Origin  = new Vector2f();
    public int ViewportX;
    public int ViewportY;
    public int ViewportWidth;
    public int ViewportHeight;
    public int FovX;
    public int FovY;
    public Matrix ProjectionMatrix;
    public Vector2f Angles = new Vector2f();

    public void SetupProjection() {
        // Use fovx and fovy to set up a matrix
        Vector2f vidSize = Ref.glRef.GetResolution();
        float aspect = vidSize.y/vidSize.x;
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
            setup3DProjection(90 * aspect, aspect, near, far);
        else
            GL11.glOrtho(-FovX/2f, FovX/2f, -FovX*aspect*0.5f, FovX*aspect*0.5f, near,far);

        
        
//        GL11.glTranslatef(-Origin.x, -Origin.y, 1);
        float z = 1;
        if(Ref.cgame.cg_viewmode.iValue == 1)
            z = ((float)Math.sin(Ref.client.realtime / 250f) + 1) * 50f;
        GLU.gluLookAt(Origin.x, Origin.y, z, Origin.x, Origin.y, 0, 0, 1, 0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
//        GL11.glRotatef((float)Math.PI/2f, 0, 1, 0);
//        GL11.glRotatef(Ref.game.level.time/100f, 0, 0, 1);
    }

    private void setup3DProjection(float fov, float aspect, float znear, float zfar) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(fov, aspect, znear, zfar);
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
