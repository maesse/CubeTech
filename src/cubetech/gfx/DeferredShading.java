package cubetech.gfx;

import cubetech.CGame.Render;
import cubetech.misc.Profiler.SecTag;
import cubetech.Game.Gentity;
import cubetech.common.IThinkMethod;
import java.util.ArrayList;
import cubetech.common.CVar;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.ARBUniformBufferObject;
import org.lwjgl.opengl.GL15;
import org.lwjgl.util.vector.Vector4f;
import cubetech.CGame.ViewParams;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.gfx.MultiRenderBuffer.Format;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Ref;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Sphere;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

/**
 *
 * @author Mads
 */
public class DeferredShading {
    MultiRenderBuffer mrt;
//    MultiRenderBuffer transbuffer;
    boolean isRendering = false;
    SSAO ssao;
    ShaderUBO lightBuffer;
    Vector3f[] lights = null;
    CVar r_deferred = Ref.cvars.Get("r_deferred", "1", null);
    CVar r_ambient = Ref.cvars.Get("r_ambient", "1.0", null); // ambient = r_ambient * ambientcube
    CVar r_scissor = Ref.cvars.Get("r_scissor", "1", null);
    CVar r_showscissor = Ref.cvars.Get("r_showscissor", "0", null);
    CubeTexture envmap = null;
    private ViewParams currentView = null;
    
    
    public DeferredShading() {
        Vector2f resolution = Ref.glRef.GetResolution();
        MultiRenderBuffer.MRBBuilder builder = new MultiRenderBuffer.MRBBuilder((int)resolution.x, (int)resolution.y);
        builder.addFormat(Format.RGBA, true);
        builder.addFormat(Format.RGBA16F, true);
        builder.addFormat(Format.RGBA16F, true);
        builder.addFormat(Format.DEPTH24, false);
        mrt = new MultiRenderBuffer(builder);
        
//        MultiRenderBuffer.MRBBuilder builder2 = new MultiRenderBuffer.MRBBuilder((int)resolution.x, (int)resolution.y);
//        builder2.addFormat(Format.RGBA, true);
//        builder2.addExistingBuffer(Format.DEPTH24, false, mrt.getHandle(Format.DEPTH24));
//        
//        transbuffer = new MultiRenderBuffer(builder2);
        ssao = new SSAO();
        
        buildLightBuffer();
    }
    
    
    
    private void buildLightBuffer() {
        Shader shader = Ref.glRef.getShader("DeferredShading");
        Ref.glRef.PushShader(shader);
        
        lightBuffer = ShaderUBO.initUBOForShader(shader, "LightDataSrc");
        
        lights = new Vector3f[32];
        for (int i = 0; i < lights.length; i++) {
            lights[i] = new Vector3f(Ref.rnd.nextFloat(),Ref.rnd.nextFloat(),Ref.rnd.nextFloat());
            lights[i].scale(2.0f);
            Vector3f.sub(lights[i], new Vector3f(1,1,0), lights[i]);
            lights[i].scale((Ref.rnd.nextFloat()) * 1000.0f);
        }
        
        Ref.glRef.PopShader();
    }
    
    public boolean isRendering() {
        return isRendering;
    }
    
    public void onResolutionChange() {
        if(mrt == null) {
            Common.Log("No existing mrt created, not reacting to resolution change");
            return;
        }
        Vector2f resolution = Ref.glRef.GetResolution();
        MultiRenderBuffer.MRBBuilder info = mrt.getInfo();
        if(resolution.x == info.getWidth() && resolution.y == info.getHeight()) return;
        mrt.dispose();
        mrt = new MultiRenderBuffer(info);
    }
    
    
    
    public void startDeferred(ViewParams view) {
        if(!r_deferred.isTrue()) return;
        isRendering = true;
        currentView = view;
        mrt.start(false, view);
    }
    
    public void stopDeferred() {
        if(!isRendering) return;
        isRendering = false;
        mrt.stop();
    }
    
    public void startPostDeferred(ViewParams view) {
        if(!r_deferred.isTrue()) return;
        mrt.start(true, view);
    }
    
    public void stopPostDeferred() {
        if(!r_deferred.isTrue()) return;
        
        
    }
    
