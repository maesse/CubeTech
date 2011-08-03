package cubetech.iqm;

import java.util.EnumMap;
import java.util.HashMap;
import cubetech.CGame.ViewParams;
import cubetech.common.Animations;
import cubetech.gfx.GLRef;
import java.nio.FloatBuffer;
import cubetech.gfx.Shader;
import cubetech.common.Common;
import cubetech.gfx.CubeTexture;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Matrix3f;
import cubetech.common.Helper;
import cubetech.misc.Ref;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import static org.lwjgl.opengl.GL11.*;
/**
 *
 * @author mads
 */
public class IQMModel {
    IQMHeader header;
    String comment;
    IQMMesh[] meshes;
    IQMTriangle[] triangles;
    IQMJoint[] joints;
    IQMBounds[] bounds;

    // Temporary data
    IQMVertexArray[] vertexarrays;
    IQMAdjacency[] adjacency;
    int[] framedata;
    byte[] text;
    IQMPose[] poses;
    public IQMAnim[] anims;
    IQMExtension[] extensions;

    Matrix4f[] frames;
    Matrix4f[] outframe;
    Matrix4f[] baseframe;
    Matrix4f[] invbaseframe;

    // Vertex data
    Vector3f[] in_position;
    Vector3f[] in_normal;
    Vector4f[] in_tangent;
    Vector2f[] in_texcoord;
    short[] in_blendindex;
    short[] in_blendweight;

    Vector3f[] out_position;
    Vector3f[] out_normal;
    Vector3f[] out_tangent;
    Vector3f[] out_bitangent;

    IQMAnim currentAnim = null;
    Matrix3f modelMatrix = new Matrix3f();
    CubeTexture envMap = null;
    EnumMap<Animations, IQMAnim> animationMap = new EnumMap<Animations, IQMAnim>(Animations.class);
    Vector3f min = new Vector3f(-32,-32,-32);
    Vector3f max = new Vector3f(32,32,32);

    IQMJoint[] thighJoints = null;


    IQMModel() {
        envMap = Ref.ResMan.LoadTexture("data/ibl_sky", true);
        envMap.textureSlot = 1;
        modelMatrix.setIdentity();
    }

    public IQMAnim getAnimation(Animations animations) {
        return animationMap.get(animations);
    }

    public void destroy() {
//        envMap = null;
    }

    public void animate(int frame, int oldframe, float backlerp) {
        if(header.num_frames <= 5) return;


        int frame1 = oldframe;
        int frame2 = frame;
        float frameOffset = 1f-backlerp;

        int mat1 = frame1 * header.num_joints;
        int mat2 = frame2 * header.num_joints;

        // Interpolate matrixes between the two closest frames and concatenate with parent matrix if necessary.
        // Concatenate the result with the inverse of the base pose.
        // You would normally do animation blending and inter-frame blending here in a 3D engine.
        Matrix4f temp = new Matrix4f();
        for (int i= 0; i < header.num_joints; i++) {
            Matrix4f m1 = frames[mat1+i];
            Matrix4f m2 = frames[mat2+i];
            Matrix4f dest = new Matrix4f();

            Helper.scale(1-frameOffset, m1, dest);
            Helper.scale(frameOffset, m2, temp);
            Matrix4f.add(dest, temp, dest);
            dest.m33 = 1;
            if(joints[i].parent >= 0) {
                Matrix4f.mul(outframe[joints[i].parent], dest, dest);
            }
            outframe[i] = dest;
        }

        // The actual vertex generation based on the matrixes follows...
        int iIndex = 0;
        int iWeight = 0;
        Matrix3f matnorm = new Matrix3f();
        for (int i= 0; i < header.num_vertexes; i++) {
            // Blend matrixes for this vertex according to its blend weights.
            // the first index/weight is always present, and the weights are
            // guaranteed to add up to 255. So if only the first weight is
            // presented, you could optimize this case by skipping any weight
            // multiplies and intermediate storage of a blended matrix.
            // There are only at most 4 weights per vertex, and they are in
            // sorted order from highest weight to lowest weight. Weights with
            // 0 values, which are always at the end, are unused.
            float f = (in_blendweight[iWeight])/255.0f;
            Matrix4f dest = new Matrix4f();
            Helper.scale(f, outframe[in_blendindex[iIndex]], dest);
            for (int j= 1; j < 4 && (in_blendweight[iWeight+j]) != 0; j++) {
                f = (in_blendweight[iWeight+j])/255.0f;
                Helper.scale(f, outframe[in_blendindex[iIndex+j]], temp);
                Matrix4f.add(dest, temp, dest);
                dest.m33 = 1;
            }

            // Transform attributes by the blended matrix.
            // Position uses the full 3x4 transformation matrix.
            // Normals and tangents only use the 3x3 rotation part
            // of the transformation matrix.
            out_position[i] = Helper.transform(dest, in_position[i], out_position[i]);

            matnorm = Helper.toNormalMatrix(dest, matnorm);
            out_normal[i] = Helper.transform(matnorm, in_normal[i], out_normal[i]);
            out_tangent[i] = Helper.transform(matnorm, in_tangent[i], out_tangent[i]);
            out_bitangent[i] = Vector3f.cross(out_normal[i], out_tangent[i], out_bitangent[i]);
            out_bitangent[i].scale(in_tangent[i].w);

            iIndex += 4;
            iWeight += 4;
        }

    }    

