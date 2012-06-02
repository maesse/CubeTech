package cubetech.gfx;

import cubetech.common.Common;
import cubetech.common.Helper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.lwjgl.BufferUtils;

/**
 *
 * @author Mads
 */
public class PNGReader {
    
    
    public static PNGFile readFile(ByteBuffer input) throws Exception {
        // PNG use big endian byteencoding
        input.order(ByteOrder.BIG_ENDIAN);
        input.position(0);
        
        // Read file signature
        byte[] signature = new byte[8];
        input.get(signature);
        
        // Look for 'PNG' string
        if(signature[1] != 0x50 || signature[2] != 0x4e || signature[3] != 0x47) {
            throw new IOException("Invalid signature in PNG file");
        }
        
        PNGFile file = new PNGFile();
        PNGHeader header = null;
        ArrayList<Chunk> dataChunks = new ArrayList<Chunk>();
        
        
        
        // Start reading chunks
        boolean eof = false;
        while(!eof) {
            Chunk chunk = new Chunk(input);
            ChunkType type = ChunkType.find(chunk.type);
            if(type == null) {
                System.out.println("Unknown chunk (ID: " + chunk.asString() + ")");
                continue;
            }
            
            switch(type) {
                case sRGB:
                    file.sRGB = true;
                    break;
                case IHDR:
                    // Read PNG header
                    header = new PNGHeader(chunk.data);

                    // Check if the format is supported
                    if((header.type != ColorType.TRUECOLOR && 
                            header.type != ColorType.TRUECOLOR_ALPHA)
                            || header.compressiontype != 0) {
                        throw new IOException("Invalid PNG colortype " + header.type.toString());
                    }

                    // Fill in values in PNGFile
                    file.width = header.width;
                    file.height = header.height;
                    file.hasAlpha = header.type == ColorType.TRUECOLOR_ALPHA;
                    break;
                case IDAT:
                    // queue chunks
                    dataChunks.add(chunk);
                    break;
                case IEND:
                    // Decompress chunks
                    ByteBuffer data = inflateData(dataChunks, header);
                    
                    // Unfilter
                    unfilterData(data, file);
                    
                    file.data = data;
                    
                    swapScanlines(file);
                    
                    eof = true;
                    break;
                default:
                    System.out.println("Unhandled chunk type: " + chunk.asString() + " len: " + chunk.length);
                    break;
            }
            
        }
        
        
        return file;
    }
    
    private static void swapScanlines(PNGFile file) {
        ByteBuffer newdata = BufferUtils.createByteBuffer(file.data.limit());
        int scanwidth = file.width * (file.hasAlpha?4:3);
        
        for (int i = 0; i < file.height; i++) {
            file.data.position((file.height - 1 - i) * scanwidth);
            file.data.limit(file.data.position() + scanwidth);
            newdata.put(file.data);
        }
        newdata.flip();
        file.data = newdata;
    }
    
