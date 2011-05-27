/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.iqm;

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
public class IQMLoaderTest {

    public IQMLoaderTest() {
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
     * Test of LoadModel method, of class IQMLoader.
     */
    @Test
    public void testLoadModel() throws Exception {
        System.out.println("LoadModel");
        String file = "C:\\Users\\mads\\Desktop\\iqm\\mrfixit.iqm";
        IQMModel model = IQMLoader.LoadModel(file);
        assert(model != null);
    }

}