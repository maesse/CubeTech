/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.misc.Ref;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CircleSplotion {
    static final int LIFETIME = 2000;
    static final float FADEOUT_TIME = 1500f;
    static final float EXPLODE_TIME = 500f;
    
    public Vector2f Position;
    public float Radius;
    public int time;
    public int Age;
    CubeTexture tex;
    Color color = new Color(255,255,255,255);
    

    public CircleSplotion(Vector2f position, float radius) {
        Position = position;
        Radius = radius;
        time = LIFETIME;
        Age = 0;
        tex = Ref.ResMan.LoadTexture("data/splotion.png");
        Ref.soundMan.playEffect(Ref.soundMan.AddWavSound("data/boom.wav"), 1.0f);
    }

    

    public void Update(int msec) {
        
        time -= msec;
        Age += msec;
        if(time < 0)
            time = 0;
    }

    public void Render() {
        if(time == 0)
            return;

        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
        
        float hagsRadius = Radius;
        if(Age <= EXPLODE_TIME)
            hagsRadius *= Age/EXPLODE_TIME;

        spr.Set(Position, hagsRadius, tex);

        // Fade out
        if(Age >= LIFETIME-FADEOUT_TIME) {
            color.setAlpha((int)(((LIFETIME-Age)/FADEOUT_TIME)*255));
            spr.SetColor(color);
        }
    }
}
