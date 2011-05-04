package cubetech.misc;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

/**
 * Just a simple class for a Plane
 * @author mads
 */
public class Plane {
    public float a,b,c,d;

    public Plane(float a, float b, float c, float d) {
        this.a = a; this.b = b; this.c = c; this.d = d;
    }

    public void set(float a, float b, float c, float d) {
        this.a = a; this.b = b; this.c = c; this.d = d;
    }

    public Plane normalize() {
        double len = Math.sqrt(a * a + b * b + c * c);
        if(len == 0)
            return this;
        a /= len;
        b /= len;
        c /= len;
        d /= len;
//        d *= 2f;
        return this;
    }

    public float testPoint(float x, float y, float z) {
        float dist = a * x + b * y + c * z - d;
        return dist;
    }

    public void DebugRender(Vector3f origin) {
        Vector3f min = projectOnPlane(-1000 + origin.x, -1000 + origin.y, -1000 + origin.z);
        Vector3f max = projectOnPlane(1000 + origin.x, 1000 + origin.y, 1000 + origin.z);

        Ref.ResMan.getWhiteTexture().Bind();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBegin(GL11.GL_QUADS);
        

        // Top: Z+
        {
            GL11.glVertex3f(min.x,             min.y,             min.y);
            GL11.glVertex3f(min.x,             min.y,             max.y);
            
            GL11.glVertex3f(max.x,             max.y,             max.y);
            GL11.glVertex3f(min.x,             max.y,             max.y);
        }

        GL11.glEnd();
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    public Vector3f projectOnPlane(float x, float y, float z) {
        Vector3f dest = new Vector3f(x-a*d,y-b*d,z-c*d);
        float dist = a * dest.x + b * dest.y + c * dest.z;
        dest.set(x - a * dist, y - b * dist, z - c * dist);
        return dest;
    }

    @Override
    public String toString() {
        return String.format("n:(%.2f,%.2f,%.2f) d:%.2f", a,b,c,d);
    }
}
