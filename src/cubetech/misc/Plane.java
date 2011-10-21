package cubetech.misc;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

/**
 * Just a simple class for a Plane
 * @author mads
 */
public class Plane {
    public Vector3f normal = new Vector3f();
    public float dist;

    public Plane(float a, float b, float c, float d) {
        set(a,b,c,d);
    }

    public final void set(float a, float b, float c, float d) {
        normal.set(a,b,c);
        dist = d;
    }

    public Plane normalize() {
        float len = normal.length();
        if(len == 0)
            return this;
        normal.scale(1f/len);
        dist /= len;
        return this;
    }

    public float findIntersection(Vector3f start, Vector3f delta) {
        // n dot u
        float ddist = Vector3f.dot(delta, normal);
        if(ddist == 0f) return 0f;

        float scalar = (dist - Vector3f.dot(start,normal));
        scalar /= ddist;
        if(scalar > 1.0f) scalar = 1.0f;
            if(scalar < 0) scalar = 0;
        return scalar;
    }

    public float testPoint(float x, float y, float z) {
        float ddist = normal.x * x + normal.y * y + normal.z * z - dist;
        return ddist;
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
        Vector3f dest = new Vector3f();
        float ddist = normal.x * x + normal.y * y + normal.z * z;
        dest.set(x - normal.x * ddist, y - normal.y * ddist, z - normal.z * ddist);
        return dest;
    }

    @Override
    public String toString() {
        return String.format("n:(%.2f,%.2f,%.2f) d:%.2f", normal.x, normal.y, normal.z,dist);
    }
}
