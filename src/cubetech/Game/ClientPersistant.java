package cubetech.Game;

import cern.colt.map.OpenLongObjectHashMap;
import cubetech.input.PlayerInput;
import cubetech.net.NetBuffer;
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

    
    public PlayerInput cmd = new PlayerInput(); // latest user command
    public boolean LocalClient; // true if this guy is running the server
    public String Name = "N/A";
    public int JoinTime; // level.time at the point of connection
    public ClientConnected connected = ClientConnected.DISCONNECTED;

    // Chunk data
    public OpenLongObjectHashMap chunkVersions = new OpenLongObjectHashMap();
    private Queue<ByteBuffer> queuedChunkData = new LinkedList<ByteBuffer>();
    private int queuedBytes = 0;

    // score stuff
    public int score;

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

    public ByteBuffer dequeueChunkData(int freeBytes) {
        if(queuedChunkData.isEmpty()) return null;
        
        ByteBuffer dest = null;
        ByteBuffer src = null;
        boolean first = true;
        while((src = queuedChunkData.peek()) != null &&
                (first || src.limit() <= freeBytes)) {
            if(first) {
                if(src.limit() > 1400) dest = NetBuffer.GetNetBuffer(false, true).GetBuffer();
                else dest = NetBuffer.GetNetBuffer(false, false).GetBuffer();
            }

            queuedChunkData.poll();
            dest.put(src);
            freeBytes -= src.limit();
            queuedBytes -= src.limit();
            first = false;
        }

        dest.flip();
        return dest;
    }
}
