/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cubetech.common;

/**
 *
 * Quaternions for LWJGL!
 *
 * @author fbi
 * @version $Revision$
 * $Id$
 */

import java.nio.FloatBuffer;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class Quaternion extends Vector implements ReadableVector4f {
	private static final long serialVersionUID = 1L;

	public float x, y, z, w;

	/**
	 * C'tor. The quaternion will be initialized to the identity.
	 */
	public Quaternion() {
		super();
		setIdentity();
	}

	/**
	 * C'tor
	 *
	 * @param src
	 */
	public Quaternion(ReadableVector4f src) {
		set(src);
	}

	/**
	 * C'tor
	 *
	 */
	public Quaternion(float x, float y, float z, float w) {
		set(x, y, z, w);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.util.vector.WritableVector2f#set(float, float)
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.util.vector.WritableVector3f#set(float, float, float)
	 */
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.util.vector.WritableVector4f#set(float, float, float,
	 *      float)
	 */
	public void set(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	/**
	 * Load from another Vector4f
	 *
	 * @param src
	 *            The source vector
	 * @return this
	 */
	public Quaternion set(ReadableVector4f src) {
		x = src.getX();
		y = src.getY();
		z = src.getZ();
		w = src.getW();
		return this;
	}

	/**
	 * Set this quaternion to the multiplication identity.
	 * @return this
	 */
	public Quaternion setIdentity() {
		return setIdentity(this);
	}

	/**
	 * Set the given quaternion to the multiplication identity.
	 * @param q The quaternion
	 * @return q
	 */
	public static Quaternion setIdentity(Quaternion q) {
		q.x = 0;
		q.y = 0;
		q.z = 0;
		q.w = 1;
		return q;
	}

	/**
	 * @return the length squared of the quaternion
	 */
	public float lengthSquared() {
		return x * x + y * y + z * z + w * w;
	}

	/**
	 * Normalise the source quaternion and place the result in another quaternion.
	 *
	 * @param src
	 *            The source quaternion
	 * @param dest
	 *            The destination quaternion, or null if a new quaternion is to be
	 *            created
	 * @return The normalised quaternion
	 */
	public static Quaternion normalise(Quaternion src, Quaternion dest) {
		float inv_l = 1f/src.length();

		if (dest == null)
			dest = new Quaternion();

		dest.set(src.x * inv_l, src.y * inv_l, src.z * inv_l, src.w * inv_l);

		return dest;
	}

	/**
	 * Normalise this quaternion and place the result in another quaternion.
	 *
	 * @param dest
	 *            The destination quaternion, or null if a new quaternion is to be
	 *            created
	 * @return the normalised quaternion
	 */
	public Quaternion normalise(Quaternion dest) {
		return normalise(this, dest);
	}

	/**
	 * The dot product of two quaternions
	 *
	 * @param left
	 *            The LHS quat
	 * @param right
	 *            The RHS quat
	 * @return left dot right
	 */
	public static float dot(Quaternion left, Quaternion right) {
		return left.x * right.x + left.y * right.y + left.z * right.z + left.w
				* right.w;
	}

	/**
	 * Calculate the conjugate of this quaternion and put it into the given one
	 *
	 * @param dest
	 *            The quaternion which should be set to the conjugate of this
	 *            quaternion
	 */
	public Quaternion negate(Quaternion dest) {
		return negate(this, dest);
	}

	/**
	 * Calculate the conjugate of this quaternion and put it into the given one
	 *
	 * @param src
	 *            The source quaternion
	 * @param dest
	 *            The quaternion which should be set to the conjugate of this
	 *            quaternion
	 */
	public static Quaternion negate(Quaternion src, Quaternion dest) {
		if (dest == null)
			dest = new Quaternion();

		dest.x = -src.x;
		dest.y = -src.y;
		dest.z = -src.z;
		dest.w = src.w;

		return dest;
	}

	/**
	 * Calculate the conjugate of this quaternion
	 */
	public Vector negate() {
		return negate(this, this);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.Vector#load(java.nio.FloatBuffer)
	 */
	public Vector load(FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		w = buf.get();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	public Vector scale(float scale) {
		return scale(scale, this, this);
	}

	/**
	 * Scale the source quaternion by scale and put the result in the destination
	 * @param scale The amount to scale by
	 * @param src The source quaternion
	 * @param dest The destination quaternion, or null if a new quaternion is to be created
	 * @return The scaled quaternion
	 */
	public static Quaternion scale(float scale, Quaternion src, Quaternion dest) {
		if (dest == null)
			dest = new Quaternion();
		dest.x = src.x * scale;
		dest.y = src.y * scale;
		dest.z = src.z * scale;
		dest.w = src.w * scale;
		return dest;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.ReadableVector#store(java.nio.FloatBuffer)
	 */
	public Vector store(FloatBuffer buf) {
		buf.put(x);
		buf.put(y);
		buf.put(z);
		buf.put(w);

		return this;
	}

	/**
	 * @return x
	 */
	public final float getX() {
		return x;
	}

	/**
	 * @return y
	 */
	public final float getY() {
		return y;
	}

	/**
	 * Set X
	 *
	 * @param x
	 */
	public final void setX(float x) {
		this.x = x;
	}

	/**
	 * Set Y
	 *
	 * @param y
	 */
	public final void setY(float y) {
		this.y = y;
	}

	/**
	 * Set Z
	 *
	 * @param z
	 */
	public void setZ(float z) {
		this.z = z;
	}

	/*
	 * (Overrides)
	 *
	 * @see org.lwjgl.vector.ReadableVector3f#getZ()
	 */
	public float getZ() {
		return z;
	}

	/**
	 * Set W
	 *
	 * @param w
	 */
	public void setW(float w) {
		this.w = w;
	}

	/*
	 * (Overrides)
	 *
	 * @see org.lwjgl.vector.ReadableVector3f#getW()
	 */
	public float getW() {
		return w;
	}

	public String toString() {
		return "Quaternion: " + x + " " + y + " " + z + " " + w;
	}

	/**
	 * Sets the value of this quaternion to the quaternion product of
	 * quaternions left and right (this = left * right). Note that this is safe
	 * for aliasing (e.g. this can be left or right).
	 *
	 * @param left
	 *            the first quaternion
	 * @param right
	 *            the second quaternion
	 */
	public static Quaternion mul(Quaternion left, Quaternion right,
			Quaternion dest) {
		if (dest == null)
			dest = new Quaternion();
		dest.set(left.x * right.w + left.w * right.x + left.y * right.z
				- left.z * right.y, left.y * right.w + left.w * right.y
				+ left.z * right.x - left.x * right.z, left.z * right.w
				+ left.w * right.z + left.x * right.y - left.y * right.x,
				left.w * right.w - left.x * right.x - left.y * right.y
				- left.z * right.z);
		return dest;
	}
        
        public static Quaternion slerp(Quaternion current, Quaternion next, double frac, Quaternion dest) {
            // quaternion to return
            if(dest == null) dest = new Quaternion();
            // Calculate angle between them.
            double cosHalfTheta = current.w * next.w + current.x * next.x + current.y * next.y + current.z * next.z;
            if (cosHalfTheta < 0) {
              next.w = -next.w; next.x = -next.x; next.y = -next.y; next.z = -next.z;
              cosHalfTheta = -cosHalfTheta;
            }
            // if qa=qb or qa=-qb then theta = 0 and we can return qa
            if (Math.abs(cosHalfTheta) >= 1.0){
                    dest.w = current.w;dest.x = current.x;dest.y = current.y;dest.z = current.z;
                    return dest;
            }
            // Calculate temporary values.
            double halfTheta = Math.acos(cosHalfTheta);
            double sinHalfTheta = Math.sqrt(1.0 - cosHalfTheta*cosHalfTheta);
            // if theta = 180 degrees then result is not fully defined
            // we could rotate around any axis normal to qa or qb
            if (Math.abs(sinHalfTheta) < 0.001){ // fabs is floating point absolute
                    dest.w = (current.w * 0.5f + next.w * 0.5f);
                    dest.x = (current.x * 0.5f + next.x * 0.5f);
                    dest.y = (current.y * 0.5f + next.y * 0.5f);
                    dest.z = (current.z * 0.5f + next.z * 0.5f);
                    return dest;
            }
            double ratioA = Math.sin((1 - frac) * halfTheta) / sinHalfTheta;
            double ratioB = Math.sin(frac * halfTheta) / sinHalfTheta; 
            //calculate Quaternion.
            dest.w = (float)(current.w * ratioA + next.w * ratioB);
            dest.x = (float)(current.x * ratioA + next.x * ratioB);
            dest.y = (float)(current.y * ratioA + next.y * ratioB);
            dest.z = (float)(current.z * ratioA + next.z * ratioB);
            return dest;
    }

	/**
	 *
	 * Multiplies quaternion left by the inverse of quaternion right and places
	 * the value into this quaternion. The value of both argument quaternions is
	 * preservered (this = left * right^-1).
	 *
	 * @param left
	 *            the left quaternion
	 * @param right
	 *            the right quaternion
	 */
	public static Quaternion mulInverse(Quaternion left, Quaternion right,
			Quaternion dest) {
		float n = right.lengthSquared();
		// zero-div may occur.
		n = (n == 0.0 ? n : 1 / n);
		// store on stack once for aliasing-safty
		if (dest == null)
			dest = new Quaternion();
		dest
			.set((left.x * right.w - left.w * right.x - left.y
						* right.z + left.z * right.y)
					* n, (left.y * right.w - left.w * right.y - left.z
						* right.x + left.x * right.z)
					* n, (left.z * right.w - left.w * right.z - left.x
						* right.y + left.y * right.x)
					* n, (left.w * right.w + left.x * right.x + left.y
						* right.y + left.z * right.z)
					* n);

		return dest;
	}
        
        public final Matrix3f quatToMatrix(Matrix3f m){
            if(m == null) m = new Matrix3f();
            float sqw = w*w;
            float sqx = x*x;
            float sqy = y*y;
            float sqz = z*z;

            // invs (inverse square length) is only required if quaternion is not already normalised
            float invs = 1f / (sqx + sqy + sqz + sqw);
            m.m00 = ( sqx - sqy - sqz + sqw)*invs ; // since sqw + sqx + sqy + sqz =1/invs*invs
            m.m11 = (-sqx + sqy - sqz + sqw)*invs ;
            m.m22 = (-sqx - sqy + sqz + sqw)*invs ;

            float tmp1 = x*y;
            float tmp2 = z*w;
            m.m10 = 2.0f * (tmp1 + tmp2)*invs ;
            m.m01 = 2.0f * (tmp1 - tmp2)*invs ;

            tmp1 = x*z;
            tmp2 = y*w;
            m.m20 = 2.0f * (tmp1 - tmp2)*invs ;
            m.m02 = 2.0f * (tmp1 + tmp2)*invs ;
            tmp1 = y*z;
            tmp2 = x*w;
            m.m21 = 2.0f * (tmp1 + tmp2)*invs ;
            m.m12 = 2.0f * (tmp1 - tmp2)*invs ; 
            return m;
        }
        
        public final Vector3f[] quatToMatrix(Vector3f[] m){
            if(m == null) m = new Vector3f[] { new Vector3f(), new Vector3f(), new Vector3f()};
            float sqw = w*w;
            float sqx = x*x;
            float sqy = y*y;
            float sqz = z*z;

            // invs (inverse square length) is only required if quaternion is not already normalised
            float invs = 1f / (sqx + sqy + sqz + sqw);
            m[0].x = ( sqx - sqy - sqz + sqw)*invs ; // since sqw + sqx + sqy + sqz =1/invs*invs
            m[1].y = (-sqx + sqy - sqz + sqw)*invs ;
            m[2].z = (-sqx - sqy + sqz + sqw)*invs ;

            float tmp1 = x*y;
            float tmp2 = z*w;
            m[0].y = 2.0f * (tmp1 + tmp2)*invs ;
            m[1].x = 2.0f * (tmp1 - tmp2)*invs ;

            tmp1 = x*z;
            tmp2 = y*w;
            m[0].z = 2.0f * (tmp1 - tmp2)*invs ;
            m[2].x = 2.0f * (tmp1 + tmp2)*invs ;
            tmp1 = y*z;
            tmp2 = x*w;
            m[1].z = 2.0f * (tmp1 + tmp2)*invs ;
            m[2].y = 2.0f * (tmp1 - tmp2)*invs ; 
            return m;
        }
        
        public void toAngleNormalized(Vector3f angle) {
            double test = x*y + z*w;
            if (test > 0.499) { // singularity at north pole
                    angle.x = (float) (2 * Math.atan2(x,w));
                    angle.y = (float) (Math.PI/2);
                    angle.z = 0;
                    return;
            }
            if (test < -0.499) { // singularity at south pole
                    angle.x = (float) (-2 * Math.atan2(x,w));
                    angle.y = (float) (- Math.PI/2);
                    angle.z = 0;
                    return;
            }
            double sqx = x*x;
            double sqy = y*y;
            double sqz = z*z;
            angle.x = (float) Math.atan2(2*y*w-2*x*z , 1 - 2*sqy - 2*sqz);
            angle.y = (float) Math.asin(2*test);
            angle.z = (float) Math.atan2(2*x*w-2*y*z , 1 - 2*sqx - 2*sqz);
        }
        
        public void toAngle(Vector3f angle) {
            double sqw = w*w;
            double sqx = x*x;
            double sqy = y*y;
            double sqz = z*z;
            double unit = sqx + sqy + sqz + sqw; // if normalised is one, otherwise is correction factor
            double test = x*y + z*w;
            if (test > 0.499*unit) { // singularity at north pole
                    angle.x = (float) (2 * Math.atan2(x,w));
                    angle.y = (float) (Math.PI/2);
                    angle.z = 0;
                    return;
            }
            if (test < -0.499*unit) { // singularity at south pole
                    angle.x = (float) (-2 * Math.atan2(x,w));
                    angle.y = (float) (-Math.PI/2);
                    angle.z = 0;
                    return;
            }
            angle.x = (float) Math.atan2(2*y*w-2*x*z , sqx - sqy - sqz + sqw);
            angle.y = (float) Math.asin(2*test/unit);
            angle.z = (float) Math.atan2(2*x*w-2*y*z , -sqx + sqy - sqz + sqw);
        }

	/**
	 * Sets the value of this quaternion to the equivalent rotation of the
	 * Axis-Angle argument.
	 *
	 * @param a1
	 *            the axis-angle: (x,y,z) is the axis and w is the angle
	 */
	public final void setFromAxisAngle(Vector4f a1) {
		x = a1.x;
		y = a1.y;
		z = a1.z;
		float n = (float) Math.sqrt(x * x + y * y + z * z);
		// zero-div may occur.
		float s = (float) (Math.sin(0.5 * a1.w) / n);
		x *= s;
		y *= s;
		z *= s;
		w = (float) Math.cos(0.5 * a1.w);
	}

	/**
	 * Sets the value of this quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m
	 *            The matrix
	 * @return this
	 */
	public final Quaternion setFromMatrix(Matrix4f m) {
		return setFromMatrix(m, this);
	}

	/**
	 * Sets the value of the source quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m
	 *            The source matrix
	 * @param q
	 *            The destination quaternion, or null if a new quaternion is to be created
	 * @return q
	 */
	public static Quaternion setFromMatrix(Matrix4f m, Quaternion q) {
            if(q == null) q = new Quaternion();
		return q.setFromMat(m.m00, m.m01, m.m02, m.m10, m.m11, m.m12, m.m20,
				m.m21, m.m22);
	}

	/**
	 * Sets the value of this quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m
	 *            The source matrix
	 */
	public final Quaternion setFromMatrix(Matrix3f m) {
		return setFromMatrix(m, this);
                
	}
        


	/**
	 * Sets the value of the source quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m
	 *            The source matrix
	 * @param q
	 *            The destination quaternion, or null if a new quaternion is to be created
	 * @return q
	 */
	public static Quaternion setFromMatrix(Matrix3f m, Quaternion q) {
		return q.setFromMat(m.m00, m.m01, m.m02, m.m10, m.m11, m.m12, m.m20,
				m.m21, m.m22);
	}
        
     

	/**
	 * Private method to perform the matrix-to-quaternion conversion
	 */
	private Quaternion setFromMat(float m00, float m01, float m02, float m10,
			float m11, float m12, float m20, float m21, float m22) {

		float s;
		float tr = m00 + m11 + m22;
		if (tr >= 0.0) {
			s = (float) Math.sqrt(tr + 1.0);
			w = s * 0.5f;
			s = 0.5f / s;
			x = (m21 - m12) * s;
			y = (m02 - m20) * s;
			z = (m10 - m01) * s;
		} else {
			float max = Math.max(Math.max(m00, m11), m22);
			if (max == m00) {
				s = (float) Math.sqrt(m00 - (m11 + m22) + 1.0);
				x = s * 0.5f;
				s = 0.5f / s;
				y = (m01 + m10) * s;
				z = (m20 + m02) * s;
				w = (m21 - m12) * s;
			} else if (max == m11) {
				s = (float) Math.sqrt(m11 - (m22 + m00) + 1.0);
				y = s * 0.5f;
				s = 0.5f / s;
				z = (m12 + m21) * s;
				x = (m01 + m10) * s;
				w = (m02 - m20) * s;
			} else {
				s = (float) Math.sqrt(m22 - (m00 + m11) + 1.0);
				z = s * 0.5f;
				s = 0.5f / s;
				x = (m20 + m02) * s;
				y = (m12 + m21) * s;
				w = (m10 - m01) * s;
			}
		}
		return this;
	}
}
