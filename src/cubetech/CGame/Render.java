package cubetech.CGame;

import cubetech.common.Common.ErrorCode;
import cubetech.common.Helper;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.GLRef;
import cubetech.gfx.Shader;
import cubetech.misc.Ref;
import java.util.LinkedList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
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
    private LinkedList<RenderEntity> renderList = new LinkedList<RenderEntity>();

    Shader softSprite = null;

    public Render() {
        for (int i= 0; i < MAX_RENDER_ENTITIES; i++) {
            entities[i] = new RenderEntity();
        }
        if(Ref.glRef.srgbBuffer != null) {
            softSprite = Ref.glRef.getShader("softsprite");
            softSprite.mapTextureUniform("tex", 0);
            softSprite.mapTextureUniform("depth", 1);
            softSprite.validate();
        }
    }

    public void renderAll() {
        GLRef.checkError();
        for (RenderEntity re : renderList) {
            switch(re.Type) {
                case MODEL:
                    renderModel(re);
                    break;
                case SPRITE:
                    renderSprite(re);
                    break;
                default:
                    Ref.common.Error(ErrorCode.FATAL, "Render.renderAll(): unknown type " + re.Type);
                    break;
            }
            
        }
        GLRef.checkError();
    }

    public void addRefEntity(RenderEntity ent) {
        if(renderList.contains(ent)) {
            int derp = 2;
        }
        renderList.add(ent);
    }
    
    private void renderModel(RenderEntity ent) {
        if(ent.model == null) return;

        float frame = ent.oldframe * ent.backlerp + ent.frame * (1f-ent.backlerp);
        ent.model.animate(ent.frame, ent.oldframe, ent.backlerp);
        ent.model.render(ent.origin, ent.axis, ent.color);
    }

    private void renderSprite(RenderEntity ent) {
        // calculate the xyz locations for the four corners
        float radius = ent.radius;
        Vector3f left = new Vector3f(Ref.cgame.cg.refdef.ViewAxis[1]);
        left.scale(radius);
        Vector3f up = new Vector3f(Ref.cgame.cg.refdef.ViewAxis[2]);
        up.scale(-radius);

        if(ent.mat != null && ent.mat.getTexture() != null) ent.mat.getTexture().Bind();
        else Ref.ResMan.getWhiteTexture().Bind();
        AddQuadStamp(ent.origin, left, up, ent.outcolor);
    }

    private void AddQuadStamp(Vector3f origin, Vector3f left, Vector3f up, Vector4f color) {
        AddQuadStampExt(origin, left, up, color,0,0,1,1);
    }

    private void AddQuadStampExt(Vector3f origin, Vector3f left, Vector3f up, Vector4f color, float s1, float t1, float s2, float t2) {
        // soft particles disabled when no fbo or r_softparticles isn't true
        boolean useSoftSprites = Ref.glRef.srgbBuffer != null && Ref.glRef.r_softparticles.isTrue();
        CubeTexture tex = null; // depth
        
        if(useSoftSprites) {
            // use soft particle shader
            Ref.glRef.PushShader(softSprite);
            softSprite.setUniform("res", Ref.glRef.GetResolution());

            // Bind depth from FBO
            int depth = Ref.glRef.srgbBuffer.getDepthTextureId();
            tex = new CubeTexture(Ref.glRef.srgbBuffer.getTarget(), depth, null);
            tex.textureSlot = 1;
            tex.loaded = true;
            tex.Bind();
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
            Ref.glRef.PopShader();
        }
        
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
