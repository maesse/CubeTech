package cubetech.CGame;

import cubetech.collision.ClientCubeMap;
import cubetech.collision.CubeChunk;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Common.ErrorCode;
import cubetech.common.Helper;
import cubetech.gfx.CubeMaterial;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.GLRef;
import cubetech.gfx.GLState;
import cubetech.gfx.RenderList;
import cubetech.gfx.Shader;
import cubetech.gfx.ShadowManager;
import cubetech.gfx.SkyBox;
import cubetech.iqm.BoneController;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

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
    private ArrayList<RenderEntity> renderList = new ArrayList<RenderEntity>();
    private ArrayList<ViewParams> viewList = new ArrayList<ViewParams>();

    Shader softSprite = null;
    CVar r_gpuskin = Ref.cvars.Get("r_gpuskin", "1", EnumSet.of(CVarFlags.NONE));
    public CVar r_nocull = Ref.cvars.Get("r_nocull", "0", EnumSet.of(CVarFlags.TEMP));
    public ViewParams view = null;
    private int currentRenderFlags = 0;

    public Render() {
        for (int i= 0; i < MAX_RENDER_ENTITIES; i++) {
            entities[i] = new RenderEntity(REType.NONE);
        }
//        if(Ref.glRef.srgbBuffer != null && softSprite == null) {
//            softSprite = Ref.glRef.getShader("softsprite");
//        }
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
        
        for (RenderEntity re : view.renderList.list) {
            if(shadowPass) {
                // Skip render entities that doesn't cast shadows
                if(re.hasFlag(RenderEntity.FLAG_NOSHADOW)) continue;
                if(re.Type == REType.SPRITE) continue; // sprites are rendered without depth writing
            }
            
            if(postDeferred != re.hasFlag(RenderEntity.FLAG_NOLIGHT)) continue;
            
            switch(re.Type) {
                case WORLD:
                    sub = Profiler.EnterSection(Sec.RENDER_WORLD);
                    renderWorld(re);
                    sub.ExitSection();
                    break;
                case SKYBOX:
                    if(re.renderObject != null && re.renderObject instanceof SkyBox) {
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
                default:
                    Ref.common.Error(ErrorCode.FATAL, "Render.renderAll(): unknown type " + re.Type);
                    break;
            }
            
        }
        
        t.ExitSection();
        GLRef.checkError();
        this.view = null;
    }

    private void renderWorld(RenderEntity ent) {
        CubeChunk[] chunkList = (CubeChunk[]) ent.renderObject;
        ClientCubeMap.renderChunkList(chunkList, view);
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
                GLState.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                
                
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(-1f, 0);
            } else {
                shader.setUniform("far", view.farDepth); 
                GL11.glDisable(GL11.GL_BLEND);
            }
            
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
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
//                GL11.glDepthMask(true);
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(0, 0);
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

        // compute side vector
        Vector3f renderOrg = Ref.cgame.cg.refdef.Origin;
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
                maxs.x, maxs.y, maxs.z, ent.outcolor);
            GL11.glPopMatrix();
        } else {
            Helper.renderBBoxWireframe(-mins.x+position.x, -mins.y+position.y, -mins.z+position.z,
                mins.x+position.x, mins.y+position.y, mins.z+position.z, ent.outcolor);
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
            GL11.glDisable(GL11.GL_CULL_FACE);
        } else {
            left.set(Ref.cgame.cg.refdef.ViewAxis[1]);
            up.set(Ref.cgame.cg.refdef.ViewAxis[2]);
        }
        left.scale(radius);
        up.scale(-radius);

        if(ent.mat != null && ent.mat.getTexture() != null) {
            // Grab texture offsets from material
            ent.mat.getTexture().Bind();
            Vector2f texSize = ent.mat.getTextureSize();
            Vector2f texOffset = ent.mat.getTextureOffset(ent.frame);
            if(ent.mat.blendmode == CubeMaterial.BlendMode.ONE) {
                GLState.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            } else {
                GLState.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            AddQuadStampExt(ent.origin, left, up, ent.outcolor,
                    texOffset.x, texOffset.y, texOffset.x + texSize.x, texOffset.y + texSize.y);
        }
        else {
            GLState.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Ref.ResMan.getWhiteTexture().Bind();
            AddQuadStamp(ent.origin, left, up, ent.outcolor);
        }

        if(useAxis) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    private void AddQuadStamp(Vector3f origin, Vector3f left, Vector3f up, Vector4f color) {
        AddQuadStampExt(origin, left, up, color,0,0,1,1);
    }

    private void AddQuadStampExt(Vector3f origin, Vector3f left, Vector3f up, Vector4f color, float s1, float t1, float s2, float t2) {
        // soft particles disabled when no fbo or r_softparticles isn't true
        boolean useSoftSprites = false;//Ref.glRef.srgbBuffer != null && Ref.glRef.r_softparticles.isTrue();
        CubeTexture tex = null; // depth
        
        if(useSoftSprites) {
            // use soft particle shader
//            Ref.glRef.PushShader(softSprite);
//            softSprite.setUniform("res", Ref.glRef.GetResolution());
//
//            // Bind depth from FBO
//            
//            tex = Ref.glRef.srgbBuffer.getAsTexture();
//            tex.textureSlot = 1;
//            tex.loaded = true;
//            tex.Bind();
        } else {
            Ref.glRef.PushShader(Ref.glRef.getShader("WorldFog"));
        }

        GL11.glDepthMask(false); // dont write to depth
        GL11.glBegin(GL11.GL_QUADS);
        {
            // Fancy pants shaders
            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, s1, t1);
            Helper.col(color);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, origin.x + left.x + up.x
                                                        , origin.y + left.y + up.y
                                                        , origin.z + left.z + up.z);

            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, s2, t1);
            Helper.col(color);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, origin.x - left.x + up.x
                                                        , origin.y - left.y + up.y
                                                        , origin.z - left.z + up.z);

            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, s2, t2);
            Helper.col(color);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, origin.x - left.x - up.x
                                                        , origin.y - left.y - up.y
                                                        , origin.z - left.z - up.z);

            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, s1, t2);
            Helper.col(color);
            GL20.glVertexAttrib3f(Shader.INDICE_POSITION, origin.x + left.x - up.x
                                                        , origin.y + left.y - up.y
                                                        , origin.z + left.z - up.z);
        }
        GL11.glEnd();
        GL11.glDepthMask(true);

        

        if(useSoftSprites) {
            tex.Unbind();
        }
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
