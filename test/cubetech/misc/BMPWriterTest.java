/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.jar.JarEntry;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mads
 */
public class BMPWriterTest {

    public BMPWriterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of encodeImage method, of class BMPWriter.
     */
    @Test
    public void testEncodeImage() {
        System.out.println("encodeImage");
        System.out.println("pngEncode (1280x800)");
        ByteBuffer buf = ByteBuffer.allocateDirect(800*1280*3);
        byte[] deflateOut = new byte[800*1280*3];
        BMPWriter instance = new BMPWriter(buf, 1280, 800);
        long start = System.nanoTime();
        int count = 30;
        byte[] result = null;
        for (int i= 0; i < count; i++) {
            result = instance.encodeImage();
        }
        long end = System.nanoTime();
        float delta = (end-start) / 1000000f;
        System.out.println(count + "encodes took " + delta + "ms");
        System.out.println(delta/count + "ms pr encode");

                
        try {

            FastZipOutputStream str = new FastZipOutputStream(new FileOutputStream("derping.zip"));
            str.setLevel(2);
            start = System.nanoTime();
            for (int i= 0; i < count; i++) {
                str.putNextEntry(new ZipEntry("e"+i));
                str.write(result);
                str.closeEntry();
            }
            
            str.close();
        } catch (IOException ex) {
            
        }

        // Deflate
//        Deflater deflate = new Deflater(3);
//        deflate.setInput(result);
//        deflate.finish();
//        int compressedSize = deflate.deflate(deflateOut);
//        System.out.println("Uncompressed: " + result.length/1024 + "kb, Compressed: "
//                + compressedSize/1024 + "kb. Ratio: " + (int)(((float)compressedSize/result.length)*100f) + "%");
//

//        for (int i= 0; i < count; i++) {
//            deflate.reset();
//            deflate.setInput(result);
//            deflate.finish();
//            deflate.deflate(deflateOut);
//        }
        end = System.nanoTime();
        delta = (end-start) / 1000000f;
        System.out.println(count + "compresses took " + delta + "ms");
        System.out.println(delta/count + "ms pr file compression");
    }

}