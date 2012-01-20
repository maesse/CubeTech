package cubetech.gfx;

import cubetech.common.Common.ErrorCode;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;

/**
 *
 * @author mads
 */
public class VBO {
    private static HashMap<Integer, ByteBuffer> cachedBuffers = new HashMap<Integer, ByteBuffer>();
    public static int TotalBytes = 0;
    private int vboId = -1; // opengl handle
    private int sizeInBytes; // current size
    private BufferTarget target; // vertex or index buffer
    private Usage usage;
    private ByteBuffer mappedBuffer = null; // currently mapped buffer
    public int stride; // not strict in any sense, but helpful to keep it here
    
    public float resizeMultiplier = 1f; // multiplier for resize when mapping
    public enum BufferTarget {
        Vertex,
        Index
    }
    public enum Usage {
        STATIC, // set once, used many times
        DYNAMIC, // updated and rendered many times
        STREAM // update once, use once
    }

    public VBO(int sizeInBytes, BufferTarget target) {
        this(sizeInBytes, target, Usage.STATIC);
    }

    private static int usageToInt(Usage u) {
        if(u == Usage.STATIC) return GL_STATIC_DRAW_ARB;
        if(u == Usage.DYNAMIC) return GL_DYNAMIC_DRAW_ARB;
        else return GL_STREAM_DRAW_ARB;
    }
    
    public VBO(int sizeInBytes, BufferTarget target, Usage usage) {
        this.sizeInBytes = sizeInBytes;
        this.target = target;
        this.usage = usage;
        if(!Ref.glRef.caps.GL_ARB_vertex_buffer_object)
            Ref.common.Error(ErrorCode.FATAL, "VBO(): VBOs are not supported on this graphics card");

        // create buffer id
        vboId = glGenBuffersARB();
        resize(sizeInBytes);
        TotalBytes += sizeInBytes;
    }

    public BufferTarget getTarget() {
        return target;
    }

    public void bind() {
        int t = BufferTargetToOpenGL(target);
        glBindBufferARB(t, vboId);
//        GLRef.checkError();
    }

    public ByteBuffer map() {
        return map(sizeInBytes);
    }

    public ByteBuffer map(int bytes) {
        if(mappedBuffer != null)
        {
            Ref.common.Error(ErrorCode.FATAL, "VBO.map(): Buffer is already mapped.");
        }

        // Bind
        bind();

        // Resize to fit
        if(bytes > sizeInBytes) {
            // create a new VBO
            TotalBytes -= sizeInBytes;
            if(resizeMultiplier >= 1f) sizeInBytes = (int) (bytes * resizeMultiplier);
            TotalBytes += sizeInBytes;
            resize(sizeInBytes);
        }

        // Check if we have a cached buffer
        Integer bufferID = (target == BufferTarget.Index?0:1) + ((vboId+1) << 1);
        ByteBuffer cachedBuf = cachedBuffers.get(bufferID);
        boolean newBuffer = false;
        if(cachedBuf != null && cachedBuf.capacity() < bytes) {
            // We're requesting a buffer buffer now, so remove the old from the cache
            cachedBuffers.remove(bufferID);
            newBuffer = true;
        } else if(cachedBuf == null) {
            newBuffer = true;
        }

        // Map it
        ByteBuffer buf =  glMapBufferARB(BufferTargetToOpenGL(target),
                GL_WRITE_ONLY_ARB, bytes, cachedBuf);
        buf.order(ByteOrder.nativeOrder());
        mappedBuffer = buf;
        mappedBuffer.clear();

        // Put in cache
        if(newBuffer) cachedBuffers.put(bufferID, buf);

        return mappedBuffer;
    }

    public int getSize() {
        return sizeInBytes;
    }

    public void unmap() {
        if(mappedBuffer == null) {
            Ref.common.Error(ErrorCode.FATAL, "VBO.unmap(): Tried to unmap buffer that wasn't mapped.");
        }
        // Assume buffer hasn't been flipped if it isn't positioned at 0
        if(mappedBuffer.position() != 0) mappedBuffer.flip();
        
        int t = BufferTargetToOpenGL(target);
        glUnmapBufferARB(t);
        mappedBuffer = null;
    }

    public void unbind() {
        unbind(target);
    }

    public static void unbind(BufferTarget target) {
        int t = BufferTargetToOpenGL(target);
        glBindBufferARB(t, 0);
        GLRef.checkError();
    }

    public void destroy() {
        if(sizeInBytes == 0) return;
        if(mappedBuffer != null) {
            unbind();
        }
        // Remove cached index & vertex buffers
        Integer bufferID = (vboId+1) << 1;
        cachedBuffers.remove(bufferID);
        cachedBuffers.remove(bufferID++);


        glDeleteBuffersARB(vboId);
        GLRef.checkError();
        sizeInBytes = 0;
    }

    public void discard() {
        bind();
        glBufferDataARB(BufferTargetToOpenGL(target), 0, usageToInt(usage));
        // Remove cached index & vertex buffers
        Integer bufferID = (vboId+1) << 1;
        cachedBuffers.remove(bufferID);
        cachedBuffers.remove(bufferID++);
        sizeInBytes = 0;
        GLRef.checkError();
    }

    private static int BufferTargetToOpenGL(BufferTarget value) {
        int t = GL_ARRAY_BUFFER_ARB;
        if(value == BufferTarget.Index)
            t = GL_ELEMENT_ARRAY_BUFFER_ARB;
        return t;
    }

    // Size is in elements, so 1 int = 1 size
    private void resize(int size) {
        int t = BufferTargetToOpenGL(target);
        bind();
        glBufferDataARB(t, size, usageToInt(usage));
        GLRef.checkError();
    }
}
