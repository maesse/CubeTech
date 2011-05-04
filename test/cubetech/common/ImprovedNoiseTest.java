/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

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
public class ImprovedNoiseTest {

    public ImprovedNoiseTest() {
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
     * Test of noise method, of class ImprovedNoise.
     */
    @Test
    public void testNoise() {
        System.out.println("noise");
        double x = 0.0;
        double y = 2.5;
        double z = 3.3;
        
        double result = ImprovedNoise.noise(x, y, z);

        double result2 = ImprovedNoise.noise(x, y, z);
        
        assertEquals(result2, result, 0.0);
    }

   
}