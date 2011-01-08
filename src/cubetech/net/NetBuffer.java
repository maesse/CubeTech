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
    private ByteBuffer buffer = null;
    //private int Offset = 0;
    //private boolean Writing = true;
    
    public NetBuffer() {
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
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

    public void Flip() {
        buffer.flip();
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
