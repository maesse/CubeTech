package cubetech.CGame;

import cern.colt.map.OpenLongObjectHashMap;
import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.common.Common.ErrorCode;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import java.lang.Thread.State;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.lwjgl.BufferUtils;

/**
 *
 * @author mads
 */
public class CubeChunkDataBuilder {
    private static final int NUM_BUILDBUFFERS = 8;
    private static final long NO_CHUNK = Long.MIN_VALUE;


    private static final Object bufferLock = new Object();
    private ByteBuffer[] buildBuffer = new ByteBuffer[NUM_BUILDBUFFERS];
    private long[] bufferRelations = new long[NUM_BUILDBUFFERS];
    private boolean[] bufferLocked = new boolean[NUM_BUILDBUFFERS];
    private boolean[] bufferBusy = new boolean[NUM_BUILDBUFFERS];
    private int freeBuffers = NUM_BUILDBUFFERS;

    final CubeChunkDataWorker worker = new CubeChunkDataWorker(this);
    private boolean workerStarted = false;
    final List<WorkerJob> workList = Collections.synchronizedList(new ArrayList<WorkerJob>());
    final List<WorkerJob> finishedList = Collections.synchronizedList(new ArrayList<WorkerJob>());

    public String[] getInfo() {
        String[] info = new String[NUM_BUILDBUFFERS+1];
        info[0] = "Worker: " + (worker.working?"ACTIVE":"IDLE");
        for (int i = 0; i < NUM_BUILDBUFFERS; i++) {
            info[i+1] = "Buffer " + i + ": " + (bufferBusy[i] ? "BUSY ":" ") + (bufferLocked[i] ? "LOCKED ":" ")
                    + "Rel: " + (bufferRelations[i] == NO_CHUNK?"IDLE":bufferRelations[i]);
        }
        
        
        return info;
    }
    
    public class WorkerJob {
        public CubeChunk chunk;
        public CubeChunk[] neighbors = new CubeChunk[27];
        public int visibleSides;
        WorkerJob(CubeChunk chunk) {
            this.chunk = chunk;
        }
    }

    public CubeChunkDataBuilder() {
        for (int i= 0; i < buildBuffer.length; i++) {
            buildBuffer[i] = BufferUtils.createByteBuffer(CubeChunk.PLANE_SIZE * CubeChunk.CHUNK_SIZE /2);
        }
    }

    public void scheduleChunk(CubeChunk chunk) {
        chunk.render.state = ChunkRender.State.WAITING;
        WorkerJob job = new WorkerJob(chunk);
        workList.add(job);
        if(workList.size() == 1 && bufferAvailable()) {
            synchronized(worker) {
                worker.interrupt(); // wakee-wakee
            }
        }
    }

    public boolean hasWork() {
        return !workList.isEmpty();
    }

    public WorkerJob getWork() {
        WorkerJob job = workList.remove(0);
        // Fill in the neighbors before passing on to the worker
        int index = 0;
        for (int z= -1; z <= 1; z++) {
            for (int y= -1; y <= 1; y++) {
                for (int x= -1; x <= 1; x++) {
                    job.neighbors[index++] = CubeMap.getChunk(
                            job.chunk.p[0]+x, job.chunk.p[1]+y, job.chunk.p[2]+z,
                            false, job.chunk.chunks);
                }
            }
        }
        
        return job;
    }

    public void finishedWork(WorkerJob job) {
        synchronized(finishedList) {
            finishedList.add(job);
        }
    }

    //
    // Intermediate buffer handling
    //
    public boolean bufferAvailable() {
        synchronized(bufferLock) {
            return freeBuffers > 0;
        }
    }

    public void markLockedChunks(OpenLongObjectHashMap chunks) {
        // boot up the worker
        if(!workerStarted) {
            workerStarted = true;
            worker.start();
        }        

        // Grab the finished work
        SecTag t = Profiler.EnterSection(Sec.CLIENTCUBES);
        synchronized(finishedList) {
            for (WorkerJob job : finishedList) {
                long chunkid = CubeMap.positionToLookup(job.chunk.p[0], job.chunk.p[1], job.chunk.p[2]);
                ByteBuffer data = getBuffer(chunkid);
                if(data == null) {
                    int test = 2;
                }
                job.chunk.render.updateVBO(data, job.visibleSides);
                releaseBuffer(chunkid);
            }
            finishedList.clear();
        }
        t.ExitSection();

        // Check our worker state
        State state = worker.getState();
        if(state == State.WAITING && workList.size() > 0) {
            // Dammit, why do you think i'm paying you?
            synchronized(worker) {
                worker.interrupt();
            }
        }
    }

    public ByteBuffer grabBuffer(long chunkid) {
        synchronized(bufferLock) {
            if(freeBuffers <= 0) return null;

            for (int i= 0; i < NUM_BUILDBUFFERS; i++) {
                // Grab the first unlocked buffer
                if(!bufferLocked[i]) {
                    // remember owner chunk
                    bufferRelations[i] = chunkid;
                    // decrement free buffer counter
                    freeBuffers--;
                    // lock this buffer
                    bufferLocked[i] = true;
                    bufferBusy[i] = true;
                    buildBuffer[i].clear();
                    return buildBuffer[i];
                }
            }

            Ref.common.Error(ErrorCode.FATAL, "freebuffers > 0, but all are locked");
            return null;
        }
    }

    // See if we can read from this buffer
    public ByteBuffer getBuffer(long chunkId) {
        synchronized(bufferLock) {
            for (int i= 0; i < NUM_BUILDBUFFERS; i++) {
                if(bufferRelations[i] == chunkId 
                        && bufferLocked[i]
                        && !bufferBusy[i]) {
                    return buildBuffer[i];
                }
            }
        }
        return null;
    }

    public void workerFinish(long chunkid) {
        synchronized(bufferLock) {
            for (int i= 0; i < NUM_BUILDBUFFERS; i++) {
                if(bufferRelations[i] == chunkid && bufferLocked[i]) {
                    bufferBusy[i] = false;
                    return;
                }
            }
        }
    }

    // After the buffer has been copied to a vbo
    public int releaseBuffer(long chunkid) {
        synchronized(bufferLock) {
            for (int i= 0; i < NUM_BUILDBUFFERS; i++) {
                if(bufferRelations[i] == chunkid && bufferLocked[i]) {
                    // unlock buffer
                    bufferRelations[i] = NO_CHUNK;
                    bufferLocked[i] = false;
                    freeBuffers++;
                    return i;
                }
            }
        }
        return -1;
    }

    public void dispose() {
        if(worker != null) {
            worker.exit = true;
            synchronized(worker) {
                worker.interrupt();
            }
        }
        buildBuffer = null;
    }
}
