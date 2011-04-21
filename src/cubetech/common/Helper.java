/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import cubetech.input.Input;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class Helper {
    public static void VectorCopy(Vector2f src, Vector2f dst) {
        dst.x = src.x;
        dst.y = src.y;
    }

    public static void VectorCopy(Vector3f src, Vector3f dst) {
        dst.x = src.x;
        dst.y = src.y;
        dst.z = src.z;
    }

    public static Vector3f[] AnglesToAxis(Vector3f angles) {
        Vector3f[] axis = new Vector3f[3];
        for (int i= 0; i < axis.length; i++) {
            axis[i] = new Vector3f();
        }
        // angle vectors returns "right" instead of "y axis"
        Vector3f right = new Vector3f();
        AngleVectors(angles, axis[0], right, axis[2]);
        Vector3f.sub(new Vector3f(), right, axis[1]);
        return axis;
    }

    public static void AngleVectors(Vector3f angles, Vector3f forward, Vector3f right, Vector3f up) {
        double angle = VectorGet(angles, Input.ANGLE_YAW) * (Math.PI*2f / 360);
        double sy = Math.sin(angle);
        double cy = Math.cos(angle);

        angle = VectorGet(angles, Input.ANGLE_PITCH) * (Math.PI*2f / 360);
        double sp = Math.sin(angle);
        double cp = Math.cos(angle);

        angle = VectorGet(angles, Input.ANGLE_ROLL) * (Math.PI*2f / 360);
        double sr = Math.sin(angle);
        double cr = Math.cos(angle);

        if(forward != null) {
            forward.x = (float) (cp * cy);
            forward.y = (float) (cp * sy);
            forward.z = (float) -sp;
        }
        if(right != null) {
            right.x = (float) (-1*sr*sp*cy+-1*cr*-sy);
            right.y = (float) (-1*sr*sp*sy+-1*cr*cy);
            right.z = (float) (-1*sr*cp);
        }
        if(up != null) {
            up.x = (float) (cr*sp*cy+-sr*-sy);
            up.y = (float) (cr*sp*sy+-sr*cy);
            up.z = (float) (cr*cp);
        }

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

    public static float Normalize(Vector3f v) {
        double len = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        if(len == 0)
            return 0;

        v.x /= len;
        v.y /= len;
        v.z /= len;
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

    public static void AddPointToBounds(Vector3f v, Vector3f mins, Vector3f maxs) {
        if(v.x < mins.x)
            mins.x = v.x;
        if(v.x > maxs.x)
            maxs.x = v.x;
        if(v.y < mins.y)
            mins.y = v.y;
        if(v.y > maxs.y)
            maxs.y = v.y;
        if(v.z < mins.z)
            mins.z = v.z;
        if(v.z > maxs.z)
            maxs.z = v.z;
    }

    public static boolean Equals(Vector2f a, Vector2f b) {
//        return false;
        return (a.x == b.x && a.y == b.y);
    }

    public static boolean Equals(Vector3f a, Vector3f b) {
        return a.x == b.x && a.y == b.y && a.z == b.z;
    }

    // Helper method
    public static float VectorGet(Vector2f src, int index) {
        if(index == 1)
            return src.y;
        return src.x;
    }

    // Helper method
    public static void VectorSet(Vector2f dst, int index, float value) {
        if(index == 1)
            dst.y = value;
        else
            dst.x = value;
    }

    public static void VectorSet(Vector3f dst, int index, float value) {
        if(index == 1)
            dst.y = value;
        else if(index == 0)
            dst.x = value;
        else
            dst.z = value;
    }

    // dest = a + scale * b
    public static void VectorMA(Vector2f a, float scale, Vector2f b, Vector2f dest) {
        dest.x = a.x + b.x * scale;
        dest.y = a.y + b.y * scale;
    }

    public static void VectorMA(Vector3f a, float scale, Vector3f b, Vector3f dest) {
        dest.x = a.x + b.x * scale;
        dest.y = a.y + b.y * scale;
        dest.z = a.z + b.z * scale;
    }

    public static float RadiusFromBounds(Vector3f mins, Vector3f maxs) {
        Vector3f corner = new Vector3f();
        float a = Math.abs(mins.x);
        float b = Math.abs(maxs.x);
        corner.x = a > b ? a : b;
        a = Math.abs(mins.y);
        b = Math.abs(maxs.y);
        corner.y = a > b ? a : b;
        a = Math.abs(mins.z);
        b = Math.abs(maxs.z);
        corner.z = a > b ? a : b;

        return corner.length();
    }

    public static int Angle2Short(float f) {
        return (int)(f * (65536/360f)) & 65535;
    }

    public static float Short2Angle(int i) {
        return i * (360f/65536);
    }

    public static float VectorGet(Vector3f vec, int axis) {
        if(axis == 0)
            return vec.x;
        if(axis == 1)
            return vec.y;
        return vec.z;
    }

    
}
