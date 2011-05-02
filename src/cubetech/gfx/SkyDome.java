/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.common.Helper;
import cubetech.misc.Ref;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class SkyDome {

    private static Sphere sph = null;

    public static void updateShader(Shader shader, boolean ground) {
        // vec3
        Vector3f sun = new Vector3f(0.4f, (float) (Math.cos(Ref.client.realtime / 10000f)), (float) (Math.sin(Ref.client.realtime / 10000f)));

        float Kr = 0.0025f;
        float Km = 0.0010f;
        float ESun = 35f;
        Vector3f waveLenght = new Vector3f(0.65f + (0.65f * 14f * 0.01f),
                                           0.57f + (0.57f * 14f * 0.01f),
                                           0.475f + (0.475f * 6f * 0.01f));

        float G = -.99f;
        float scaleDepth = 0.8f;
        float innerRadius = 1500f;
        float outerRadius = 4000f;

        if(ground) {
            ESun = 10;
            innerRadius = 200f;
            outerRadius = 2000;
            scaleDepth = 500f;
        }

        float scale = 1.0f / ((outerRadius) - innerRadius);
        float scaleOverscaleDepth = scale / scaleDepth;
        float KrESun = Kr * ESun;
        float KmESun = Km * ESun;

        float Kr4PI = (float) (Kr * 4.0f * Math.PI);
        float Km4PI = (float) (Km * 4.0f * Math.PI);
        Vector3f invWavelenght = new Vector3f((float)(1.0f / Math.pow(waveLenght.x, 4.0f)),
                                              (float)(1.0f / Math.pow(waveLenght.y, 4.0f)),
                                              (float)(1.0f / Math.pow(waveLenght.z, 4.0f)));
        float camHeight = Ref.cgame.cg.refdef.Origin.z;// - CubeChunk.BLOCK_SIZE * CubeChunk.SIZE * CubeMap.MIN_Z;
        Vector3f campos = Ref.cgame.cg.refdef.Origin;
        if(!ground) {
            campos = new Vector3f(0,0,Ref.cgame.cg.refdef.Origin.z);
        
            if(campos.z < innerRadius)
                campos.z  = innerRadius;
        } else {
//            if(campos.z > outerRadius)
//                campos.z = outerRadius;

        }
        camHeight = campos.z;
//        campos.z = camHeight;
        Vector3f lightpos = Vector3f.sub(sun, campos, null);
        Helper.Normalize(sun);
        shader.setUniform("v3CameraPos", campos); // The camera's current position
        shader.setUniform("v3LightPos", sun); // The direction vector to the light source
        shader.setUniform("v3InvWavelength", invWavelenght); // 1 / pow(wavelength, 4) for the red, green, and blue channels
        // floats
        shader.setUniform("fCameraHeight", camHeight); // The camera's current height
        shader.setUniform("fCameraHeight2", camHeight*camHeight); // fCameraHeight^2
        shader.setUniform("fOuterRadius", outerRadius); // The outer (atmosphere) radius
        shader.setUniform("fOuterRadius2", (outerRadius) * (outerRadius)); // fOuterRadius^2
        shader.setUniform("fInnerRadius", innerRadius); // The inner (planetary) radius
        shader.setUniform("fInnerRadius2", innerRadius * innerRadius); // fInnerRadius^2
        shader.setUniform("fKrESun", KrESun); // Kr * ESun
        shader.setUniform("fKmESun", KmESun);  // Km * ESun
        shader.setUniform("fKr4PI", Kr4PI); // Kr * 4 * PI
        shader.setUniform("fKm4PI", Km4PI); // Km * 4 * PI
        shader.setUniform("fScale", scale); // 1 / (fOuterRadius - fInnerRadius)
        shader.setUniform("fScaleDepth", scaleDepth); // The scale depth (i.e. the altitude at which the atmosphere's average density is found)
        shader.setUniform("fScaleOverScaleDepth", scaleOverscaleDepth); // fScale / fScaleDepth
        shader.setUniform("g", G);
        shader.setUniform("g2", G*G);
    }
    
    public static void RenderDome(Vector3f position, float radius, boolean debug) {
        if(sph == null) {
            sph = new Sphere();
            sph.setOrientation(GLU.GLU_INSIDE);
        }

        if(debug) sph.setDrawStyle(GLU.GLU_LINE);
        else sph.setDrawStyle(GLU.GLU_FILL);

        // Prepare shader
        Shader shader = Ref.glRef.getShader("SkyFromAtmosphere");
        if(shader == null)
            shader = Ref.glRef.shader;
        Ref.glRef.PushShader(shader);

        updateShader(shader, false);

        GL11.glPushMatrix();
        //GL11.glLoadIdentity();
        GL11.glTranslatef(position.x, position.y, CubeMap.MIN_Z*CubeChunk.BLOCK_SIZE*CubeChunk.SIZE);
        sph.draw(radius, 64, 64);
        GL11.glPopMatrix();
        Ref.glRef.PopShader();
    }
}
