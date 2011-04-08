/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class Helper {
    public static void VectorCopy(Vector2f src, Vector2f dst) {
        dst.x = src.x;
        dst.y = src.y;
    }

    /**
     * Finds the first occurence of color sequence (^0-9)
     * @param str
     * @return -1 if nothing was found
     */
    public static int ColorIndex(String str) {
        int delimOffset = -1;
        while((delimOffset = str.indexOf('^', delimOffset+1)) != -1) {
            char nextChar = str.charAt(delimOffset+1);
            if(nextChar >= '0' && nextChar <= '9')
                return delimOffset;
        }

        return -1;
    }

    public static float Normalize(Vector2f v) {
        double len = Math.sqrt(v.x * v.x + v.y * v.y);
        if(len == 0)
            return 0;

        v.x /= len;
        v.y /= len;
        return (float)len;
    }

    public static Vector2f CreateVector(float angle, float lenght, Vector2f dest) {
        if(dest == null)
            dest = new Vector2f();

        dest.x = (float)Math.cos(angle);
        dest.y = (float)Math.sin(angle);
        dest.scale(lenght);
        return dest;
    }

    public static void AddPointToBounds(Vector2f v, Vector2f mins, Vector2f maxs) {
        if(v.x < mins.x)
            mins.x = v.x;
        if(v.x > maxs.x)
            maxs.x = v.x;
        if(v.y < mins.y)
            mins.y = v.y;
        if(v.y > maxs.y)
            maxs.y = v.y;
    }

    public static boolean Equals(Vector2f a, Vector2f b) {
//        return false;
        return (a.x == b.x && a.y == b.y);
    }

    // Helper method
    public static float VectorGet(Vector2f src, int index) {
        if(index == 1)
            return src.y;
        return src.x;
    }

    // Helper method
    public static void VectorSet(Vector2f src, int index, float value) {
        if(index == 1)
            src.y = value;
        else
            src.x = value;
    }

    // dest = a + scale * b
    public static void VectorMA(Vector2f a, float scale, Vector2f b, Vector2f dest) {
        dest.x = a.x + b.x * scale;
        dest.y = a.y + b.y * scale;
    }

    public static float RadiusFromBounds(Vector2f mins, Vector2f maxs) {
        Vector2f corner = new Vector2f();
        float a = Math.abs(mins.x);
        float b = Math.abs(maxs.x);
        corner.x = a > b ? a : b;
        a = Math.abs(mins.y);
        b = Math.abs(maxs.y);
        corner.y = a > b ? a : b;

        return corner.length();
    }
}
