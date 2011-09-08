package cubetech.iqm;

import org.lwjgl.opengl.GL12;
import cubetech.gfx.Matrix3x4;
import org.lwjgl.opengl.GL15;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL20;
import java.nio.ByteBuffer;
import cubetech.gfx.VBO;
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
import java.nio.IntBuffer;
import org.lwjgl.opengl.ARBUniformBufferObject;
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
    private static Shader gpushader;
    private static Shader gpushaderShadow;
    
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
    byte[] in_blendindex;
    byte[] in_blendweight;

    Vector3f[] out_position;
    Vector3f[] out_normal;
    Vector3f[] out_tangent;
    Vector3f[] out_bitangent;

    IQMAnim currentAnim = null;
    Matrix4f modelMatrix = new Matrix4f();
    EnumMap<Animations, IQMAnim> animationMap = new EnumMap<Animations, IQMAnim>(Animations.class);

    Vector3f min = new Vector3f(-32,-32,-32);
    Vector3f max = new Vector3f(32,32,32);
    CubeTexture envMap = null;

    IQMJoint[] thighJoints = null;
    VBO staticModelVB = null;
    VBO staticModelIB = null;
    private int stride;
    public boolean gpuskinning = true;
    
    private boolean gpu_skin_dirty = true;
    private boolean gpu_skin_shadow_dirty = true;

    Vector3f[] jointPose;

    // Joints that have attachment
    HashMap<String, BoneAttachment> attachments = new HashMap<String, BoneAttachment>();


    

