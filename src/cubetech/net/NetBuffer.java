package cubetech.net;

import cubetech.collision.CubeChunk;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.common.Quaternion;
import cubetech.common.items.Weapon;
import cubetech.misc.Ref;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class NetBuffer {
    final static int BIG_BUFFER_SIZE = 16384; // Allocate 1400 bytes
    final static int BUFFER_SIZE = 1400; // Allocate 1400 bytes
    final static int BIG_POOL_SIZE = 128;
    final static int SMALL_POOL_SIZE = 256;
    static NetBuffer[] BufferPool = new NetBuffer[SMALL_POOL_SIZE]; // Circular buffer
    static NetBuffer[] BigBufferPool = new NetBuffer[BIG_POOL_SIZE]; // Circular buffer
    static int PoolIndex = 0;
    static int BigPoolIndex = 0;
    static boolean poolInit = false; // false untill first GetNetBuffer

    public boolean allowOverflow = false; 
    private ByteBuffer buffer = null;

    public static NetBuffer GetNetBuffer(boolean writeMagicHeader, boolean allowOverflow) {
        // Init pool the first time
        if(!poolInit) {
            for (int i = 0; i < SMALL_POOL_SIZE; i++) {
                BufferPool[i] = new NetBuffer(BUFFER_SIZE);   
            }
            for (int i = 0; i < BIG_POOL_SIZE; i++) {
                BigBufferPool[i] = new NetBuffer(BIG_BUFFER_SIZE);
            }

            poolInit = true;
        }

        NetBuffer buf;
        if(allowOverflow)
            buf = BigBufferPool[BigPoolIndex++ % BIG_POOL_SIZE];
        else 
            buf = BufferPool[PoolIndex++ % SMALL_POOL_SIZE];
        buf.Clear();
        buf.allowOverflow = allowOverflow;
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

    public void Write(short value) {
        buffer.putShort(value);
    }

    public void Write(byte b) {
        buffer.put(b);
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

    public void WriteDelta(short old, short newval) {
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

    public void WriteDelta(Vector3f old, Vector3f newval) {
        if(old != null && Helper.Equals(old, newval))
            Write(false);
        else
        {
            Write(true);
            Write(newval.x);
            Write(newval.y);
            Write(newval.z);
        }
    }
    
    public void WriteDelta(ReadableVector4f old, ReadableVector4f newval) {
        if(old != null && Helper.Equals(old, newval))
            Write(false);
        else
        {
            Write(true);
            Write(newval.getX());
            Write(newval.getY());
            Write(newval.getZ());
            Write(newval.getW());
        }
    }

    public int ReadDeltaInt(int old) {
        int newval = old;
        if(ReadBool())
            newval = ReadInt();
        return newval;
    }

    public short ReadDeltaShort(short old) {
        short newval = old;
        if(ReadBool())
            newval = ReadShort();
        return newval;
    }

    public float ReadDeltaFloat(float old) {
        float newval = old;
        if(ReadBool())
            newval = ReadFloat();
        return newval;
    }

    public Vector3f ReadDeltaVector(Vector3f old) {
        Vector3f newval = new Vector3f();
        if(ReadBool()) {
            newval.x = ReadFloat();
            newval.y = ReadFloat();
            newval.z = ReadFloat();
        } else if(old != null) {
            newval.x = old.x;
            newval.y = old.y;
            newval.z = old.z;
        }
        return newval;
    }
    
    public Quaternion ReadDeltaVector(Quaternion old) {
        Quaternion newval = new Quaternion();
        if(ReadBool()) {
            newval.x = ReadFloat();
            newval.y = ReadFloat();
            newval.z = ReadFloat();
            newval.w = ReadFloat();
        } else if(old != null) {
            newval.x = old.getX();
            newval.y = old.getY();
            newval.z = old.getZ();
            newval.w = old.getW();
        }
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

    private static byte ONE = 1; // lulz
    private static byte ZERO = 0;
    public void Write(boolean value) {
        buffer.put(value?ONE:ZERO);
    }

    public boolean ReadBool() {
        return buffer.get() == (byte)1;
    }

    public void Write(String str) {
        try {
            byte[] strData = str.getBytes("UTF-8");
            buffer.putInt(strData.length);
            if (strData.length > 0) {
                buffer.put(strData);
            }
        } catch (UnsupportedEncodingException ex) {
            Ref.common.Error(Common.ErrorCode.FATAL, "Current JVM does not support UTF-8: " + Common.getExceptionString(ex));
        }
    }

    public String ReadString() {
        try {
            int lenght = buffer.getInt();
            if (lenght < 0 || lenght >= 1024) {
                Common.LogDebug("NetBuffer.ReadString(): Invalid lenght: " + lenght);
                return null;
            }
            if (lenght == 0) {
                return "";
            }
            byte[] strData = new byte[lenght];
            buffer.get(strData);
            String str = new String(strData, "UTF-8");
            return str;
        } catch (UnsupportedEncodingException ex) {
            Ref.common.Error(Common.ErrorCode.FATAL, "Current JVM does not support UTF-8: " + Common.getExceptionString(ex));
            return "";
        }
    }

    public float ReadFloat() {
        return buffer.getFloat();
    }

    public int ReadInt() {
        return buffer.getInt();
    }

    public <T extends Enum<T>> void WriteEnum(T t) {
        if(t == null) WriteByte(-1);
        else WriteByte(t.ordinal());
    }

    public <T extends Enum> T ReadEnum(Class<T> c) {
        byte b = ReadByte();
        if(b < 0)return null;
        return c.getEnumConstants()[b];
    }

    public void compress() {
        if(!buffer.hasArray()) {
            Ref.common.Error(Common.ErrorCode.FATAL, "NetBuffer.compress(): Can't compress a buffer that has no backing array");
        }
        
        byte[] data = buffer.array();
        int srclen = buffer.position();
        byte[] dest = new byte[srclen+84];
        int wrote = CubeChunk.compressData(data, srclen, dest, 0, 4);
        buffer.clear();
        buffer.put(dest, 0, wrote);
    }

}
