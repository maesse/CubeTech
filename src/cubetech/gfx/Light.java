package cubetech.gfx;

import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class Light {
    Vector4f ambient;
    Vector4f diffuse;
    Vector4f specular;
    Vector4f position;
    Vector4f halfVector;
    Vector3f spotDir;
    float spotExponent;
    float spotCutoff;
    float spotCosCutoff;
    float constantAttenuation;
    float linearAttenuation;
    float quadraticAttenuation;


    public static void test() {
        FloatBuffer buf = ByteBuffer.allocateDirect(4*4).asFloatBuffer();
        buf.put(1f);
        buf.put(1f);
        buf.put(1f);
        buf.put(1f);
        buf.flip();
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, buf);

        buf = ByteBuffer.allocateDirect(4*4).asFloatBuffer();
        //buf.put(1f * ((float)Math.sin(Ref.client.realtime/ 100000f)));
        buf.put(10f);
        buf.put(10f);
        buf.put(100f);
        buf.put(1f);
        
//        buf.flip();
        buf.flip();
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, buf);
        buf = ByteBuffer.allocateDirect(4*4).asFloatBuffer();
        buf.put(1f);
        buf.put(0f);
        buf.put(0f);
        buf.put(0.5f);
        buf.flip();
//        buf.flip();
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_SPECULAR, buf);
        GL11.glLightf(GL11.GL_LIGHT0, GL11.GL_LINEAR_ATTENUATION, 1f);
        GL11.glLightf(GL11.GL_LIGHT0, GL11.GL_CONSTANT_ATTENUATION, 1f);
        GL11.glLightf(GL11.GL_LIGHT0, GL11.GL_QUADRATIC_ATTENUATION, 01f);
    }
}
