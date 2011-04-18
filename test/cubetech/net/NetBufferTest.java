/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.net;

import java.util.Random;
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
public class NetBufferTest {

    public NetBufferTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    Random rnd;

    @Before
    public void setUp() {
        rnd = new Random(System.currentTimeMillis());
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of Write method, of class NetBuffer.
     */
    @Test
    public void testWrite_int() {
        int value = rnd.nextInt();
        System.out.println("Writing " + value);
        NetBuffer instance = NetBuffer.GetNetBuffer(false, false);
        instance.Write(value);
        instance.Flip();
        int outval = instance.ReadInt();
        assertEquals(value, outval);
    }

    /**
     * Test of Write method, of class NetBuffer.
     */
    @Test
    public void testWrite_float() {
        float value = rnd.nextFloat()*(rnd.nextFloat()+1);
        System.out.println("Writing " + value);
        NetBuffer instance = NetBuffer.GetNetBuffer(false, false);
        instance.Write(value);
        instance.Flip();
        float outval = instance.ReadFloat();
        assertEquals(value, outval, 0.0f);
    }

    /**
     * Test of Write method, of class NetBuffer.
     */
    @Test
    public void testWrite_String() {
        String str = "";
        int lenght = rnd.nextInt(128);
        for (int i= 0; i < lenght; i++) {
            str += (char)((int)'a' + rnd.nextInt(25));
        }
        str += "æøå";
        System.out.println("Writing " + str);
        NetBuffer instance = NetBuffer.GetNetBuffer(false, false);
        instance.Write(str);
        instance.Flip();
        String outval = instance.ReadString();
        assertEquals(str, outval);
    }

    /**
     * Test of multiple writes and reads
     */
    @Test
    public void testWrite_All() {
        String str = "";
        int lenght = rnd.nextInt(128);
        for (int i= 0; i < lenght; i++) {
            str += (char)((int)'a' + rnd.nextInt(25));
        }
        System.out.println("Writing " + str);
        NetBuffer instance = NetBuffer.GetNetBuffer(false, false);
        instance.Write(str);

        float value = rnd.nextFloat()*(rnd.nextFloat()+1);
        System.out.println("Writing " + value);
        instance.Write(value);

        int value2 = rnd.nextInt();
        System.out.println("Writing " + value2);
        instance.Write(value2);
        
        instance.Flip();
        
        String outval = instance.ReadString();
        assertEquals(str, outval);

        float value1read = instance.ReadFloat();
        assertEquals(value, value1read, 0.0f);

        int value2read = instance.ReadInt();
        assertEquals(value2, value2read);
    }

}