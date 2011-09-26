/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author mads
 */
public class BMPWriter {
    ByteBuffer source = null;
    int width, height;
    private byte[] dest = null;
    
    public BMPWriter(ByteBuffer src, int width, int height) {
        this.source = src;
        this.width = width;
        this.height = height;
    }

    public void setSource(ByteBuffer source) {
        this.source = source;
    }
    
    public byte[] encodeImage() {
        source.position(0);

        int fileheader = 14;
        int bitmapheader = 40;

        int filelen = width*height*3 + fileheader + bitmapheader;
        if(dest == null || dest.length < filelen) dest = new byte[filelen];
        
        ByteBuffer dst = ByteBuffer.wrap(dest);
        dst.order(ByteOrder.LITTLE_ENDIAN);

        // Write file header
        dst.put((byte)0x42); // magic byte
        dst.put((byte)0x4D); // magic byte
        dst.putInt(dest.length);
        dst.putInt(0);
        dst.putInt(fileheader + bitmapheader); // pixel data offset
        // Write BITMAPINFOHEADER
        dst.putInt(bitmapheader); // size of header
        dst.putInt(width);
        dst.putInt(height);
        dst.putShort((short)1); // color planes? :S
        dst.putShort((short)24); // bits pr pixel
        dst.putInt(0); // compression 0 = RGB
        dst.putInt(width*height*3); // bitmap data size
        dst.putInt(3000); // pixels pr meter :S
        dst.putInt(3000); // pixels pr meter :S
        dst.putInt(0); // colors in the color palette
        dst.putInt(0); // number of "important" colors
        dst.put(source);

        return dest;
    }
}
