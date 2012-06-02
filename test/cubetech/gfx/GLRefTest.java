package cubetech.gfx;

import cubetech.misc.Ref;
import cubetech.common.CVars;
import cubetech.common.Commands;

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
public class GLRefTest {
    

    public GLRefTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Ref.commands = new Commands();
        Ref.cvars = new CVars();
        Ref.rnd = new Random();
        Ref.glRef = new GLRef();
        Ref.ResMan = new ResourceManager();

        try {
            Ref.glRef.InitWindow(null, null, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GLRefTest.class.getName()).log(Level.SEVERE, null, ex);
            fail(ex.getMessage());
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        Ref.glRef.Destroy();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testVBO() {
        System.out.println("VBO test");
        
    }

}