    public void render(Vector3f position, Vector3f[] axis, Vector4f color, boolean shadowPass) {
        Shader shader = null;
        if(shadowPass) {
            shader = Ref.glRef.getShader("unlitObject");
        } else {
            shader = Ref.glRef.getShader("litobjectpixel");
        }
        
        Ref.glRef.PushShader(shader);


        if(!shadowPass) {
            // Bind environmentmap
            envMap.textureSlot = 2;
            envMap.Bind();
            // Set light
            Ref.glRef.setLIght();
            CubeTexture depth = Ref.cgame.shadowMan.getDepthTexture();
            depth.textureSlot = 1;
            depth.Bind();
            Matrix4f[] shadowmat = Ref.cgame.shadowMan.getShadowViewProjections(4);
            shader.setUniform("shadowMatrix", shadowmat);
            Vector4f[] shadowDepths = Ref.cgame.shadowMan.getCascadeDepths();

            shader.setUniform("cascadeDistances", shadowDepths[0]);

//            shader.setUniform("cascadeColors", Ref.cgame.shadowMan.getCascadeColors());
//            shader.setUniform("fog_factor", 1f/(view.farDepth));
//            shader.setUniform("fog_color", (Vector4f)new Vector4f(95,87,67,255).scale(1/255f)); // 145, 140, 129
            shader.setUniform("shadow_bias", Ref.cvars.Find("shadow_bias").fValue);

            shader.setUniform("pcfOffsets", Ref.cgame.shadowMan.getPCFoffsets());
            shader.setUniform("lightDirection", Ref.glRef.getLightAngle());
            GLRef.checkError();
        }

        // Set rotation matrix
        FloatBuffer viewbuffer = Ref.glRef.matrixBuffer;
        viewbuffer.position(0);
        viewbuffer.put(axis[0].x);viewbuffer.put(axis[1].x);viewbuffer.put(axis[2].x);viewbuffer.put(0);
        viewbuffer.put(axis[0].y);viewbuffer.put(axis[1].y);viewbuffer.put(axis[2].y); viewbuffer.put(0);
        viewbuffer.put(axis[0].z); viewbuffer.put(axis[1].z); viewbuffer.put(axis[2].z);viewbuffer.put(0);
        viewbuffer.put(0);
        viewbuffer.put(0);
        viewbuffer.put(0);
        viewbuffer.put(1);
        viewbuffer.flip();
        Matrix4f mMatrix = (Matrix4f) new Matrix4f().load(viewbuffer);
        mMatrix.invert();
         mMatrix.m30 = position.x;
        mMatrix.m31 = position.y;
        mMatrix.m32 = position.z;
        shader.setUniform("Modell", mMatrix);
        viewbuffer.clear();


        if(color != null) Helper.col(color);
        for (int i= 0; i < header.num_meshes; i++) {
            IQMMesh mesh = meshes[i];
            if(mesh.name.startsWith("@")) continue; // dont draw control-meshes
            if(!shadowPass) {
                mesh.bindTexture();
                glCullFace(GL_FRONT);
            }
            glBegin(GL_TRIANGLES);
            for (int j= 0; j < mesh.num_triangles; j++) {
                IQMTriangle tri = triangles[mesh.first_triangle+j];
                for (int k= 0; k < tri.vertex.length; k++) {
                    int indice = tri.vertex[k];
                    if(indice < 0) continue;
                    Vector2f coords = in_texcoord[indice];
                    Helper.tex(coords.x, 1-coords.y);

                    Vector3f normal;
                    Vector3f pos;
                    if(header.num_frames <= 5) {
                        // assume un-animated
                        normal = in_normal[indice];
                        pos = in_position[indice];
                    } else {
                        normal = out_normal[indice];
                        pos = out_position[indice];
                    }

                    glNormal3f(normal.x, normal.y, normal.z);
                    glVertex3f(pos.x, pos.y, pos.z);
                }
            }
            glEnd();
            if(!shadowPass) glCullFace(GL_BACK);
        }
        Ref.glRef.PopShader();

        // Draw BBOx
        if( false) {
//            IQMBounds b = bounds[currentFrameIndex];
            glPushMatrix();
            glTranslatef(position.x, position.y, position.z);
            Helper.renderBBoxWireframe(min.x, min.y, min.z, max.x, max.y, max.z);
            glPopMatrix();
        }

        // Draw normals
        if(false) {
            float normalSize = 1f;
            Helper.col(0, 1, 0);

            glBegin(GL_LINES);
            for (IQMMesh mesh : meshes) {
                for (int i= 0; i < mesh.num_vertexes; i++) {
                    Vector3f normal;
                    Vector3f pos;
                    if(header.num_frames <= 5) {
                        // assume un-animated
                        normal = in_normal[i];
                        pos = in_position[i];
                    } else {
                        normal = out_normal[i];
                        pos = out_position[i];
                    }
                    
                    normal.normalise();

                    glVertex3f(pos.x, pos.y, pos.z);
                    glVertex3f(pos.x + normal.x * normalSize,
                            pos.y + normal.y * normalSize,
                            pos.z + normal.z * normalSize);
                }
            }
            glEnd();
        }        
        
//        glPopMatrix();

        // Draw axis vectors
        if(false) {
            glDisable(GL_DEPTH_TEST);
            Ref.ResMan.getWhiteTexture().Bind();
            
            float lineSize = 20;
            glBegin(GL_LINES);
            for (int i= 0; i < 3; i++) {
                if(i == 0) Helper.col(1, 0, 0);
                if(i == 1) Helper.col(0, 1, 0);
                if(i == 2) Helper.col(0, 0, 1);
                glVertex3f(position.x, position.y, position.z);
                glVertex3f(position.x + axis[i].x * lineSize,
                        position.y + axis[i].y * lineSize,
                        position.z + axis[i].z * lineSize);
            }

            glEnd();

            glEnable(GL_DEPTH_TEST);
        }
    }

    

    

    

    
}
