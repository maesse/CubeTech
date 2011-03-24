package cubetech.net;

import cubetech.common.Helper;
import java.nio.ByteBuffer;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class NetBuffer {
    final static int BUFFER_SIZE = 16384; // Allocate 1400 bytes
    final static int POOL_SIZE = 128;
    static NetBuffer[] BufferPool = new NetBuffer[POOL_SIZE]; // Circular buffer
    static int PoolIndex = 0;
    static boolean poolInit = false; // false untill first GetNetBuffer
    public boolean allowOverflow = false; 

    private ByteBuffer buffer = null;

    public static NetBuffer GetNetBuffer(boolean writeMagicHeader) {
        // Init pool the first time
        if(!poolInit) {
            for (int i = 0; i < POOL_SIZE; i++) {
                BufferPool[i] = new NetBuffer(BUFFER_SIZE);
            }
            poolInit = true;
        }

        NetBuffer buf = BufferPool[PoolIndex++ % POOL_SIZE];
        buf.Clear();
        if(writeMagicHeader)
            buf.Write(Net.MAGIC_NUMBER);
        return buf;
    }

    // Creates a custom netbuffer from a bytearray
    public static NetBuffer CreateCustom(ByteBuffer bytes) {
        NetBuffer buf = new NetBuffer(bytes);
        return buf;
    }

    private NetBuffer() {} // Should only be used for custom netbuffer usage
    
    private NetBuffer(int bufSize) {
        buffer = ByteBuffer.allocate(bufSize);
    }

    public NetBuffer(ByteBuffer buf) {
        buffer = buf;
    }

    public ByteBuffer GetBuffer() {
        return buffer;
    }

    public void Clear() {
        buffer.clear();
    }

    public void Write(byte[] data, int offset, int lenght) {
        buffer.put(data, offset, lenght);
    }

    // Flips the buffer
    public void Flip() {
        buffer.flip();
    }

    public void Write(int value) {
        buffer.putInt(value);
    }

    public void WriteByte(int value) {
        buffer.put((byte)value);
    }

    public short ReadShort() {
        return buffer.getShort();
    }

    public byte ReadByte() {
        return buffer.get();
    }

    public void WriteShort(int value) {
        buffer.putShort((short)value);
    }

    public void Write(Vector2f value) {
        Write(value.x);
        Write(value.y);
    }

    public void WriteDelta(int old, int newval) {
        if(old == newval)
            Write(false);
        else
        {
            Write(true);
            Write(newval);
        }
    }

    public void WriteDelta(float old, float newval) {
        if(old == newval)
            Write(false);
        else
        {
            Write(true);
            Write(newval);
        }
    }

    public void WriteDelta(Vector2f old, Vector2f newval) {
        if(old != null && Helper.Equals(old, newval))
            Write(false);
        else
        {
            Write(true);
            Write(newval.x);
            Write(newval.y);
        }
    }

    public int ReadDeltaInt(int old) {
        int newval = old;
        if(ReadBool())
            newval = ReadInt();
        return newval;
    }

    public float ReadDeltaFloat(float old) {
        float newval = old;
        if(ReadBool())
            newval = ReadFloat();
        return newval;
    }

    public Vector2f ReadDeltaVector(Vector2f old) {
        Vector2f newval = new Vector2f();
        if(ReadBool()) {
            newval.x = ReadFloat();
            newval.y = ReadFloat();
        } else if(old != null) {
            newval.x = old.x;
            newval.y = old.y;
        }
        return newval;
    }

    public Vector2f ReadVector() {
        Vector2f newval = new Vector2f();
        newval.x = ReadFloat();
        newval.y = ReadFloat();
        return newval;
    }

    public void Write(float value) {
        buffer.putFloat(value);
    }

    public void Write(boolean value) {
        buffer.put(value?(byte)1:(byte)0);
    }

    public boolean ReadBool() {
        return buffer.get() == (byte)1;
    }

    public void Write(String str) {
        byte[] strData = str.getBytes();
        buffer.putInt(strData.length);
        buffer.put(strData);
    }

    public String ReadString() {
        int lenght = buffer.getInt();
        if(lenght < 0 ||lenght >= 1024) {
            System.out.println("NetBuffer.ReadString(): Invalid lenght: " + lenght);
            return null;
        }
        byte[] strData = new byte[lenght];
        buffer.get(strData);
        String str = new String(strData);
        return str;
    }

    public float ReadFloat() {
        return buffer.getFloat();
    }

    public int ReadInt() {
        return buffer.getInt();
    }
}