    private static ByteBuffer unfilterData(ByteBuffer input, PNGFile file) throws DataFormatException {
        byte[] data = input.array();
        int read = 0;
        int write = 0;
        
        int bpp = (file.hasAlpha?4:3);
        int scanwidth = file.width * bpp;
        byte zero = (byte)0;
        for (int i = 0; i < file.height; i++) {
            // Read filter type
            FilterType filter = FilterType.find(data[read++] & 0xff);
            file.filterstats[filter.type]++;
            switch(filter) {
                case NONE:
                    // Move raw data to the write offset
                    for (int j = 0; j < scanwidth; j++) {
                        data[write++] = data[read++];
                    }
                    break;
                case SUB:
                    for (int j = 0; j < scanwidth; j++) {
                        // recon(x) = raw(x) + recon(x-bpp)
                        data[write] = (byte)(data[read] + (j<bpp ? zero : data[write-bpp]));
                        write++;
                        read++;
                    }
                    break;
                case UP:
                    boolean noDeltaScanline = i == 0;
                    for (int j = 0; j < scanwidth; j++) {
                        // recon(x) = raw(x) + recon(x-scanline)
                        data[write] = (byte)(data[read] + (noDeltaScanline ? zero : data[write-scanwidth]));
                        write++;
                        read++;
                    }
                    break;
                case AVERAGE:
                    if(i == 0) {
                        for (int j = 0; j < scanwidth; j++) {
                            // recon(x) = raw(x) + floor( (recon(x-bpp) + recon(x-scanline)) / 2 )
                            int recona = (j<bpp ? zero : data[write-bpp] & 0xff);
                            data[write] = (byte)(data[read] + recona / 2);
                            write++;
                            read++;
                        }
                    } else {
                        for (int j = 0; j < bpp; j++) {
                            // recon(x) = raw(x) + floor( (recon(x-bpp) + recon(x-scanline)) / 2 )
                            int reconb = data[write-scanwidth] & 0xff;
                            int delta = (reconb / 2);
                            data[write] = (byte)(data[read] + delta);
                            write++;
                            read++;
                        }
                        for (int j = bpp; j < scanwidth; j++) {
                            // recon(x) = raw(x) + floor( (recon(x-bpp) + recon(x-scanline)) / 2 )
                            int recona = data[write-bpp] & 0xff;
                            int reconb = data[write-scanwidth] & 0xff;
                            
                            int delta = ((recona + reconb) / 2);
                            
                            data[write] = (byte)(data[read] + delta);
                            write++;
                            read++;
                        }
                    }
                    
                    break;
                case PAETH:
                    if(i == 0) {
                        for (int j = 0; j < scanwidth; j++) {
                            // recon(x) = raw(x) + paethPredictor(recon(x-bpp), recon(x-scanline), recon(x-scanline-bpp));
                            int recona = (j<bpp ? 0 : data[write-bpp] & 0xff);
                            data[write] = (byte)((data[read]) + paethPredictor(recona, 0,0));
                            write++;
                            read++;
                        }
                    } else {
                        for (int j = 0; j < bpp; j++) {
                            // recon(x) = raw(x) + paethPredictor(recon(x-bpp), recon(x-scanline), recon(x-scanline-bpp));
                            int reconb = data[write-scanwidth] & 0xff;
                            data[write] = (byte)(data[read] + reconb);
                            write++;
                            read++;
                        }
                        for (int j = bpp; j < scanwidth; j++) {
                            // recon(x) = raw(x) + paethPredictor(recon(x-bpp), recon(x-scanline), recon(x-scanline-bpp));
                            int recona = data[write-bpp] & 0xff;
                            int reconb = data[write-scanwidth] & 0xff;
                            int reconc = data[write-scanwidth-bpp] & 0xff;
                            
                            int out = reconc;
                            int p = recona + reconb - reconc;
                            int pa = Math.abs(p - recona);
                            int pb = Math.abs(p - reconb);
                            int pc = Math.abs(p - reconc);
                            if(pa <= pb && pa <= pc) out = recona;
                            else if(pb <= pc) out = reconb;
                            
                            data[write] = (byte)(data[read] + out);
//                            data[write] = (byte)((data[read] ) + paethPredictor(recona, reconb, reconc));
                            write++;
                            read++;
                        }
                    }
                    break;
            }
        }
        
        input.limit(write);
        return input;
    }
    
    private static int paethPredictor(int a, int b, int c) {
        int p = a + b - c;
        int pa = (p - a); if(pa < 0) pa = -pa;
        int pb = (p - b); if(pb < 0) pb = -pb;
        int pc = (p - c); if(pc < 0) pc = -pc;
        if(pa <= pb && pa <= pc) return a;
        else if(pb <= pc) return b;
        else return c;
    }
    
    private static int paethPredictor2(int a, int b, int c) {
        a *= a;
        b *= b;
        c *= c;
        int p = a + b - c;
        int pa = (p - a);
        int pb = (p - b);
        int pc = (p - c);
        if(pa <= pb && pa <= pc) return a;
        else if(pb <= pc) return b;
        else return c;
    }

