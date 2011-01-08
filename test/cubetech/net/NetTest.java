/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

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
public class NetTest {

    public NetTest() {
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
     * Test of GetPacket method, of class Net.
     */
    @Test
    public void testGetPacket() {
        System.out.println("GetPacket");
        Net instance = new Net();
        Packet expResult = null;
        Packet result = instance.GetPacket();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of InitServerSocket method, of class Net.
     */
    @Test
    public void testInitServerSocket() throws Exception {
        System.out.println("InitServerSocket");
        Net instance = new Net();
        
        
    }

    /**
     * Test of DestroyServerSocket method, of class Net.
     */
    @Test
    public void testDestroyServerSocket() {
        System.out.println("DestroyServerSocket");
        Net instance = new Net();
        instance.DestroyServerSocket();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}