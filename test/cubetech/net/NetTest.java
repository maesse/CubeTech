/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import java.util.Random;
import cubetech.common.Commands;
import cubetech.misc.Ref;
import cubetech.common.CVars;
import cubetech.common.Common;
import cubetech.gfx.ResourceManager;
import cubetech.net.Packet.ReceiverType;
import java.net.InetSocketAddress;
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
        // Required by the net subsystem
        Ref.commands = new Commands();
        Ref.cvars = new CVars();
        Ref.rnd = new Random();
        Ref.ResMan = new ResourceManager();
        Ref.common = new Common();
    }

    @After
    public void tearDown() {
    }


    /**
     * Test of GetPacket method, of class Net.
     */
    @Test
    public void testOOBPacket() {
        System.out.println("GetPacket");
        DefaultNet net = new DefaultNet();
        net.SendOutOfBandPacket(ReceiverType.CLIENT, new InetSocketAddress("localhost", DefaultNet.DEFAULT_PORT), "YO DAWG");
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(NetTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        net.PumpNet();
        Packet result = net.GetPacket();
        assertTrue(result.outOfBand);
        assertEquals("YO DAWG", result.buf.ReadString());
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }



}