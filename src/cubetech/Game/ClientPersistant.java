package cubetech.Game;

import cern.colt.map.OpenLongObjectHashMap;
import cubetech.input.PlayerInput;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * client data that stays across multiple respawns, but is cleared
 * on each level change or team change at ClientBegin()
 * @author mads
 */
public class ClientPersistant {

    
    public enum ClientConnected {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    
    public PlayerInput cmd; // latest user command
    public boolean LocalClient; // true if this guy is running the server
    public String Name = "N/A";
    public int JoinTime; // level.time at the point of connection
    public ClientConnected connected = ClientConnected.DISCONNECTED;

    // Chunk data
    public OpenLongObjectHashMap chunkVersions = new OpenLongObjectHashMap();
    private Queue<ByteBuffer> queuedChunkData = new LinkedList<ByteBuffer>();
    private int queuedBytes = 0;

    public ByteBuffer dequeueChunkData() {
        return dequeueChunkData(0);
    }

    public boolean isQueueEmpty() {
        return queuedChunkData.isEmpty();
    }

    public void queueChunkData(ByteBuffer data) {
        queuedChunkData.add(data);
        queuedBytes += data.limit();
    }

    public ByteBuffer dequeueChunkData(int maxBytes) {
        ByteBuffer base = queuedChunkData.poll();
        if(base == null) return null;
        queuedBytes -= base.limit();
        
        int currentBytes = base.limit();
        // If we're past out max limit, just return the base
        if(currentBytes >= maxBytes) return base;

        int freeBytes = base.capacity()-base.limit();
        ByteBuffer next;
        while((next = queuedChunkData.peek()) != null
                && freeBytes - next.limit() > 0
                && currentBytes + next.limit() < maxBytes) {
            queuedChunkData.poll();

            int addSize = next.limit();
            // prepare base buffer
            base.limit(currentBytes + addSize);
            base.position(currentBytes);

            queuedBytes -= addSize;
            currentBytes += addSize;
            freeBytes -= addSize;
            base.put(next);
        }
        base.position(0);
        return base;
    }
}
