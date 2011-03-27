/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class Cloud {
    public Vector2f position = new Vector2f();
    public float radius = 20;
    public int layer = -75;
    public float angle = 0f;
    public Vector4f color = new Vector4f(1, 1, 1, 0.5f);
    public float AngularVelocity = 0f;
    public Vector2f Velocity = new Vector2f();
    public float lifetime = 10;
    public float width = 1f;
    public float widthvelo = 0f;



    public Cloud(Vector2f spawn) {
        position(spawn);
    }

    public boolean isOutOfView(float minx, float avgy) {
        return (position.x + radius * width + 1 < minx || Math.abs(position.y - avgy) > Ref.cgame.cg.refdef.h || lifetime < 0f);
    }

    public void position(Vector2f spawnArea) {
        lifetime = 10;
        angle = 0f;
        Velocity.x = -Ref.rnd.nextFloat() * 50;
        Velocity.y = (Ref.rnd.nextFloat() - 0.5f) * 20;
        float angleFrac = Ref.rnd.nextFloat();
        AngularVelocity = (angleFrac - 0.5f) * 0.4f;

        width = 1f + Ref.rnd.nextFloat() * (1f- angleFrac);
        widthvelo = (Ref.rnd.nextFloat()-0.5f) * 10f;


        float rnd = Ref.rnd.nextFloat()-0.5f;
        float maxRndPosition = 50;
        position.x = spawnArea.x + rnd * maxRndPosition;
        rnd = Ref.rnd.nextFloat();
        if(Ref.cgame.cg != null)
            maxRndPosition = Ref.cgame.cg.refdef.h*2;
        position.y = spawnArea.y + (rnd-0.5f) * (maxRndPosition);

        float maxRadius = 200, minRadius = 30;
        rnd = Ref.rnd.nextFloat();
        radius = minRadius + (maxRadius - minRadius) * rnd;
        position.x += radius;

        int iRnd = Ref.rnd.nextInt(30)-15;
        layer = -60 - iRnd;
//        layer = -20;

        color.x = color.y = color.z = 255;
        color.w = Ref.rnd.nextInt(128);
        if(color.w < 60)
            color.w = 60;
    }

    public void Render(float frametime) {
        lifetime -= frametime;

        angle += AngularVelocity * frametime;
        width += widthvelo * frametime;
        if(width < radius * 0.5f)
            width = radius * 0.5f;
        position.x += frametime * Velocity.x;
        position.y += frametime * Velocity.y;
        
        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);



        spr.Set(position, new Vector2f(radius+ width, radius), Ref.ResMan.LoadTexture("data/cloud2.png"), new Vector2f(0,0),new Vector2f(1,1));
        float alpha = (int)color.w;
        float deathtime = 3f;
        if(lifetime < deathtime) {
            alpha *= lifetime/deathtime;
            if(alpha < 0f)
                alpha = 0f;
        }
        spr.SetColor((int)color.x, (int)color.y, (int)color.z, (int)alpha);
        //spr.SetColor(255, 255, 255, 255);
        spr.SetDepth(layer);
        spr.SetAngle(angle);
    }
}