    private static ByteBuffer inflateData(ArrayList<Chunk> dataChunks, PNGHeader header) throws Exception {
        // predict uncompressed size
        int size = (header.width * header.height * (header.type == ColorType.TRUECOLOR?3:4) * (header.bitdepth / 8)) + header.height+1;
        
        // data goes here
        byte[] data = new byte[size];
        int offset = 0;
        int chunkOffset = 0;
        
        Inflater inflater = new Inflater();
        
        inflater.setInput(dataChunks.get(chunkOffset).data.array());
        
        boolean keepReading = true;
        while(keepReading) {
            if(size-offset == 0) {
                Common.LogDebug("[PNGDecoder] Info: Underestimated the destination buffer size.");
                // 
                size += 1024;
                byte[] newdata = new byte[size];
                System.arraycopy(data, 0, newdata, 0, offset);
                data = newdata;
            }
            // Try to decompress
            int wrote = inflater.inflate(data, offset, size-offset);
            offset += wrote;

            if(wrote == 0) {
                if(inflater.finished()) {
                    keepReading = false;
                } else if(inflater.needsInput()) {
                    if(chunkOffset+1 < dataChunks.size()) {
                        chunkOffset++;
                        inflater.setInput(dataChunks.get(chunkOffset).data.array());
                    } else {
                        throw new Exception("IDAT Chunk underflow");
                    }
                } else if(inflater.needsDictionary()) {
                    throw new Exception("IDAT Inflater needs dictionary");
                } else {
                    throw new Exception("Unknown error");
                }
            }
        }
        
        inflater.end();
        ByteBuffer b = ByteBuffer.wrap(data, 0, offset);
        return b;
    }
    
    public enum FilterType {
        NONE(0),
        SUB(1),
        UP(2),
        AVERAGE(3),
        PAETH(4);
        
        int type = -1;
        FilterType(int type) {
            this.type = type;
        }
        
        static FilterType find(int value) throws DataFormatException {
            for (FilterType filterType : FilterType.values()) {
                if(filterType.type == value) return filterType;
            }
            throw new DataFormatException("Unsupported filtertype " + value);
        }
    }
    
    public enum ColorType {
        GRAYSCALE(0),
        TRUECOLOR(2),
        INDEXED(3),
        GRAYSCALE_ALPHA(4),
        TRUECOLOR_ALPHA(6);
        
        int type = -1;
        ColorType(int type) {
            this.type = type;
        }
        
        public static ColorType find(int type) {
            for (ColorType colorType : ColorType.values()) {
                if(colorType.type == type) return colorType;
            }
            return null;
        }
    }
    
    private static class PNGHeader {
        static int SIZE = 4 + 4 + 5;
        int width, height;
        int bitdepth;
        ColorType type;
        int colortype;
        int compressiontype;
        int filtermethod;
        int interlacemethod;
        PNGHeader(ByteBuffer b) {
            width = b.getInt();
            height = b.getInt();
            bitdepth = b.get() & 0xff;
            colortype = b.get() & 0xff;
            type = ColorType.find(colortype);
            compressiontype = b.get() & 0xff;
            filtermethod = b.get() & 0xff;
            interlacemethod = b.get() & 0xff;
        }
    }
    
    private enum ChunkType {
        IHDR,
        IDAT,
        IEND,
        PLTE,
        sRGB;
        
        byte[] code;
        private ChunkType() {
            String str = toString();
            code = str.getBytes(Charset.forName("US-ASCII"));
        }
        public static ChunkType find(byte[] type) {
            for (ChunkType chunkType : ChunkType.values()) {
                int i = 0;
                for (; i < 4; i++) {
                    if(type[i] != chunkType.code[i]) break;
                }
                if(i == 4) return chunkType;
            }
            return null;
        }
    }
    
    private static class Chunk {
        int length;
        int crc;
        byte[] type = new byte[4];
        ByteBuffer data;
        
        Chunk(ByteBuffer b) {
            length = b.getInt();
            b.get(type);
            byte[] dat = new byte[length];
            b.get(dat);
            data = ByteBuffer.wrap(dat);
            data.order(ByteOrder.BIG_ENDIAN);
            crc = b.getInt();
        }
        
        String asString() {
            try {
                String str = new String(type, "US-ASCII");
                return str;
            } catch (UnsupportedEncodingException ex) {
            }
            return null;
        }
    }
    
    public static class PNGFile {
        ByteBuffer data;
        int width, height;
        boolean hasAlpha = false;
        boolean sRGB = false;
        
        // load stats
        int[] filterstats = new int[5];
        
    }
}
