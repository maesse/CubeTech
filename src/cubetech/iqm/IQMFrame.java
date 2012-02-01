package cubetech.iqm;

import cubetech.CGame.Render;
import cubetech.CGame.ViewParams;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.gfx.*;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import nbullet.util.Transform;
import org.lwjgl.opengl.ARBUniformBufferObject;
import org.lwjgl.opengl.ARBVertexShader;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL20.*;
import org.lwjgl.util.vector.*;

/**
 * Things that are generated from an iqmmodel thats only valid for one animation frame
 * @author Mads
 */
public class IQMFrame {
    private static HashMap<Shader, ShaderUBO> uboMap = new HashMap<Shader, ShaderUBO>();
    private static Shader gpushader;
    private static Shader gpushaderShadow;
    private static Shader gpushaderLit;
    private static Shader gpushaderLitDeferred;
    
    static {
        initGPUShader();
    }
    
    private IQMModel model;
    
    // For oldschool rendering
    private CubeTexture envMap = null;
    private Vector3f[] out_position;
    private Vector3f[] out_normal;
    private Vector3f[] out_tangent;
    private Vector3f[] out_bitangent;
    
    // Frame data
    private boolean isStatic = false;
    Matrix4f[] outframe;
    int frame;
    int oldframe;
    float backlerp;
    IQMAnim currentAnim = null;
    public ArrayList<BoneController> controllers = null;
    HashMap<String, BoneAttachment> attachments = new HashMap<String, BoneAttachment>();
    
    RigidBoneMesh rigidBoneMesh;
    
    // Generated
    Matrix4f modelMatrix = new Matrix4f();
    HashMap<IQMMesh, Vector3f[]> boneMeshes = null;
    
    
    public IQMFrame(IQMModel model, int frame, int oldframe, float backlerp) {
        this.model = model;
        this.frame = frame;
        this.oldframe = oldframe;
        this.backlerp = backlerp;
        isStatic = model.isStatic();
    }
    
    public IQMModel getModel() {
        return model;
    }
    
//    private void animateBoneMeshes() {
//        boneMeshes = new HashMap<IQMMesh, Vector3f[]>();
//        for (IQMMesh mesh : model.boneMeshes.keySet()) {
//            IQMJoint joint = model.boneMeshes.get(mesh);
//            RigidBoneMesh bmesh = new RigidBoneMesh(Ref.cgame.physics.getWorld(), model, mesh, joint);
//            Vector3f[] vertices = bmesh.poseMesh(this);
//            boneMeshes.put(mesh, vertices);
//        }
//        
//    }
    
    
    
    
    
    
    
    
    // modelspace skinned mesh
    private Vector3f[] getAnimatedVertices(IQMMesh mesh) {
        int first = mesh.first_vertex;
        int count = mesh.num_vertexes;
        Vector3f[] vertices = new Vector3f[count];
        buildVertices(first, count, vertices, null, null, null);
        return vertices;
    }
    
    
    
    private void renderStatic(Vector3f position, Vector3f[] axis, Vector4f color, int renderFlags, ViewParams view) {
        createStaticBuffer(true);
        boolean shadows = (renderFlags & Render.RF_SHADOWPASS) != 0;
        boolean deferred = (renderFlags & Render.RF_POSTDEFERRED) == 0;
        Shader shader = null;
        if(shadows) {
            shader = Ref.glRef.getShader("unlitObject");
        } else {
            if(deferred) {
                shader = Ref.glRef.getShader("modelDeferred");
            } else {
                shader = Ref.glRef.getShader("litobjectpixel_1");
            }
        }

        Ref.glRef.PushShader(shader);

        if(!shadows) {
            preShadowRecieve(shader, view);
        }

        Helper.getModelMatrix(axis, position, modelMatrix);
        shader.setUniform("Modell", modelMatrix);
        Matrix4f.mul(view.viewMatrix, modelMatrix, modelMatrix);
        shader.setUniform("ModelView", modelMatrix);
        shader.setUniform("Projection", view.ProjectionMatrix);
        shader.setUniform("far", view.farDepth);

        if(color != null) Helper.col(color);
        glDisable(GL_BLEND);
        renderFromVBO(shadows);
        
        glEnable(GL_BLEND);

        
        
        Ref.glRef.PopShader();
    }

    

