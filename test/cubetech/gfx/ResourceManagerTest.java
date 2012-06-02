/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.common.CVars;
import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.misc.Ref;
import java.util.AbstractMap;
import cubetech.net.NetBuffer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class ResourceManagerTest {

    public ResourceManagerTest() {
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
    public void test() {
        Ref.commands = new Commands();
        Ref.common = new Common();
        
        Ref.cvars = new CVars();
        Ref.cvars.Set2("developer", "1", true);
        Ref.glRef = new GLRef();
        Ref.rnd = new Random();
        Ref.ResMan = new ResourceManager();
        Ref.common.Init();
        try {
            Ref.glRef.InitWindow(null, null, false);
            CubeTexture tex = Ref.ResMan.LoadTexture("data/models/interior_normal.png");
            System.out.println(tex.loaded);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(ResourceManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    /**
     * Test of OpenFileAsNetBuffer method, of class ResourceManager.
     */
//    @Test
//    public void testOpenFileAsNetBuffer() throws Exception {
//        System.out.println("OpenFileAsNetBuffer");
//        //String path = "data/HarmonicMan.ogg";
//        String path = "E:/downloads/complete/entourage/Adventure Time 203b - Slow Love/Adventure Time 2x03b - Slow Love (lump) [mentos].avi";
//        //NetBuffer expResult = null;
//        AbstractMap.SimpleEntry<NetBuffer, Integer> result = ResourceManager.OpenFileAsNetBuffer(path, false);
////        result = ResourceManager.OpenFileAsNetBuffer(path);
////        result = ResourceManager.OpenFileAsNetBuffer(path);
////        result = ResourceManager.OpenFileAsNetBuffer(path);
////        result = ResourceManager.OpenFileAsNetBuffer(path);
////        result = ResourceManager.OpenFileAsNetBuffer(path);
////        result = ResourceManager.OpenFileAsNetBuffer(path);
////        result = ResourceManager.OpenFileAsNetBuffer(path);
////        result = ResourceManager.OpenFileAsNetBuffer(path);
////        result = ResourceManager.OpenFileAsNetBuffer(path);
//        System.out.println(result.getKey().GetBuffer().toString());
//        System.out.println("Checksum: " + result.getValue());
//        //assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        //fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of LoadResource method, of class ResourceManager.
//     */
//    @Test
//    public void testLoadResource() {
//        System.out.println("LoadResource");
//        String filename = "";
//        ResourceManager instance = new ResourceManager();
//        Resource expResult = null;
//        Resource result = instance.LoadResource(filename);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getTexture method, of class ResourceManager.
//     */
//    @Test
//    public void testGetTexture() throws Exception {
//        System.out.println("getTexture");
//        String resourceName = "";
//        int target = 0;
//        int dstPixelFormat = 0;
//        int minFilter = 0;
//        int magFilter = 0;
//        ResourceManager instance = new ResourceManager();
//        CubeTexture expResult = null;
//        CubeTexture result = instance.getTexture(resourceName, target, dstPixelFormat, minFilter, magFilter);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of Cleanup method, of class ResourceManager.
//     */
//    @Test
//    public void testCleanup() {
//        System.out.println("Cleanup");
//        ResourceManager instance = new ResourceManager();
//        instance.Cleanup();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

}