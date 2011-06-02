package cubetech.Game;

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
    public HashMap<Long, ChunkEntry> chunkVersions = new HashMap<Long, ChunkEntry>();
    public Queue<ByteBuffer> queuedChunkData = new LinkedList<ByteBuffer>();
    public int queuedBytes = 0;
}
