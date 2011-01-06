/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

/**
 *
 * @author mads
 */
public class NetBuffer {
    byte[] data;
    int offset = 0;
    
    public NetBuffer() {
        data = new byte[1024];
        offset = 0;
    }

    public void ResetOffset() {
        offset = 0;
    }

    public void Write(int value) {
        data[offset] = (byte)( value >> 24 );
        data[offset+1] =(byte)( (value << 8) >> 24 );
        data[offset+2] =(byte)( (value << 16) >> 24 );
        data[offset+3] =(byte)( (value << 24) >> 24 );
        offset += 4;
    }

    public void Write(float value) {
        int newval = Float.floatToRawIntBits(value);
        Write(newval);
    }

    public void Write(String str) {
        byte[] strData = str.getBytes();
        Write(strData.length);
        System.arraycopy(strData, 0, data, offset, strData.length);

        offset += strData.length;
    }

    public String ReadString() {
        int nBytes = ReadInt();
        String str = new String(data, offset, nBytes);
        offset += nBytes;
        return str;
    }

    public float ReadFloat() {
        int val = ReadInt();
        return Float.intBitsToFloat(val);
    }

    public int ReadInt() {
        int value = (data[offset] & 0x000000ff)<<24;
        value += (data[offset+1]& 0x000000ff)<<16;
        value += (data[offset+2]& 0x000000ff)<<8;
        value += (data[offset+3]& 0x000000ff);
        offset += 4;

        return value;
    }
}
