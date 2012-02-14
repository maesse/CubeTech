/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cubetech.misc;

import java.util.ArrayList;
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
public class PoissonGeneratorTest {
    
    public PoissonGeneratorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        Ref.rnd = new Random();
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of generateUnitSphere method, of class PoissonGenerator.
     */
    @Test
    public void testGenerateUnitSphere() {
        
        System.out.println("generateUnitSphere");
        float minimumDistance = 0.3f;
        int maxiter = 100;
        ArrayList<Vector3f> result = PoissonGenerator.generateUnitSphere(null, minimumDistance, maxiter);
        System.out.println("Generated " + result.size() + " points.");
        System.out.println("---------------");
        for (Vector3f v : result) {
            System.out.println(v.toString());
        }
        System.out.println("---------------");
    }

    /**
     * Test of generateHemiSphere method, of class PoissonGenerator.
     */
    @Test
    public void testGenerateHemiSphere() {
        System.out.println("generateHemiSphere");
        float minimumDistance = 0.3f;
        int maxiter = 100;
        ArrayList<Vector3f> result = PoissonGenerator.generateUnitSphere(new Vector3f(0, 0, 1), minimumDistance, maxiter);
        System.out.println("Generated " + result.size() + " points.");
        System.out.println("---------------");
        for (Vector3f v : result) {
            System.out.println(v.toString());
        }
        System.out.println("---------------");
    }
    
    /**
     * Test of generateHemiSphere method, of class PoissonGenerator.
     */
    @Test
    public void testGenerateHemiSphere2() {
        System.out.println("generateFixedUnitspehere 32");
        ArrayList<Vector3f> result = PoissonGenerator.generateUnitSphere(new Vector3f(0, 0, 1), 32, 32);
        System.out.println("Generated " + result.size() + " points.");
        System.out.println("---------------");
        for (Vector3f v : result) {
            System.out.println(v.toString());
        }
        System.out.println("---------------");
    }

}
