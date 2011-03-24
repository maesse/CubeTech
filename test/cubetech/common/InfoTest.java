/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

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
public class InfoTest {

    public InfoTest() {
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
     * Test of ValueForKey method, of class Info.
     */
    @Test
    public void testValueForKey() {
        System.out.println("ValueForKey");
        String s = "\\hej\\sup there in the city\\hmm\\123123\\wwutut\\1233";
        String key = "hej";
        String expResult = "sup there in the city";
        String result = Info.ValueForKey(s, key);
        assertEquals(expResult, result);
        key = "hmm";
        expResult = "123123";
        result = Info.ValueForKey(s, key);
        assertEquals(expResult, result);
        key = "wwutut";
        expResult = "1233";
        result = Info.ValueForKey(s, key);
        assertEquals(expResult, result);

        key = "dude";
        result = Info.ValueForKey(s, key);
        assertEquals("", result);
    }

    /**
     * Test of SetValueForKey method, of class Info.
     */
    @Test
    public void testSetValueForKey() {
        System.out.println("SetValueForKey");
        String s = "\\yo\\hej\\one\\two";
        String key = "yo";
        String value = "dogg";
        String result = Info.SetValueForKey(s, key, value);
        assertTrue(value.equals(Info.ValueForKey(result, key)));
        //assertEquals(expResult, result);
    }

    /**
     * Test of RemoveKey method, of class Info.
     */
    @Test
    public void testRemoveKey() {
        System.out.println("RemoveKey");
        String s = "\\yo\\hej\\123\\do\\ah\\do";
        String key = "yo";
        String expResult = "\\123\\do\\ah\\do";
        String result = Info.RemoveKey(s, key);
        assertEquals(expResult, result);

        s = "\\yo\\hej\\123\\do\\ah\\do";
        key = "123";
        expResult = "\\yo\\hej\\ah\\do";
        result = Info.RemoveKey(s, key);
        assertEquals(expResult, result);

        s = "\\yo\\hej\\123\\do\\ah\\do";
        key = "ah";
        expResult = "\\yo\\hej\\123\\do";
        result = Info.RemoveKey(s, key);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
    }

}