/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.iqm;

import java.util.Collection;
import cubetech.gfx.ResourceManager;
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
public class ShapeKeyLoaderTest {

    public ShapeKeyLoaderTest() {
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
     * Test of loadSkapeKey method, of class ShapeKeyLoader.
     */
    @Test
    public void testLoadSkapeKey() throws Exception {
        System.out.println("loadSkapeKey");
        String file = "C:\\Users\\mads\\Documents\\Old\\Blender filer\\army\\untitled.shapekeys";
        ShapeKeyCollection shapeKeyObjects = ShapeKeyLoader.loadSkapeKey(file, true);

        String file2 = "C:\\Users\\mads\\Desktop\\untitled.iqm";
        IQMModel model = IQMLoader.LoadModel(file2);

        shapeKeyObjects.attachToModel(model);
    }

}