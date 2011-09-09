/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import cubetech.misc.Ref;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class HelperTest {

    public HelperTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        Ref.rnd = new Random(System.currentTimeMillis());
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of VectorCopy method, of class Helper.
     */
    @Test
    public void testVectorCopy() {
        System.out.println("VectorCopy");

        Vector2f org = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
        Vector2f src = new Vector2f(org);

        Vector2f dest = new Vector2f();
        Helper.VectorCopy(src, dest);

        assert(Helper.Equals(org, src));
        assert(Helper.Equals(org, dest));
        src.x = Ref.rnd.nextFloat();
        src.y = Ref.rnd.nextFloat();
        assert(Helper.Equals(org, dest));
        assert(!Helper.Equals(org, src));
    }

    /**
     * Test of ColorIndex method, of class Helper.
     */
    @Test
    public void testColorIndex() {
        System.out.println("ColorIndex");
        String str = "asd^3asd";
        int expResult = 3;
        int result = Helper.ColorIndex(str);
        assertEquals(expResult, result);

        str = "asd^^3asd";
        expResult = 4;
        result = Helper.ColorIndex(str);
        assertEquals(expResult, result);
    }

    @Test
    public void testFloatToInt() {
        System.out.println("FloatToInt");
        Vector3f v = new Vector3f((float) Ref.rnd.nextGaussian(), (float) Ref.rnd.nextGaussian(), (float) Ref.rnd.nextGaussian());
        v.normalise();
        int i = Helper.normalToInt(v);
        Vector3f v2 = Helper.intToNormal(i);
        assertTrue(Helper.Equals(v, v2,0.01f));
    }

    /**
     * Test of VectorGet method, of class Helper.
     */
    @Test
    public void testVectorGet() {
        System.out.println("VectorGet");
        Vector2f src = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());

        float result = Helper.VectorGet(src, 0);
        assertEquals(src.x, result, 0.0);

        result = Helper.VectorGet(src, 1);
        assertEquals(src.y, result, 0.0);
    }

    /**
     * Test of VectorSet method, of class Helper.
     */
    @Test
    public void testVectorSet() {
        System.out.println("VectorSet");
        Vector2f src = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
        Vector2f dst = new Vector2f();

        Helper.VectorSet(dst, 0, src.x);
        assertEquals(src.x, dst.x, 0.0);

        Helper.VectorSet(dst, 1, src.y);
        assertEquals(src.y, dst.y, 0.0);
    }

    @Test
    public void testAngle2Short() {
        for (int i= 0; i < 360; i++) {
            int val =Helper.Angle2Short(i);
            float ang = Helper.Short2Angle(val);
            assertEquals((float)i, ang, 0.0055f);
        }
        
    }

    /**
     * Test of VectorMA method, of class Helper.
     */
//    @Test
//    public void testVectorMA() {
//        System.out.println("VectorMA");
//
//        Vector2f a = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//        float scale = Ref.rnd.nextFloat();
//        Vector2f b = new Vector2f(Ref.rnd.nextFloat(), Ref.rnd.nextFloat());
//        Vector2f dest = new Vector2f();
//        Helper.VectorMA(a, scale, b, dest);
//
//
//    }

}