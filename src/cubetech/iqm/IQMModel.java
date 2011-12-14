package cubetech.iqm;

import cubetech.gfx.Light;
import cubetech.CGame.ViewParams;
import cubetech.gfx.ShaderUBO;
import java.util.ArrayList;
import cubetech.common.Common;
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
import cubetech.common.Animations;
import cubetech.gfx.GLRef;
import java.nio.FloatBuffer;
import cubetech.gfx.Shader;
import cubetech.gfx.CubeTexture;
import org.lwjgl.util.vector.Matrix3f;
import cubetech.common.Helper;
import cubetech.gfx.ResourceManager;
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
    private static Shader gpushaderLit;
    private static Shader gpushaderShadowDeferred;
    private static Shader gpushaderLitDeferred;
    
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

    ShapeKeyCollection shapeKeys = null;
    // only for cpu
    boolean[] boneUsage = null;

    // Joints that have attachment
    HashMap<String, BoneAttachment> attachments = new HashMap<String, BoneAttachment>();
    public BoneController[] controllers = null;

    private static HashMap<Shader, ShaderUBO> uboMap = new HashMap<Shader, ShaderUBO>();

    IQMModel() {
        
        modelMatrix.setIdentity();
    }

    public Vector3f getAttachment(String attachPoint, Vector3f dest) {
        if(dest == null) dest = new Vector3f();
        Vector4f src = attachments.get(attachPoint).lastposition;
        dest.set(src);
        return dest;
    }

    public BoneAttachment getAttachment(String attachPoint) {
        return attachments.get(attachPoint);
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

    public void loadShapeFile(String path) {
        
    }

    private void createDynamicBuffer() {
        if(staticModelVB != null) return;
        
        stride = 0;
        if(in_position != null) stride += 4*3;
        if(in_normal != null) stride += 4*3;
        if(in_tangent != null) stride += 4*4;
        if(in_texcoord != null) stride += 4*2;
        if(in_blendindex != null) stride += 4;
        if(in_blendweight != null) stride += 4;
        
        staticModelVB = new VBO(header.num_vertexes * stride, VBO.BufferTarget.Vertex);
        ByteBuffer vb = staticModelVB.map();


        for (int i= 0; i < header.num_vertexes; i++) {
            if(in_position != null)vb.putFloat(in_position[i].x).putFloat(in_position[i].y).putFloat(in_position[i].z);
            if(in_normal != null)vb.putFloat(in_normal[i].x).putFloat(in_normal[i].y).putFloat(in_normal[i].z);
            if(in_tangent != null)vb.putFloat(in_tangent[i].x).putFloat(in_tangent[i].y).putFloat(in_tangent[i].z).putFloat(in_tangent[i].w);
            if(in_texcoord != null)vb.putFloat(in_texcoord[i].x).putFloat(1f-in_texcoord[i].y);
            if(in_blendindex != null)vb.put(in_blendindex[i*4]).put(in_blendindex[i*4+1]).put(in_blendindex[i*4+2]).put(in_blendindex[i*4+3]);
            if(in_blendweight != null)vb.put(in_blendweight[i*4]).put(in_blendweight[i*4+1]).put(in_blendweight[i*4+2]).put(in_blendweight[i*4+3]);
        }

        staticModelVB.unmap();

        createIndiceBuffer();

        gpushader = Ref.glRef.getShader("gpuskin");
        gpushaderShadow = Ref.glRef.getShader("gpuskinShadowed");
        gpushaderShadowDeferred = Ref.glRef.getShader("gpuskinShadowedDeferred");
        gpushaderLit = Ref.glRef.getShader("gpuskinLit");
        gpushaderLitDeferred = Ref.glRef.getShader("gpuskinLitDeferred");
        initGPUShader();
    }

    private static void initGPUShader() {
        if(uboMap.size() > 0) return;

        ShaderUBO ubo = ShaderUBO.initUBOForShader(gpushader, "animdata");
        ubo.registerUniform("bonemats");
        if(ubo != null) uboMap.put(gpushader, ubo);
        ubo = ShaderUBO.initUBOForShader(gpushaderShadow, "animdata");
        ubo.registerUniform("bonemats");
        if(ubo != null) uboMap.put(gpushaderShadow, ubo);
        ubo = ShaderUBO.initUBOForShader(gpushaderLitDeferred, "animdata");
        ubo.registerUniform("bonemats");
        if(ubo != null) uboMap.put(gpushaderLitDeferred, ubo);
        ubo = ShaderUBO.initUBOForShader(gpushaderLit, "animdata");
        ubo.registerUniform("bonemats");
        if(ubo != null) uboMap.put(gpushaderLit, ubo);
    }

    private void createIndiceBuffer() {
        staticModelIB = new VBO(4*triangles.length*3, VBO.BufferTarget.Index);
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
        staticModelVB = new VBO(vertexStride, VBO.BufferTarget.Vertex);
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

    // Contains the values of the latest call
    private int _frame = -1;
    private int _lastframe;
    private float _backlerp;
    
    
    
    private void blendAnimation(int frame, int lastframe, float backlerp) {
        // Avoid animating the same frame over again
        if(frame == _frame && lastframe == _lastframe && backlerp == _backlerp) return;
        _frame = frame;
        _lastframe = lastframe;
        _backlerp = backlerp;
        
        int frame1 = frame;
        int frame2 = lastframe;
        float frameOffset = backlerp;

        frame1 %= header.num_frames;
        frame2 %= header.num_frames;

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

            Helper.scale((1-frameOffset), m1, dest);
            Helper.scale((frameOffset), m2, temp);
            Matrix4f.add(dest, temp, dest);
            dest.m33 = 1;
            
            if(controllers != null) {
                for (BoneController bCtrl : controllers) {
                    if(bCtrl.boneName.equals(joints[i].name)) {
                        Vector3f[] faxis = Helper.AnglesToAxis(bCtrl.boneAngles);
                        Matrix4f rot = Helper.axisToMatrix(faxis, null);
                        Matrix4f.mul(rot, dest, dest);
                        break;
                    }
                }
            }

            if(joints[i].parent >= 0) {
                Matrix4f.mul(outframe[joints[i].parent], dest, dest);
            }
        }

        updateAttachments();
        
        
    }

    public void animate(int frame, int oldframe, float backlerp) {
        if(isStatic()) {
            if(staticModelVB == null) {
                createStaticBuffer();
            }
            return;
        }

        blendAnimation(frame, oldframe, backlerp);
        
        if(gpuskinning && shapeKeys == null) {
            createDynamicBuffer();
            return;
        }

        if(shapeKeys != null) {
            for (int i= 0; i < in_position.length; i++) {
                if(out_position[i] == null) out_position[i] = new Vector3f();
                out_position[i].set(in_position[i]);
            }
            shapeKeys.setShapeKey("Key 1", (float) Math.abs(Math.sin(Ref.client.realtime/576f)));
            shapeKeys.setShapeKey("Key 2", (float) Math.abs(Math.sin(Ref.client.realtime/1000f)));
            shapeKeys.applyShapes(out_position);
        }

        // The actual vertex generation based on the matrixes follows...
        int iIndex = 0;
        int iWeight = 0;
        Matrix3f matnorm = new Matrix3f();
        Matrix4f dest = new Matrix4f();
        Matrix4f temp = new Matrix4f();
        boolean buildBoneUsage = false;
        if(boneUsage == null || true) {
            buildBoneUsage = true;
            boneUsage = new boolean[header.num_joints];
        }
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
            if(buildBoneUsage) boneUsage[in_blendindex[iIndex]&0xff] = true;
            Helper.scale(f, outframe[in_blendindex[iIndex]&0xff], dest);
            for (int j= 1; j < 4 && (in_blendweight[iWeight+j]&0xff) != 0; j++) {
                if(buildBoneUsage) boneUsage[in_blendindex[iIndex+j]&0xff] = true;
                f = (in_blendweight[iWeight+j])/255.0f;
                Helper.scale(f, outframe[in_blendindex[iIndex+j]&0xff], temp);
                Matrix4f.add(dest, temp, dest);
                dest.m33 = 1;
            }

            // Transform attributes by the blended matrix.
            // Position uses the full 3x4 transformation matrix.
            // Normals and tangents only use the 3x3 rotation part
            // of the transformation matrix.
            if(shapeKeys != null) {
                out_position[i] = Helper.transform(dest, out_position[i], out_position[i]);
            } else {
                out_position[i] = Helper.transform(dest, in_position[i], out_position[i]);
            }

            matnorm = Helper.toNormalMatrix(dest, matnorm);
            out_normal[i] = Helper.transform(matnorm, in_normal[i], out_normal[i]);
            out_tangent[i] = Helper.transform(matnorm, in_tangent[i], out_tangent[i]);
            out_bitangent[i] = Vector3f.cross(out_normal[i], out_tangent[i], out_bitangent[i]);
            out_bitangent[i].scale(in_tangent[i].w);

            iIndex += 4;
            iWeight += 4;
        }
        
        int unused = 0;
        for (int i = 0; i < boneUsage.length; i++) {
            if(!boneUsage[i]) unused++;
        }
        
            int test = 2;

    }

    private Matrix4f tempMatrix = new Matrix4f();
    private void updateAttachments() {
        if(attachments.isEmpty()) return;
       
        for (BoneAttachment boneAttachment : attachments.values()) {
            int bone = boneAttachment.boneIndex;
            int j1 = bone*2+1;
            Vector4f v = boneAttachment.lastposition;
            v.set(jointPose[j1].x,jointPose[j1].y,jointPose[j1].z,1);
            Matrix4f.transform(outframe[bone], v, v);

            tempMatrix = Matrix4f.transpose(outframe[bone], tempMatrix);
            Helper.matrixToAxis(tempMatrix, boneAttachment.axis);
        }
    }

    private void renderStatic(Vector3f position, Vector3f[] axis, Vector4f color, boolean shadowPass, ViewParams view) {
        Shader shader = null;
        if(shadowPass) {
            shader = Ref.glRef.getShader("unlitObject");
        } else {
            shader = Ref.glRef.getShader("litobjectpixel_1");
        }

        Ref.glRef.PushShader(shader);

        if(!shadowPass) {
            preShadowRecieve(shader, view);
        }

        getModelMatrix(axis, position);
        shader.setUniform("Modell", modelMatrix);

        if(color != null) Helper.col(color);
        glDisable(GL_BLEND);
        renderFromVBO(shadowPass);
        
        glEnable(GL_BLEND);

        
        
        Ref.glRef.PopShader();
    }

    private void renderGPU(Vector3f position, Vector3f[] axis, Vector4f color, boolean shadowCasting, ViewParams view) {
        Shader shader;
        if(Ref.cgame != null && Ref.glRef.shadowMan.isEnabled()) {
            if(shadowCasting) {
                shader = gpushader;
            } else {
                
                shader = Ref.glRef.deferred.isRendering()? gpushaderLitDeferred : gpushaderShadow;
            }
        } else {
            shader = Ref.glRef.deferred.isRendering()? gpushaderLitDeferred : gpushaderLit;
        }
        
        Ref.glRef.PushShader(shader);
        if(Ref.glRef.deferred.isRendering()) {
            shader.setUniform("far", Ref.cvars.Find("cg_depthfar").fValue); 
            shader.setUniform("near", Ref.cvars.Find("cg_depthnear").fValue); 
        }

        // Update ubo if necesary
        //if(gpu_skin_dirty && shadowCasting || gpu_skin_shadow_dirty && !shadowCasting) {
            ShaderUBO ubo = uboMap.get(shader);
            if(ubo == null) {
                Ref.common.Error(Common.ErrorCode.FATAL, "Implement ubo workaround");
            }

            int jointCapacity = ubo.getSize()/(4*4*3);
            int bufferJoints = header.num_joints;
            if(bufferJoints > jointCapacity) {
                Common.Log("Too many bones!");
                bufferJoints = jointCapacity;
            }
            
            ByteBuffer bonebuffer = ubo.getBuffer(true);
            for (int i= 0; i < bufferJoints; i++) {
                Matrix3x4.storeMatrix4f(outframe[i], bonebuffer);
                //Matrix3x4.storeMatrix4f((Matrix4f)new Matrix4f().setIdentity(), bonebuffer);
            }
            ubo.submitBuffer(0, true);
            
            ARBUniformBufferObject.glUniformBlockBinding(shader.getShaderId(), 0, 0);
            ARBUniformBufferObject.glBindBufferBase(ARBUniformBufferObject.GL_UNIFORM_BUFFER, 0, ubo.getHandle());
            
            GLRef.checkError();
            if(gpu_skin_dirty && shadowCasting) gpu_skin_dirty = false;
            else gpu_skin_shadow_dirty = false;
        //}

        if(envMap == null) {
            envMap = Ref.ResMan.LoadTexture("data/textures/skybox/ibl_sky", true);
            envMap.textureSlot = 1;
        }

        if(Ref.cgame == null || !Ref.glRef.shadowMan.isEnabled()) {
            // Bind environmentmap
            envMap.textureSlot = 2;
            envMap.Bind();
            // Set light

            //Ref.glRef.setLIght();
            shader.setUniform("lightDirection", view.lights.get(0).getDirection());
        } else if(!shadowCasting) {
            // bind depth texture and setup shadow uniforms
            preShadowRecieve(shader, view);
        }

        // Set model matrix
        getModelMatrix(axis, position);

        shader.setUniform("Modell", modelMatrix);

        // render
        glDisable(GL_BLEND);
        if(color != null) Helper.col(color);
        renderFromVBO(shadowCasting);
        glEnable(GL_BLEND);
        Ref.glRef.PopShader();
    }

    private void preShadowRecieve(Shader shader, ViewParams view) {
        if(Ref.glRef.deferred.isRendering()) return; // only for forward rendering
        // Bind environmentmap
        if(envMap == null) envMap = Ref.ResMan.LoadTexture("data/textures/skybox/ibl_sky", true);
        envMap.textureSlot = 2;
        envMap.Bind();
        
        Light light = view.lights.get(0);
        CubeTexture depth = light.getShadowResult().getDepthTexture();
        depth.textureSlot = 1;
        depth.Bind();

        Matrix4f[] shadowmat = light.getShadowResult().getShadowViewProjections(4, view.lights.get(0));
        Vector4f shadowDepths = view.lights.get(0).getShadowResult().getCascadeDepths();
        shader.setUniform("shadowMatrix", shadowmat);
        shader.setUniform("cascadeDistances", shadowDepths);
        shader.setUniform("shadow_bias", Ref.cvars.Find("shadow_bias").fValue);
        shader.setUniform("shadow_factor", Ref.cvars.Find("shadow_factor").fValue);
        shader.setUniform("pcfOffsets", light.getShadowResult().getPCFoffsets());
        shader.setUniform("lightDirection", view.lights.get(0).getDirection());
        GLRef.checkError();
    }

    public void render(Vector3f position, Vector3f[] axis, Vector4f color, boolean shadowPass, ViewParams view) {
        if(isStatic()) {
            renderStatic(position, axis, color, shadowPass, view);
            return;
        } else if(gpuskinning && shapeKeys == null) {
            renderGPU(position, axis, color, shadowPass, view);
            return;
        }

        Shader shader = null;
        if(shadowPass) {
            shader = Ref.glRef.getShader("unlitObject");
        }  else {
            shader = Ref.glRef.getShader("litobjectpixel");
        }
        
        Ref.glRef.PushShader(shader);

        if(!shadowPass && Ref.cgame != null) {
            preShadowRecieve(shader, view);
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
        viewbuffer.put(axis[0].x); viewbuffer.put(axis[0].y); viewbuffer.put(axis[0].z);viewbuffer.put(0);
        viewbuffer.put(axis[1].x); viewbuffer.put(axis[1].y); viewbuffer.put(axis[1].z); viewbuffer.put(0);
        viewbuffer.put(axis[2].x); viewbuffer.put(axis[2].y); viewbuffer.put(axis[2].z);viewbuffer.put(0);
        viewbuffer.put(position.x); viewbuffer.put(position.y); viewbuffer.put(position.z); viewbuffer.put(1);
        viewbuffer.flip();
        Matrix4f mMatrix = (Matrix4f) modelMatrix.load(viewbuffer);
        viewbuffer.clear();
        return mMatrix;
    }

    private void renderFromVBO(boolean shadowPass) {
        for (IQMMesh mesh : meshes) {
            if(mesh.name.startsWith("@")) continue; // dont draw control-meshes
            if(!shadowPass) {
                mesh.bindTextures();
                glCullFace(GL_FRONT);
            } else {
                glCullFace(GL_BACK);
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
            if(!shadowPass) {
                mesh.unbindTextures();
                glCullFace(GL_BACK);
            } else {
                glCullFace(GL_FRONT);
            }
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

        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS, 4, GL11.GL_FLOAT, false, stride, offset);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COORDS); // tan
        offset += 4*4;
        
        if(in_texcoord != null) {
            glTexCoordPointer(2, GL_FLOAT, stride, offset);
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            offset += 4*2;
        }
        if(in_blendindex != null) {
            ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COLOR, 4, GL11.GL_UNSIGNED_BYTE, false, stride, offset);
            ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COLOR); // position

            offset += 4;
        }
        if(in_blendweight != null) {
            ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS2, 4, GL11.GL_UNSIGNED_BYTE, true, stride, offset);
            ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COORDS2); // position
            offset += 4;
        }
    }

    private void postVBOGpu() {
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_NORMAL_ARRAY);
        ARBVertexShader.glDisableVertexAttribArrayARB(1); // position
        if(in_texcoord != null) {
            glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        }
        if(in_blendindex != null) {
            ARBVertexShader.glDisableVertexAttribArrayARB(Shader.INDICE_COLOR); // position
        }
        if(in_blendweight != null) {
            ARBVertexShader.glDisableVertexAttribArrayARB(Shader.INDICE_COORDS2); // position
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
                mesh.bindTextures();
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
                        if(pos == null) {
                            pos = in_position[indice];
                        }
                    }

                    if(normal != null) glNormal3f(normal.x, normal.y, normal.z);
                    
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
        
        if( true && attachments != null) {
            Ref.glRef.pushShader("World");
            glDepthFunc(GL_ALWAYS);
            for (BoneAttachment attach : attachments.values()) {
                Vector4f attachPos = new Vector4f(attach.lastposition);
                Matrix4f.transform(modelMatrix, attachPos, attachPos);
                
                float lineSize = 5f;
                glBegin(GL_LINES);
                for (int j= 0; j < 3; j++) {
                    if(j == 0) Helper.col(10, 0, 0);
                    if(j == 1) Helper.col(0, 10, 0);
                    if(j == 2) Helper.col(0, 0, 10);

                    glVertex3f(attachPos.x- attach.axis[j].x * lineSize,
                            attachPos.y- attach.axis[j].y * lineSize,
                            attachPos.z- attach.axis[j].z * lineSize);
                    glVertex3f( attachPos.x+attach.axis[j].x * lineSize,
                           attachPos.y+ attach.axis[j].y * lineSize,
                             attachPos.z+attach.axis[j].z * lineSize);
                }
                glEnd();
                
            }
            glDepthFunc(GL_LEQUAL);
            Ref.glRef.PopShader();
        }

        if(true) {
            Ref.glRef.pushShader("World");
            glLineWidth(2.0f);
            glDepthFunc(GL_ALWAYS);
            glBegin(GL_LINES);
            Vector4f v = new Vector4f();
            for (int i= 0; i < joints.length; i++) {
                IQMJoint joint = joints[i];
                IQMJoint parent = joint.parent >= 0 ? joints[joint.parent] : null;
                if(!boneUsage[i]) continue;
                
                Helper.col(0, 0, 4);
                v.set(0,0,0,1);
                Matrix4f.transform(joint.baseframe, v, v);
                Matrix4f.transform(outframe[i], v, v);
                Matrix4f.transform(modelMatrix, v, v);
                
                Vector3f[] boneaxis = new Vector3f[3];
                for (int j = 0; j < 3; j++) {
                    boneaxis[j] = new Vector3f();
                }
                Matrix4f boneMatrix = Matrix4f.transpose(outframe[i], null);
                Helper.matrixToAxis(Matrix4f.mul(boneMatrix, Matrix4f.invert(modelMatrix, null), null), boneaxis);
                
                if(parent != null) {
                    float lineSize = 3f;
                    for (int j= 0; j < 3; j++) {
                        if(j == 0) Helper.col(10, 0, 0);
                        if(j == 1) Helper.col(0, 10, 0);
                        if(j == 2) Helper.col(0, 0, 10);
                        boneaxis[j].normalise();
                        
                        glVertex3f(v.x - boneaxis[j].x * lineSize,
                                v.y - boneaxis[j].y * lineSize,
                                v.z - boneaxis[j].z * lineSize);
                        glVertex3f(v.x + boneaxis[j].x * lineSize,
                                v.y + boneaxis[j].y * lineSize,
                                v.z + boneaxis[j].z * lineSize);
                    }
                }
                
                glVertex3f(v.x, v.y, v.z);
                
                if(parent != null) {
                    Helper.col(4, 0, 0);
                    v.set(0,0,0,1);
                    Matrix4f.transform(parent.baseframe, v, v);
                    Matrix4f.transform(outframe[joint.parent], v, v);
                    Matrix4f.transform(modelMatrix, v, v);
                    glVertex3f(v.x, v.y, v.z);
                } else {
                    Helper.col(0, 4, 0);
                    glVertex3f(position.x, position.y, position.z);
                }
                
            }

            glEnd();
            glDepthFunc(GL_LEQUAL);
            Ref.glRef.PopShader();
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
        if(true) {
            Ref.glRef.pushShader("World");
            //glDepthMask(false);
            glDepthFunc(GL_ALWAYS);
            
            glLineWidth(2.0f);
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
            //glDepthMask(true);
            glDepthFunc(GL_LEQUAL);
            
            Ref.glRef.PopShader();
        }
    }

    public Vector3f getMins() {
        return min;
    }

    public Vector3f getMaxs() {
        return max;
    }
   
}
