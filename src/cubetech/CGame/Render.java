package cubetech.CGame;

import cubetech.collision.ClientCubeMap;
import cubetech.collision.CubeChunk;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Common.ErrorCode;
import cubetech.common.Helper;
import cubetech.gfx.*;
import cubetech.gfx.PolyBatcher.PolyBatch;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.*;

/**
 *
 * @author mads
 */
public class Render {
    public static final int RF_SHADOWPASS = 1;
    public static final int RF_POSTDEFERRED = 2;
    private static final int MAX_RENDER_ENTITIES = 1000;
    private RenderEntity[] entities = new RenderEntity[MAX_RENDER_ENTITIES];
    private int next = 0;
    
    CVar r_gpuskin = Ref.cvars.Get("r_gpuskin", "1", EnumSet.of(CVarFlags.NONE));
    public CVar r_nocull = Ref.cvars.Get("r_nocull", "0", EnumSet.of(CVarFlags.TEMP));
    public CVar r_bonedebug = Ref.cvars.Get("r_bonedebug", "0", EnumSet.of(CVarFlags.TEMP));
    public CVar r_batchsprites = Ref.cvars.Get("r_batchsprites", "1", EnumSet.of(CVarFlags.TEMP));
    
    private ArrayList<RenderEntity> renderList = new ArrayList<RenderEntity>();
    private ArrayList<ViewParams> viewList = new ArrayList<ViewParams>();
    public ViewParams view = null;
    private int currentRenderFlags = 0;
    
    private PolyBatcher polyBatcher = new PolyBatcher();

    public Render() {
        for (int i= 0; i < MAX_RENDER_ENTITIES; i++) {
            entities[i] = new RenderEntity(REType.NONE);
        }
    }
    
    public void newFrame() {
        polyBatcher.reset();
    }
    
    /**
     * Attached the current renderlist to the given view and clears
     * the list for the next view
     * @param view 
     */
    public void assignAndClear(ViewParams view) {
        view.renderList = new RenderList(renderList);
        renderList = new ArrayList<RenderEntity>();
        viewList.add(view);
    }

    public void renderAll(ViewParams view, int renderFlags) {
        this.view = view;
        SecTag t = Profiler.EnterSection(Sec.RENDER);
        SecTag sub;
        GLRef.checkError();
        currentRenderFlags = renderFlags;
        
        boolean shadowPass = (renderFlags & RF_SHADOWPASS) != 0;
        boolean postDeferred = (renderFlags & RF_POSTDEFERRED) != 0;
        
        
        
        if(postDeferred && !shadowPass && r_batchsprites.isTrue()) {
            sub = Profiler.EnterSection(Sec.RENDER_POLYBATCH);
            polyBatcher.beginList();
            sub.ExitSection();
        }
        
        
        
        for (RenderEntity re : view.renderList.list) {
            if(shadowPass) {
                // Skip render entities that doesn't cast shadows
                if(re.hasFlag(RenderEntity.FLAG_NOSHADOW)) continue;
                if(re.Type == REType.SPRITE) continue; // sprites are rendered without depth writing
            }
            
            if(postDeferred != re.hasFlag(RenderEntity.FLAG_NOLIGHT)) {
                if(re.Type == REType.MODEL && re.model.needMultiPass) {
                    // This model needs to be rendered multiple times
                } else
                {
                    continue;
                }
            }
            
            Matrix4f tmpMatrix = null;
            if(re.hasFlag(RenderEntity.FLAG_WEAPONPROJECTION)) {
                tmpMatrix = view.ProjectionMatrix;
                view.ProjectionMatrix = view.getWeaponProjection();
            }
            
            switch(re.Type) {
                case WORLD:
                    sub = Profiler.EnterSection(Sec.RENDER_WORLD);
                    renderWorld(re);
                    sub.ExitSection();
                    break;
                case SKYBOX:
                    if(re.renderObject instanceof SkyBox) {
                        SkyBox sky = (SkyBox) re.renderObject;
                        sky.Render(view);
                    }
                    break;
                case MODEL:
                    sub = Profiler.EnterSection(Sec.RENDER_IQM);
                    renderModel(re);
                    sub.ExitSection();
                    break;
                case SPRITE:
                    sub = Profiler.EnterSection(Sec.RENDER_SPRITE);
                    renderSprite(re);
                    sub.ExitSection();
                    break;
                case BBOX:
                    renderBBox(re);
                    break;
                case BEAM:
                    sub = Profiler.EnterSection(Sec.RENDER_SPRITE);
                    renderBeam(re);
                    sub.ExitSection();
                    break;
                case POLY:
                    sub = Profiler.EnterSection(Sec.RENDER_SPRITE);
                    renderPoly(re, shadowPass);
                    sub.ExitSection();
                    break;
                case POLYBATCH:
                    sub = Profiler.EnterSection(Sec.RENDER_POLYBATCH);
                    renderPolyBatch(re);
                    sub.ExitSection();
                    break;
                default:
                    Ref.common.Error(ErrorCode.FATAL, "Render.renderAll(): unknown type " + re.Type);
                    break;
            }
            
            if(re.hasFlag(RenderEntity.FLAG_WEAPONPROJECTION)) {
                view.ProjectionMatrix = tmpMatrix;
            }
            
        }
        
        if(postDeferred && !shadowPass && r_batchsprites.isTrue()) {
            sub = Profiler.EnterSection(Sec.RENDER_POLYBATCH);
            PolyBatch b = polyBatcher.finishList();
            if(b != null) {
                // hot-wire a renderentity
                
                RenderEntity rent = new RenderEntity(REType.POLYBATCH);
                rent.renderObject = b;
                
                // render batch
                
                renderPolyBatch(rent);
                
            }
            sub.ExitSection();
        }
        
        t.ExitSection();
        GLRef.checkError();
        this.view = null;
    }

