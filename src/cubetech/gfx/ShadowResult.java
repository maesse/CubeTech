package cubetech.gfx;

import cubetech.misc.Ref;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Output from the shadow map generation for a single light
 * @author Mads
 */
public class ShadowResult {
    float[] cascadeEyespaceDepths;
    Matrix4f[] shadowProjections;
    Matrix4f[] shadowViewProjections;
    int nLevels;
    FrameBuffer depthBuffer;
    private Vector4f[] pcfOffsets = new Vector4f[] {
        new Vector4f(),
        new Vector4f(),
        new Vector4f(),
        new Vector4f()
    };

    ShadowResult() {
        this.nLevels = 4;
        cascadeEyespaceDepths = new float[4];
        shadowProjections = new Matrix4f[nLevels];
        shadowViewProjections = new Matrix4f[nLevels];
        for (int i = 0; i < nLevels; i++) {
            shadowProjections[i] = new Matrix4f();
            shadowViewProjections[i] = new Matrix4f();
            shadowViewProjections[i].setIdentity();
            cascadeEyespaceDepths[i] = 10000000;
        }
    }
    
    public Vector4f[] getPCFoffsets() {
        setPcfOffsets();
        return pcfOffsets;
    }

    private void setPcfOffsets() {
        float dist = Ref.glRef.shadowMan.shadow_kernel.fValue/depthBuffer.getResolution();

        pcfOffsets[0].x = -dist*3f;
        pcfOffsets[0].y = -dist*3f;

        pcfOffsets[1].x = dist;
        pcfOffsets[1].y = -dist*3f;

        pcfOffsets[2].x = -dist*3f;
        pcfOffsets[2].y = dist;

        pcfOffsets[3].x = dist;
        pcfOffsets[3].y = dist;

    }
    
    public CubeTexture getDepthTexture() {
        FrameBuffer depthFrameBuffer = depthBuffer;
        CubeTexture tex = new CubeTexture(depthFrameBuffer.getTextureTarget(), 
                                            depthFrameBuffer.getTextureId(),
                                            "depth");
        tex.loaded = true;
        return tex;
    }

    public Vector4f getCascadeDepths() {
        return new Vector4f(cascadeEyespaceDepths[0],
                cascadeEyespaceDepths[1],
                cascadeEyespaceDepths[2],
                cascadeEyespaceDepths[3]);
    }

    public Matrix4f[] getShadowViewProjections(int max, Light light) {
        Matrix4f[] out = new Matrix4f[max];
        if(nLevels < max) max = nLevels;
        System.arraycopy(shadowViewProjections, 0, out, 0, max);
        return out;
    }

    public void applyShadowProjection(int index) {
        if(shadowProjections[index] == null) return;
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        Matrix4f m = shadowProjections[index];

        Ref.glRef.matrixBuffer.clear();
        m.store(Ref.glRef.matrixBuffer);
        Ref.glRef.matrixBuffer.position(0);
        GL11.glLoadMatrix(Ref.glRef.matrixBuffer);
    }

    private Matrix4f getShadowViewProjection(int cascadeIndex, Light light) {
        Matrix4f scaleBias = new Matrix4f();
        scaleBias.setIdentity();
        scaleBias.m00 = 0.5f; scaleBias.m11 = 0.5f; scaleBias.m22 = 0.5f;
        scaleBias.m30 = 0.5f;
        scaleBias.m31 = 0.5f;
        scaleBias.m32 = 0.5f;
        Matrix4f m = shadowProjections[cascadeIndex];
        Matrix4f viewM = Matrix4f.load(light.getLightMatrix(), null);

        Matrix4f.mul(scaleBias, m, scaleBias);
        Matrix4f.mul(scaleBias, viewM, scaleBias);
        return scaleBias;
    }
}