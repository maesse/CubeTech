package cubetech.CGame;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.IndexedMesh;
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.common.CVar;
import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import cubetech.common.Helper;
import cubetech.gfx.*;
import cubetech.gfx.GLRef.BufferTarget;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector4f;

/**
 * On the client side, a ChunkRender is piggybacked onto each chunk.
 * @author mads
 */
public class ChunkRender {
    private static final float VBO_RESIZE_MULTIPLIER = 1.1f;
    private static final int LAZY_TIME = 1000;
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
    }

    public void destroy() {
        if(vbo == null) return;
        VBOPool.Global.freeVBO(vbo);
        vbo = null;
        state = State.DIRTY;
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
        preVbo();
            
        GL11.glDrawArrays(GL11.GL_QUADS, 0, vbo_nFaces*4);
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
        int stride = 32;
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_POSITION); // position
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_POSITION, 3, GL11.GL_FLOAT, false, stride, 0);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COLOR); // color
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COLOR, 4, GL11.GL_UNSIGNED_BYTE, true, stride, 3*4);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COORDS); // coords
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS, 2, GL11.GL_FLOAT, false, stride, 4*4);
    }

    public static void postVbo() {
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
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
        long chunkid = CubeMap.positionToLookup(chunk.p[0], chunk.p[1], chunk.p[2]);

        // Create a VBO if we don't have one
        long startTime = System.nanoTime();
        boolean newVBO = vbo == null;
        if(vbo == null) {
            // Start off with an exact vbo size.
            vbo = VBOPool.Global.allocateVBO(PLANE_SIZE * sidesRendered, BufferTarget.Vertex);
            // overgrow a bit when resizing
            vbo.resizeMultiplier = VBO_RESIZE_MULTIPLIER;
        } else if(vbo.getSize() < PLANE_SIZE * sidesRendered) {
            VBO temp = VBOPool.Global.allocateVBO(PLANE_SIZE * sidesRendered, vbo.getTarget());
            VBOPool.Global.freeVBO(vbo);
            vbo = temp;
        }

        // Copy data to VBO
        boolean vboResized = vbo.getSize() < PLANE_SIZE * sidesRendered; // shouldn't happen
        ByteBuffer buffer = vbo.map(PLANE_SIZE * sidesRendered);
        long startTime2 = System.nanoTime();
        if(buffer.limit()-buffer.position() < src.limit()) {
            Ref.common.Error(ErrorCode.FATAL, "bytebuffer -> vbo overflow in chunkRender");
        }
        buffer.put(src);

        vbo.unmap();
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
        Ref.cgame.map.nVBOthisFrame++;

        vboReady = true;
        if(queuedDirty) {
            // return to dirty :/
            state = State.DIRTY;
        } else {
            state = State.READY;
        }
    }
}
