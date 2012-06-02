package cubetech.gfx;

import cubetech.CGame.ViewParams;
import cubetech.common.CVar;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.misc.PoissonGenerator;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author Mads
 */
public class SSAO {
    CubeTexture noiseTexture;
    Vector3f[] kernel;
    
    // shader uniforms
    private CVar ssao_kernelRadius = Ref.cvars.Get("ssao_kernelRadius", "36", null);
    public CVar ssao_enable = Ref.cvars.Get("ssao_enable", "1", null);
    public CVar ssao_display = Ref.cvars.Get("ssao_display", "0", null); // render on top of everything
    private CVar ssao_renderscale = Ref.cvars.Get("ssao_renderscale", "0.5", null);
    
    private FrameBuffer lowresTarget;
    private FrameBuffer lowresBlurTarget;
    private Vector2f targetResolution;
    
    public SSAO() {
        noiseTexture = generateNoiseTexture(4,4);
        kernel = generatePoissonKernel(32);
        ssao_renderscale.modified = false;
        int w = (int) (Ref.glRef.GetResolution().x * ssao_renderscale.fValue);
        int h = (int) (Ref.glRef.GetResolution().y * ssao_renderscale.fValue);
        lowresTarget = new FrameBuffer(true, false, w, h, GL11.GL_TEXTURE_2D);
        lowresBlurTarget = new FrameBuffer(true, false, w, h, GL11.GL_TEXTURE_2D);
        targetResolution = new Vector2f(Ref.glRef.GetResolution());
        Ref.commands.AddCommand("ssao_rebuildKernel", ssao_rebuildKernel);
    }
    
    private ICommand ssao_rebuildKernel = new ICommand() {
        public void RunCommand(String[] args) {
            int kernelSize = 32;
            kernel = generatePoissonKernel(kernelSize);
        }
    };
    
    public void debugDraw() {
        if(ssao_display.isTrue()) {
            blitTarget();
        }
    }
    
    public void run(DeferredShading s, ViewParams view) {
        if(!ssao_enable.isTrue()) return;
        
        SecTag t = Profiler.EnterSection(Sec.SSAO);
        
        int w = (int) (Ref.glRef.GetResolution().x * ssao_renderscale.fValue);
        int h = (int) (Ref.glRef.GetResolution().y * ssao_renderscale.fValue);
        
        if(!Helper.Equals(targetResolution, Ref.glRef.GetResolution()) || ssao_renderscale.modified) {
            // resize target    
            lowresTarget.resize(w, h);
            lowresBlurTarget.resize(w, h);
            targetResolution.set(Ref.glRef.GetResolution());
            ssao_renderscale.modified = false;
        }
        float sw = view.rectXScale * w;
        float sh = view.rectYScale * h;
        GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
        GL11.glViewport((int)(sw), (int)(sh), (int)(view.rectWidthScale * w), (int)(view.rectHeightScale * h));
        GL11.glDisable(GL11.GL_BLEND);
        // Push shader to gfx system
        Shader shader = Ref.glRef.getShader("DeferredAO");
        Ref.glRef.PushShader(shader);
        
        // Apply textures and uniforms
        applyUniforms(shader, view);
        
        lowresTarget.Bind();
        
        // Do fullscreen pass
        s.fullscreenPass(shader, false, false, true);
        
        // Clean up
        lowresTarget.Unbind();
        Ref.glRef.PopShader();
        
        // Blur the ssao
        shader = Ref.glRef.getShader("SSAOBlur");
        Ref.glRef.PushShader(shader);
        Vector4f viewOffset = new Vector4f(view.rectXScale, view.rectYScale, view.rectWidthScale, view.rectHeightScale);
        shader.setUniform("viewOffset", viewOffset);
        shader.setUniform("texelSize", new Vector2f(1f/(targetResolution.x * ssao_renderscale.fValue), 1f/(targetResolution.y  * ssao_renderscale.fValue)));
        
        lowresBlurTarget.Bind();
        CubeTexture tex = getSSAOTarget(false);
        tex.setWrap(GL13.GL_CLAMP_TO_BORDER);
        tex.Bind();
        s.fullscreenPass(shader, false, false, false);
        tex.Unbind();
        lowresBlurTarget.Unbind();
        
        Ref.glRef.PopShader();
        
        GL11.glPopAttrib();
        GL11.glEnable(GL11.GL_BLEND);
        
        t.ExitSection();
        
    }
    
    public CubeTexture getSSAOTarget(boolean blurTarget) {
        CubeTexture tex = null;
        if(blurTarget) {
            tex = new CubeTexture(lowresBlurTarget.getTarget(), lowresBlurTarget.getTextureId(), "Lowres AO target");
        } else {
            tex = new CubeTexture(lowresTarget.getTarget(), lowresTarget.getTextureId(), "Lowres AO target");
        }
        tex.textureSlot = 4;
        tex.loaded = true;
        if(blurTarget) {
            tex.setFiltering(false, GL11.GL_LINEAR);
            tex.setFiltering(true, GL11.GL_LINEAR);
        } else {
            tex.setFiltering(false, GL11.GL_NEAREST);
            tex.setFiltering(true, GL11.GL_NEAREST);
        }
        
        return tex;
    }
    
