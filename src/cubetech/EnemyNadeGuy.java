/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;
import org.openmali.FastMath;

/**
 *
 * @author mads
 */
public class EnemyNadeGuy extends EnemyBase {
    static final float KILLSCORE = 20;
    int nextThinkTime = 0;
    int killtime = 0;
    int anim = 0;
    int animTime = 0;
    float spriteAngle = 0;
    Vector2f spritePosition = new Vector2f();;
    Vector2f direction = new Vector2f();
    Vector2f grenadePosition = new Vector2f();;
    Vector2f grenadeDirection = new Vector2f();
    int theThrowTime = 0;
    int throwTime = 0;
    int grenadeTime = 0;
    int grenadeAnimTime = 0;
    boolean throwGrenade = false;
    boolean grenadeInAir = false;
    float grenadeStep = 0;

    public EnemyNadeGuy(int x, int y) {
        super(new Vector2f(x, y));
    }
    
    @Override
    public void Update(int msec, Vector2f PlayerPosition) {
        // dont get too clever now!
        nextThinkTime -= msec;
        if(nextThinkTime <= 0 && throwTime <= 0) {
            // the direction the enemy wishes to move
            Vector2f wishdir = new Vector2f(); 
            Vector2f.sub(PlayerPosition, getPosition(), wishdir);

            // Normalize
            float len = FastMath.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);
            if(len != 0) {
                    wishdir.x /= len;
                    wishdir.y /= len;
            }

            // Set speed in units per second
            wishdir.x *= 30+(Ref.rnd.nextFloat()-0.5f)*5;
            wishdir.y *= 30+(Ref.rnd.nextFloat()-0.5f)*5;
            setVelocity(wishdir);
            nextThinkTime = 350;
            if (len < 50){
                theThrowTime = (int)(1300+400*Ref.rnd.nextFloat());
                throwTime = theThrowTime;
            }
        }

        animTime += msec;
        if (animTime >= 125){
            animTime = 0;
            if (anim < 5){
                anim += 1;
            }else{
                anim = 0;
            }
        }



        Vector2f.sub(PlayerPosition, getPosition(), direction);

        if (direction.y < 0){
            spriteAngle=-(float)Math.atan((direction.x/direction.y));
        }else{
            spriteAngle=-(3.14f+(float)Math.atan((direction.x/direction.y)));
        }

        // apply velocity
        if (throwTime <= 0){
            Vector2f currPos = getPosition();
            Vector2f currVel = getVelocity();
            currPos.x += currVel.x * (float)msec/1000f;
            currPos.y += currVel.y * (float)msec/1000f;
        }
        if (throwGrenade){
            float len = FastMath.sqrt(direction.x * direction.x + direction.y * direction.y);
            if(len != 0) {
                    direction.x /= len;
                    direction.y /= len;
            }
            grenadeDirection.x = direction.x;
            grenadeDirection.y = direction.y;
            Vector2f pos = getPosition();
            grenadePosition.x = pos.x-12.5f+grenadeDirection.x*10f;
            grenadePosition.y = pos.y-12.5f+grenadeDirection.y*10f;

            throwGrenade = false;
        }
        if (grenadeInAir){
            grenadeTime -= msec;
            if (grenadeTime >= 200){
                grenadePosition.x += grenadeDirection.x*msec*0.05f;
                grenadePosition.y += grenadeDirection.y*msec*0.05f;
                grenadeAnimTime += msec;

                if (grenadeAnimTime >= 35){
                    grenadeAnimTime = 0;
                    grenadeStep += 1;
                }
            }
            if (grenadeTime <= 0){
                grenadeInAir = false;
                //BAAANG DØD!!!!! BAAANG DØD!!!!! BAAANG DØD!!!!! BAAANG DØD!!!!!
                //BAAANG DØD!!!!! BAAANG DØD!!!!! BAAANG DØD!!!!! BAAANG DØD!!!!!
                //BAAANG DØD!!!!! BAAANG DØD!!!!! BAAANG DØD!!!!! BAAANG DØD!!!!! 
            }
        }
        if(throwTime > 0) {
            throwTime -= msec;
            if(throwTime <= theThrowTime-450 && !grenadeInAir && throwTime > 500){
                throwGrenade = true;
                grenadeInAir = true;
                grenadeTime = theThrowTime-500;
            }
        }
        if(killtime > 0) {
            killtime -= msec;
            if(killtime <= 0)
                RemoveMe = true;
        }
    }

    @Override
    public void Render() {
        CubeTexture tex = (CubeTexture)(Ref.ResMan.LoadResource("data/enemy4.png").Data);
        CubeTexture nadetex = (CubeTexture)(Ref.ResMan.LoadResource("data/grenadeexplosion.png").Data);
        float size = 25;
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        Vector2f pos = getPosition();

        spritePosition.x = pos.x - size/2f;
        spritePosition.y = pos.y - size/2f;
        if(throwTime <= 0){
        spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(1f/16f*anim, 0f), new Vector2f(1f/16f, 1f));
        }else if(throwTime > theThrowTime-150){
            spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(1f/16f*6, 0f), new Vector2f(1f/16f, 1f));
        }else if(throwTime > theThrowTime-300){
            spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(1f/16f*7, 0f), new Vector2f(1f/16f, 1f));
        }else if(throwTime > theThrowTime-450){
            spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(1f/16f*8, 0f), new Vector2f(1f/16f, 1f));
        }else if(throwTime <= theThrowTime-450){
            spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(1f/16f*9, 0f), new Vector2f(1f/16f, 1f));
        }
        spr.SetAngle(spriteAngle);
        if (grenadeInAir){
            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            if (grenadeTime > 200){
                spr.Set(grenadePosition, new Vector2f(size, size), tex, new Vector2f(1f/16f*10, 0f), new Vector2f(1f/16f, 1f));
                spr.SetAngle((1f/8f)*2f*3.14f*grenadeStep);
            }else if (grenadeTime <= 200 && grenadeTime >= 125){
                spr.Set(grenadePosition, new Vector2f(size, size), nadetex, new Vector2f(0f, 0f), new Vector2f(1f/2f, 1f));
            }else if (grenadeTime < 125 && grenadeTime >= 50){
                spr.Set(grenadePosition, new Vector2f(size, size), nadetex, new Vector2f(1/2f, 0f), new Vector2f(1f/2f, 1f));
            }else if (grenadeTime < 50){
                spr.Set(grenadePosition, new Vector2f(size, size), nadetex, new Vector2f(0f, 0f), new Vector2f(1f/2f, 1f));
            }
        }
    }

    @Override
    public void Die() {
        killtime = 200; // remove totally in 50 msec
        Ref.world.player.score += KILLSCORE;
    }

}
