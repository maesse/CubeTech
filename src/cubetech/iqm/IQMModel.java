package cubetech.iqm;

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
    private int currentFrameIndex = 0;
    Matrix3f modelMatrix = new Matrix3f();
    CubeTexture envMap = null;

    IQMModel() {
        try {
            envMap = Ref.ResMan.loadCubemap("data/ibl_sky");
            //envMap = Ref.ResMan.LoadTexture("data/sky_up.png");
            envMap.textureSlot = 1;
        } catch (IOException ex) {
            Common.Log("Couldn't load envmap for IQMModel");
        }
        modelMatrix.setIdentity();
    }

    public void animate(int frame, int oldframe, float backlerp) {
        if(header.num_frames == 0) return;
//        currentAnim = anims[0];

        int frame1 = oldframe;
        int frame2 = frame;
        float frameOffset = 1f-backlerp;
//        if(currentAnim == null) {
//            frame1 %= header.num_frames;
//            frame2 %= header.num_frames;
//        } else {
//            frame1 %= currentAnim.num_frames;
//            frame2 %= currentAnim.num_frames;
//            frame1 += currentAnim.first_frame;
//            frame2 += currentAnim.first_frame;
//        }
        currentFrameIndex = frame1;
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
//            out_position[i].y *= -1f;

            iIndex += 4;
            iWeight += 4;
        }

    }    

    public void render(Vector3f position, Vector3f[] axis) {
        //Vector3f position = new Vector3f(0,0,800);

        glPushMatrix();
//        glRotatef(Ref.cgame.cg.time/10f, 0, 0, 1);
        glTranslatef(position.x, position.y, position.z);

        Shader shader = Ref.glRef.getShader("litobjectpixel");
        shader.mapTextureUniform("envmap", 1);
        Ref.glRef.PushShader(shader);
        envMap.textureSlot = 1;
        envMap.Bind();
//        shader.setUniform("envmap", envMap.GetID());
        shader.setUniform("eye", Ref.cgame.cg.refdef.Origin);
//        Vector3f[] axis = Helper.AnglesToAxis(new Vector3f(0, angle, 0));
//        axis[0].scale(-1);
//        axis[1].scale(-1);
        FloatBuffer viewbuffer = Ref.glRef.matrixBuffer;

        viewbuffer.position(0);
        viewbuffer.put(axis[0].x);viewbuffer.put(axis[1].x); viewbuffer.put(axis[2].x);
        viewbuffer.put(axis[0].y);viewbuffer.put(axis[1].y); viewbuffer.put(axis[2].y);
        viewbuffer.put(axis[0].z);viewbuffer.put(axis[1].z); viewbuffer.put(axis[2].z);
        viewbuffer.flip();
        shader.setUniformMat3("Modell", viewbuffer);
        Ref.glRef.setLIght();

        
        
//        Ref.ResMan.LoadTexture("data/tile.png").Bind();
//
//        envMap.Bind();
        glDisable(GL_CULL_FACE);
        for (int i= 0; i < header.num_meshes; i++) {
            IQMMesh mesh = meshes[i];
            mesh.bindTexture();

            //glFrontFace(GL_CCW);

            glCullFace(GL_FRONT);
            glBegin(GL_TRIANGLES);
            for (int j= 0; j < mesh.num_triangles; j++) {
                IQMTriangle tri = triangles[mesh.first_triangle+j];
                for (int k= 0; k < tri.vertex.length; k++) {
                    int indice = tri.vertex[k]-1;
                    if(indice < 0) continue;
                    Vector2f coords = in_texcoord[indice];
                    Helper.tex(coords.x, 1-coords.y);

                    Vector3f normal = out_normal[indice];
                    glNormal3f(normal.x, normal.y, normal.z);
                    
                    Vector3f pos = out_position[indice];
                    glVertex3f(pos.x, pos.y, pos.z);
                    
                }
            }
            glEnd();
            glCullFace(GL_BACK);
        }
        glEnable(GL_CULL_FACE);

        Ref.glRef.PopShader();

        // Draw BBOx
        if(bounds != null && false) {
            IQMBounds b = bounds[currentFrameIndex];
            Helper.renderBBoxWireframe(b.bbmins[0],b.bbmins[1],b.bbmins[2], b.bbmaxs[0], b.bbmaxs[1], b.bbmaxs[2]);
        }

        // Draw normals
        if(false) {
            float normalSize = 1f;
            Helper.col(0, 1, 0);

            glBegin(GL_LINES);
            for (IQMMesh mesh : meshes) {
                for (int i= 0; i < mesh.num_vertexes; i++) {
                    Vector3f start = out_position[i];
                    Vector3f dir = out_normal[i];
                    dir.normalise();

                    glVertex3f(start.x, start.y, start.z);
                    glVertex3f(start.x + dir.x * normalSize,
                            start.y + dir.y * normalSize,
                            start.z + dir.z * normalSize);
                }
            }
            glEnd();
        }        
        
        glPopMatrix();
    }

    

    
}
