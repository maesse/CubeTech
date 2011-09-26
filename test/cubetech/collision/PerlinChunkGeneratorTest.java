/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.collision;

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
public class PerlinChunkGeneratorTest {

    public PerlinChunkGeneratorTest() {
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
     * Test of generateChunk method, of class PerlinChunkGenerator.
     */
    @Test
    public void testGenerateChunk() {
        PerlinChunkGenerator instance = new PerlinChunkGenerator();
        CubeMap map = new CubeMap(instance, 0,0,0);
        
        System.out.println("generateChunk timing");
        instance.generateChunk(map, -1,0,0);

        long startTime = System.nanoTime();
        int count = 1000;
        for (int i= 0; i < count; i++) {
            instance.generateChunk(map, i,0,0);
        }
        long endTime = System.nanoTime();
        float deltams = (endTime - startTime) / 1000000f;
        System.out.println(count + " chunks took " + deltams + "ms");
        System.out.println(deltams/count + "ms pr. chunk");

    }

}