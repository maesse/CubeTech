package cubetech.gfx;

import cubetech.CGame.ViewParams;
import cubetech.misc.Ref;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class SkyBox {
    CubeTexture fw, bk; // x
    CubeTexture rt, lf; // y
    CubeTexture up, dn; // z
    public SkyBox(String skyname) {
        // Try to load the textures
        fw = Ref.ResMan.LoadTexture(skyname+ "_ft.png");
        fw.setWrap(GL12.GL_CLAMP_TO_EDGE);
        bk = Ref.ResMan.LoadTexture(skyname+ "_bk.png");
        bk.setWrap(GL12.GL_CLAMP_TO_EDGE);
        rt = Ref.ResMan.LoadTexture(skyname+ "_rt.png");
        rt.setWrap(GL12.GL_CLAMP_TO_EDGE);
        lf = Ref.ResMan.LoadTexture(skyname+ "_lf.png");
        lf.setWrap(GL12.GL_CLAMP_TO_EDGE);
        up = Ref.ResMan.LoadTexture(skyname+ "_up.png");
        up.setWrap(GL12.GL_CLAMP_TO_EDGE);
        dn = Ref.ResMan.LoadTexture(skyname+ "_dn.png");
        dn.setWrap(GL12.GL_CLAMP_TO_EDGE);
    }

    public void Render(ViewParams view) {
        

        // Render thyme!
        Vector4f tx = new Vector4f(0,0,1,1);
        float radius = (int) (view.farDepth * 0.55f);
        GL11.glCullFace(GL11.GL_FRONT);
        GL11.glDepthMask(false);
        float offset = 0.2f;
        view.Origin.z += radius*offset;
        // Top: Z+
        up.Bind();
        GL11.glBegin(GL11.GL_QUADS);
        {
            tex(tx.x, tx.w);
            GL11.glVertex3f(view.Origin.x - radius,view.Origin.y - radius,view.Origin.z + radius);
            tex(tx.z, tx.w);
            GL11.glVertex3f(view.Origin.x + radius,view.Origin.y - radius,view.Origin.z + radius);
            tex(tx.z, tx.y);
            GL11.glVertex3f(view.Origin.x + radius,view.Origin.y + radius,view.Origin.z + radius);
            tex(tx.x, tx.y);
            GL11.glVertex3f(view.Origin.x - radius,view.Origin.y + radius,view.Origin.z + radius);
        }
        GL11.glEnd();

        // Bottom: Z-
        dn.Bind();
        GL11.glBegin(GL11.GL_QUADS);
        {
            tex(tx.x, tx.y);
            GL11.glVertex3f(view.Origin.x - radius,view.Origin.y - radius,view.Origin.z - radius );
            tex(tx.x, tx.w);
            GL11.glVertex3f(view.Origin.x - radius,view.Origin.y + radius,    view.Origin.z - radius);
            tex(tx.z, tx.w);
            GL11.glVertex3f(view.Origin.x + radius,view.Origin.y + radius,    view.Origin.z - radius );
            tex(tx.z, tx.y);
            GL11.glVertex3f(view.Origin.x + radius,view.Origin.y - radius,view.Origin.z - radius);
        }
        GL11.glEnd();

        // Y+
        rt.Bind();
        GL11.glBegin(GL11.GL_QUADS);
        {
            tex(tx.x, tx.y);
            GL11.glVertex3f(view.Origin.x - radius,view.Origin.y + radius,     view.Origin.z - radius );
            tex(tx.x, tx.w);
            GL11.glVertex3f(view.Origin.x - radius,view.Origin.y + radius,    view.Origin.z + radius);
            tex(tx.z, tx.w);
            GL11.glVertex3f(view.Origin.x + radius,view.Origin.y + radius,    view.Origin.z + radius);
            tex(tx.z, tx.y);
            GL11.glVertex3f(view.Origin.x + radius,view.Origin.y + radius,     view.Origin.z - radius);
        }
        GL11.glEnd();

        // Y-
        lf.Bind();
        GL11.glBegin(GL11.GL_QUADS);
        {
            tex(tx.z, tx.y);
            GL11.glVertex3f(view.Origin.x - radius,view.Origin.y - radius,     view.Origin.z - radius );
            tex(tx.x, tx.y);
            GL11.glVertex3f(view.Origin.x + radius,view.Origin.y - radius,     view.Origin.z - radius);
            tex(tx.x, tx.w);
            GL11.glVertex3f(view.Origin.x + radius,view.Origin.y - radius ,    view.Origin.z + radius);
            tex(tx.z, tx.w);
            GL11.glVertex3f(view.Origin.x - radius,view.Origin.y - radius,    view.Origin.z + radius);
        }
        GL11.glEnd();

        // X+
        fw.Bind();
        GL11.glBegin(GL11.GL_QUADS);
        {
            tex(tx.z, tx.y);
            GL11.glVertex3f(view.Origin.x + radius, view.Origin.y - radius,view.Origin.z - radius );
            tex(tx.x, tx.y);
            GL11.glVertex3f(view.Origin.x + radius ,view.Origin.y + radius,view.Origin.z - radius);
            tex(tx.x, tx.w);
            GL11.glVertex3f(view.Origin.x + radius ,view.Origin.y + radius,view.Origin.z + radius);
            tex(tx.z, tx.w);
            GL11.glVertex3f(view.Origin.x + radius, view.Origin.y - radius,view.Origin.z + radius);
        }
        GL11.glEnd();

        // X-
        bk.Bind();
        GL11.glBegin(GL11.GL_QUADS);
        {
            tex(tx.x, tx.y);
            GL11.glVertex3f(view.Origin.x - radius, view.Origin.y - radius,view.Origin.z - radius );
            tex(tx.x, tx.w);
            GL11.glVertex3f(view.Origin.x - radius, view.Origin.y - radius,view.Origin.z + radius);
            tex(tx.z, tx.w);
            GL11.glVertex3f(view.Origin.x - radius ,view.Origin.y + radius,view.Origin.z + radius);
            tex(tx.z, tx.y);
            GL11.glVertex3f(view.Origin.x - radius ,view.Origin.y + radius,view.Origin.z - radius);
        }
        GL11.glEnd();
        GL11.glDepthMask(true);
        view.Origin.z -= radius*offset;
        GL11.glCullFace(GL11.GL_BACK);
        GLRef.checkError();
    }

    private void tex(float x, float y) {
        if(Ref.glRef.isShadersSupported())
            GL20.glVertexAttrib2f(2, x, y);
        else
            GL11.glTexCoord2f(x, y);
    }
}
