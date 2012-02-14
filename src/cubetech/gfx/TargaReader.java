package cubetech.gfx;
// http://paulbourke.net/dataformats/tga/
// little endian multi-byte integers: "low-order byte,high-order byte"

import cubetech.misc.FileWatcher;
import cubetech.misc.Ref;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class TargaReader {

    public static TargaFile getImage(String fileName, boolean needDirect) throws IOException {
        ByteBuffer data = ResourceManager.OpenFileAsByteBuffer(fileName, true).getKey();
        TargaFile f = decode(data, needDirect);
        FileWatcher watcher = new FileWatcher(ResourceManager.OpenFileAsFile(fileName), fileName, Ref.ResMan.onFileModified);
        return f;
    }

    private static TargaFile decode(ByteBuffer buf, boolean needDirect) throws IOException {
        // Read header
        Header h = new Header(buf);

        // Allocate output buffer
        int nPixels = h.width * h.height;
        ByteBuffer pixelData = needDirect ? ByteBuffer.allocateDirect(nPixels * 4) : ByteBuffer.allocate(nPixels * 4);

        // Read the image data
        boolean readAlpha = h.bpp == 32;
        if (h.imageType == Header.ImageType.TRUECOLOR) {
            // Easy peasy - BGR(A)
            for (int i = 0; i < nPixels; i++) {
                byte b = buf.get();
                byte g = buf.get();
                byte r = buf.get();
                byte a = readAlpha ? buf.get() : 127;
                pixelData.put(r);
                pixelData.put(g);
                pixelData.put(b);
                pixelData.put(a);
            }
        } else if (h.imageType == Header.ImageType.RLE_TRUECOLOR) {
            // Run length encoded
            while (pixelData.position() < pixelData.capacity()) {
                int packet = buf.get() & 0xff;
                boolean isRLE = (packet & (1 << 7)) != 0;
                int count = (packet & 127) + 1;

                byte r = 0, g = 0, b = 0, a = 0;
                for (int i = 0; i < count; i++) {
                    if (!isRLE || i == 0) {
                        b = buf.get();
                        g = buf.get();
                        r = buf.get();
                        a = readAlpha ? buf.get() : -1;
                    }
                    pixelData.put(r);
                    pixelData.put(g);
                    pixelData.put(b);
                    pixelData.put(a);
                }
            }
        } else {
            throw new RuntimeException("TGA loading failed. Unsupported image compression: " + h.imageType.name());
        }

        // Create result
        TargaFile file = new TargaFile();
        file.data = (ByteBuffer) pixelData.flip();
        file.width = h.width;
        file.height = h.height;
        file.hasAlpha = h.bpp == 32;
        return file;
    }

    public static final class TargaFile {
        public ByteBuffer data;
        public int width;
        public int height;
        public boolean hasAlpha;
    }

    private static final class Header {
        ImageType imageType;
        int width;
        int height;
        int bpp;

        Header(ByteBuffer b) {
            b.order(ByteOrder.LITTLE_ENDIAN);
            imageType = ImageType.load(b.get(2) & 0xff);
            width = b.getShort(12) & 0xffff;
            height = b.getShort(14) & 0xffff;
            bpp = b.get(16) & 0xff;
            if (imageType == null) {
                throw new RuntimeException("Invalid TGA file! Unsupported image format: " + (b.get(2) & 0xff));
            }
            b.position(18);
        }

        private enum ImageType {
            NO_DATA(0),
            COLOR_MAPPED(1),
            TRUECOLOR(2),
            BW(3),
            RLE_COLOR_MAPPED(9),
            RLE_TRUECOLOR(10),
            RLE_BW(11);
            private final int value;

            private ImageType(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }

            public static ImageType load(int i) {
                for (ImageType imageType : ImageType.values()) {
                    if (imageType.value == i) {
                        return imageType;
                    }
                }
                return null;
            }
        }
    }
}
