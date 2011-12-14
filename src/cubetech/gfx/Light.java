package cubetech.gfx;

import cubetech.CGame.ViewParams;
import cubetech.common.Helper;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class Light {

    public static Light skylight = Directional(new Vector3f(0, 0, -1));

    public enum Type {

        DIRECTIONAL,
        POINT
    }
    // Light properties
    private Type lightType;
    private Vector3f position = new Vector3f(); // for point lights
    private Vector3f direction = new Vector3f(); // for directional and spot
    private float radius = 200f;
    private boolean castShadow = false;
    private int shadowQuality = 2;
    // Color
    private Vector3f diffuse = new Vector3f(1, 1, 1);
    private Vector3f ambient = new Vector3f();
    private Vector3f specular = new Vector3f(1, 1, 1);
    private ShadowResult shadowResult = null; // rendered shadowmaps
    // maintain light matrices
    private boolean invalid = true; // true when lightmatrix and light axis needs update
    private Matrix4f lightMatrix;
    private Vector3f[] lightAxis;
    private Vector4f scissorRect = new Vector4f();

    public Light(Type type) {
        this.lightType = type;
        lightMatrix = new Matrix4f();
        lightAxis = new Vector3f[3];
        for (int i = 0; i < lightAxis.length; i++) {
            lightAxis[i] = new Vector3f();
        }
    }

    public void setDiffuse(Vector3f diffuse) {
        this.diffuse.set(diffuse);
    }

    public Vector3f getDiffuse() {
        return diffuse;
    }

    public void setSpecular(Vector3f specular) {
        this.specular.set(specular);
    }

    public Vector3f getSpecular() {
        return specular;
    }
    
    public float getRadius() {
        return radius;
    }
    
    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void enableShadowCasting(boolean enable) {
        castShadow = enable;
    }

    public boolean isCastingShadows() {
        return castShadow;
    }

    public int getShadowQuality() {
        return shadowQuality;
    }

    public void setShadowQuality(int quality) {
        shadowQuality = quality;
        if (shadowQuality < 0) {
            shadowQuality = 0;
        }
        if (shadowQuality > 2) {
            shadowQuality = 2;
        }
    }

    public Matrix4f getLightMatrix() {

        updateMatrices();
        return lightMatrix;
    }

    public Vector3f[] getLightAxis() {
        updateMatrices();
        return lightAxis;
    }

    public Type getType() {
        return lightType;
    }

    public void setDirection(Vector3f direction) {
        this.direction.set(direction);
        this.direction.normalise();
        invalid = true;
    }

    public Vector3f getDirection() {
        return direction;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    private void updateMatrices() {
        if (!invalid) {
            return;
        }
//        float x = (float)Math.cos(Ref.client.realtime/100000f);
//        float y = (float)Math.sin(Ref.client.realtime/100000f);
//        setDirection(new Vector3f(x, y, -1f));
        if (lightType == Type.DIRECTIONAL) {
            // Create view matrix for light
            lightAxis = Helper.DirectionToAxis(direction, new Vector3f(0, 0, 1), lightAxis);
            lightMatrix = Helper.axisToMatrix(lightAxis, lightMatrix);
            lightMatrix.transpose();
        }
        invalid = false;
    }

    public static Light Directional(Vector3f direction) {
        Light light = new Light(Type.DIRECTIONAL);
        light.direction.set(direction);
        light.direction.normalise();
        light.castShadow = true;
        return light;
    }

    public static Light Point(Vector3f origin) {
        Light light = new Light(Type.POINT);
        light.position.set(origin);
        return light;
    }

    public void setShadowResult(ShadowResult result) {
        shadowResult = result;
    }
    
    public Vector4f getScissor() {
        return scissorRect;
    }

    public int calculateScissor(ViewParams view) {
        int sx = (int)Ref.glRef.GetResolution().x;
        int sy = (int)Ref.glRef.GetResolution().y;
        int[] rect={ 0,0,sx,sy };
        float d;
        float r = radius;
        float r2 = r * r;
        // Transform light position to viewspace
        Vector4f viewpos = new Vector4f(position.x, position.y, position.z, 1.0f);
        Matrix4f.transform(view.viewMatrix, viewpos, viewpos);
        if(viewpos.z > radius) {
            // Behind viewer
            return 0;
        }
        Vector3f l = new Vector3f(viewpos);
        Vector3f l2 = new Vector3f(l.x*l.x, l.y*l.y,l.z*l.z);

        float e1 = 1.2f;
        float aspect = (float)sx/sy;
        float e2 = 1.2f * aspect;
        d = r2 * l2.x - (l2.x + l2.z) * (r2 - l2.z);
        if (d >= 0) {
            d = (float)Math.sqrt(d);
            float nx1 = (r * l.x + d) / (l2.x + l2.z);
            float nx2 = (r * l.x - d) / (l2.x + l2.z);
            float nz1 = (r - nx1 * l.x) / l.z;
            float nz2 = (r - nx2 * l.x) / l.z;

            float e = 1.25f;
            float a = aspect;
            float pz1 = (l2.x + l2.z - r2) / (l.z - (nz1 / nx1) * l.x);
            float pz2 = (l2.x + l2.z - r2) / (l.z - (nz2 / nx2) * l.x);

            if (pz1 < 0) {
                float fx = nz1 * e1 / nx1;
                int ix = (int) ((fx + 1.0f) * sx * 0.5f);
                float px = -pz1 * nz1 / nx1;
                if (px < l.x) {
                    rect[0] = Math.max(rect[0], ix);
                } else {
                    rect[2] = Math.min(rect[2], ix);
                }
            }

            if (pz2 < 0) {
                float fx = nz2 * e1 / nx2;
                int ix = (int) ((fx + 1.0f) * sx * 0.5f);
                float px = -pz2 * nz2 / nx2;
                if (px < l.x) {
                    rect[0] = Math.max(rect[0], ix);
                } else {
                    rect[2] = Math.min(rect[2], ix);
                }
            }
        }

        d = r2 * l2.y - (l2.y + l2.z) * (r2 - l2.z);
        if (d >= 0) {
            d = (float)Math.sqrt(d);

            float ny1 = (r * l.y + d) / (l2.y + l2.z);
            float ny2 = (r * l.y - d) / (l2.y + l2.z);
            float nz1 = (r - ny1 * l.y) / l.z;
            float nz2 = (r - ny2 * l.y) / l.z;
            float pz1 = (l2.y + l2.z - r2) / (l.z - (nz1 / ny1) * l.y);
            float pz2 = (l2.y + l2.z - r2) / (l.z - (nz2 / ny2) * l.y);

            if (pz1 < 0) {
                float fy = nz1 * e2 / ny1;
                int iy = (int) ((fy + 1.0f) * sy * 0.5f);
                float py = -pz1 * nz1 / ny1;
                if (py < l.y) {
                    rect[1] = Math.max(rect[1], iy);
                } else {
                    rect[3] = Math.min(rect[3], iy);
                }
            }

            if (pz2 < 0) {
                float fy = nz2 * e2 / ny2;
                int iy = (int) ((fy + 1.0f) * sy * 0.5f);
                float py = -pz2 * nz2 / ny2;
                if (py < l.y) {
                    rect[1] = Math.max(rect[1], iy);
                } else {
                    rect[3] = Math.min(rect[3], iy);
                }
            }
        }

        int n = (rect[2] - rect[0]) * (rect[3] - rect[1]);
        if (n <= 0) {
            return 0;
        }
        if (n == sx * sy) {
            //GL11.glDisable(GL11.GL_SCISSOR_TEST);
            scissorRect.set(0,0,sx,sy);
            return sx * sy;
        }
        scissorRect.set(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]);
        //GL11.glScissor(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]);
        //GL11.glEnable(GL11.GL_SCISSOR_TEST);
        return n;

    }

    public ShadowResult getShadowResult() {
        return shadowResult;
    }
}
