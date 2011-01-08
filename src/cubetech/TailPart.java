/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class TailPart {
    public int FADE_TIME = 2000; // 2 sec
    public static final int SIZE = 6;
    public static final float POPMSEC = 150f;
    public Vector2f Position = new Vector2f();
    public int time;
    public int Age;
    CubeTexture tex;
    int poptime = -1;
    boolean popdown = true;

    public TailPart(CubeTexture tex) {
        this.tex = tex;
    }

    public void SetPopTime(int msec) {
        poptime = msec;
    }

    public boolean Free()
    {
        return time == 0;
    }

    public void SetTime(int msec) {
        FADE_TIME = msec;
        time = msec;
        Age = 0;
        poptime = -1;
        popdown = true;
    }

    public void Update(int msec) {
        if(poptime != -1 && popdown) {
            poptime -= msec;
            if(poptime >= 0) {
                return;
            }
            popdown = false;
            poptime = 0;
            return;
        } else if(popdown == false) {
            poptime += msec;

            if(poptime >= 100) {
                time = 0;
                Age = FADE_TIME;
            }
            return;
        }
        time -= msec;
        Age += msec;
        if(time < 0)
            time = 0;
    }

    public float GetRadius() {
        return (float)SIZE * ((float)time/(float)FADE_TIME);
    }

    public void Render() {
        if(time == 0)
            return;
        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
        float alpha = 1f;
        if(popdown)
            spr.Set(Position, (float)SIZE * ((float)time/(float)FADE_TIME), tex);
        else {
            float frac = poptime/POPMSEC;
            alpha = 1-frac;
            frac *= 16;
            spr.Set(Position, (float)SIZE * ((float)time/(float)FADE_TIME)+frac, tex);

        }

        
        spr.SetColor(new Vector4f(1, 1, 1, alpha));

        
    }
}