    private void blitDepth() {
        int w = mrt.getInfo().getWidth();
        int h = mrt.getInfo().getHeight();
        GLRef.checkError();
        ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_READ_FRAMEBUFFER, mrt.fbHandle);
        GLRef.checkError();
        ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_DRAW_FRAMEBUFFER, 0);
        GLRef.checkError();
        ARBFramebufferObject.glBlitFramebuffer(0, 0, w-1, h-1, 
                0, 0, w-1, h-1, 
                GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        GLRef.checkError();
        ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_READ_FRAMEBUFFER, 0);
        
        GLRef.checkError();
    }
    
    public Vector3f[] calcFarPlaneCorners() {
        Vector3f forward = new Vector3f(0,0,-1);
        Vector3f right = new Vector3f(1, 0, 0);
        Vector3f up = new Vector3f(0, 1, 0);
        
        ViewParams view = currentView;
        forward.scale(view.farDepth);
        float hfar = (float) ( Math.tan(view.FovY * Math.PI / 360f) * (view.farDepth));
        float wfar = (float) ( Math.tan(view.FovX * Math.PI / 360f) * (view.farDepth));
        
        Vector3f[] corners = new Vector3f[4];
        corners[0] = new Vector3f(forward);
        Helper.VectorMA(corners[0], -wfar, right, corners[0]);
        Helper.VectorMA(corners[0], -hfar, up, corners[0]);
        
        corners[1] = new Vector3f(forward);
        Helper.VectorMA(corners[1], wfar, right, corners[1]);
        Helper.VectorMA(corners[1], -hfar, up, corners[1]);
        
        corners[2] = new Vector3f(forward);
        Helper.VectorMA(corners[2], wfar, right, corners[2]);
        Helper.VectorMA(corners[2], hfar, up, corners[2]);
        
        corners[3] = new Vector3f(forward);
        Helper.VectorMA(corners[3], -wfar, right, corners[3]);
        Helper.VectorMA(corners[3], hfar, up, corners[3]);
        return corners;
    }
    
    public boolean isEnabled() {
        return r_deferred.isTrue();
    }
    
    private boolean scissored = false;
    
    /**
     * Returns true when light should be culled
     * @param view
     * @param light
     * @return 
     */
    private boolean setScissor(ViewParams view, Light light) {
        if(r_scissor.isTrue()) {
            light.setRadius(1f/0.001f);
            int area = light.calculateScissor(view);
            if(area == 0) return true; // not in view
            Vector4f scissor = light.getScissor();
            if(area != view.ViewportHeight * view.ViewportWidth) {
                // Let's cull!
                scissored = true;
                        GL11.glEnable(GL11.GL_SCISSOR_TEST);
                        glScissor((int)scissor.x, (int)scissor.y, (int)scissor.z, (int)scissor.w);
            }
            if(r_showscissor.isTrue()) {
                Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);

                spr.Set(scissor.x, scissor.y, scissor.z, scissor.w, null);
            }
        }
        return false;
    }
    
    private void clearScissor(ViewParams view) {
        if(scissored) {
            glScissor(view.ViewportX, view.ViewportY, view.ViewportWidth, view.ViewportHeight);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            scissored = false;
        }
    }
    
    public void finalizeShading() {
        if(!r_deferred.isTrue()) return;
        if(currentView == null) return;
        
        
        
        boolean shadowsEnabled = Ref.glRef.shadowMan.isEnabled();
        if(shadowsEnabled) {
            IRenderCallback renderFunction = new IRenderCallback() {
                public void render(ViewParams view) {
                    Ref.render.renderAll(view, Render.RF_SHADOWPASS);
                }
            };
            for (Light light : currentView.lights) {
                if(light.isCastingShadows()) {
                    Ref.glRef.shadowMan.renderShadowsForLight(light, currentView, renderFunction);
                }
            }
            
        }
//        transbuffer.start(false, currentView);
        
        // Set HUD render projection
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(currentView.ViewportX, currentView.ViewportWidth+currentView.ViewportX, currentView.ViewportY, currentView.ViewportY+currentView.ViewportHeight, 1, -1000);
        //GL11.glOrtho(0, (int)Ref.glRef.GetResolution().x, 0, (int)Ref.glRef.GetResolution().y, 1,-1000);
        
        GLState.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        
        GL11.glDepthMask(false);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GLState.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        
        SecTag t = Profiler.EnterSection(Sec.AMBIENT_PASS);
        Shader shader = Ref.glRef.getShader("DeferredAmbientCube");
        Ref.glRef.PushShader(shader);
        shader.setUniform("viewmatrix", Matrix4f.invert(currentView.viewMatrix, null));
        shader.setUniform("ambientFactor", r_ambient.fValue);
        if(envmap == null) envmap = Ref.ResMan.LoadTexture("data/textures/skybox/ibl_sky", true);
        envmap.textureSlot = 3;
        envmap.Bind();
        fullscreenPass(shader, true, false, true);
        envmap.Unbind();
        Ref.glRef.PopShader();
        t.ExitSection();
        
        
        
        ArrayList<Light> lightList = currentView.lights;
        for (Light light : lightList) {
            // if last pass used scissoring, disable it for this pass
            
            Light.Type type = light.getType();
            if(type == Light.Type.DIRECTIONAL) {
                t = Profiler.EnterSection(Sec.DIR_LIGHT);
                if(light.isCastingShadows() && shadowsEnabled) {
                    ShadowResult shadows = light.getShadowResult();
                    shader = Ref.glRef.getShader("DeferredDirectionalLightShadowed");
                    Ref.glRef.PushShader(shader);
                    Vector3f direction = light.getDirection();
                    Vector4f shaderPosition = new Vector4f(-direction.x,
                                                           -direction.y,
                                                           -direction.z, 0f);
                    Matrix4f.transform(currentView.viewMatrix, shaderPosition, shaderPosition);
                    shader.setUniform("lightPosition", shaderPosition);
                    shader.setUniform("lightDiffuse", light.getDiffuse());
                    shader.setUniform("lightSpecular", light.getSpecular());
                    // Set light
                    CubeTexture depth = shadows.getDepthTexture();
                    depth.textureSlot = 3;
                    depth.Bind();

                    Matrix4f[] shadowmat = shadows.getShadowViewProjections(4, light);
                    Vector4f shadowDepths = shadows.getCascadeDepths();
                    shader.setUniform("invModelView", Matrix4f.invert(currentView.viewMatrix, null));
                    shader.setUniform("projectionMatrix", currentView.ProjectionMatrix);
                    shader.setUniform("shadowMatrix", shadowmat);
                    shader.setUniform("cascadeDistances", shadowDepths);
                    shader.setUniform("shadow_bias", Ref.cvars.Find("shadow_bias").fValue);
                    shader.setUniform("shadow_factor", Ref.cvars.Find("shadow_factor").fValue);
                    shader.setUniform("pcfOffsets", shadows.getPCFoffsets());
                    GLRef.checkError();
                    fullscreenPass(shader, true, true, true);
                    Ref.glRef.PopShader();
                    depth.Unbind();
                } else {
                    shader = Ref.glRef.getShader("DeferredDirectionalLight");
                    Ref.glRef.PushShader(shader);
                    Vector3f direction = light.getDirection();
                    Vector4f shaderPosition = new Vector4f(-direction.x,
                                                           -direction.y,
                                                           -direction.z, 0f);
                    Matrix4f.transform(currentView.viewMatrix, shaderPosition, shaderPosition);
                    shader.setUniform("lightPosition", shaderPosition);
                    fullscreenPass(shader, true, true, true);
                    Ref.glRef.PopShader();
                }
                
                
               t.ExitSection(); 
            } else if(type == Light.Type.POINT) {
                
                
                
                t = Profiler.EnterSection(Sec.POINT_LIGHT);
                if(light.isCastingShadows() && shadowsEnabled) {
                    if(setScissor(currentView, light)) {
                        // cull
                        // fix: don't scissor when light has specular
                        t.ExitSection();
                        continue;
                    }
                    ShadowResult shadows = light.getShadowResult();
                    shader = Ref.glRef.getShader("DeferredPointLightShadowed");
                    Ref.glRef.PushShader(shader);
                    Vector3f position = light.getPosition();
                    Vector4f shaderPosition = new Vector4f(position.x,
                                                           position.y,
                                                           position.z, 1f);
                    Matrix4f.transform(currentView.viewMatrix, shaderPosition, shaderPosition);
                    shader.setUniform("lightPosition", shaderPosition);
                    shader.setUniform("lightDiffuse", light.getDiffuse());
                    shader.setUniform("lightSpecular", light.getSpecular());
                    shader.setUniform("attenuation", new Vector4f(0.000f, 0.000f, 0.0001f, 0.0f));
                    // Set light
                    CubeTexture depth = shadows.getDepthTexture();
                    depth.textureSlot = 3;
                    depth.Bind();

                    shader.setUniform("invModelView", Matrix4f.invert(currentView.viewMatrix, null));
                    shader.setUniform("projectionMatrix", currentView.ProjectionMatrix);
                    shader.setUniform("shadow_bias", Ref.cvars.Find("shadow_bias").fValue);
                    shader.setUniform("shadow_factor", Ref.cvars.Find("shadow_factor").fValue);
                    GLRef.checkError();
                    fullscreenPass(shader, true, true, true);
                    clearScissor(currentView);
                    Ref.glRef.PopShader();
                    depth.Unbind();
                } else {
                    if(setScissor(currentView, light)) {
                        // cull
                        // fix: don't scissor when light has specular
                        t.ExitSection();
                        continue;
                    }
                    shader = Ref.glRef.getShader("DeferredPointLight");
                    Ref.glRef.PushShader(shader);
                    // Transform light origin to view space
                    Vector3f position = light.getPosition();
                    Vector4f shaderPosition = new Vector4f(position.x,
                                                           position.y,
                                                           position.z, 1f);
                    Matrix4f.transform(currentView.viewMatrix, shaderPosition, shaderPosition);
                    shader.setUniform("lightPosition", shaderPosition);
                    shader.setUniform("attenuation", new Vector4f(0.000f, 0.000f, 0.00005f, 0.0f));
                    // Render the light
                    fullscreenPass(shader, true, true, true);
                    clearScissor(currentView);
                    Ref.glRef.PopShader();
                }
                t.ExitSection();
            }
        }
        GLState.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);   
        
        shader = Ref.glRef.getShader("DeferredFog");
        Ref.glRef.PushShader(shader);
        shader.setUniform("viewmatrix", Matrix4f.invert(currentView.viewMatrix, null));
        fullscreenPass(shader, false, false, true);
        Ref.glRef.PopShader();
        
        ssao.run(this);
        

        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        currentView = null;
        
        blitDepth();

    }
    
    private void singlePassLighting() {
        
        
        Shader shader = Ref.glRef.getShader("DeferredShading");
        Ref.glRef.PushShader(shader);
        
        
        CubeTexture ssaoTex = null;
        if(ssao.ssao_enable.isTrue()) {
            ssaoTex = ssao.getSSAOTarget();
            Integer mapid = shader.textureMap.get("ssao");
            if(mapid != null && mapid >= 0) {
                int uniformid = shader.GetTextureIndex(3);
                shader.setUniform(uniformid, 3);
                ssaoTex.textureSlot = 3;
                ssaoTex.Bind();
            }
            shader.setUniform("pixelOffset", new Vector2f(2f/Ref.glRef.GetResolution().x, 2f/Ref.glRef.GetResolution().y));
        }
        
        
        updateLightData();
        
        
        
        fullscreenPass(shader, true, true, true);
        
        if(ssao.ssao_enable.isTrue()) ssaoTex.Unbind();
        Ref.glRef.PopShader();
    }
    
    private void updateLightData() {
        int nLights = 6;
        Vector4f[] lightOut = new Vector4f[nLights*2];
        Matrix4f viewMatrix = currentView.viewMatrix;
        // Transform the lights to view space
        for (int i = 0; i < nLights; i++) {
            Vector4f lightDir = new Vector4f(lights[i].x, lights[i].y, lights[i].z ,1f);
            Matrix4f.transform(viewMatrix, lightDir, lightDir);
            lightOut[i * 2] = lightDir;
            lightOut[i * 2 + 1] = new Vector4f(0.001f, 0.0001f, 0.0001f, 0.001f);
        }
        
        // Fill buffer
        int nBytes = 16 + nLights * 4 * 8;
        ByteBuffer data = BufferUtils.createByteBuffer(nBytes);
        data.putInt(nLights);
        data.putInt(nLights);
        data.putInt(nLights);
        data.putInt(nLights);
        for (int i = 0; i < nLights; i++) {
            data.putFloat(lightOut[i * 2].x);
            data.putFloat(lightOut[i * 2].y);
            data.putFloat(lightOut[i * 2].z);
            data.putFloat(lightOut[i * 2].w);
            data.putFloat(lightOut[i * 2+1].x);
            data.putFloat(lightOut[i * 2+1].y);
            data.putFloat(lightOut[i * 2+1].z);
            data.putFloat(lightOut[i * 2+1].w);
        }
        
        data.flip();
        
        // Submit data to UBO
        int target = ARBUniformBufferObject.GL_UNIFORM_BUFFER;
        lightBuffer.bind();
        GL15.glBufferData(target, nBytes, GL15.GL_DYNAMIC_DRAW);
        GL15.glBufferSubData(target, 0, data);
        ShaderUBO.unbind();

        // Bind for rendering
        ARBUniformBufferObject.glBindBufferBase(target, 0, lightBuffer.getHandle());
    }
    
    public void setTextures(Shader shader, boolean tex0, boolean tex1, boolean tex2) {
        for (int i = 0; i < 3; i++) {
            if(i == 0 && !tex0 || i == 1 && !tex1 || i == 2 && !tex2) continue;
            Integer mapid = shader.textureMap.get("tex" + i);
            if(mapid != null && mapid >= 0) {
                int shaderTexIndex = shader.GetTextureIndex(i);
                if(shaderTexIndex < 0) continue;
                CubeTexture tex = mrt.asTexture(i);
                tex.textureSlot = i;
                tex.setFiltering(false, GL11.GL_NEAREST);
                tex.setFiltering(true, GL11.GL_NEAREST);
//                tex.setFiltering(false, GL11.GL_LINEAR);
//                tex.setFiltering(true, GL11.GL_LINEAR);
                tex.Bind();
                shader.setUniform(shaderTexIndex, i);
            }
        }
    }
    
    public void unsetTextures(boolean tex0, boolean tex1, boolean tex2) {
        if(tex0) mrt.asTexture(0).Unbind();
        if(tex1) mrt.asTexture(1).Unbind();
        if(tex2) mrt.asTexture(2).Unbind();
    }
    
    public void fullscreenPass(Shader shader, boolean tex0, boolean tex1, boolean tex2) {
        GLRef.checkError();
        setTextures(shader, tex0, tex1, tex2);
        
        Vector3f[] corners = calcFarPlaneCorners();
        
        GLRef.checkError();
        boolean coords = shader.attributes.containsValue(Shader.INDICE_COORDS);
        boolean needsCorners = shader.attributes.containsValue(Shader.INDICE_NORMAL);
        needsCorners &= corners != null;
        Vector2f res = Ref.glRef.GetResolution();
        float sx = 0, sy = 0, tx = 1, ty = 1;
        if(currentView.ViewportWidth * 2 == (int)res.x) {
            if(currentView.ViewportX > 0) {
                sx = 0.5f;
            } else {
                tx = 0.5f;
            }
        }
        if(currentView.ViewportHeight * 2 == (int)res.y) {
            if(currentView.ViewportY > 0) {
                sy = 0.5f;
            } else {
                ty = 0.5f;
            }
        }
        
        // Texture coords are flipped on y axis
        glBegin(GL_QUADS);
        {
            if(Ref.glRef.isShadersSupported()) {
                // Fancy pants shaders
                
                if(coords) glVertexAttrib2f(Shader.INDICE_COORDS, sx, sy);
                if(needsCorners) glVertexAttrib3f(Shader.INDICE_NORMAL, corners[0].x, corners[0].y, corners[0].z);
                glVertexAttrib3f(Shader.INDICE_POSITION, currentView.ViewportX, currentView.ViewportY, 0);

                if(coords) glVertexAttrib2f(Shader.INDICE_COORDS, tx, sy);
                if(needsCorners) glVertexAttrib3f(Shader.INDICE_NORMAL, corners[1].x, corners[1].y, corners[1].z);
                glVertexAttrib3f(Shader.INDICE_POSITION, currentView.ViewportX + currentView.ViewportWidth, currentView.ViewportY, 0);

                if(coords) glVertexAttrib2f(Shader.INDICE_COORDS, tx, ty);
                if(needsCorners) glVertexAttrib3f(Shader.INDICE_NORMAL, corners[2].x, corners[2].y, corners[2].z);
                glVertexAttrib3f(Shader.INDICE_POSITION, currentView.ViewportX + currentView.ViewportWidth, currentView.ViewportY + currentView.ViewportHeight, 0);

                if(coords) glVertexAttrib2f(Shader.INDICE_COORDS, sx, ty);
                if(needsCorners) glVertexAttrib3f(Shader.INDICE_NORMAL, corners[3].x, corners[3].y, corners[3].z);
                glVertexAttrib3f(Shader.INDICE_POSITION, currentView.ViewportX, currentView.ViewportY + currentView.ViewportHeight, 0);
            }
        }
        glEnd();
        
        GLRef.checkError();
        
        unsetTextures(tex0,tex1,tex2);
    }
}
