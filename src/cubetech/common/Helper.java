/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import cubetech.gfx.Shader;
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

    public static int Clamp(int value, int min, int max) {
        if(value < min) value = min;
        if(value > max) value = max;
        return value;
    }

    public static float Clamp(float value, float min, float max) {
        if(value < min) value = min;
        if(value > max) value = max;
        return value;
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

    // hermite basis function for smooth interpolation
    // Similar to Gain() above, but very cheap to call
    // value should be between 0 & 1 inclusive
    public static float SimpleSpline(float val) {
        float valSq = val * val;
        return 3 * valSq - 2 * valSq * val;
    }

    //
    public static void renderBBoxWireframe(float xmin, float ymin, float zmin, 
            float xmax, float ymax, float zmax,
            Vector4f color) {
        // ready the texture
        Ref.ResMan.getWhiteTexture().Bind();
        if(color != null) {
            col(color.x, color.y, color.z, color.w);
        } else {
            col(1,0,0);
        }
        GL11.glBegin(GL11.GL_LINES);

        // Top: Z+
        {
            GL11.glVertex3f(xmin,             ymin,             zmax);
            GL11.glVertex3f(xmax,ymin,             zmax);
            GL11.glVertex3f(xmax,ymin,             zmax);
            GL11.glVertex3f(xmax,ymax,zmax);
            GL11.glVertex3f(xmax,ymax,zmax);
            GL11.glVertex3f(xmin,ymax,zmax);
            GL11.glVertex3f(xmin,ymax,zmax);
            GL11.glVertex3f(xmin,ymin,zmax);
        }

        // Bottom: Z-
        {
            GL11.glVertex3f(xmin,             ymin,             zmin);
            GL11.glVertex3f(xmax,ymin,             zmin);
            GL11.glVertex3f(xmax,ymin,             zmin);
            GL11.glVertex3f(xmax,ymax,zmin);
            GL11.glVertex3f(xmax,ymax,zmin);
            GL11.glVertex3f(xmin,ymax,zmin);
            GL11.glVertex3f(xmin,ymax,zmin);
            GL11.glVertex3f(xmin,ymin,zmin);
        }

        // Y+
        {
            GL11.glVertex3f(xmax,ymax,    zmax);
            GL11.glVertex3f(xmax,ymax,     zmin);
        }

        // Y-
        {
            GL11.glVertex3f(xmin,             ymin,     zmin );
            GL11.glVertex3f(xmin,ymin,     zmax);
        }

        // X+
        {
            GL11.glVertex3f(xmax ,ymin ,    zmin);
            GL11.glVertex3f(xmax, ymin,                 zmax);
        }

        // X-
        {
            GL11.glVertex3f(xmin ,ymax ,    zmax);
            GL11.glVertex3f(xmin ,ymax,     zmin);
        }
        GL11.glEnd();
    }

    public static void tex(float x, float y) {
        if(Ref.glRef.isShadersSupported())
            GL20.glVertexAttrib2f(Shader.INDICE_COORDS, x, y);
        else
            GL11.glTexCoord2f(x, y);
    }

    /**
     * Uses normalized 0-1 values
     * @param r
     * @param g
     * @param b
     */
    public static void col(float r, float g, float b) {
        col(r,g,b,1);
    }

    public static void col(float r, float g, float b, float a) {
        if(Ref.glRef.isShadersSupported())
            GL20.glVertexAttrib4f(Shader.INDICE_COLOR, r,g,b,a);
        else
            GL11.glColor4f(r,g,b,a);
    }

    public static void col(Vector4f color) {
        if(Ref.glRef.isShadersSupported())
            GL20.glVertexAttrib4f(Shader.INDICE_COLOR, color.x/255f,color.y/255f,color.z/255f,color.w/255f);
        else
            GL11.glColor4f(color.x/255f,color.y/255f,color.z/255f,color.w/255f);
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

    public static Vector3f[] AnglesToAxis(Vector3f angles, Vector3f[] axis) {
        if(axis == null) {
            axis = new Vector3f[3];
            for (int i= 0; i < 3; i++) {
                axis[i] = new Vector3f();
            }
        }
        assert(axis.length >= 3);
        // angle vectors returns "right" instead of "y axis"
        AngleVectors(angles, axis[0], axis[1], axis[2]);
        axis[1].scale(-1f);
        return axis;
    }

    public static Vector3f[] AnglesToAxis(Vector3f angles) {
        return AnglesToAxis(angles, null);
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

    public static void clearBounds(Vector3f mins, Vector3f maxs) {
        if(mins != null) mins.set(99999,99999,99999);
        if(maxs != null) maxs.set(-99999,-99999,-99999);
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

    public static Vector3f[] mul(Vector3f[] left, Vector3f[] right, Vector3f[] dest) {
        assert(dest != null);

        float m00 =
                left[0].x * right[0].x + left[0].y * right[1].x + left[0].z * right[2].x;
        float m01 =
                left[1].x * right[0].x + left[1].y * right[1].x + left[1].z * right[2].x;
        float m02 =
                left[2].x * right[0].x + left[2].y * right[1].x + left[2].z * right[2].x;
        float m10 =
                left[0].x * right[0].y + left[0].y * right[1].y + left[0].z * right[2].y;
        float m11 =
                left[1].x * right[0].y + left[1].y * right[1].y + left[1].z * right[2].y;
        float m12 =
                left[2].x * right[0].y + left[2].y * right[1].y + left[2].z * right[2].y;
        float m20 =
                left[0].x * right[0].z + left[0].y * right[1].z + left[0].z * right[2].z;
        float m21 =
                left[1].x * right[0].z + left[1].y * right[1].z + left[1].z * right[2].z;
        float m22 =
                left[2].x * right[0].z + left[2].y * right[1].z + left[2].z * right[2].z;


        dest[0].x = m00;
        dest[1].x = m01;
        dest[2].x = m02;
        dest[0].y = m10;
        dest[1].y = m11;
        dest[2].y = m12;
        dest[0].z = m20;
        dest[1].z = m21;
        dest[2].z = m22;

        return dest;
    }

    public static void matrixToAxis(Matrix4f m, Vector3f[] dest) {
//        m.invert();
        assert(dest != null && dest.length >= 3);
        dest[0].x = m.m00;
        dest[0].y = m.m10;
        dest[0].z = m.m20;

        dest[1].x = m.m01;
        dest[1].y = m.m11;
        dest[1].z = m.m21;

        dest[2].x = m.m02;
        dest[2].y = m.m12;
        dest[2].z = m.m22;
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

    public static boolean Equals(Vector3f a, Vector3f b, float epsilon) {
        return a.x-epsilon < b.x && a.x + epsilon > b.x &&
                a.y-epsilon < b.y && a.y + epsilon > b.y &&
                a.z-epsilon < b.z && a.z + epsilon > b.z;
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

    public static Vector3f LerpAngles(Vector3f from, Vector3f to, Vector3f dest, float frac) {
        if(dest == null) dest = new Vector3f();
        dest.x = LerpAngle(from.x, to.x, frac);
        dest.y = LerpAngle(from.y, to.y, frac);
        dest.z = LerpAngle(from.z, to.z, frac);
        return dest;
    }

    public static float LerpAngle(float from, float to, float frac) {
        if(to - from > 180) to -= 360;
        if(to - from < -180) to += 360;
        return from + frac * (to -from);
    }



    public static Vector3f intToNormal(int i) {
        int bits = 10; // for each direction
        float x = unpackFloat(i, 0, bits);
        float y = unpackFloat(i, 1, bits);
        float z = unpackFloat(i, 2, bits);
        return new Vector3f(x, y, z);
    }

    private static float unpackFloat(int i, int bitoffset, int nbits) {
        int bitmask = (1 << nbits)-1;
        int valuemask = (1<<(nbits-1))-1;

        int v = (i >> bitoffset * nbits) & bitmask; // mask of everything unneeded

        float f = (float)(v & valuemask) / valuemask;
        boolean neg = (v & (valuemask+1)) != 0;
        if(neg) f = -f;
        return f;
    }

    private static int packFloat(float v, int bitoffset, int nbits) {
        float v2 = v;
        if(v2 < 0) {
            v2 = -v2;
        }
        int bitsize = (1<<(nbits-1))-1;
        int i = ((int)(v2 * bitsize));
        if(v < 0) i |= (1<<(nbits-1));
        i <<= (nbits * bitoffset);

        return i;
    }

    public static int normalToInt(Vector3f v) {
        int bits = 10; // for each direction
        int i = packFloat(v.x, 0, bits) | packFloat(v.y, 1, bits) | packFloat(v.z, 2, bits);
        
        return i;
    }

    
}
