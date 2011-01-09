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
public class EnemyCrap extends EnemyBase {
    static final float KILLSCORE = 3;
    int nextThinkTime = 0;
    int killtime = 0;
    int anim = 0;
    int animTime = 0;
    float spriteAngle = 0;
    Vector2f spritePosition = new Vector2f();;
    Vector2f direction = new Vector2f();

    public EnemyCrap(int x, int y) {
        super(new Vector2f(x, y));
    }
    
    @Override
    public void Update(int msec, Vector2f PlayerPosition) {
        // dont get too clever now!
        nextThinkTime -= msec;
        if(nextThinkTime <= 0) {
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
            wishdir.x *= 25+(Ref.rnd.nextFloat()-0.5f)*30;
            wishdir.y *= 25+(Ref.rnd.nextFloat()-0.5f)*30;
            setVelocity(wishdir);

            nextThinkTime = 550; // dumb fuck
        }

        animTime += msec;
        if (animTime >= 150){
            animTime = 0;
            if (anim < 3){
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
        Vector2f currPos = getPosition();
        Vector2f currVel = getVelocity();
        currPos.x += currVel.x * (float)msec/1000f;
        currPos.y += currVel.y * (float)msec/1000f;

        if(killtime > 0) {
            killtime -= msec;
            if(killtime <= 0)
                RemoveMe = true;
        }
    }

    @Override
    public void Render() {
        CubeTexture tex = (CubeTexture)(Ref.ResMan.LoadResource("data/enemy1.png").Data);
        float size = 12;
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        Vector2f pos = getPosition();

        spritePosition.x = pos.x - size/2f;
        spritePosition.y = pos.y - size/2f;

                
        spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(1f/4f*anim, 0f), new Vector2f(1f/4f, 1f));

        spr.SetAngle(spriteAngle);
    }

    @Override
    public void Die() {
        killtime = 200; // remove totally in 50 msec
        Ref.world.player.score += KILLSCORE;
    }

}