    private void renderWorld(RenderEntity ent) {
        CubeChunk[] chunkList = (CubeChunk[]) ent.renderObject;
        ClientCubeMap.renderChunkList(chunkList, view);
    }
    
    private void renderPolyBatch(RenderEntity re) {
        // get render batches
        if(!(re.renderObject instanceof AbstractBatchRender)) return;
        AbstractBatchRender calls = (AbstractBatchRender)re.renderObject;
        
        calls.setState(view);
        
        // setup vbo
        calls.vbo.bind();
        calls.vbo.strideInfo.enableAtribs();
        calls.vbo.strideInfo.applyInfo();
        
        
        // bind default materials
        Ref.ResMan.getNoNormalTexture().Bind();
        Ref.ResMan.getNoSpecularTexture().Bind();
        
        // Submit rendercalls
        for (int i = 0; i < calls.calls.size(); i++) {
            IBatchCall call = calls.calls.get(i);
            
            // change texture
            call.getMaterial().getTexture().Bind();
            
            // render vertices
            GL11.glDrawArrays(call.getDrawMode(), call.getVertexOffset(), call.getVertexCount());
        }
        
        // unbind vbo
        calls.vbo.strideInfo.disableAttribs();
        calls.vbo.unbind();
        
        calls.unsetState();
    }

    public void renderPoly(RenderEntity ent, boolean shadowpass) {
        Shader shader = null;
        boolean deferredPoly = false;
        if(shadowpass) {
            shader = Ref.glRef.getShader("unlitObject");
        } else {
            if(Ref.glRef.deferred.isRendering() && !ent.hasFlag(RenderEntity.FLAG_NOLIGHT)) {
                shader = Ref.glRef.getShader("PolyDeferred");
                deferredPoly = true;
            }
            else shader = Ref.glRef.getShader("Poly");
        }
        
        Ref.glRef.PushShader(shader);
        if(shadowpass) {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1f, 1);
            shader.setUniform("far", view.farDepth);
            Matrix4f modelMatrix = Helper.getModelMatrix(ent.axis, ent.origin, null);
            shader.setUniform("Modell", modelMatrix);
            GL11.glCullFace(GL11.GL_FRONT);
        } 
        else { 
            if(!deferredPoly) {
                Helper.col(ent.color);
                
                GL11.glEnable(GL11.GL_BLEND);
                GLState.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                
                
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(-1f, 0);
                GL11.glDepthMask(false);
            } else {
                shader.setUniform("far", view.farDepth); 
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDepthMask(true);
            }
            
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            
            GL11.glCullFace(GL11.GL_BACK);
            Matrix4f viewMatrix = view.viewMatrix;
            if(ent.hasFlag(RenderEntity.FLAG_SPRITE_AXIS)) {
                Matrix4f modelMatrix = Helper.getModelMatrix(ent.axis, ent.origin, null);
                viewMatrix = Matrix4f.mul(viewMatrix, modelMatrix, modelMatrix);
            }
            shader.setUniform("ModelView", viewMatrix);
            shader.setUniform("Projection", view.ProjectionMatrix);
        }
        
        
        if(ent.mat != null) ent.mat.getTexture().Bind();
        else Ref.ResMan.getWhiteTexture().Bind();
        Ref.ResMan.getNoNormalTexture().Bind();
        Ref.ResMan.getNoSpecularTexture().Bind();
        
