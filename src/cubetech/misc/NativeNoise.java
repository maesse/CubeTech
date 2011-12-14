package cubetech.misc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author mads
 */
public class NativeNoise {
    private static native boolean getNoise(ByteBuffer buffer, float x, float y, float z,
                                 float sx, float sy, float sz,
                                 int xcount, int ycount, int zcount);
    private static boolean initialized = false;
    private static boolean failed = false;
    static ByteBuffer buffer = null;

    private static void init() {
        if(!initialized && !failed) {
            try {
                System.load("C:\\MinGW\\msys\\1.0\\home\\mads\\snoise.dll");
                initialized = true;
            } catch (java.lang.UnsatisfiedLinkError ex) {
                failed = true;
                System.out.println(ex);
            }
        }
    }

    public static void noise(float[] dst, float x, float y, float z, float xscale, float yscale, float zscale, int axissize, float ampl) {
        init();
        if(failed) return;
        if(dst == null || dst.length < axissize*axissize*axissize) {
            throw new IllegalArgumentException("dst array too small");
        }
        if(buffer == null || buffer.capacity() < axissize * axissize * axissize * 4) {
            buffer = ByteBuffer.allocateDirect(axissize * axissize * axissize * 4);
            buffer.order(ByteOrder.nativeOrder());
        }
        getNoise(buffer, x, y, z, xscale, yscale, zscale, axissize, axissize, axissize);
        buffer.position(0);
        for (int i= 0; i < axissize*axissize*axissize; i++) {
            dst[i] = buffer.getFloat() * ampl;
        }
    }

    public void test() {
        init();
        int axissize = 32;
        if(buffer == null || buffer.capacity() < axissize * axissize * axissize * 4) {
            buffer = ByteBuffer.allocateDirect(axissize * axissize * axissize * 4);
        }
        getNoise(buffer, 0,0,0,1,1,1,axissize,axissize,axissize);
    }
}
