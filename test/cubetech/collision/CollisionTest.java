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
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Mads
 */
public class CollisionTest {
    
    public CollisionTest() {
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
     * Test of TestAABBBox method, of class Collision.
     */
    @Test
    public void testTestAABBBox() {
        Collision instance = new Collision();
        System.out.println("TestAABBBox");
        
        // Body A
        Vector3f center = new Vector3f(0, 0, 0);
        Vector3f v = new Vector3f(-1000, 0, 0);
        Vector3f Extent = new Vector3f(10, 10, 10);
        
        // Body B
        Vector3f min = new Vector3f(-10,-10,-10);
        Vector3f max = new Vector3f(10,10,10);
        Vector3f org = new Vector3f(25,0,0);
        // Set body B
        instance.SetBoxModel(min, max, org);
        
        // Prepare result
        CollisionResult res = instance.GetNext();
        res.reset(center, v, Extent);
        // Do it
        instance.TestAABBBox(center, v, Extent, res);
        
        System.out.println("Hit: " + res.hit);
        System.out.println("TOI fraction: " + res.frac);
        System.out.println("Hitnormal: " + res.hitAxis);
        assertTrue(res.hit);
    }

}