        int mode = ent.oldframe == 0? GL11.GL_QUADS : ent.oldframe;
        GL11.glBegin(mode);
        {
            for (int i= 0; i < ent.frame; i++) {
                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, ent.verts[i].s, ent.verts[i].t);
                Vector3f v = ent.verts[i].normal;
                if(v != null) {
                    GL20.glVertexAttrib3f(Shader.INDICE_NORMAL, v.x, v.y, v.z);
                }
                v = ent.verts[i].xyz;
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, v.x, v.y, v.z);
            }
        }
        GL11.glEnd();

        
        if(shadowpass) {
            GL11.glCullFace(GL11.GL_BACK);
        } else {
            if(!deferredPoly) {
                GL11.glDepthMask(true);
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
//                GL11.glPolygonOffset(0, 0);
            }
        }
        Ref.glRef.PopShader();
    }

    public void addRefEntity(RenderEntity ent) {
        if(renderList.contains(ent)) {
            Ref.common.Error(ErrorCode.FATAL, "RenderEntity already contained");
        }
        renderList.add(ent);
    }

    private void renderBeam(RenderEntity ent) {
        Vector3f start = ent.oldOrigin;
        Vector3f end = ent.origin;

        float len = Vector3f.sub(end, start, null).length();
        if(len < 1f) return;
        // compute side vector
        Vector3f renderOrg = view.Origin;
        Vector3f v1 = Vector3f.sub(start, renderOrg, null);
        Vector3f v2 = Vector3f.sub(end, renderOrg, null);
        Helper.Normalize(v1); Helper.Normalize(v2);
        Vector3f right = Vector3f.cross(v1, v2, null);

        if(ent.mat != null) ent.mat.getTexture().Bind();
        else Ref.ResMan.getWhiteTexture().Bind();
        
        doBeam(start, end, right, len, ent.radius, ent.outcolor);
    }

    private void doBeam(Vector3f start, Vector3f end, Vector3f up, float len, float spanWidth, Vector4f color) {
        // soft particles disabled when no fbo or r_softparticles isn't true
        boolean useSoftSprites = false;//Ref.glRef.srgbBuffer != null && Ref.glRef.r_softparticles.isTrue();
        CubeTexture tex = null; // depth

        Shader sh = Ref.glRef.getShader("Poly");
        Ref.glRef.PushShader(sh);
        sh.setUniform("ModelView", view.viewMatrix);
        sh.setUniform("Projection", view.ProjectionMatrix);
//        if(useSoftSprites) {
//            // use soft particle shader
//            Ref.glRef.PushShader(softSprite);
//            softSprite.setUniform("res", Ref.glRef.GetResolution());
//
//            // Bind depth from FBO
//            int depth = Ref.glRef.srgbBuffer.getDepthTextureId();
//            tex = new CubeTexture(Ref.glRef.srgbBuffer.getTarget(), depth, null);
//            tex.textureSlot = 1;
//            tex.loaded = true;
//            tex.Bind();
//        }

        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glDepthMask(false); // dont write to depth
        GL11.glBegin(GL11.GL_QUADS);
        {
            // Fancy pants shaders
            Vector3f v = Helper.VectorMA(start, spanWidth, up, null);
            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, 1, 0);
            Helper.col(color);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, v.x, v.y, v.z);

            Helper.VectorMA(start, -spanWidth, up, v);
            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, 1, 1);
            Helper.col(color);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, v.x, v.y, v.z);

            

            Helper.VectorMA(end, -spanWidth, up, v);
            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, 0, 1);
            Helper.col(color);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, v.x, v.y, v.z);

            Helper.VectorMA(end, spanWidth, up, v);
            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, 0, 0);
            Helper.col(color);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, v.x, v.y, v.z);
        }
        GL11.glEnd();
        GL11.glDepthMask(true);

        GL11.glEnable(GL11.GL_CULL_FACE);

        if(useSoftSprites) {
            tex.Unbind();
            Ref.glRef.PopShader();
        }
        Ref.glRef.PopShader();
    }
    
    private void renderModel(RenderEntity ent) {
        if(ent.model == null) return;
        if(r_gpuskin.isTrue()) {
            ent.model.renderGPUSkinned(ent.origin, ent.axis, ent.color, currentRenderFlags, view);
        } else {
            ent.model.renderCPUSkinned(ent.origin, ent.axis, ent.color, currentRenderFlags, view);
        }
        
        // Render it
        
        
        //Vector3f org = new Vector3f(ent.origin);
        //int count = 10;
//        for (int i = 0; i < count; i++) {
//            for (int j = 0; j < count; j++) {
//                org.x = ent.origin.x + (i-4) * 64;
//                org.y = ent.origin.y + (j-4) * 64;
//
//
//                ent.model.render(org, ent.axis, ent.color, Ref.cgame.shadowMan.isRendering());
//            }
//        }
        
    }

    private void renderBBox(RenderEntity ent) {
        Vector3f position = ent.origin;
        Vector3f mins = ent.oldOrigin;
        Vector3f maxs = ent.oldOrigin2;
        if(Vector3f.sub(maxs, mins, null).lengthSquared() < 0.001f) {
            mins.set(-1f,-1f,-1f);
            maxs.set(1f,1f,1f);
        }
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_CULL_FACE);
        if(ent.hasFlag(RenderEntity.FLAG_SPRITE_AXIS)) {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            
            Matrix4f model = Helper.getModelMatrix(ent.axis, position, null);
            Matrix4f viewmatrix = view.viewMatrix;
            Matrix4f.mul(viewmatrix, model, model);
            Ref.glRef.glLoadMatrix(model);
            Helper.renderBBoxWireframe(mins.x, mins.y, mins.z,
                maxs.x, maxs.y, maxs.z, ent.outcolor, view);
            GL11.glPopMatrix();
        } else {
            Helper.renderBBoxWireframe(-mins.x+position.x, -mins.y+position.y, -mins.z+position.z,
                mins.x+position.x, mins.y+position.y, mins.z+position.z, ent.outcolor, view);
        }
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    private Vector3f left = new Vector3f(), up = new Vector3f();
    private void renderSprite(RenderEntity ent) {
        // calculate the xyz locations for the four corners
        float radius = ent.radius;
        boolean useAxis = (ent.flags & RenderEntity.FLAG_SPRITE_AXIS) == RenderEntity.FLAG_SPRITE_AXIS;
        if(useAxis) {
            left.set(ent.axis[0]);
            up.set(ent.axis[1]);
        } else {
            left.set(view.ViewAxis[1]);
            up.set(view.ViewAxis[2]);
        }
        left.scale(radius);
        up.scale(-radius);

        float s1 = 0, t1 = 0, s2 = 1, t2 = 1;
        
        if(ent.mat != null && ent.mat.getTexture() != null) {
            Vector2f texSize = ent.mat.getTextureSize();
            Vector2f texOffset = ent.mat.getTextureOffset(ent.frame);
            s1 = texOffset.x;
            t1 = texOffset.y;
            s2 = s1 + texSize.x;
            t2 = t1 + texSize.y;
        } 
        
        boolean rendernow = !r_batchsprites.isTrue() || currentRenderFlags != RF_POSTDEFERRED;
        if(rendernow) {
            // Setup render state
            if(ent.mat != null && ent.mat.getTexture() != null) {
                // Grab texture offsets from material
                ent.mat.getTexture().Bind();
                if(ent.mat.blendmode == CubeMaterial.BlendMode.ONE) {
                    GLState.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                } else {
                    GLState.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                }
            } else {
                GLState.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                Ref.ResMan.getWhiteTexture().Bind();
            }

            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(false); // dont write to depth

            AddQuadStampExt(ent.origin, left, up, ent.outcolor,s1,t1,s2,t2);

            // clear renderstate
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(true);
        } else {
            // add to polygon batch and render later
            polyBatcher.addSpriteCall(ent);
            ByteBuffer dst = polyBatcher.getMappedBuffer();
            writeQuadStamp(dst, ent.origin, left, up, ent.outcolor,s1,t1,s2,t2);
        }
    }
    
    private void writeQuadStamp(ByteBuffer dst, Vector3f origin, Vector3f left, Vector3f up, Vector4f color, float s1, float t1, float s2, float t2) {
        float r = color.x/255f, g = color.y/255f, b = color.z/255f, a = color.w/255f;
        
        dst.putFloat(origin.x + left.x + up.x);
        dst.putFloat(origin.y + left.y + up.y);
        dst.putFloat(origin.z + left.z + up.z);
        
        dst.putFloat(0f);
        dst.putFloat(0f);
        dst.putFloat(0f);
        
        dst.putFloat(s1);
        dst.putFloat(t1);
        
        dst.put((byte)color.x); dst.put((byte)color.y); dst.put((byte)color.z); dst.put((byte)color.w);
        
        dst.putFloat(origin.x - left.x + up.x);
        dst.putFloat(origin.y - left.y + up.y);
        dst.putFloat(origin.z - left.z + up.z);
        
        dst.putFloat(0f);
        dst.putFloat(0f);
        dst.putFloat(0f);
        
        dst.putFloat(s2);
        dst.putFloat(t1);
        
        dst.put((byte)color.x); dst.put((byte)color.y); dst.put((byte)color.z); dst.put((byte)color.w);
        
        dst.putFloat(origin.x - left.x - up.x);
        dst.putFloat(origin.y - left.y - up.y);
        dst.putFloat(origin.z - left.z - up.z);
        
        dst.putFloat(0f);
        dst.putFloat(0f);
        dst.putFloat(0f);
        
        dst.putFloat(s2);
        dst.putFloat(t2);
        
        dst.put((byte)color.x); dst.put((byte)color.y); dst.put((byte)color.z); dst.put((byte)color.w);
        
        dst.putFloat(origin.x + left.x - up.x);
        dst.putFloat(origin.y + left.y - up.y);
        dst.putFloat(origin.z + left.z - up.z);
        
        dst.putFloat(0f);
        dst.putFloat(0f);
        dst.putFloat(0f);
        
        dst.putFloat(s1);
        dst.putFloat(t2);
        
        dst.put((byte)color.x); dst.put((byte)color.y); dst.put((byte)color.z); dst.put((byte)color.w);
    }

    private void AddQuadStampExt(Vector3f origin, Vector3f left, Vector3f up, Vector4f color, float s1, float t1, float s2, float t2) {
        Shader sh = Ref.glRef.getShader("Poly");
        Ref.glRef.PushShader(sh);
        sh.setUniform("ModelView", view.viewMatrix);
        sh.setUniform("Projection", view.ProjectionMatrix);
        
        float r = color.x/255f, g = color.y/255f, b = color.z/255f, a = color.w/255f;
        
        GL11.glBegin(GL11.GL_QUADS);
        {
            // Fancy pants shaders
            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, s1, t1);
            GL20.glVertexAttrib4f(Shader.INDICE_COLOR, r,g,b,a);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, origin.x + left.x + up.x
                                                        , origin.y + left.y + up.y
                                                        , origin.z + left.z + up.z);

            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, s2, t1);
            GL20.glVertexAttrib4f(Shader.INDICE_COLOR, r,g,b,a);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, origin.x - left.x + up.x
                                                        , origin.y - left.y + up.y
                                                        , origin.z - left.z + up.z);

            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, s2, t2);
            GL20.glVertexAttrib4f(Shader.INDICE_COLOR, r,g,b,a);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, origin.x - left.x - up.x
                                                        , origin.y - left.y - up.y
                                                        , origin.z - left.z - up.z);

            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, s1, t2);
            GL20.glVertexAttrib4f(Shader.INDICE_COLOR, r,g,b,a);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, origin.x + left.x - up.x
                                                        , origin.y + left.y - up.y
                                                        , origin.z + left.z - up.z);
        }
        GL11.glEnd();
        

        Ref.glRef.PopShader();
        
    }

    /**
     * Resets the renderer for a completely new frame
     * Should only be called after all the views have been rendered
     */
    public void reset() {
        next = 0;
        renderList.clear();
        viewList.clear();
    }

    public RenderEntity createEntity() {
        return createEntity(REType.SPRITE);
    }

    public RenderEntity createEntity(REType type) {
        RenderEntity ent = entities[next];
        ent.clear();
        next++;

        ent.Type = type;
        return ent;
    }

    public ArrayList<ViewParams> getViewList() {
        return viewList;
    }

    


    
}
