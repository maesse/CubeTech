package cubetech.gfx;

import cubetech.misc.Profiler.SecTag;
import cubetech.CGame.ViewParams;
import java.nio.ByteBuffer;
import cubetech.CGame.RenderEntity;
import cubetech.common.Common.ErrorCode;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Ref;
import java.util.ArrayList;
import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author Mads
 */
public class PolyBatcher  {
    // main vbo, used for multiple batches & multiple views
    private VBO vbo;
    
    // finished batches that's valid for the current vbo state
    private ArrayList<PolyBatch> validBatches = new ArrayList<PolyBatch>();
    private PolyBatch lastestBatch = null;
    
    // current batch builder
    private ArrayList<IBatchCall> calls;
    private ByteBuffer mappedBuffer;
    private BatchCall currentCall = null;
    
    private int vboSize = 10000 * 36;
    
    public PolyBatcher() {
        if(vbo == null) {
            vbo = new VBO(vboSize, VBO.BufferTarget.Vertex, VBO.Usage.DYNAMIC);
            StrideInfo info = new StrideInfo(36);
            info.addItem(Shader.INDICE_POSITION, 3, GL_FLOAT, false, 0);
            info.addItem(Shader.INDICE_NORMAL, 3, GL_FLOAT, false, 12);
            info.addItem(Shader.INDICE_COORDS, 2, GL_FLOAT, false, 24);
            info.addItem(Shader.INDICE_COLOR, 4, GL_UNSIGNED_BYTE, true, 32);
            vbo.strideInfo = info;
        }
    }
    
    public final void reset() {
        validBatches.clear();
        
        lastestBatch = null;
    }
    
    public final void beginList() {
        if(mappedBuffer != null) {
            Ref.common.Error(ErrorCode.FATAL, "PolyBatch.begin() without finish()");
        }
        
        // init list builder
        calls = new ArrayList<IBatchCall>();
        currentCall = null;
    }
    
    private int getBufferOffset() {
        // Need to position buffer for multiple views
        if(lastestBatch != null) {
            IBatchCall call = lastestBatch.calls.get(lastestBatch.calls.size()-1);
            int offset = call.getVertexCount() + call.getVertexOffset();
            return offset;
            
        }
        return 0;
    }
    
    public final PolyBatch finishList() {
        if(mappedBuffer == null) {
            calls = null;
            return null;
        }
        
        // add any dangling builder call to the list
        if(currentCall != null) {
            calls.add(currentCall);
            currentCall = null;
        }
        
        mappedBuffer = null;
        vbo.bind();
        vbo.unmap();
        vbo.unbind();
        
        if(!calls.isEmpty()) {
            // create polybatch
            PolyBatch batch = new PolyBatch(vbo, calls);
            validBatches.add(batch);
            lastestBatch = batch;
            calls = null;
            return batch;
        } else {
            calls = null;
            return null;
        }
    }
    
    public void addPolyCall(RenderEntity ent) {
        
    }
    
    public void addSpriteCall(RenderEntity ent) {
        if(currentCall == null) {
            
            currentCall = new BatchCall(ent.mat, ent.flags, getBufferOffset(), 4);
        } else {
            // Try to batch this call
            if(!currentCall.add(ent.mat, ent.flags, 4)) {
                
                // didn't fit in the current batchcall, make a new one
                calls.add(currentCall);
                int newoffset = currentCall.vertexOffset + currentCall.vertexCount;
                currentCall = new BatchCall(ent.mat, ent.flags, newoffset, 4);
            }
        }
    }
    
    public ByteBuffer getMappedBuffer() {
        if(mappedBuffer == null) {
            SecTag t = Profiler.EnterSection(Sec.VBO_MAP);
            vbo.discard();
            mappedBuffer = vbo.map(vboSize);
            mappedBuffer.position(getBufferOffset()*36);
            t.ExitSection();
        }
        return mappedBuffer;
    }

    // This will end up being one call
    public static class BatchCall implements IBatchCall {
        // Call state
        private CubeMaterial mat;
        private int entityFlags;
        
        private int vertexOffset;
        private int vertexCount;
        
        private BatchCall(CubeMaterial mat, int flags, int offset, int nverts) {
            if(mat == null) mat = Ref.ResMan.getWhiteTexture().asMaterial();
            this.mat = mat;
            entityFlags = flags;
            vertexOffset = offset;
            vertexCount = nverts;
        }
        
        private boolean add(CubeMaterial mat, int flags, int nverts) {
            if(mat == null) mat = Ref.ResMan.getWhiteTexture().asMaterial();
            if(this.mat != mat || entityFlags != flags) return false;
            
            vertexCount += nverts;
            return true;
        }

        public CubeMaterial getMaterial() {
            return mat;
        }

        public int getVertexOffset() {
            return vertexOffset;
        }

        public int getVertexCount() {
            return vertexCount;
        }
    }
    
    public static class PolyBatch extends AbstractBatchRender {
        private PolyBatch(VBO vbo, ArrayList<IBatchCall> calls) {
            this.vbo = vbo;
            this.calls = calls;
        }
        
        @Override
        public void setState(ViewParams view) {
            Shader sh = Ref.glRef.getShader("Poly");
            Ref.glRef.PushShader(sh);
            sh.setUniform("ModelView", view.viewMatrix);
            sh.setUniform("Projection", view.ProjectionMatrix);
            glDisable(GL_CULL_FACE);
            glDepthMask(false); // dont write to depth
            GLState.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        }

        @Override
        public void unsetState() {
            glEnable(GL_CULL_FACE);
            glDepthMask(true);
            Ref.glRef.PopShader();
        }
        
    }
    
}
