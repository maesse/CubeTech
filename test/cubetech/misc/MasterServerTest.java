/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import java.net.InetSocketAddress;
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
public class MasterServerTest {

    public MasterServerTest() {
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
     * Test of sendHeartbeat method, of class MasterServer.
     */
    @Test
    public void testSendHeartbeat() {
        System.out.println("sendHeartbeat");
        int port = 1337;
        MasterServer.sendHeartbeat(port);
    }

    /**
     * Test of getServerList method, of class MasterServer.
     */
    @Test
    public void testGetServerList() {
        System.out.println("getServerList");
        InetSocketAddress[] result = MasterServer.getServerList();
        assert(result.length > 0);
        // TODO review the generated test code and remove the default call to fail.
    }

}