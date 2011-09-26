/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

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
public class NativeNoiseTest {

    public NativeNoiseTest() {
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
     * Test of test method, of class NativeNoise.
     */
    @Test
    public void testTest() {
        System.out.println("test");
        NativeNoise instance = new NativeNoise();
        long start = System.nanoTime();
        for (int i= 0; i < 100; i++) {
             instance.test();
        }
        long end = System.nanoTime();
        float delta = (end-start) / 1000000f;
        System.out.println(100 + " noise took " + delta + "ms");
        System.out.println(delta/100 + "ms pr noise");

        start = System.nanoTime();
        for (int i= 0; i < 100; i++) {
             for (int j= 0; j < 5*5*5; j++) {
                double d = SimplexNoise.noise(1f,1f,1f);
            }
        }
        end = System.nanoTime();
        delta = (end-start) / 1000000f;
        System.out.println(100 + " noise took " + delta + "ms");
        System.out.println(delta/100 + "ms pr noise");
    }

}