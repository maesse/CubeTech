package cubetech.gfx;

import cubetech.common.CVar;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.GL11;
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
    private CVar ssao_kernelRadius = Ref.cvars.Get("ssao_kernelRadius", "24", null);
    public CVar ssao_enable = Ref.cvars.Get("ssao_enable", "0", null);
    public CVar ssao_display = Ref.cvars.Get("ssao_display", "0", null); // render on top of everything
    private CVar ssao_renderscale = Ref.cvars.Get("ssao_renderscale", "1.0", null);
    
    private FrameBuffer lowresTarget;
    private Vector2f targetResolution;
    
    public SSAO() {
        noiseTexture = generateNoiseTexture(4,4);
        kernel = generateKernel(32);
        ssao_renderscale.modified = false;
        int w = (int) (Ref.glRef.GetResolution().x * ssao_renderscale.fValue);
        int h = (int) (Ref.glRef.GetResolution().y * ssao_renderscale.fValue);
        lowresTarget = new FrameBuffer(true, false, w, h, GL11.GL_TEXTURE_2D);
        targetResolution = new Vector2f(Ref.glRef.GetResolution());
        Ref.commands.AddCommand("ssao_rebuildKernel", ssao_rebuildKernel);
    }
    
    private ICommand ssao_rebuildKernel = new ICommand() {
        public void RunCommand(String[] args) {
            int kernelSize = 32;
            kernel = generateKernel(kernelSize);
        }
    };
    
    public void run(DeferredShading s) {
        if(!ssao_enable.isTrue()) return;
        SecTag t = Profiler.EnterSection(Sec.SSAO);
        // Push shader to gfx system
        Shader shader = Ref.glRef.getShader("DeferredAO");
        Ref.glRef.PushShader(shader);
        
        // Apply textures and uniforms
        applyUniforms(shader);
        
        int w = (int) (Ref.glRef.GetResolution().x * ssao_renderscale.fValue);
        int h = (int) (Ref.glRef.GetResolution().y * ssao_renderscale.fValue);
        
        if(!Helper.Equals(targetResolution, Ref.glRef.GetResolution()) || ssao_renderscale.modified) {
            // resize target    
            lowresTarget.resize(w, h);
            targetResolution.set(Ref.glRef.GetResolution());
            ssao_renderscale.modified = false;
        }
        
        GL11.glViewport(0, 0, w, h);
        
        lowresTarget.Bind();
        // Do fullscreen pass
        s.fullscreenPass(shader, false, false, true);
        
        lowresTarget.Unbind();
        
        GL11.glViewport(0, 0, (int)Ref.glRef.GetResolution().x, (int)Ref.glRef.GetResolution().y);
        
        // Clean up
        Ref.glRef.PopShader();
        
        if(ssao_display.isTrue()) {
            blitTarget();
        }
        
        //blitTarget();
        
        // POsition test
//        Vector3f[] corners = s.calcFarPlaneCorners();
//        Vector3f texcoords = new Vector3f(1f,1f, 0.5f);
//        
//        Vector3f viewray = (Vector3f) new Vector3f(corners[2]).scale(texcoords.z);
//        // back to screen space
//        Vector4f projview = new Vector4f(viewray.x,viewray.y,viewray.z,1.0f);
//        Matrix4f.transform(Ref.cgame.cg.refdef.ProjectionMatrix, projview, projview);
//        // scale & bias to texture space
//        projview.scale(1.0f / projview.w);
//        projview.x = projview.x * 0.5f + 0.5f;
//        projview.y = projview.y * 0.5f + 0.5f;
//        
//        Vector2f diff = new Vector2f(texcoords.x - projview.x, texcoords.y - projview.y);
        t.ExitSection();
        
    }
    
    public CubeTexture getSSAOTarget() {
        CubeTexture tex = new CubeTexture(lowresTarget.getTarget(), lowresTarget.getTextureId(), "Lowres AO target");
        tex.loaded = true;
        tex.setFiltering(false, GL11.GL_LINEAR);
        return tex;
    }
    
    private void blitTarget() {
        Ref.glRef.pushShader("Blit");
        CubeTexture tex = getSSAOTarget();
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
    
    private void applyUniforms(Shader shader) {
        Vector2f noiseScale = new Vector2f(Ref.glRef.GetResolution());
        noiseScale.scale(1f/(2*noiseTexture.Width));
        
        
        Integer mapid = shader.textureMap.get("noise");
        if(mapid != null && mapid >= 0) {
            int uniformid = shader.GetTextureIndex(3);
            shader.setUniform(uniformid, 3);
            noiseTexture.setFiltering(false, GL11.GL_LINEAR);
            noiseTexture.setWrap(GL11.GL_REPEAT);
            noiseTexture.textureSlot = 3;
            noiseTexture.Bind();
        }
        
        
        shader.setUniform("far", Ref.cgame.cg.refdef.farDepth);
        shader.setUniform("kernel", kernel);
        shader.setUniform("noiseScale", noiseScale);
        shader.setUniform("kernelRadius", ssao_kernelRadius.fValue);
        shader.setUniform("projectionMatrix", Ref.cgame.cg.refdef.ProjectionMatrix);
        
    }
    
    /**
     * Generates a hemisphere kernel
     * @param sampleCount
     * @return 
     */
    public static Vector3f[] generateKernel(int sampleCount) {
        Vector3f[] kernel = new Vector3f[sampleCount];
        
        Vector3f kernelsum = new Vector3f();
        
        for (int i = 0; i < sampleCount; i++) {
            // Generate hemisphere point
            kernel[i] = new Vector3f(((float)Ref.rnd.nextFloat() - 0.5f) * 2f,
                                     ((float)Ref.rnd.nextFloat() - 0.5f) * 2f,
                                     ((float)Ref.rnd.nextFloat()));
            
            // Normalize
            kernel[i].normalise();
            
            // Distribute
            float scale = (float)i / sampleCount;
            scale = 0.2f + (0.8f * scale * scale);
            kernel[i].scale(scale);
            kernelsum.x += kernel[i].x < 0? -kernel[i].x : kernel[i].x;
            kernelsum.y += kernel[i].y < 0? -kernel[i].y : kernel[i].y;
            kernelsum.z += kernel[i].z < 0? -kernel[i].z : kernel[i].z;
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
        int texid = Ref.ResMan.CreateEmptyTexture(width, height, GL11.GL_TEXTURE_2D, false, null);
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
