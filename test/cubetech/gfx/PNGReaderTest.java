/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cubetech.gfx;

import cubetech.common.CVars;
import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.gfx.PNGReader.PNGFile;
import cubetech.misc.BMPWriter;
import cubetech.misc.BMPWriter.Format;
import cubetech.misc.Ref;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Mads
 */
public class PNGReaderTest {
    
    public PNGReaderTest() {
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
    
    private void gc() {
        System.gc();System.gc();System.gc();System.gc();System.gc();
    }

    /**
     * Test of readFile method, of class PNGReader.
     */
    @Test
    public void testReadFile() throws Exception {
        System.out.println("readFile");
        Ref.commands = new Commands();
        Ref.common = new Common();
        
        Ref.cvars = new CVars();
        Ref.cvars.Set2("developer", "1", true);
        Ref.glRef = new GLRef();
        Ref.rnd = new Random();
        Ref.ResMan = new ResourceManager();
        Ref.common.Init();
        long mem0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        ByteBuffer input = ResourceManager.OpenFileAsByteBuffer("data/models/interior_normal.png", false).getKey();
        gc();
        long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Loading file took " + (mem1-mem0)/(1024*1024) + "mb");
        PNGFile result = null;
        for (int i = 0; i < 20; i++) {
            result = PNGReader.readFile(input);
            gc();
            long mem2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.out.println("Loading PNG took " + (mem2-mem1)/(1024*1024) + "mb");
        }

        BMPWriter writer = new BMPWriter(result.data, result.width, result.height, Format.RGBA);
        byte[] bmpdata = writer.encodeImage();
        File f = new File("derp.bmp");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(bmpdata);
        fos.flush();
        fos.close();
    }
}
