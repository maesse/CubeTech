/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class Segment {
    public Vector2f P1;
    public Vector2f P2;
    private Vector2f Vector2f;

    Vector2f GetSpan(Vector2f Axis)
    {
        Vector2f res = new Vector2f();

        float p1 = Vector2f.dot(P1, Axis);
        float p2 = Vector2f.dot(P2, Axis);

        if (p1 < p2)
        {
            res.x = p1;
            res.y = p2;
        }
        else
        {
            res.x = p2;
            res.y = p1;
        }
        return res;
    }

    

    boolean BoxAxisClip(Vector2f C, Vector2f D, float r, Vector2f minmax)
    {
	float c = Vector2f.dot(C,D);
	float p1 = Vector2f.dot(P1, D);
	float p2 = Vector2f.dot(P2 , D);
	return AxisClip(c-r, c+r, p1, p2, minmax);
    }


    boolean Clip(float minx, float maxx, float miny, float maxy, Segment ClippedSeg)
    {
        Vector2f minmaxx = new Vector2f();
        Vector2f minmaxy = new Vector2f();


        if (!AxisClip(minx, maxx, P1.x, P2.x, minmaxx))
            return false;

        if (!AxisClip(miny, maxy, P1.y, P2.y, minmaxy))
            return false;

        if (minmaxx.x > minmaxy.y || minmaxx.y < minmaxy.x)
            return false;

        float tmin, tmax;
        tmin = minmaxx.x;
        tmax = minmaxx.y;

        if(minmaxy.x > minmaxx.x)
            tmin = minmaxy.x;

        if(minmaxy.y < minmaxx.y)
            tmax = minmaxy.y;

        Vector2f D = new Vector2f(P2.x - P1.x, P2.y - P1.y);
        ClippedSeg.P1 = new Vector2f(P1.x + tmin * D.x, P1.y + tmin * D.y);
        
        ClippedSeg.P2 = new Vector2f(P1.x + tmax * D.x, P1.y + tmax * D.y);

        return true;
    }

    static boolean AxisClip(float min, float max, float p1, float p2, Vector2f minmax)
    {
        float d = p2 - p1;

        boolean sign = (d > 0.0f);

        if (!sign)
        {
             float temp = p1;
             p1 = p2;
             p2 = temp;
             d  = -d;
        }
        if (p1 > max || p2 < min)
            return false;

        minmax.x = 0.0f;
        minmax.y = 1.0f;

        if (d < 0.0000001f)
            return true;

        if (p1 < min)
            minmax.x = (min - p1) / (d);

        if (p2 > max)
            minmax.y = (max - p1) / (d);

        if (!sign)
        {
              float temp = p1;
              p1 = 1.0f - p2;
              p2 = 1.0f - temp;
        }
        return true;
    }



}
