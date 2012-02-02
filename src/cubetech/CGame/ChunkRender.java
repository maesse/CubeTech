package cubetech.CGame;

import cubetech.collision.ClientCubeMap;
import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.common.CVar;
import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import cubetech.gfx.*;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;

/**
 * On the client side, a ChunkRender is piggybacked onto each chunk.
 * @author mads
 */
public class ChunkRender {
    private static final float VBO_RESIZE_MULTIPLIER = 1.1f;
    public static final int SIZE = CubeChunk.SIZE;
    public static final int PLANE_SIZE = CubeChunk.PLANE_SIZE;
    public static final int BLOCK_SIZE = CubeChunk.BLOCK_SIZE;
    // render
    private VBO vbo = null;
    private int vbo_nFaces;
    private CubeChunk chunk;
    private boolean queuedDirty;
    private boolean vboReady = false;

    // temp variables
    private int[] org = new int[3];
    
    
    public State state = State.DIRTY;

    public enum State {
        EMPTY, // no cubes to render
        DIRTY, // needs an update
        WAITING, // waiting for a refresh
        BUILDING, // new data being built currently
        READY // ready to render
    }

    public ChunkRender(CubeChunk chunk) {
        this.chunk = chunk;
        Ref.cgame.physics.addChunk(chunk);
    }

    public void destroy() {
        if(vbo == null) return;
        VBOPool.Global.freeVBO(vbo);
        vbo = null;
        if(state != State.BUILDING && state != State.WAITING) state = State.DIRTY;
        
        vboReady = false;
    }

    public void Render() {
        if(state == State.EMPTY) return;
        if(state == State.DIRTY) {
            if(chunk.nCubes == 0) {
                // todo: check if there is an vbo that should be released
                state = State.EMPTY;
                return;
            }
            queuedDirty = false;
            Ref.cgame.map.builder.scheduleChunk(chunk);
        }
        if(state == State.READY || vboReady) renderVBO();
    }
    
    private void renderVBO() {
        vbo.bind();
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_POSITION, 3, GL11.GL_FLOAT, false, 32, 0);
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COLOR, 4, GL11.GL_UNSIGNED_BYTE, true, 32, 3*4);
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS, 2, GL11.GL_FLOAT, false, 32, 4*4);
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_NORMAL, 2, GL11.GL_FLOAT, false, 32, 6*4);
        GL12.glDrawRangeElements(GL11.GL_TRIANGLES, 0, vbo_nFaces*4 - 1, vbo_nFaces*6, GL11.GL_UNSIGNED_INT, 0);
        //GL11.glDrawArrays(GL11.GL_QUADS, 0, vbo_nFaces*4);
    }

    // notify all neighboughrs
    public void notifyChange() {
        notifyChange(0, false);
        notifyChange(0, true);
        notifyChange(1, false);
        notifyChange(1, true);
        notifyChange(2, false);
        notifyChange(2, true);
    }

    // Notify a neighbourgh chunk of a change on the edge of
    public void notifyChange(int axis, boolean pos) {
        org[0] = org[1] = org[2] = 0;
        org[axis] = pos?1:-1;
        CubeChunk c = getChunkInDirection(org);

        // mark it dirty
        if(c != null && c.render != null) c.render.setDirty(true, true);
    }

    public void setDirty(boolean isDirty, boolean lazy) {
        if(state == State.EMPTY || state == State.READY) {
            state = State.DIRTY;
            return;
        }

        if(state == State.BUILDING) {
            queuedDirty = true;
        }
    }

    public int getRenderFaces() {
        return vbo_nFaces;
    }

    public static void preVbo() {
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_POSITION); // position
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COLOR); // color
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COORDS); // coords
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_NORMAL); // coords
        
        
    }

    public static void postVbo() {
        GL20.glDisableVertexAttribArray(Shader.INDICE_POSITION);
        GL20.glDisableVertexAttribArray(Shader.INDICE_COLOR);
        GL20.glDisableVertexAttribArray(Shader.INDICE_COORDS);
        GL20.glDisableVertexAttribArray(Shader.INDICE_NORMAL);
    }

    private CubeChunk getChunkInDirection(int[] dir) {
        return CubeMap.getChunk(chunk.p[0]+dir[0], chunk.p[1]+dir[1], chunk.p[2]+dir[2], false, chunk.chunks);
    }



    public void updateVBO(ByteBuffer src, int sidesRendered) {
        if(sidesRendered == 0) {
            state = ChunkRender.State.EMPTY;

            if(vbo != null) {
                VBOPool.Global.freeVBO(vbo);
                vboReady = false;
                vbo = null;
            }
            return;
        }

        // Create a VBO if we don't have one
        long startTime = System.nanoTime();
        boolean newVBO = vbo == null;
        if(vbo == null) {
            // Start off with an exact vbo size.
            vbo = VBOPool.Global.allocateVBO(PLANE_SIZE * sidesRendered, VBO.BufferTarget.Vertex);
            // overgrow a bit when resizing
            vbo.resizeMultiplier = VBO_RESIZE_MULTIPLIER;
        } else if(vbo.getSize() < PLANE_SIZE * sidesRendered) {
            VBO temp = VBOPool.Global.allocateVBO(PLANE_SIZE * sidesRendered, vbo.getTarget());
            VBOPool.Global.freeVBO(vbo);
            vbo = temp;
        }

        // Copy data to VBO
        boolean vboResized = vbo.getSize() < PLANE_SIZE * sidesRendered; // shouldn't happen
        vbo.discard();
        ByteBuffer buffer = vbo.map(PLANE_SIZE * sidesRendered);
        long startTime2 = System.nanoTime();
        if(buffer == null) Ref.common.Error(ErrorCode.FATAL, "vbo.map == null");
        if(src == null) Ref.common.Error(ErrorCode.FATAL, "src == null");
        if(buffer.limit()-buffer.position() < src.limit()) {
            Ref.common.Error(ErrorCode.FATAL, "bytebuffer -> vbo overflow in chunkRender");
        }
        buffer.put(src);

        vbo.unmap();
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_POSITION, 3, GL11.GL_FLOAT, false, 32, 0);
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COLOR, 4, GL11.GL_UNSIGNED_BYTE, true, 32, 3*4);
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS, 2, GL11.GL_FLOAT, false, 32, 4*4);
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_NORMAL, 2, GL11.GL_FLOAT, false, 32, 6*4);
        vbo.unbind();
        vbo_nFaces = sidesRendered;
        
        long endTime = System.nanoTime();
        float ms = (endTime - startTime)/(1000F*1000F);
        float ms2 = (endTime - startTime2)/(1000F*1000F);
        float percent = (100f/ms) * ms2;
        CVar dev = Ref.cvars.Find("developer");
        if(ms > 8.0f && dev != null && dev.isTrue())  {
            Common.LogDebug("[CR] Building Chunk VBO took %.2fms (%d sides) %s(put+unmap %.0f%%)",
                ms, sidesRendered,newVBO?"(new) ":(vboResized?"(resized) ":" "), percent);
        }
        //Common.LogDebug("Finished reading buffer");
        ClientCubeMap.nVBOthisFrame++;

        vboReady = true;
        if(queuedDirty) {
            // return to dirty :/
            state = State.DIRTY;
        } else {
            state = State.READY;
        }
    }
}
