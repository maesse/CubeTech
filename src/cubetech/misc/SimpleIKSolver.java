package cubetech.misc;

public class SimpleIKSolver {
//-------- SOLVE TWO LINK INVERSE KINEMATICS -------------

// Given a two link joint from [0,0,0] to end effector position P,
// let link lengths be a and b, and let norm |P| = c.  Clearly a+b >= c.
//
// Problem: find a "knee" position Q such that |Q| = a and |P-Q| = b.
//
// In the case of a point on the x axis R = [c,0,0], there is a
// closed form solution S = [d,e,0], where |S| = a and |R-S| = b:
//
//    d2+e2 = a2                  -- because |S| = a
//    (c-d)2+e2 = b2              -- because |R-S| = b
//
//    c2-2cd+d2+e2 = b2           -- combine the two equations
//    c2-2cd = b2 - a2
//    c-2d = (b2-a2)/c
//    d - c/2 = (a2-b2)/c / 2
//
//    d = (c + (a2-b2/c) / 2      -- to solve for d and e.
//    e = sqrt(a2-d2)
    static float findD(float a, float b, float c) {
        return Math.max(0, Math.min(a, (c + (a * a - b * b) / c) / 2));
    }

    static float findE(float a, float d) {
        return (float)Math.sqrt(a * a - d * d);
    }
// This leads to a solution to the more general problem:
//
//   (1) R = Mfwd(P)         -- rotate P onto the x axis
//   (2) Solve for S
//   (3) Q = Minv(S)         -- rotate back again
    static float[][] Mfwd = new float[3][3], Minv = new float[3][3];

    static public boolean solve(float A, float B, float[] P, float[] D, float[] Q) {
        float[] R = new float[3];
        defineM(P, D);
        rot(Minv, P, R);
        float d = findD(A, B, norm(R));
        float e = findE(A, d);
        float[] S = {d, e, 0};
        rot(Mfwd, S, Q);
        return d > 0 && d < A;
    }

// If "knee" position Q needs to be as close as possible to some point D,
// then choose M such that M(D) is in the y>0 half of the z=0 plane.
//
// Given that constraint, define the forward and inverse of M as follows:
    static void defineM(float[] P, float[] D) {
        float[] X = Minv[0], Y = Minv[1], Z = Minv[2];

// Minv defines a coordinate system whose x axis contains P, so X = unit(P).

        for (int i = 0; i < 3; i++) {
            X[i] = P[i];
        }
        normalize(X);

// The y axis of Minv is perpendicular to P, so Y = unit( D - X(D·X) ).

        float dDOTx = dot(D, X);
        for (int i = 0; i < 3; i++) {
            Y[i] = D[i] - dDOTx * X[i];
        }
        normalize(Y);

// The z axis of Minv is perpendicular to both X and Y, so Z = X×Y.

        cross(X, Y, Z);

// Mfwd = (Minv)T, since transposing inverts a rotation matrix.

        for (int i = 0; i < 3; i++) {
            Mfwd[i][0] = Minv[0][i];
            Mfwd[i][1] = Minv[1][i];
            Mfwd[i][2] = Minv[2][i];
        }
    }

//------------ GENERAL VECTOR MATH SUPPORT -----------
    static float norm(float[] v) {
        return (float)Math.sqrt(dot(v, v));
    }

    static void normalize(float[] v) {
        float norm = norm(v);
        for (int i = 0; i < 3; i++) {
            v[i] /= norm;
        }
    }

    static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static void cross(float[] a, float[] b, float[] c) {
        c[0] = a[1] * b[2] - a[2] * b[1];
        c[1] = a[2] * b[0] - a[0] * b[2];
        c[2] = a[0] * b[1] - a[1] * b[0];
    }

    static void rot(float[][] M, float[] src, float[] dst) {
        for (int i = 0; i < 3; i++) {
            dst[i] = dot(M[i], src);
        }
    }
}
