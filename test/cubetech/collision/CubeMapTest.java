/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cubetech.collision;

import cern.colt.map.OpenLongObjectHashMap;
import cubetech.common.Helper;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Mads
 */
public class CubeMapTest {
    
    public CubeMapTest() {
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
    
    
    
    
    @Test
    public void floorTest() {
        
        float f = -10.0f;
        float incr = 0.001f;
        int iter = 10000000;
        long sum = 0;
        long startTime = System.nanoTime();
        for (int i = 0; i < iter; i++) {
            sum += (long)Math.floor(f);
            f += incr;
        }
        
        f = -10f;
        long sum2 = 0;
        long startTime2 = System.nanoTime();
        for (int i = 0; i < iter; i++) {
            
            sum2 += Helper.fastFloor(f);
            f += incr;
        }
        long endTime = System.nanoTime();
        double delta1ms = (endTime - startTime) / 1000000D;
        double delta2ms = (endTime - startTime2) / 1000000D;
        System.out.println("Sum1: " + sum + " took " + delta1ms);
        System.out.println("Sum2: " + sum2 + " took " + delta2ms);
    }

    /**
     * Test of getChunk method, of class CubeMap.
     */
    @Test
    public void testChunkSpeed() {
        System.out.println("getChunk");
        IChunkGenerator gen = new PerlinChunkGenerator();
        CubeMap map = new CubeMap(gen, 16, 16, 18);
        
        Vector3f min = new Vector3f(1,1,-255);
        Vector3f max = new Vector3f(255,255,1);
        min.scale(32f);
        max.scale(32f);
        for (int i = 0; i < 1; i++) {
            ChunkAreaQuery cubes = CubeMap.getCubesInVolume(min, max, map.chunks, false);
            //int nCubes = cubes.getCount();
        }
        
        long startTime = System.nanoTime();
        for (int i = 0; i < 1; i++) {
            ChunkAreaQuery cubes = CubeMap.getCubesInVolume(min, max, map.chunks, false);
            //int nCubes = cubes.getCount();
        }
        long endTime = System.nanoTime();
        double deltams = (endTime - startTime) / 1000000D;
        System.out.println("Took " + deltams / 1D + "ms");
        double praccess = (deltams / 1D) / (255*255*255);
        System.out.println("Pr access: " + praccess * 1000D);
    }
    
    /**
     * Test of getChunk method, of class CubeMap.
     */
    @Test
    public void chunkRandomAccess() {
        System.out.println("chunkRandomAccess");
        IChunkGenerator gen = new PerlinChunkGenerator();
        CubeMap map = new CubeMap(gen, 16, 16, 18);
        
        
        
        Vector3f min = new Vector3f(1,1,-255);
        Vector3f max = new Vector3f(255,255,1);
        min.scale(32f);
        max.scale(32f);
        
        
        int count = 255*255*255;
        int[] rnd = new int[count];
        Random rndGen = new Random();
        for (int i = 0; i < count; i++) {
            rnd[i] = rndGen.nextInt((int)max.x);
        }
        int sum = 0;
        long startTime = System.nanoTime();
        for (int i = 0; i < count-2; i++) {
            sum += map.getCube(rnd[i], rnd[i+1], -rnd[i+2]);
        }
        System.out.println("sum: " + sum);
        long endTime = System.nanoTime();
        double deltams = (endTime - startTime) / 1000000D;
        System.out.println("Took " + deltams + "ms");
        double praccess = (deltams) / (255*255*255);
        System.out.println("Pr access: " + praccess * 1000D);
    }
}
