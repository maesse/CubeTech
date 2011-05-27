/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class Matrix3x4 {
    Vector4f a, b, c;
    public Matrix3x4(Quaternion q, Vector3f scale, Vector3f trans) {
        convertquat(q);
        MultiplyVectors(a, scale, a);
        MultiplyVectors(b, scale, b);
        MultiplyVectors(c, scale, c);
        a.w = trans.x;
        b.w = trans.y;
        c.w = trans.z;
    }

    public Matrix4f toMatrix4f(Matrix4f src) {
        if(src == null)
            src = new Matrix4f();
        src.m00 = a.x; src.m10 = a.y; src.m20 = a.z; src.m30 = a.w;
        src.m01 = b.x; src.m11 = b.y; src.m21 = b.z; src.m31 = b.w;
        src.m02 = c.x; src.m12 = c.y; src.m22 = c.z; src.m32 = c.w;
        return src;
    }
    
    private Vector4f MultiplyVectors(Vector4f src, Vector3f src2, Vector4f dst) {
        if(dst == null)
            dst = new Vector4f();
        dst.x = src.x*src2.x;
        dst.y = src.y*src2.y;
        dst.z = src.z*src2.z;

        return dst;
    }

    private void convertquat(Quaternion q) {
        float x = q.x, y = q.y, z = q.z, w = q.w,
              tx = 2*x, ty = 2*y, tz = 2*z,
              txx = tx*x, tyy = ty*y, tzz = tz*z,
              txy = tx*y, txz = tx*z, tyz = ty*z,
              twx = w*tx, twy = w*ty, twz = w*tz;

        a = new Vector4f(1 - (tyy + tzz), txy - twz, txz + twy,0);
        b = new Vector4f(txy + twz, 1 - (txx + tzz), tyz - twx,0);
        c = new Vector4f(txz - twy, tyz + twx, 1 - (txx + tyy),0);
    }
}
