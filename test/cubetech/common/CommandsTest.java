/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import cubetech.common.Commands.ExecType;
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
public class CommandsTest {

    public CommandsTest() {
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
     * Test of ExecuteText method, of class Commands.
     */
//    @Test
//    public void testExecuteText() {
//        System.out.println("ExecuteText");
//        ExecType when = null;
//        String text = "";
//        Commands instance = new Commands();
//        instance.ExecuteText(when, text);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of AddCommand method, of class Commands.
     */
//    @Test
//    public void testAddCommand() {
//        System.out.println("AddCommand");
//        String name = "";
//        ICommand cmd = null;
//        Commands instance = new Commands();
//        instance.AddCommand(name, cmd);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of RemoveCommand method, of class Commands.
     */
//    @Test
//    public void testRemoveCommand() {
//        System.out.println("RemoveCommand");
//        String cmd = "";
//        Commands instance = new Commands();
//        instance.RemoveCommand(cmd);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of Args method, of class Commands.
     */
    @Test
    public void testArgs() {
        System.out.println("Args");
        String[] tokens = new String[] {"1","2","3","4"};
        String expResult = "2 3 4";
        String result = Commands.Args(tokens);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of ArgsFrom method, of class Commands.
     */
    @Test
    public void testArgsFrom() {
        System.out.println("ArgsFrom");
        String[] tokens = new String[] {"1","2","3","4"};
        int arg = 0;
        String expResult = "1 2 3 4";
        String result = Commands.ArgsFrom(tokens, arg);
        assertEquals(expResult, result);

        arg = 1;
        expResult = "2 3 4";
        result = Commands.ArgsFrom(tokens, arg);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of TokenizeString method, of class Commands.
     */
    @Test
    public void testTokenizeString() {
        System.out.println("TokenizeString");
        String str = "Yo people, what is \"Up in the Town?\" Fo Reals";
        boolean ignoreQuotes = false;
        String[] expResult = new String[] {"Yo", "people,", "what", "is", "Up in the Town?", "Fo", "Reals"};
        String[] result = Commands.TokenizeString(str, ignoreQuotes);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

}