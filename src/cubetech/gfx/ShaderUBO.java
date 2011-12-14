/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.common.Common;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import org.lwjgl.BufferUtils;
import static  org.lwjgl.opengl.ARBUniformBufferObject.*;
import org.lwjgl.opengl.GL15;

/**
 *
 * @author mads
 */
public class ShaderUBO {
    private HashMap<String, RegisteredUniform> uniforms = new HashMap<String, RegisteredUniform>();
    private int size;
    private int id;
    private Shader shader;
    private ByteBuffer buffer;

    private ShaderUBO(Shader shader) {
        this.shader = shader;
    }

    public int getSize() {
        return size;
    }

    public int getHandle() {
        return id;
    }

    public int getOffset(String uniform) {
        RegisteredUniform register = null;
        if((register = uniforms.get(uniform)) != null) {
            return register.offset;
        }
        
        return -1;
    }

    public int getIndex(String uniform) {
        RegisteredUniform register = null;
        if((register = uniforms.get(uniform)) != null) {
            return register.index;
        }
        
        return -1;
    }
    
    public void submitBuffer(int offset, boolean flip) {
        if(flip) buffer.flip();
        if(offset < 0) return;
        bind();
        GL15.glBufferSubData(GL_UNIFORM_BUFFER, offset, buffer);
    }

    public static void printUnifomBlockInfo(Shader shader, int blockIndex) {
        int program = shader.getShaderId();

        // Get block name
        int nameLen = glGetActiveUniformBlock(program, blockIndex, GL_UNIFORM_BLOCK_NAME_LENGTH);
        String blockname = glGetActiveUniformBlockName(program, blockIndex, nameLen);

        // Grab uniform info
        int nActive = glGetActiveUniformBlock(program, blockIndex, GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS);
        IntBuffer indices = BufferUtils.createIntBuffer(nActive<16?16:nActive);
//        
        glGetActiveUniformBlock(program, blockIndex, GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES, indices);
        indices.limit(nActive);
        IntBuffer name_lenghts = BufferUtils.createIntBuffer(nActive<16?16:nActive);
        IntBuffer offsets = BufferUtils.createIntBuffer(nActive<16?16:nActive);
        IntBuffer types = BufferUtils.createIntBuffer(nActive<16?16:nActive);
        IntBuffer sizes = BufferUtils.createIntBuffer(nActive<16?16:nActive);
        IntBuffer strides = BufferUtils.createIntBuffer(nActive<16?16:nActive);
        
        glGetActiveUniforms(program, indices, GL_UNIFORM_NAME_LENGTH, name_lenghts);
        glGetActiveUniforms(program, indices, GL_UNIFORM_OFFSET, offsets);
        glGetActiveUniforms(program, indices, GL_UNIFORM_TYPE, types);
        glGetActiveUniforms(program, indices, GL_UNIFORM_SIZE, sizes);
        glGetActiveUniforms(program, indices, GL_UNIFORM_ARRAY_STRIDE, strides);

        Common.Log("Inspecting uniform block '%s' (index %d) in %s", blockname, blockIndex, shader.getName());

        for (int i= 0; i < nActive; i++) {
            String name = glGetActiveUniformName(program, indices.get(i), name_lenghts.get(i));
            int offset = offsets.get(i);
            int type = types.get(i);
            int size = sizes.get(i);
            int stride = strides.get(i);

            Common.Log("   %s: 0x%d size: %d, stride: %d, type: %d", name, offset, size, stride, type);
        }
    }
    
    private class RegisteredUniform {
        String name;
        int index;
        int offset;
        int size;
        RegisteredUniform(String name, int index, int offset, int size) {
            this.name = name; this.index = index;
            this.offset = offset; this.size = size;
        }
    }
    
    public void registerUniform(String uniform) {
        if(uniforms.containsKey(uniform)) return;
        IntBuffer intbuffer = BufferUtils.createIntBuffer(4);
        glGetUniformIndices(shader.getShaderId(), new CharSequence[] {uniform}, intbuffer);
        int uniformindex = intbuffer.get(0);
        int uniformoffset = glGetActiveUniforms(shader.getShaderId(), uniformindex, GL_UNIFORM_OFFSET);
        int uniformsize = glGetActiveUniforms(shader.getShaderId(), uniformindex, GL_UNIFORM_SIZE);
        GLRef.checkError();
        
        uniforms.put(uniform, new RegisteredUniform(uniform, uniformindex, uniformoffset, uniformsize));
    }

    public static ShaderUBO initUBOForShader(Shader shader, String blockname) {
        if(Ref.glRef.getGLCaps().GL_ARB_uniform_buffer_object) {
            ShaderUBO ubo = new ShaderUBO(shader);
            int blockIndex = glGetUniformBlockIndex(shader.getShaderId(), blockname);
            printUnifomBlockInfo(shader, blockIndex);
            GLRef.checkError();
            
            ubo.size = glGetActiveUniformBlock(shader.getShaderId(),blockIndex,GL_UNIFORM_BLOCK_DATA_SIZE);
            GLRef.checkError();
            
            ubo.id = GL15.glGenBuffers();
            ubo.bind();
            GL15.glBufferData(GL_UNIFORM_BUFFER, ubo.size, GL15.GL_DYNAMIC_DRAW);
            unbind();
            GLRef.checkError();
            
            ubo.buffer = BufferUtils.createByteBuffer(ubo.size);
            ubo.buffer.order(ByteOrder.nativeOrder());
            return ubo;
        }
        return null;
    }

    public void bind() {
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, id);
    }

    public static void unbind() {
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    public ByteBuffer getBuffer(boolean clear) {
        if(clear) buffer.clear();
        return buffer;
    }
}