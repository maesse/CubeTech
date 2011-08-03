/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.collision;

import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CubeChunkTest {

    public CubeChunkTest() {
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
     * Test of destroy method, of class CubeChunk.
     */
    @Test
    public void testSize() {
        System.out.println("Getting CubeChunk size");
        
        MemoryTestBench m = new MemoryTestBench();
        m.showMemoryUsage(new ObjectFactory() {
            public Object makeObject() {
                return new CubeChunk(null, 0, 0, 0);
            }
        });
    }
    
    public interface ObjectFactory {
        public Object makeObject();
    }

    public class MemoryTestBench {
      public long calculateMemoryUsage(ObjectFactory factory) {
        Object handle = factory.makeObject();
        long mem0 = Runtime.getRuntime().totalMemory() -
          Runtime.getRuntime().freeMemory();
        long mem1 = Runtime.getRuntime().totalMemory() -
          Runtime.getRuntime().freeMemory();
        handle = null;
        System.gc(); System.gc(); System.gc(); System.gc();
        System.gc(); System.gc(); System.gc(); System.gc();
        System.gc(); System.gc(); System.gc(); System.gc();
        System.gc(); System.gc(); System.gc(); System.gc();
        mem0 = Runtime.getRuntime().totalMemory() -
          Runtime.getRuntime().freeMemory();
        handle = factory.makeObject();
        System.gc(); System.gc(); System.gc(); System.gc();
        System.gc(); System.gc(); System.gc(); System.gc();
        System.gc(); System.gc(); System.gc(); System.gc();
        System.gc(); System.gc(); System.gc(); System.gc();
        mem1 = Runtime.getRuntime().totalMemory() -
          Runtime.getRuntime().freeMemory();
        return mem1 - mem0;
      }
      public void showMemoryUsage(ObjectFactory factory) {
        long mem = calculateMemoryUsage(factory);
        System.out.println(
          factory.getClass().getName() + " produced " +
          factory.makeObject().getClass().getName() +
          " which took " + mem + " bytes");
      }
    }

}