    private void updateUBO(Shader shader) {
        ShaderUBO ubo = uboMap.get(shader);
        if(ubo == null) {
            Ref.common.Error(Common.ErrorCode.FATAL, "Implement ubo workaround");
        }

        int jointCapacity = ubo.getSize()/(4*4*3);
        int bufferJoints = model.header.num_joints;
        if(bufferJoints > jointCapacity) {
            Common.Log("Too many bones!");
            bufferJoints = jointCapacity;
        }

        ByteBuffer bonebuffer = ubo.getBuffer(true);
        for (int i= 0; i < bufferJoints; i++) {
            Matrix3x4.storeMatrix4f(outframe[i], bonebuffer);
        }
        ubo.submitBuffer(0, true);

        ARBUniformBufferObject.glUniformBlockBinding(shader.getShaderId(), 0, 0);
        ARBUniformBufferObject.glBindBufferBase(ARBUniformBufferObject.GL_UNIFORM_BUFFER, 0, ubo.getHandle());

        GLRef.checkError();
    }
    
    public void renderGPUSkinned(Vector3f position, Vector3f[] axis, Vector4f color, int renderFlags, ViewParams view) {
        if(model.isStatic()) {
            renderStatic(position, axis, color, renderFlags, view);
            return;
        }
        createDynamicBuffer(false);
        boolean shadows = (renderFlags & Render.RF_SHADOWPASS) != 0;
        boolean deferred = (renderFlags & Render.RF_POSTDEFERRED) == 0;
        
        //boolean shadowsEnabled = fix
        
        // Determine shader to use
        Shader shader;
        if(shadows) {
            shader = gpushader;
        } else {
            shader = deferred ? gpushaderLitDeferred : gpushaderShadow;
        }
        Ref.glRef.PushShader(shader);
        
        // Set up shader-specific uniforms
        if(deferred) {
            shader.setUniform("far", Ref.cvars.Find("cg_depthfar").fValue); 
            shader.setUniform("near", Ref.cvars.Find("cg_depthnear").fValue); 
        } else {
            if(envMap == null) {
                envMap = Ref.ResMan.LoadTexture("data/textures/skybox/ibl_sky", true);
                envMap.textureSlot = 1;
            }
            if(Ref.cgame == null || !Ref.glRef.shadowMan.isEnabled()) {
                // Bind environmentmap
                envMap.textureSlot = 2;
                envMap.Bind();
                // Set light
                shader.setUniform("lightDirection", view.lights.get(0).getDirection());
            } else if(!shadows) {
                // bind depth texture and setup shadow uniforms
                preShadowRecieve(shader, view);
            }
        }

        // Update ubo if necesary
        updateUBO(shader);        

        // Set model matrix
        Helper.getModelMatrix(axis, position, modelMatrix);
        shader.setUniform("ModelView", Matrix4f.mul(view.viewMatrix, modelMatrix, modelMatrix));
        shader.setUniform("Projection", view.ProjectionMatrix);

        // render
        glDisable(GL_BLEND);
        if(color != null) Helper.col(color);
        renderFromVBO(shadows);
        glEnable(GL_BLEND);
        
        Ref.glRef.PopShader();
        
//        debugdraw(position, axis, view);
    }
    
