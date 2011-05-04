/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.common.Common.ErrorCode;
import cubetech.gfx.GLRef.BufferTarget;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;

/**
 *
 * @author mads
 */
public class VBO {
    private int vboId = -1;
    private int sizeInBytes;
    private BufferTarget target;
    private ByteBuffer mappedBuffer = null;
    public float resizeMultiplier = 1f;

    public VBO(int sizeInBytes, BufferTarget target) {
        this.sizeInBytes = sizeInBytes;
        this.target = target;

        if(!Ref.glRef.fboSupported)
            Ref.common.Error(ErrorCode.FATAL, "VBO(): VBOs are not supported on this graphics card");

        vboId = Ref.glRef.createVBOid();
        Ref.glRef.sizeVBO(target, vboId, sizeInBytes);
    }

    public void bind() {
        Ref.glRef.bindVBO(target, vboId);
    }

    public ByteBuffer map() {
        return map(sizeInBytes);
    }

    public ByteBuffer map(int bytes) {
        if(mappedBuffer != null)
        {
            Ref.common.Error(ErrorCode.FATAL, "VBO.map(): Buffer is already mapped.");
        }

        if(bytes > sizeInBytes) {
            // create a new VBO
            //Ref.glRef.destroyVBO(vboId);
            sizeInBytes = (int) (bytes * resizeMultiplier);
            if(sizeInBytes < bytes) // just to be sure..
                sizeInBytes = bytes;
            //vboId = Ref.glRef.createVBOid();
            Ref.glRef.sizeVBO(target, vboId, sizeInBytes);
        }
        mappedBuffer = Ref.glRef.mapVBO(target, vboId, bytes);
        mappedBuffer.limit(mappedBuffer.capacity());
        return mappedBuffer;
    }

    public void unmap() {
        if(mappedBuffer == null) {
            Ref.common.Error(ErrorCode.FATAL, "VBO.unmap(): Tried to unmap buffer that wasn't mapped.");
        }
        mappedBuffer.flip();
        Ref.glRef.unmapVBO(target, false);
        mappedBuffer = null;
    }

    public void unbind() {
        Ref.glRef.unbindVBO(target);
    }
}