    private void blitTarget() {
        Ref.glRef.pushShader("Blit");
        CubeTexture tex = getSSAOTarget(true);
        tex.textureSlot = 0;
        tex.Bind();
        float w = Ref.glRef.GetResolution().x;
        float h = Ref.glRef.GetResolution().y;
        
        // Texture coords are flipped on y axis
        GL11.glBegin(GL11.GL_QUADS);
        {
            if(Ref.glRef.isShadersSupported()) {
                // Fancy pants shaders
                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, 0, 0);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, 0, 0, 0);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, 1, 0);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, w, 0, 0);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, 1, 1);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, w, h, 0);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, 0, 1);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, 0, h, 0);
            }
        }
        GL11.glEnd();
        tex.Unbind();
        Ref.glRef.PopShader();
        
        GLRef.checkError();
    }
    
    private void applyUniforms(Shader shader, ViewParams view) {
        Vector2f noiseScale = new Vector2f(Ref.glRef.GetResolution());
        noiseScale.scale(1f/(noiseTexture.Width/ssao_renderscale.fValue));
        
        Integer mapid = shader.textureMap.get("noise");
        if(mapid != null && mapid >= 0) {
            int uniformid = shader.GetTextureIndex(3);
            shader.setUniform(uniformid, 3);
            noiseTexture.setFiltering(false, GL11.GL_NEAREST);
            noiseTexture.setWrap(GL11.GL_REPEAT);
            noiseTexture.textureSlot = 3;
            noiseTexture.Bind();
        }
        
        shader.setUniform("far", Ref.cgame.cg.refdef.farDepth);
        shader.setUniform("kernel", kernel);
        shader.setUniform("noiseScale", noiseScale);
        shader.setUniform("kernelRadius", ssao_kernelRadius.fValue);
        shader.setUniform("projectionMatrix", Ref.cgame.cg.refdef.ProjectionMatrix);
        Vector4f viewOffset = new Vector4f(view.rectXScale, view.rectYScale, view.rectWidthScale, view.rectHeightScale);
        shader.setUniform("viewOffset", viewOffset);
    }
    
    public static Vector3f[] generatePoissonKernel(int sampleCount) {
        ArrayList<Vector3f> kernel = PoissonGenerator.generateUnitSphere(new Vector3f(0, 0, 1), sampleCount/2, sampleCount/2);
        ArrayList<Vector3f> kernel2 = PoissonGenerator.generateUnitSphere(new Vector3f(0, 0, 1), sampleCount/2, sampleCount/2);
        for (int i = 0; i < kernel2.size(); i++) {
            kernel2.get(i).scale(0.3f);
        }
        if((kernel.size() + kernel2.size()) != sampleCount) {
            Common.LogDebug("Couldn't generate wanted poisson kernel size of " + sampleCount + ". (got " + kernel.size() + ")");
        }
        Vector3f[] dest = new Vector3f[kernel.size() + kernel2.size()];
        for (int i = 0; i < kernel.size(); i++) {
            dest[i] = kernel.get(i);
        }
        for (int i = 0; i < kernel2.size(); i++) {
            dest[i+kernel.size()] = kernel2.get(i);
        }
        return dest;
    }
    
    /**
     * Generates a hemisphere kernel
     * @param sampleCount
     * @return 
     */
    public static Vector3f[] generateKernel(int sampleCount) {
        
        Vector3f[] kernel = new Vector3f[sampleCount];
        
        
        for (int i = 0; i < sampleCount; i++) {
            // Generate hemisphere point
            kernel[i] = new Vector3f(((float)Ref.rnd.nextFloat() - 0.5f) * 2f,
                                     ((float)Ref.rnd.nextFloat() - 0.5f) * 2f,
                                     ((float)Ref.rnd.nextFloat() - 0.5f) * 2f);
            
            // Normalize
            kernel[i].normalise();
            
            if(kernel[i].z < 0) kernel[i].z *= -1f;
            
            // Distribute
            float scale = (float)i / (sampleCount-1);
            scale = 0.1f + (0.9f  * scale);
            
            kernel[i].scale(scale);
        }
        
        return kernel;
    }
    
    /**
     * Creates n vectors with the x and y component set to random
     * values between -1 and 1
     * @param count
     * @return 
     */
    public static Vector3f[] generateNoise(int count) {
        Vector3f[] noise = new Vector3f[count];
        for (int i = 0; i < count; i++) {
            noise[i] = new Vector3f((Ref.rnd.nextFloat()),
                                    (Ref.rnd.nextFloat()),
                                    0.5f);
        }
        return noise;
    }
    
    public static CubeTexture generateNoiseTexture(int width, int height) {
        Vector3f[] noiseData = generateNoise(width * height);
        int texid = Ref.ResMan.getTextureLoader().CreateEmptyTexture(width, height, GL11.GL_TEXTURE_2D, false, null);
        ByteBuffer buf = ByteBuffer.allocateDirect(width*height*3);
        for (int i= 0; i < width*height; i++) {
            buf.put((byte)(noiseData[i].x * 255));
            buf.put((byte)(noiseData[i].y * 255));
            buf.put((byte)(noiseData[i].z * 255));
        }
        buf.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buf);
        CubeTexture tex = new CubeTexture(GL11.GL_TEXTURE_2D, texid, "Generated Noise texture");
        tex.loaded = true;
        tex.Width = width;
        tex.Height = height;
        GLRef.checkError();
        return tex;
    }
}
