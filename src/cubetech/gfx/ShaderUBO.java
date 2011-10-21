/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.misc.Ref;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBUniformBufferObject;
import org.lwjgl.opengl.GL15;

/**
 *
 * @author mads
 */
public class ShaderUBO {
    private static final int TARGET = ARBUniformBufferObject.GL_UNIFORM_BUFFER;
    private int gpu_bonematIndex;
    private int gpu_ubosize;
    private int gpu_bonematOffset;
    private int gpu_ubo;
    private Shader shader;

    private ShaderUBO(Shader shader) {
        this.shader = shader;
    }

    public int getSize() {
        return gpu_ubosize;
    }

    public int getHandle() {
        return gpu_ubo;
    }

    public int getOffset() {
        return gpu_bonematOffset;
    }

    public int getIndex() {
        return gpu_bonematIndex;
    }

    public static ShaderUBO initUBOForShader(Shader shader) {
        if(Ref.glRef.getGLCaps().GL_ARB_uniform_buffer_object) {
            ShaderUBO ubo = new ShaderUBO(shader);
            IntBuffer intBuff = BufferUtils.createIntBuffer(1);
            int blockIndex = ARBUniformBufferObject.glGetUniformBlockIndex(shader.getShaderId(), "animdata");
            ARBUniformBufferObject.glGetUniformIndices(shader.getShaderId(), new CharSequence[] {"bonemats"}, intBuff);
            ubo.gpu_bonematIndex = intBuff.get(0); intBuff.clear();

            ubo.gpu_ubosize = ARBUniformBufferObject.glGetActiveUniformBlock(
                    shader.getShaderId(),
                    blockIndex,
                    ARBUniformBufferObject.GL_UNIFORM_BLOCK_DATA_SIZE);

            ubo.gpu_bonematOffset = ARBUniformBufferObject.glGetActiveUniforms(shader.getShaderId(), ubo.gpu_bonematIndex, ARBUniformBufferObject.GL_UNIFORM_OFFSET);

            ARBUniformBufferObject.glUniformBlockBinding(shader.getShaderId(), blockIndex, 0);
            ubo.gpu_ubo = GL15.glGenBuffers();
            return ubo;
        }
        return null;
    }

    public void bind() {
        GL15.glBindBuffer(TARGET, gpu_ubo);
    }

    public static void unbind() {
        GL15.glBindBuffer(TARGET, 0);
    }
}