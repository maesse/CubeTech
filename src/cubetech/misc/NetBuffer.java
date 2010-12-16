/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

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

    public void Write(int value) {
        data[offset+0] =(byte)( value >> 24 );
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
        for (int i= 0; i < strData.length; i++) {
            data[offset+i] = strData[i];
        }

        offset += strData.length;
    }

    public String ReadString() {
        int nBytes = ReadInt();
        String str = new String(data, offset, nBytes);
        return str;
    }

    public float ReadFloat() {
        int val = ReadInt();
        return Float.intBitsToFloat(val);
    }

    public int ReadInt() {
        int value = 0;
        value |= data[offset]<<24;
        value |= data[offset+1]<<16;
        value |= data[offset+2]<<8;
        value |= data[offset+3];
        offset += 4;

        return value;
    }
}
