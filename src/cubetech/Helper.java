package cubetech;

import cubetech.input.Input;
import cubetech.misc.Ref;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

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

    /**
     * Get the closest greater power of 2 to the fold number
     *
     * @param fold The target number
     * @return The power of 2
     */
    public static int get2Fold(int fold) {
        int ret = 2;
        while (ret < fold) {
            ret *= 2;
        }
        return ret;
    }

    public static Matrix4f scale(float f, Matrix4f src, Matrix4f dest) {
        if (dest == null) {
            dest = new Matrix4f();
        }
        dest.m00 = src.m00 * f;
        dest.m01 = src.m01 * f;
        dest.m02 = src.m02 * f;
        dest.m03 = src.m03 * f;

        dest.m10 = src.m10 * f;
        dest.m11 = src.m11 * f;
        dest.m12 = src.m12 * f;
        dest.m13 = src.m13 * f;

        dest.m20 = src.m20 * f;
        dest.m21 = src.m21 * f;
        dest.m22 = src.m22 * f;
        dest.m23 = src.m23 * f;

        dest.m30 = src.m30 * f;
        dest.m31 = src.m31 * f;
        dest.m32 = src.m32 * f;
        dest.m33 = src.m33 * f;
        return dest;
    }


    public static void tex(float x, float y) {
        GL11.glTexCoord2f(x, y);
    }


    

    public static Vector3f transform(Matrix4f left, Vector3f right, Vector3f dest) {
        if (dest == null)
            dest = new Vector3f();

        float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z + left.m30;
        float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z + left.m31;
        float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z + left.m32;

        dest.x = x;
        dest.y = y;
        dest.z = z;

        return dest;
    }

    public static Vector3f transform(Matrix3f left, Vector4f right, Vector3f dest) {
        if (dest == null)
            dest = new Vector3f();

        float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z;
        float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z;
        float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z;

        dest.x = x;
        dest.y = y;
        dest.z = z;

        return dest;
    }

    public static Vector3f transform(Matrix3f left, Vector3f right, Vector3f dest) {
        if (dest == null)
            dest = new Vector3f();

        float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z;
        float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z;
        float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z;

        dest.x = x;
        dest.y = y;
        dest.z = z;

        return dest;
    }

    public static String stripPath(String s) {
        s = s.replace('\\', '/');
        int i = s.lastIndexOf('/');
        String ext = null;
        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1);
        }

        if(ext == null || ext.isEmpty())
            return s;
        return ext;
    }

    public static String getPath(String s) {
        s = s.replace('\\', '/');
        int i = s.lastIndexOf('/');
        if(i == s.length()-1)
            return s; // ends with /, so assume directory

        String ext = null;
        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(0,i+1);
        }

        if(ext == null || ext.isEmpty())
            return "";
        return ext;
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

    public static void rotateAroundDirection(Vector3f[] axis, float yaw) {
        // create an arbitrary axis[1]
        perpendicularVector(axis[0], axis[1]);

        // rotate it around axis[0] by yaw
        if(yaw != 0) {
            Vector3f temp = new Vector3f(axis[1]);
            rotatePointAroundVector(axis[1], axis[0], temp, yaw);
        }

        // cross to get axis[2]
        Vector3f.cross(axis[0], axis[1], axis[2]);
    }

    public static float[] fillMatrixBuffer(Vector3f[] axis, Vector3f origin) {
        float[] view = new float[16];
        view[0] = axis[0].x;
        view[4] = axis[0].y;
        view[8] = axis[0].z;

        view[1] = axis[1].x;
        view[5] = axis[1].y;
        view[9] = axis[1].z;

        view[2] = axis[2].x;
        view[6] = axis[2].y;
        view[10] = axis[2].z;

        view[3] = view[7] = view[11] = 0;
        view[15] = 1;

        if(origin != null)
        {
            view[12] = -origin.x * view[0] + -origin.y * view[4] + -origin.z * view[8];
            view[13] = -origin.x * view[1] + -origin.y * view[5] + -origin.z * view[9];
            view[14] = -origin.x * view[2] + -origin.y * view[6] + -origin.z * view[10];
        }
        return view;
    }

    public static Vector3f rotatePointAroundVector(Vector3f dest, Vector3f dir, Vector3f point, float deg) {
        if(dest == null) dest = new Vector3f();
        Vector3f vr = perpendicularVector(dir, null);
        Vector3f vup = Vector3f.cross(vr, dir, null);

        float m[] = new float[3*3];
        m[0 * 3 + 0] = vr.x;
        m[1 * 3 + 0] = vr.y;
        m[2 * 3 + 0] = vr.z;

        m[0 * 3 + 1] = vup.x;
        m[1 * 3 + 1] = vup.y;
        m[2 * 3 + 1] = vup.z;

        m[0 * 3 + 2] = dir.x;
        m[1 * 3 + 2] = dir.y;
        m[2 * 3 + 2] = dir.z;

        float im[] = new float[3*3]; // inverse
        im[0 * 3 + 0] = m[0 * 3 + 0];
        im[0 * 3 + 1] = m[1 * 3 + 0];
        im[0 * 3 + 2] = m[2 * 3 + 0];
        im[1 * 3 + 0] = m[0 * 3 + 1];
        im[1 * 3 + 1] = m[1 * 3 + 1];
        im[1 * 3 + 2] = m[2 * 3 + 1];
        im[2 * 3 + 0] = m[0 * 3 + 2];
        im[2 * 3 + 1] = m[1 * 3 + 2];
        im[2 * 3 + 2] = m[2 * 3 + 2];

        float zrot[] = new float[9];
        zrot[0] = zrot[1 * 3 + 1] = zrot[2 * 3 + 2] = 1f;

        float rad = (float) ((deg * Math.PI) / 180f);
        zrot[0 * 3 + 0] = (float) Math.cos(rad);
        zrot[0 * 3 + 1] = (float) Math.sin(rad);
        zrot[1 * 3 + 0] = (float) -Math.sin(rad);
        zrot[1 * 3 + 1] = (float) Math.cos(rad);

        float[] tmpmat = new float[9];
        float[] rot = new float[9];
        matrixMult(m, zrot, tmpmat);
        matrixMult(tmpmat, im, rot);

        dest.x = rot[0 * 3 + 0] * point.x + rot[0 * 3 + 1] * point.y + rot[0 * 3 + 2] * point.z;
        dest.y = rot[1 * 3 + 0] * point.x + rot[1 * 3 + 1] * point.y + rot[1 * 3 + 2] * point.z;
        dest.z = rot[2 * 3 + 0] * point.x + rot[2 * 3 + 1] * point.y + rot[2 * 3 + 2] * point.z;
        return dest;
    }

    public static void matrixMult(float[] in1, float[] in2, float[] out) {
        out[0 * 3 + 0] = in1[0 * 3 + 0] * in2[0 * 3 + 0] + in1[0 * 3 + 1] * in2[1 * 3 + 0] +
				in1[0 * 3 + 2] * in2[2 * 3 + 0];
	out[0 * 3 + 1] = in1[0 * 3 + 0] * in2[0 * 3 + 1] + in1[0 * 3 + 1] * in2[1 * 3 + 1] +
				in1[0 * 3 + 2] * in2[2 * 3 + 1];
	out[0 * 3 + 2] = in1[0 * 3 + 0] * in2[0 * 3 + 2] + in1[0 * 3 + 1] * in2[1 * 3 + 2] +
				in1[0 * 3 + 2] * in2[2 * 3 + 2];
	out[1 * 3 + 0] = in1[1 * 3 + 0] * in2[0 * 3 + 0] + in1[1 * 3 + 1] * in2[1 * 3 + 0] +
				in1[1 * 3 + 2] * in2[2 * 3 + 0];
	out[1 * 3 + 1] = in1[1 * 3 + 0] * in2[0 * 3 + 1] + in1[1 * 3 + 1] * in2[1 * 3 + 1] +
				in1[1 * 3 + 2] * in2[2 * 3 + 1];
	out[1 * 3 + 2] = in1[1 * 3 + 0] * in2[0 * 3 + 2] + in1[1 * 3 + 1] * in2[1 * 3 + 2] +
				in1[1 * 3 + 2] * in2[2 * 3 + 2];
	out[2 * 3 + 0] = in1[2 * 3 + 0] * in2[0 * 3 + 0] + in1[2 * 3 + 1] * in2[1 * 3 + 0] +
				in1[2 * 3 + 2] * in2[2 * 3 + 0];
	out[2 * 3 + 1] = in1[2 * 3 + 0] * in2[0 * 3 + 1] + in1[2 * 3 + 1] * in2[1 * 3 + 1] +
				in1[2 * 3 + 2] * in2[2 * 3 + 1];
	out[2 * 3 + 2] = in1[2 * 3 + 0] * in2[0 * 3 + 2] + in1[2 * 3 + 1] * in2[1 * 3 + 2] +
				in1[2 * 3 + 2] * in2[2 * 3 + 2];
    }

    public static Vector3f perpendicularVector(Vector3f src, Vector3f dest) {
        if(dest == null) dest = new Vector3f();
        /*
	** find the smallest magnitude axially aligned vector
	*/

        float min = 1f;
        Vector3f temp = new Vector3f();
        int pos = 0;
        if(Math.abs(src.x) < min) {
            pos = 0; min = Math.abs(src.x);
        }
        if(Math.abs(src.y) < min) {
            pos = 1; min = Math.abs(src.y);
        }
        if(Math.abs(src.z) < min) {
            pos = 2; min = Math.abs(src.z);
        }
        VectorSet(temp, pos, 1);
        /*
	** project the point onto the plane defined by src
	*/
        
        projectPointOnPlane(dest, temp, src);

        Normalize(dest);
        return dest;
    }

    public static void projectPointOnPlane(Vector3f dst, Vector3f p, Vector3f normal) {
        float inv_denom = Vector3f.dot(normal, normal);
        inv_denom = 1.0f / inv_denom;
        float d = Vector3f.dot(normal, p) * inv_denom;

        dst.x = p.x - d * normal.x * inv_denom;
        dst.y = p.y - d * normal.y * inv_denom;
        dst.z = p.z - d * normal.z * inv_denom;
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

    public static void AddPointToBounds(Vector4f v, Vector3f mins, Vector3f maxs) {
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

    public static Vector3f VectorMA(Vector3f a, float scale, Vector3f b, Vector3f dest) {
        if(dest == null) dest = new Vector3f();
        dest.x = a.x + b.x * scale;
        dest.y = a.y + b.y * scale;
        dest.z = a.z + b.z * scale;
        return dest;
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

    public static Matrix3f toNormalMatrix(Matrix4f dest, Matrix3f matnorm) {
        if(matnorm == null)
            matnorm = new Matrix3f();
        
        // Matrix3x3 matnorm(mat.b.cross3(mat.c), mat.c.cross3(mat.a), mat.a.cross3(mat.b));
        // 1y * 2z - 1z * 2y,
        // Vec3(y*o.z-z*o.y, z*o.x-x*o.z, x*o.y-y*o.x);

        
        
        matnorm.m00 = dest.m11 * dest.m22 - dest.m21 * dest.m12;
        matnorm.m10 = dest.m21 * dest.m02 - dest.m01 * dest.m22;
        matnorm.m20 = dest.m01 * dest.m12 - dest.m11 * dest.m02;

        matnorm.m01 = dest.m12 * dest.m20 - dest.m22 * dest.m10;
        matnorm.m11 = dest.m22 * dest.m00 - dest.m02 * dest.m20;
        matnorm.m21 = dest.m02 * dest.m10 - dest.m12 * dest.m00;

        matnorm.m02 = dest.m10 * dest.m21 - dest.m20 * dest.m11;
        matnorm.m12 = dest.m20 * dest.m01 - dest.m00 * dest.m21;
        matnorm.m22 = dest.m00 * dest.m11 - dest.m10 * dest.m01;
        return matnorm;
    }

    public static float VectorDistance(Vector3f cameraOrigin, Vector3f entityOrigin) {
        // Delta
        float x = cameraOrigin.x - entityOrigin.x;
        float y = cameraOrigin.y - entityOrigin.y;
        float z = cameraOrigin.z - entityOrigin.z;
        // Lenght of delta
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static void multMatrix(float[] a, float[] b, FloatBuffer d) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                d.put(i * 4 + j,
                        a[i * 4 + 0] * b[0 * 4 + j]
                        + a[i * 4 + 1] * b[1 * 4 + j]
                        + a[i * 4 + 2] * b[2 * 4 + j]
                        + a[i * 4 + 3] * b[3 * 4 + j]
                        );
            }
        }
    }

    public static void toFloatBuffer(float[] input, FloatBuffer dest) {
        dest.clear();
        for (int i= 0; i < input.length; i++) {
            dest.put(input[i]);
        }
        dest.position(0);
    }

    public static void VectorFloor(Vector3f input) {
       input.x = (float) Math.floor(input.x);
       input.y = (float) Math.floor(input.y);
       input.z = (float) Math.floor(input.z);
    }

    public static Vector3f VectorMult(Vector3f a, Vector3f b, Vector3f dest) {
        if(dest == null) dest = new Vector3f();
        dest.x = a.x * b.x;
        dest.y = a.y * b.y;
        dest.z = a.z * b.z;
        return dest;
    }

    public static Vector4f VectorMult(Vector4f a, Vector4f b, Vector4f dest) {
        if(dest == null) dest = new Vector4f();
        dest.x = a.x * b.x;
        dest.y = a.y * b.y;
        dest.z = a.z * b.z;
        dest.w = a.w * b.w;
        return dest;
    }

    public static Matrix4f createOthoMatrix(float left, float right, float bottom, float top, float nearPlane, float farPlane) {
        Matrix4f m = new Matrix4f();
        m.m00 = 2f / (right-left);
        m.m11 = 2f / (top-bottom);
        m.m22 = 1f / (nearPlane - farPlane);
        m.m33 = 1f;
        m.m30 = (left+right) / (left-right);
        m.m31 = (top+bottom) / (bottom-top);
        m.m32 = nearPlane / (nearPlane - farPlane);
//        m.m03 = (left+right) / (left-right);
//        m.m13 = (top+bottom) / (bottom-top);
//        m.m23 = nearPlane / (nearPlane - farPlane);
        return m;
    }

    public static float AngleMod(float a) {
        a = (360.0f/65536) * ((int)(a*(65536/360.0)) & 65535);
        return a;
    }

    public static float AngleSubtract(float a1, float a2) {
        float a = a1 - a2;
        while(a > 180) {
            a -= 360;
        }
        while(a < -180) {
            a += 360;
        }
        return a;
    }

    
}
