/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import java.nio.ByteBuffer;
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
public class PngEncoderTest {

    public PngEncoderTest() {
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
     * Test of pngEncode method, of class PngEncoder.
     */
    @Test
    public void testPngEncode() {
        System.out.println("pngEncode (1280x800)");
        ByteBuffer buf = ByteBuffer.allocateDirect(800*1280*3);
        PngEncoder instance = new PngEncoder(buf, false, PngEncoder.FILTER_NONE, 4, 1280, 800);
        long start = System.nanoTime();
        int count = 30;
        for (int i= 0; i < count; i++) {
            byte[] result = instance.pngEncode();
        }
        long end = System.nanoTime();
        float delta = (end-start) / 1000000f;
        System.out.println(count + "encodes took " + delta + "ms");
        System.out.println(delta/count +"ms pr encode");
        
    }


}