//    private class IQMVertex {
//        public Vector3f position;
//        public Vector3f normal;
//        public Vector3f tangent;
//        public Vector2f texcoord;
//        public byte[] blendindex = new byte[4];
//        public byte[] blendweight = new byte[4];
//    }

    IQMModel() {
        envMap = Ref.ResMan.LoadTexture("data/ibl_sky", true);
        envMap.textureSlot = 1;
        modelMatrix.setIdentity();
    }

    public Vector3f getAttachment(String attachPoint, Vector3f dest) {
        if(dest == null) dest = new Vector3f();
        Vector3f src = attachments.get(attachPoint).lastposition;
        dest.set(src);
        return dest;
    }

    public IQMAnim getAnimation(Animations animations) {
        return animationMap.get(animations);
    }

    public void destroy() {
//        envMap = null;
    }

    public boolean isStatic() {
        return anims == null || joints == null;
    }

    private void createDynamicBuffer() {
        if(staticModelVB != null) return;
        createIndiceBuffer();
        //IQMVertex[] verts = new IQMVertex[header.num_vertexes];

        stride = 0;
        if(in_position != null) stride += 4*3;
        if(in_normal != null) stride += 4*3;
        if(in_tangent != null) stride += 4*4;
        if(in_texcoord != null) stride += 4*2;
        if(in_blendindex != null) stride += 4;
        if(in_blendweight != null) stride += 4;
        
        staticModelVB = new VBO(header.num_vertexes * stride, GLRef.BufferTarget.Vertex);
        ByteBuffer vb = staticModelVB.map();

        for (int i= 0; i < header.num_vertexes; i++) {
            if(in_position != null)vb.putFloat(in_position[i].x).putFloat(in_position[i].y).putFloat(in_position[i].z);
            if(in_normal != null)vb.putFloat(in_normal[i].x).putFloat(in_normal[i].y).putFloat(in_normal[i].z);
            if(in_tangent != null)vb.putFloat(in_tangent[i].x).putFloat(in_tangent[i].y).putFloat(in_tangent[i].z).putFloat(in_tangent[i].w);
            if(in_texcoord != null)vb.putFloat(in_texcoord[i].x).putFloat(1f-in_texcoord[i].y);
            if(in_blendindex != null)vb.put(in_blendindex[i*4]).put(in_blendindex[i*4+1]).put(in_blendindex[i*4+2]).put(in_blendindex[i*4+3]);
            //vb.put((byte)127).put((byte)127).put((byte)127).put((byte)127);
            if(in_blendweight != null)vb.put(in_blendweight[i*4]).put(in_blendweight[i*4+1]).put(in_blendweight[i*4+2]).put(in_blendweight[i*4+3]);
//            IQMVertex v = new IQMVertex();
//            if(in_position != null) v.position = in_position[i];
//            if(in_normal != null) v.normal = in_normal[i];
//            if(in_texcoord != null) v.texcoord = in_texcoord[i];
//            if(in_blendindex != null) System.arraycopy(in_blendindex, i*4, v.blendindex, 0, 4);
//            if(in_blendweight != null) System.arraycopy(in_blendweight, i*4, v.blendweight, 0, 4);
        }

        staticModelVB.unmap();

        gpushader = Ref.glRef.getShader("gpuskin");
        gpushaderShadow = Ref.glRef.getShader("gpuskinShadowed");
        initGPUShader();
        
    }

    private static void initGPUShader() {
        initUBOForShader(gpushader);
        initUBOForShader(gpushaderShadow);
    }
    
    private static void initUBOForShader(Shader shader) {
        if(Ref.glRef.getGLCaps().GL_ARB_uniform_buffer_object) {
            IntBuffer intBuff = BufferUtils.createIntBuffer(1);
            int blockIndex = ARBUniformBufferObject.glGetUniformBlockIndex(shader.getShaderId(), "animdata");
            ARBUniformBufferObject.glGetUniformIndices(shader.getShaderId(), new CharSequence[] {"bonemats"}, intBuff);
            gpu_bonematIndex = intBuff.get(0); intBuff.clear();

            gpu_ubosize = ARBUniformBufferObject.glGetActiveUniformBlock(
                    shader.getShaderId(),
                    blockIndex,
                    ARBUniformBufferObject.GL_UNIFORM_BLOCK_DATA_SIZE);
            
            gpu_bonematOffset = ARBUniformBufferObject.glGetActiveUniforms(shader.getShaderId(), gpu_bonematIndex, ARBUniformBufferObject.GL_UNIFORM_OFFSET);

            ARBUniformBufferObject.glUniformBlockBinding(shader.getShaderId(), blockIndex, 0);
            gpu_ubo = GL15.glGenBuffers();
        }
    }


    private static int gpu_bonematIndex;
    private static int gpu_ubosize;
    private static int gpu_bonematOffset;
    private static int gpu_ubo;

    private void createIndiceBuffer() {
        staticModelIB = new VBO(4*triangles.length*3, GLRef.BufferTarget.Index);
        ByteBuffer indiceBuffer = staticModelIB.map();
        for (int i= 0; i < triangles.length; i++) {
            IQMTriangle tri = triangles[i];
            for (int j= 0; j < 3; j++) {
                int indice = tri.vertex[j];
                indiceBuffer.putInt(indice);
            }
        }
        staticModelIB.unmap();
    }

    private void createStaticBuffer() {
        createIndiceBuffer();

        // Create vertex buffer
        int vertexStride = 32 * in_position.length;
        staticModelVB = new VBO(vertexStride, GLRef.BufferTarget.Vertex);
        ByteBuffer vertexBuffer = staticModelVB.map();
        for (int i= 0; i < in_position.length; i++) {
            Vector3f p = in_position[i];
            Vector3f n = in_normal[i];
            Vector2f t = in_texcoord[i];
            vertexBuffer.putFloat(p.x).putFloat(p.y).putFloat(p.z);
            vertexBuffer.putFloat(n.x).putFloat(n.y).putFloat(n.z);
            vertexBuffer.putFloat(t.x).putFloat(1f-t.y);
        }
        staticModelVB.unmap();
    }

    private void gpuAnimate(int frame, int lastframe, float backlerp) {
        createDynamicBuffer();
        
        int frame1 = frame;
        int frame2 = lastframe;
        float frameOffset = backlerp;

        frame1 %= header.num_frames;
        frame2 %= header.num_frames;

        int blendStart = anims[Animations.WALK.ordinal()].first_frame;
        int blendMax = anims[Animations.WALK.ordinal()].num_frames;

        int blendframe = ((Ref.cgame.cg.time >> 4) % blendMax) + blendStart;

        int mat1 = frame1 * header.num_joints;
        int mat2 = frame2 * header.num_joints;
        int mat3 = blendframe * header.num_joints;

        float blendweight = (float) Math.abs(Math.sin(Ref.cgame.cg.time/100f));
        blendweight = 1f;
        //blendweight = 1f;
        // Interpolate matrixes between the two closest frames and concatenate with parent matrix if necessary.
        // Concatenate the result with the inverse of the base pose.
        // You would normally do animation blending and inter-frame blending here in a 3D engine.
        Matrix4f temp = new Matrix4f();
        for (int i= 0; i < header.num_joints; i++) {
            Matrix4f m1 = frames[mat1+i];
            Matrix4f m2 = frames[mat2+i];

            Matrix4f m3 = frames[mat3+i];
            if(outframe[i] == null) {
                outframe[i] = new Matrix4f();
            }
            Matrix4f dest = outframe[i];

            Helper.scale((1-frameOffset) * blendweight, m1, dest);
            Helper.scale((frameOffset) * blendweight, m2, temp);
            Matrix4f.add(dest, temp, dest);
            //Matrix4f.mul(m3, dest, dest);
            //Helper.scale(1f-blendweight, m3, temp);
            //Matrix4f.add(dest, temp, dest);
            dest.m33 = 1;
            if(joints[i].parent >= 0) {
                Matrix4f.mul(outframe[joints[i].parent], dest, dest);
            }
        }

        updateAttachments();
        gpu_skin_dirty = true; // need to update bonemats
        gpu_skin_shadow_dirty = true;
    }

    public void animate(int frame, int oldframe, float backlerp) {
        if(isStatic()) {
            if(staticModelVB == null) {
                createStaticBuffer();
            }
            return;
        }

        if(gpuskinning) {
            gpuAnimate(frame, oldframe, backlerp);
            return;
        }

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
            if(outframe[i] == null) {
                outframe[i] = new Matrix4f();
            }
            Matrix4f dest = outframe[i];

            Helper.scale(1-frameOffset, m1, dest);
            Helper.scale(frameOffset, m2, temp);
            Matrix4f.add(dest, temp, dest);
            dest.m33 = 1;
            if(joints[i].parent >= 0) {
                Matrix4f.mul(outframe[joints[i].parent], dest, dest);
            }
        }

        // The actual vertex generation based on the matrixes follows...
        int iIndex = 0;
        int iWeight = 0;
        Matrix3f matnorm = new Matrix3f();
        Matrix4f dest = new Matrix4f();
        for (int i= 0; i < header.num_vertexes; i++) {
            // Blend matrixes for this vertex according to its blend weights.
            // the first index/weight is always present, and the weights are
            // guaranteed to add up to 255. So if only the first weight is
            // presented, you could optimize this case by skipping any weight
            // multiplies and intermediate storage of a blended matrix.
            // There are only at most 4 weights per vertex, and they are in
            // sorted order from highest weight to lowest weight. Weights with
            // 0 values, which are always at the end, are unused.
            float f = (in_blendweight[iWeight]&0xff)/255.0f;
            
            Helper.scale(f, outframe[in_blendindex[iIndex]&0xff], dest);
            for (int j= 1; j < 4 && (in_blendweight[iWeight+j]&0xff) != 0; j++) {
                f = (in_blendweight[iWeight+j])/255.0f;
                Helper.scale(f, outframe[in_blendindex[iIndex+j]&0xff], temp);
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

    private void updateAttachments() {
        if(attachments.isEmpty()) return;

        Vector4f v = new Vector4f();
        Matrix4f m = new Matrix4f();
        for (BoneAttachment boneAttachment : attachments.values()) {
            int bone = boneAttachment.boneIndex;
            int j1 = bone*2+1;
            v.set(jointPose[j1].x,jointPose[j1].y,jointPose[j1].z,1);
            Matrix4f.transform(outframe[bone], v, v);
            boneAttachment.lastposition.set(v);

            m = Matrix4f.invert(outframe[bone], m);
            Helper.matrixToAxis(m, boneAttachment.axis);
        }
    }

    private void renderStatic(Vector3f position, Vector3f[] axis, Vector4f color, boolean shadowPass) {
        Shader shader = null;
        if(shadowPass) {
            shader = Ref.glRef.getShader("unlitObject");
        } else {
            shader = Ref.glRef.getShader("litobjectpixel_1");
        }

        Ref.glRef.PushShader(shader);

        if(!shadowPass) {
            preShadowRecieve(shader);
        }

        getModelMatrix(axis, position);
        shader.setUniform("Modell", modelMatrix);

        if(color != null) Helper.col(color);
        glDisable(GL_BLEND);
        renderFromVBO(shadowPass);
        glEnable(GL_BLEND);
        Ref.glRef.PopShader();
    }
    private FloatBuffer bonebuffer = null;
    private void renderGPU(Vector3f position, Vector3f[] axis, Vector4f color, boolean shadowCasting) {
        Shader shader;
        if(shadowCasting) {
            shader = gpushader;
        } else {
            shader = gpushaderShadow;
        }
        Ref.glRef.PushShader(shader);

        // Update ubo if necesary
        if(gpu_skin_dirty && shadowCasting || gpu_skin_shadow_dirty && !shadowCasting) {
            int target = ARBUniformBufferObject.GL_UNIFORM_BUFFER;

            if(bonebuffer == null) bonebuffer = BufferUtils.createFloatBuffer(header.num_joints * 4 * 4);
            bonebuffer.clear();
            for (int i= 0; i < header.num_joints; i++) {
                Matrix3x4.storeMatrix4f(outframe[i], bonebuffer);
            }
            bonebuffer.flip();

            GL15.glBindBuffer(target, gpu_ubo);
            GL15.glBufferData(target, gpu_ubosize, GL15.GL_STREAM_DRAW);
            GL15.glBufferSubData(target, gpu_bonematOffset, bonebuffer);
            GL15.glBindBuffer(target, 0);

            ARBUniformBufferObject.glBindBufferBase(target, 0, gpu_ubo);
            if(gpu_skin_dirty && shadowCasting) gpu_skin_dirty = false;
            else gpu_skin_shadow_dirty = false;
        }
        
        if(!shadowCasting) {
            // bind depth texture and setup shadow uniforms
            preShadowRecieve(shader);
            
        }

        // Set model matrix
        getModelMatrix(axis, position);

        shader.setUniform("Modell", modelMatrix);

//        Matrix4f viewmatrix = Ref.cgame.cg.refdef.viewMatrix;
//        Matrix4f.mul(viewmatrix, modelMatrix,  modelMatrix);
//        FloatBuffer viewbuffer = Ref.cgame.cg.refdef.viewbuffer;
//        viewbuffer.clear();
//        modelMatrix.store(viewbuffer); viewbuffer.flip();
//        glMatrixMode(GL11.GL_MODELVIEW);
//        GL11.glPushMatrix();
//        GL11.glLoadMatrix(viewbuffer);

        // render
        glDisable(GL_BLEND);
        if(color != null) Helper.col(color);
        renderFromVBO(shadowCasting);
        glEnable(GL_BLEND);
        // finish

//        GL11.glPopMatrix();
        Ref.glRef.PopShader();

        for (BoneAttachment boneAttachment : attachments.values()) {
            Vector4f derp = new Vector4f(boneAttachment.lastposition.x
                    , boneAttachment.lastposition.y
                    , boneAttachment.lastposition.z
                    , 1);
            
            Matrix4f.transform(modelMatrix, derp, derp);
            Helper.renderBBoxWireframe(derp.x-1,derp.y-1,derp.z-1,derp.x+1,derp.y+1,derp.z+1,null);

            IQMModel model = Ref.ResMan.loadModel("data/ak47.iqm");
            model.animate(0, 0, 0);
            Vector3f derpp = new Vector3f(derp);

            Helper.mul(boneAttachment.axis, axis, boneAttachment.axis);
            
            model.render(derpp, boneAttachment.axis, color, shadowCasting);
        }
    }

    private void preShadowRecieve(Shader shader) {
        // Bind environmentmap
        envMap.textureSlot = 2;
        envMap.Bind();
        // Set light
        Ref.glRef.setLIght();
        CubeTexture depth = Ref.cgame.shadowMan.getDepthTexture();
        depth.textureSlot = 1;
        depth.Bind();

        Matrix4f[] shadowmat = Ref.cgame.shadowMan.getShadowViewProjections(4);
        Vector4f[] shadowDepths = Ref.cgame.shadowMan.getCascadeDepths();
        shader.setUniform("shadowMatrix", shadowmat);
        shader.setUniform("cascadeDistances", shadowDepths[0]);
        shader.setUniform("shadow_bias", Ref.cvars.Find("shadow_bias").fValue);
        shader.setUniform("shadow_factor", Ref.cvars.Find("shadow_factor").fValue);
        shader.setUniform("pcfOffsets", Ref.cgame.shadowMan.getPCFoffsets());
        shader.setUniform("lightDirection", Ref.glRef.getLightAngle());
        GLRef.checkError();
    }

    public void render(Vector3f position, Vector3f[] axis, Vector4f color, boolean shadowPass) {
        if(isStatic()) {
            renderStatic(position, axis, color, shadowPass);
            return;
        } else if(gpuskinning) {
            renderGPU(position, axis, color, shadowPass);
            return;
        }

        Shader shader = null;
        if(shadowPass) {
            shader = Ref.glRef.getShader("unlitObject");
        }  else {
            shader = Ref.glRef.getShader("litobjectpixel");
        }
        
        Ref.glRef.PushShader(shader);

        if(!shadowPass) {
            preShadowRecieve(shader);
        }

        shader.setUniform("Modell", getModelMatrix(axis, position));

        if(color != null) Helper.col(color);

        renderOldSchool(shadowPass);
        
        Ref.glRef.PopShader();

        debugdraw(position, axis);
    }

    private Matrix4f getModelMatrix(Vector3f[] axis, Vector3f position) {
        // Set rotation matrix
        FloatBuffer viewbuffer = Ref.glRef.matrixBuffer;
        viewbuffer.position(0);
        viewbuffer.put(axis[0].x);viewbuffer.put(axis[1].x);viewbuffer.put(axis[2].x);viewbuffer.put(0);
        viewbuffer.put(axis[0].y);viewbuffer.put(axis[1].y);viewbuffer.put(axis[2].y); viewbuffer.put(0);
        viewbuffer.put(axis[0].z); viewbuffer.put(axis[1].z); viewbuffer.put(axis[2].z);viewbuffer.put(0);
        viewbuffer.put(0); viewbuffer.put(0); viewbuffer.put(0); viewbuffer.put(1);
        viewbuffer.flip();
        Matrix4f mMatrix = (Matrix4f) modelMatrix.load(viewbuffer);
        mMatrix.invert();
        mMatrix.m30 = position.x;
        mMatrix.m31 = position.y;
        mMatrix.m32 = position.z;
        
        viewbuffer.clear();
        return mMatrix;
    }

    private void renderFromVBO(boolean shadowPass) {
        for (IQMMesh mesh : meshes) {
            if(mesh.name.startsWith("@")) continue; // dont draw control-meshes
            if(!shadowPass) {
                mesh.bindTexture();
                glCullFace(GL_FRONT);
            }
            staticModelIB.bind();
            staticModelVB.bind();
            if(isStatic()) preVboStatic();
            else preVBOGpu();
            int firstVertex = mesh.first_vertex;
            int lastVertex = mesh.first_vertex + mesh.num_vertexes;
            GL12.glDrawRangeElements(GL11.GL_TRIANGLES, firstVertex, lastVertex
                    , mesh.num_triangles*3, GL11.GL_UNSIGNED_INT, mesh.first_triangle*3*4);
            //GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.num_triangles*3, GL11.GL_UNSIGNED_INT, mesh.first_triangle);
            if(isStatic()) postVboStatic();
            else postVBOGpu();
            staticModelIB.unbind();
            staticModelVB.unbind();
            if(!shadowPass) glCullFace(GL_BACK);
        }
    }

    private void preVBOGpu() {
        int offset = 0;
        
        glVertexPointer(3, GL_FLOAT, stride, offset);
        glEnableClientState(GL_VERTEX_ARRAY);
        offset += 4*3;

        glNormalPointer(GL_FLOAT, stride, offset);
        glEnableClientState(GL_NORMAL_ARRAY);
        offset += 4*3;

//        ARBVertexShader.glVertexAttribPointerARB(7, 4, GL11.GL_FLOAT, false, stride, offset);
//        ARBVertexShader.glEnableVertexAttribArrayARB(7); // tan
        offset += 4*4;
        
        if(in_texcoord != null) {
            glTexCoordPointer(2, GL_FLOAT, stride, offset);
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            offset += 4*2;
        }
        if(in_blendindex != null) {
            ARBVertexShader.glVertexAttribPointerARB(6, 4, GL11.GL_UNSIGNED_BYTE, false, stride, offset);
            ARBVertexShader.glEnableVertexAttribArrayARB(6); // position

            offset += 4;
        }
        if(in_blendweight != null) {
            ARBVertexShader.glVertexAttribPointerARB(1, 4, GL11.GL_UNSIGNED_BYTE, true, stride, offset);
            ARBVertexShader.glEnableVertexAttribArrayARB(1); // position
            offset += 4;
        }
    }

    private void postVBOGpu() {
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_NORMAL_ARRAY);
        ARBVertexShader.glDisableVertexAttribArrayARB(1); // position
        if(in_texcoord != null) {
            glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        }
        if(in_blendindex != null) {
            ARBVertexShader.glDisableVertexAttribArrayARB(7); // position
        }
        if(in_blendweight != null) {
            ARBVertexShader.glDisableVertexAttribArrayARB(6); // position
        }
    }

    private static void preVboStatic() {
        int stride = 32;
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_POSITION); // position
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_POSITION, 3, GL11.GL_FLOAT, false, stride, 0);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_NORMAL); // color
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_NORMAL, 3, GL11.GL_FLOAT, true, stride, 3*4);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COORDS); // coords
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS, 2, GL11.GL_FLOAT, false, stride, 6*4);
    }

    private static void postVboStatic() {
        GL20.glDisableVertexAttribArray(Shader.INDICE_POSITION);
        GL20.glDisableVertexAttribArray(Shader.INDICE_NORMAL);
        GL20.glDisableVertexAttribArray(Shader.INDICE_COORDS);
    }

    private void renderOldSchool(boolean shadowPass) {
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
                    if(indice < 0) {
                        continue;
                    }
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


    }

    private void debugdraw(Vector3f position, Vector3f[] axis) {
        // Draw BBOx
        if( false) {
//            IQMBounds b = bounds[currentFrameIndex];
            glPushMatrix();
            glTranslatef(position.x, position.y, position.z);
            Helper.renderBBoxWireframe(min.x, min.y, min.z, max.x, max.y, max.z,null);
            glPopMatrix();
        }

        if(true) {
            glDisable(GL_DEPTH_TEST);
            glBegin(GL_LINES);
            Vector4f v = new Vector4f();
            for (int i= 0; i < joints.length; i++) {
                int parent = joints[i].parent;
                int j1 = i*2+1;
                int j2 = parent*2+1;
                if(parent < 0) {
                    continue;
                }
                Helper.col(0, 0, 1);
                v.set(jointPose[j1].x,jointPose[j1].y,jointPose[j1].z,1);
                Matrix4f.transform(modelMatrix, v, v);
                glVertex3f(v.x, v.y, v.z);
                Helper.col(1, 0, 0);
                v.set(jointPose[j2].x,jointPose[j2].y,jointPose[j2].z,1);
                Matrix4f.transform(modelMatrix, v, v);
                glVertex3f(v.x, v.y, v.z);
            }

            glEnd();
            glEnable(GL_DEPTH_TEST);
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

    public Vector3f getMins() {
        return min;
    }

    public Vector3f getMaxs() {
        return max;
    }
   
}
