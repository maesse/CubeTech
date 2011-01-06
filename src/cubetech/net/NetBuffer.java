/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import java.nio.ByteBuffer;

/**
 *
 * @author mads
 */
public class NetBuffer {
    final static int BUFFER_SIZE = 1024;
    ByteBuffer buffer = null;
    
    public NetBuffer() {
        buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    public void ResetOffset() {
        buffer.rewind();
    }

    public void Write(int value) {
        buffer.putInt(value);
    }

    public void Write(float value) {
        buffer.putFloat(value);
    }

    public void Write(String str) {
        byte[] strData = str.getBytes();
        buffer.putInt(strData.length);
        buffer.put(strData);
    }

    public String ReadString() {
        byte[] data2 = new byte[buffer.getInt()];
        buffer.get(data2);
        String str = new String(data2);
        return str;
    }

    public float ReadFloat() {
        return buffer.getFloat();
    }

    public int ReadInt() {
        return buffer.getInt();
    }
}