    public void renderCPUSkinned(Vector3f position, Vector3f[] axis, Vector4f color, int renderFlags, ViewParams view) {
        if(model.isStatic()) {
            renderStatic(position, axis, color, renderFlags, view);
            return;
        }
        if(out_bitangent == null) out_bitangent = new Vector3f[model.header.num_vertexes];
        if(out_normal == null) out_normal = new Vector3f[model.header.num_vertexes];
        if(out_position == null) out_position = new Vector3f[model.header.num_vertexes];
        if(out_tangent == null) out_tangent = new Vector3f[model.header.num_vertexes];
        buildVertices(0, model.header.num_vertexes, out_position, out_normal, out_tangent, out_bitangent);
        
        boolean shadows = (renderFlags & Render.RF_SHADOWPASS) != 0;
        boolean deferred = (renderFlags & Render.RF_POSTDEFERRED) == 0;
        Shader shader = null;
        if(shadows) {
            shader = Ref.glRef.getShader("unlitObject");
        }  else {
//            if(Ref.glRef.deferred.isRendering()) {
//                shader = Ref.glRef.getShader("modelDeferred");
//            } else
            {
                shader = Ref.glRef.getShader("litobjectpixel");
            }
        }
        
        Ref.glRef.PushShader(shader);
        
        if(deferred) {
            shader.setUniform("far", Ref.cvars.Find("cg_depthfar").fValue); 
            shader.setUniform("near", Ref.cvars.Find("cg_depthnear").fValue); 
        }

        if(!shadows && Ref.cgame != null) {
            preShadowRecieve(shader, view);
        }

        Helper.getModelMatrix(axis, position, modelMatrix);
        shader.setUniform("Modell", modelMatrix);
        Matrix4f.mul(view.viewMatrix, modelMatrix, modelMatrix);
        shader.setUniform("ModelView", modelMatrix);
        shader.setUniform("Projection", view.ProjectionMatrix);
        

        if(color != null) Helper.col(color);

        renderOldSchool(shadows);
        
        Ref.glRef.PopShader();

        debugdraw(position, axis, view);
    }    

