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
import cubetech.gfx.Shader;
import cubetech.gfx.ShadowManager;
import cubetech.iqm.BoneController;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class Render {
    private static final int MAX_RENDER_ENTITIES = 1000;
    private RenderEntity[] entities = new RenderEntity[MAX_RENDER_ENTITIES];
    private int next = 0;
    private ArrayList<RenderEntity> renderList = new ArrayList<RenderEntity>();

    Shader softSprite = null;
    CVar r_gpuskin = Ref.cvars.Get("r_gpuskin", "1", EnumSet.of(CVarFlags.NONE));
    public CVar r_nocull = Ref.cvars.Get("r_nocull", "0", EnumSet.of(CVarFlags.TEMP));
    public ViewParams view = null;

    public Render() {
        for (int i= 0; i < MAX_RENDER_ENTITIES; i++) {
            entities[i] = new RenderEntity(REType.NONE);
        }
//        if(Ref.glRef.srgbBuffer != null && softSprite == null) {
//            softSprite = Ref.glRef.getShader("softsprite");
//        }
    }

    public void renderAll(ViewParams view, boolean shadowPass) {
        this.view = view;
        SecTag t = Profiler.EnterSection(Sec.RENDER);
        SecTag sub;
        GLRef.checkError();
        for (RenderEntity re : renderList) {
            if(shadowPass) {
                // Skip render entities that doesn't cast shadows
                if((re.flags & RenderEntity.FLAG_NOSHADOW) == RenderEntity.FLAG_NOSHADOW) continue;
                if(re.Type == REType.SPRITE) continue; // sprites are rendered without depth writing
            }
            
            switch(re.Type) {
                case WORLD:
                    sub = Profiler.EnterSection(Sec.RENDER_WORLD);
                    renderWorld(re);
                    sub.ExitSection();
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
                    renderPoly(re);
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
        CubeChunk[] chunkList = (CubeChunk[]) ent.controllers;
        ClientCubeMap.renderChunkList(chunkList, view);
    }

    public void renderPoly(RenderEntity ent) {
        Ref.glRef.PushShader(Ref.glRef.getShader("WorldFog"));
//        GL11.glDisable(GL11.GL_CULL_FACE);
        Helper.col(ent.color);
        GL11.glEnable(GL11.GL_BLEND);
        GLState.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        ent.mat.getTexture().Bind();

        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1f, 0);
        
        GL11.glBegin(GL11.GL_QUADS);
        {
            for (int i= 0; i < ent.frame; i++) {
                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, ent.verts[i].s, ent.verts[i].t);
                Vector3f v = ent.verts[i].xyz;
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, v.x, v.y, v.z);
            }
        }
        GL11.glEnd();

        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(0, 0);
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
        if(r_gpuskin.isTrue()) ent.flags |= RenderEntity.FLAG_GPUSKINNED;
        else ent.flags &= ~RenderEntity.FLAG_GPUSKINNED;

        //float frame = ent.oldframe * ent.backlerp + ent.frame * (1f-ent.backlerp);
        ent.model.gpuskinning = (ent.flags & RenderEntity.FLAG_GPUSKINNED) != 0;
        if(ent.controllers == null) ent.model.controllers = null;
        else  ent.model.controllers = (BoneController[]) ent.controllers;
        
        ent.model.animate(ent.frame, ent.oldframe, ent.backlerp);
        //Vector3f org = new Vector3f(ent.origin);

        //int count = 10;
        boolean shadowpass = Ref.cgame != null && Ref.glRef.shadowMan.isRendering();
        ent.model.render(ent.origin, ent.axis, ent.color, shadowpass, view);
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
        Vector3f size = ent.oldOrigin;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glLineWidth(1f);
            GL11.glDisable(GL11.GL_CULL_FACE);
        Helper.renderBBoxWireframe(position.x, position.y, position.z,
                position.x+size.x, position.y+size.y, position.z+size.z, ent.outcolor);
        
            //cube.chunk.render.renderSingleWireframe(cube.x, cube.y, cube.z, CubeType.DIRT);
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

    public void reset() {
        next = 0;
        renderList.clear();
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


    
}