    private void renderFromVBO(boolean shadowPass) {
        for (IQMMesh mesh : model.meshes) {
            boolean collisionMesh = mesh.name.equalsIgnoreCase("@convexcollision");
            if(collisionMesh) {
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            } else if(mesh.name.startsWith("@")) continue; // dont draw control-meshes
            
            if(!shadowPass) {
                mesh.bindTextures();
                glCullFace(GL_FRONT);
            } else {
                glDisable(GL_BLEND);
                glCullFace(GL_BACK);
            }
            model.cachedIndiceBuffer.bind();
            model.cachedVertexBuffer.bind();
            if(model.isStatic()) preVboStatic(model.cachedVertexBuffer);
            else preVBOGpu(model.cachedVertexBuffer);
            int firstVertex = mesh.first_vertex;
            int lastVertex = mesh.first_vertex + mesh.num_vertexes;
            glDrawRangeElements(GL_TRIANGLES, firstVertex, lastVertex
                    , mesh.num_triangles*3, GL_UNSIGNED_INT, mesh.first_triangle*3*4);
            //GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.num_triangles*3, GL11.GL_UNSIGNED_INT, mesh.first_triangle);
            if(isStatic) postVboStatic();
            else postVBOGpu();
            model.cachedIndiceBuffer.unbind();
            model.cachedVertexBuffer.unbind();
            if(!shadowPass) {
                mesh.unbindTextures();
                glCullFace(GL_BACK);
            } else {
                glCullFace(GL_FRONT);
            }
            if(collisionMesh) glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }

    

    private void renderOldSchool(boolean shadowPass) {
        for (int i= 0; i < model.header.num_meshes; i++) {
            IQMMesh mesh = model.meshes[i];
            if(mesh.name.startsWith("@")) continue; // dont draw control-meshes
            if(!shadowPass) {
                mesh.bindTextures();
                glCullFace(GL_FRONT);
            }

            glBegin(GL_TRIANGLES);
            for (int j= 0; j < mesh.num_triangles; j++) {
                IQMTriangle tri = model.triangles[mesh.first_triangle+j];
                for (int k= 0; k < tri.vertex.length; k++) {
                    int indice = tri.vertex[k];
                    if(indice < 0) {
                        continue;
                    }
                    Vector2f coords = model.in_texcoord[indice];
                    Helper.tex(coords.x, 1-coords.y);

                    Vector3f normal;
                    Vector3f pos;
                    if(model.header.num_frames <= 5) {
                        // assume un-animated
                        normal = model.in_normal[indice];
                        pos = model.in_position[indice];
                    } else {
                        normal = out_normal[indice];
                        pos = out_position[indice];
                        if(pos == null) {
                            pos = model.in_position[indice];
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
    
    private void preVBOGpu(VBO buffer) {
        int offset = 0;
        int stride = buffer.stride;
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_POSITION, 3, GL_FLOAT, false, stride, offset);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_POSITION);
        offset += 4*3;

        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_NORMAL, 3, GL_FLOAT, false, stride, offset);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_NORMAL);
        offset += 4*3;

        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_TANGENT, 4, GL_FLOAT, false, stride, offset);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_TANGENT); // tan
        offset += 4*4;
        
        if(model.in_texcoord != null) {
            ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS, 2, GL_FLOAT, false, stride, offset);
            ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COORDS); // position
            offset += 4*2;
        }
        if(model.in_blendindex != null) {
            ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_BONEINDEX, 4, GL_UNSIGNED_BYTE, false, stride, offset);
            ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_BONEINDEX); // position

            offset += 4;
        }
        if(model.in_blendweight != null) {
            ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_WEIGHT, 4, GL_UNSIGNED_BYTE, true, stride, offset);
            ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_WEIGHT); // position
            offset += 4;
        }
        
    }

    private void postVBOGpu() {
        ARBVertexShader.glDisableVertexAttribArrayARB(Shader.INDICE_POSITION); // position
        ARBVertexShader.glDisableVertexAttribArrayARB(Shader.INDICE_NORMAL); // position
        ARBVertexShader.glDisableVertexAttribArrayARB(Shader.INDICE_TANGENT); // position
        if(model.in_texcoord != null) {
            ARBVertexShader.glDisableVertexAttribArrayARB(Shader.INDICE_COORDS); // position
        }
        if(model.in_blendindex != null) {
            ARBVertexShader.glDisableVertexAttribArrayARB(Shader.INDICE_BONEINDEX); // position
        }
        if(model.in_blendweight != null) {
            ARBVertexShader.glDisableVertexAttribArrayARB(Shader.INDICE_WEIGHT); // position
        }
    }

    private void preVboStatic(VBO buffer) {
        int offset = 0;
        int stride = buffer.stride;
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_POSITION); // position
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_POSITION, 3, GL_FLOAT, false, stride, offset);
        offset += 4*3;
        
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_NORMAL); // color
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_NORMAL, 3, GL_FLOAT, true, stride, offset);
        offset += 4*3;
        
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COORDS); // coords
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS, 2, GL_FLOAT, false, stride, offset);
        offset += 4*2;
        
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_TANGENT); // coords
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_TANGENT, 4, GL_FLOAT, false, stride, offset);
    }

    private void postVboStatic() {
        glDisableVertexAttribArray(Shader.INDICE_POSITION);
        glDisableVertexAttribArray(Shader.INDICE_NORMAL);
        glDisableVertexAttribArray(Shader.INDICE_COORDS);
        glDisableVertexAttribArray(Shader.INDICE_TANGENT);
    }
    
    public BoneAttachment getAttachment(String attachPoint) {
        return attachments.get(attachPoint);
    }
    
    
    // Creates two VBOs to be used for rendering skinned models
    private void createDynamicBuffer(boolean alignStride) {
        if(model.cachedVertexBuffer != null) return;
        
        // Calculate stride
        int stride = 0;
        if(model.in_position != null) stride += 4*3;
        if(model.in_normal != null) stride += 4*3;
        if(model.in_tangent != null) stride += 4*4;
        if(model.in_texcoord != null) stride += 4*2;
        if(model.in_blendindex != null) stride += 4;
        if(model.in_blendweight != null) stride += 4;
        
        if(alignStride) {
            // Align to 32byte boundary
            // Some say this is faster. Others say it isn't really any faster.
            stride = (int)Math.ceil(stride/32f)*32;
        }
        
        model.cachedVertexBuffer = new VBO(model.header.num_vertexes * stride, VBO.BufferTarget.Vertex);
        ByteBuffer vb = model.cachedVertexBuffer.map();
        for (int i= 0; i < model.header.num_vertexes; i++) {
            if(alignStride && vb.position() % stride != 0) {
                // align to stride
                vb.position(i*stride);
            }
            if(model.in_position != null)vb.putFloat(model.in_position[i].x).putFloat(model.in_position[i].y).putFloat(model.in_position[i].z);
            if(model.in_normal != null)vb.putFloat(model.in_normal[i].x).putFloat(model.in_normal[i].y).putFloat(model.in_normal[i].z);
            if(model.in_tangent != null)vb.putFloat(model.in_tangent[i].x).putFloat(model.in_tangent[i].y).putFloat(model.in_tangent[i].z).putFloat(model.in_tangent[i].w);
            if(model.in_texcoord != null)vb.putFloat(model.in_texcoord[i].x).putFloat(1f-model.in_texcoord[i].y);
            if(model.in_blendindex != null)vb.put(model.in_blendindex[i*4]).put(model.in_blendindex[i*4+1]).put(model.in_blendindex[i*4+2]).put(model.in_blendindex[i*4+3]);
            if(model.in_blendweight != null)vb.put(model.in_blendweight[i*4]).put(model.in_blendweight[i*4+1]).put(model.in_blendweight[i*4+2]).put(model.in_blendweight[i*4+3]);
        }
        model.cachedVertexBuffer.stride = stride;

        model.cachedVertexBuffer.unmap();

        createIndiceBuffer();
    }
    
    private void createIndiceBuffer() {
        model.cachedIndiceBuffer = new VBO(4*model.triangles.length*3, VBO.BufferTarget.Index);
        ByteBuffer ib = model.cachedIndiceBuffer.map();
        
        for (int i= 0; i < model.triangles.length; i++) {
            IQMTriangle tri = model.triangles[i];
            for (int j= 0; j < 3; j++) {
                int indice = tri.vertex[j];
                ib.putInt(indice);
            }
        }
        
        model.cachedIndiceBuffer.unmap();
    }

    // Statically initialized
    private static void initGPUShader() {
        if(uboMap.size() > 0) return;
        gpushader = Ref.glRef.getShader("gpuskin");
        gpushaderShadow = Ref.glRef.getShader("gpuskinShadowed");
        gpushaderLit = Ref.glRef.getShader("gpuskinLit");
        gpushaderLitDeferred = Ref.glRef.getShader("gpuskinLitDeferred");
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

    private void createStaticBuffer(boolean alignStride) {
        if(model.cachedVertexBuffer != null) return;
        createIndiceBuffer();

        // Create vertex buffer
        int stride = 48;
        if(alignStride) stride = 64;
        int size = stride * model.in_position.length;
        model.cachedVertexBuffer = new VBO(size, VBO.BufferTarget.Vertex);
        ByteBuffer vb = model.cachedVertexBuffer.map();
        for (int i= 0; i < model.in_position.length; i++) {
            if(vb.position() % stride != 0) {
                // align to stride
                vb.position(i*stride);
            }
            Vector3f p = model.in_position[i];
            Vector3f n = model.in_normal[i];
            Vector2f t = model.in_texcoord[i];
            Vector4f tan = model.in_tangent[i];
            vb.putFloat(p.x).putFloat(p.y).putFloat(p.z);
            vb.putFloat(n.x).putFloat(n.y).putFloat(n.z);
            vb.putFloat(t.x).putFloat(1f-t.y);
            vb.putFloat(tan.x).putFloat(tan.y).putFloat(tan.z).putFloat(tan.w);
            
        }
        model.cachedVertexBuffer.stride = stride;
        model.cachedVertexBuffer.unmap();
    }
    
    private void buildVertices(int start, int count, 
            Vector3f[] out_position, 
            Vector3f[] out_normal, 
            Vector3f[] out_tangent, 
            Vector3f[] out_bitangent) {
        
        // The actual vertex generation based on the matrixes follows...
        int iIndex = 0;
        int iWeight = 0;
        Matrix3f matnorm = new Matrix3f();
        Matrix4f dest = new Matrix4f();
        Matrix4f temp = new Matrix4f();

        for (int i= start; i < start+count; i++) {
            iIndex = i*4;
            iWeight = i*4;
            // Blend matrixes for this vertex according to its blend weights.
            // the first index/weight is always present, and the weights are
            // guaranteed to add up to 255. So if only the first weight is
            // presented, you could optimize this case by skipping any weight
            // multiplies and intermediate storage of a blended matrix.
            // There are only at most 4 weights per vertex, and they are in
            // sorted order from highest weight to lowest weight. Weights with
            // 0 values, which are always at the end, are unused.
            float f = (model.in_blendweight[iWeight]&0xff)/255.0f;
            //if(buildBoneUsage) model.boneUsage[model.in_blendindex[iIndex]&0xff] = true;
            Helper.scale(f, outframe[model.in_blendindex[iIndex]&0xff], dest);
            for (int j= 1; j < 4 && (model.in_blendweight[iWeight+j]&0xff) != 0; j++) {
                //if(buildBoneUsage) model.boneUsage[model.in_blendindex[iIndex+j]&0xff] = true;
                f = (model.in_blendweight[iWeight+j])/255.0f;
                Helper.scale(f, outframe[model.in_blendindex[iIndex+j]&0xff], temp);
                Matrix4f.add(dest, temp, dest);
                dest.m33 = 1;
            }

            // Transform attributes by the blended matrix.
            // Position uses the full 3x4 transformation matrix.
            // Normals and tangents only use the 3x3 rotation part
            // of the transformation matrix.
            if(model.shapeKeys != null) {
                out_position[i-start] = Helper.transform(dest, out_position[i-start], out_position[i-start]);
            } else {
                out_position[i-start] = Helper.transform(dest, model.in_position[i], out_position[i-start]);
            }

            matnorm = Helper.toNormalMatrix(dest, matnorm);
            if(out_normal != null) out_normal[i-start] = Helper.transform(matnorm, model.in_normal[i], out_normal[i-start]);
            if(out_tangent != null) out_tangent[i-start] = Helper.transform(matnorm, model.in_tangent[i], out_tangent[i-start]);
            if(out_normal != null && out_tangent != null && out_bitangent != null) {
                out_bitangent[i-start] = Vector3f.cross(out_normal[i-start], out_tangent[i-start], out_bitangent[i-start]);
                out_bitangent[i-start].scale(model.in_tangent[i].w);
            }

            
        }
    }

    private void debugdraw(Vector3f position, Vector3f[] axis, ViewParams view) {
        // Draw BBOx
        if( false) {
//            IQMBounds b = bounds[currentFrameIndex];
            glPushMatrix();
            glTranslatef(position.x, position.y, position.z);
            Vector3f min = model.min;
            Vector3f max = model.max;
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
        
//        animateBoneMeshes();
//        if(boneMeshes != null) {
//            
//            Shader shader = Ref.glRef.getShader("modelDeferred");
//            Ref.glRef.PushShader(shader);
//            shader.setUniform("ModelView", modelMatrix);
//            shader.setUniform("Projection", view.ProjectionMatrix);
//            shader.setUniform("far", Ref.cvars.Find("cg_depthfar").fValue); 
//            shader.setUniform("near", Ref.cvars.Find("cg_depthnear").fValue); 
//            glDepthFunc(GL_ALWAYS);
//            glDepthFunc(GL_LEQUAL);
//            glBegin(GL_TRIANGLES);
//            for (IQMMesh mesh : boneMeshes.keySet()) {
//                Vector3f[] vertices = boneMeshes.get(mesh);
//                for (int i = 0; i < mesh.num_triangles; i++) {
//                    IQMTriangle tri = model.triangles[mesh.first_triangle+i];
//                    int indice = tri.vertex[0]-mesh.first_vertex;
//                    
//                    glVertexAttrib3f(Shader.INDICE_POSITION, vertices[indice].x, vertices[indice].y, vertices[indice].z);
//                    indice = tri.vertex[2]-mesh.first_vertex;
//                    glVertexAttrib3f(Shader.INDICE_POSITION, vertices[indice].x, vertices[indice].y, vertices[indice].z);
//                    indice = tri.vertex[1]-mesh.first_vertex;
//                    glVertexAttrib3f(Shader.INDICE_POSITION, vertices[indice].x, vertices[indice].y, vertices[indice].z);
//                    
//                }
//                
//            }
//            glEnd();
//            glDepthFunc(GL_LEQUAL);
//            Ref.glRef.PopShader();
//        }

        if(true) {
            Ref.glRef.pushShader("World");
            glLineWidth(2.0f);
            glDepthFunc(GL_ALWAYS);
            glBegin(GL_LINES);
            Vector4f v = new Vector4f();
            for (int i= 0; i < model.joints.length; i++) {
                IQMJoint joint = model.joints[i];
//                if(model.boneUsage != null && !model.boneUsage[i]) continue;
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
                Helper.matrixToAxis(Matrix4f.mul(boneMatrix, Matrix4f.invert(modelMatrix, null), boneMatrix), boneaxis);
                
                float lineSize = 3f;
                for (int j= 0; j < 3; j++) {
                    lineSize = 3f;
                    if(j == 0) Helper.col(10, 0, 0);
                    if(j == 1) Helper.col(0, 10, 0);
                    if(j == 2) {Helper.col(0, 0, 10); lineSize *= 2f;}
                    boneaxis[j].normalise();

                    glVertex3f(v.x - boneaxis[j].x * lineSize * 0.5f,
                            v.y - boneaxis[j].y * lineSize* 0.5f,
                            v.z - boneaxis[j].z * lineSize* 0.5f);
                    glVertex3f(v.x + boneaxis[j].x * lineSize,
                            v.y + boneaxis[j].y * lineSize,
                            v.z + boneaxis[j].z * lineSize);
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
            for (IQMMesh mesh : model.meshes) {
                for (int i= 0; i < mesh.num_vertexes; i++) {
                    Vector3f normal;
                    Vector3f pos;
                    if(model.header.num_frames <= 5) {
                        // assume un-animated
                        normal = model.in_normal[i];
                        pos = model.in_position[i];
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
    
    // Gets bone transforms for this current frame
    private void getBoneInfo(IQMJoint joint, Matrix4f modelMatrix, Vector3f origin, Matrix3f axis) {
        Vector4f v = new Vector4f(0,0,0,1);
        Matrix4f.transform(joint.baseframe, v, v);
        Matrix4f.transform(outframe[joint.index], v, v);
        Matrix4f.transform(modelMatrix, v, v);

        Vector3f[] boneaxis = new Vector3f[3];
        for (int j = 0; j < 3; j++) {
            boneaxis[j] = new Vector3f();
        }
        Matrix4f boneMatrix = Matrix4f.transpose(outframe[joint.index], null);
        Matrix4f b = Matrix4f.mul(boneMatrix, Matrix4f.invert(modelMatrix, null), boneMatrix);
        Helper.matrixCopy(b, axis);
        origin.set(v);
    }

    public Matrix4f getPoseMatrix(IQMJoint joint) {
        if(joint.index < 0 || joint.index >= outframe.length) return null;
        return outframe[joint.index];
    }
    
    public void forcePoseMatrix(IQMJoint joint, Matrix4f pose) {
        if(joint.index < 0 || joint.index >= outframe.length) return;
        outframe[joint.index].load(pose);
    }
}
