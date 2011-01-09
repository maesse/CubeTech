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
public class EnemyPlane extends EnemyBase {
    static final float KILLSCORE = 4;
    int nextThinkTime = 0;
    int killtime = 0;
    int anim = 0;
    int animTime = 0;

    float spriteAngle = 0;
    float anglePlayer = 0;
    float lastAnglePlayer = 0;
    float spriteAngleChange = 0;
    Vector2f spritePosition = new Vector2f();;
    Vector2f direction = new Vector2f();
    Vector2f wishdir = new Vector2f();
    Vector2f vectorAdd = new Vector2f();
    public EnemyPlane(int x, int y) {
        super(new Vector2f(x, y));
    }

    @Override
    public void Update(int msec, Vector2f PlayerPosition) {

        Vector2f vel = getVelocity();
        
        float speed = FastMath.sqrt(vel.x * vel.x + vel.y * vel.y);
        if (speed > 80){
            vel.x *= 0.9f;
            vel.y *= 0.9f;
        }
        nextThinkTime -= msec;
        if(nextThinkTime <= 0) {

            // the direction the enemy wishes to move
            Vector2f.sub(PlayerPosition, getPosition(), wishdir);
            if (wishdir.y < 0){
                spriteAngle=-(float)Math.atan((wishdir.x/wishdir.y));
            }else{
                spriteAngle=-(3.14f+(float)Math.atan((wishdir.x/wishdir.y)));
            }
            // Normalize
            float len = FastMath.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);
            if(len != 0) {
                wishdir.x /= len;
                wishdir.y /= len;
            }
            nextThinkTime = 60;
        }
        


            
        Vector2f.add(vel, wishdir, vectorAdd);
        //System.out.println(vectorAdd+"");
        
        setVelocity(vectorAdd);

        Vector2f.sub(PlayerPosition, getPosition(), direction);

        if (direction.y < 0){
            anglePlayer=-(float)Math.atan((direction.x/direction.y));
        }else{
            anglePlayer=-(3.14f+(float)Math.atan((direction.x/direction.y)));
        }
        if (anglePlayer < 0){
            anglePlayer += 2f*3.14f;
        }
        if (anglePlayer > 3.14f){
            anglePlayer -= 2f*3.14f;
        }

        if (lastAnglePlayer > 0 && anglePlayer < 0 || lastAnglePlayer < 0 && anglePlayer > 0 && Math.abs(anglePlayer) > 0.5f*3.14f){
            if (spriteAngle > 0){
                spriteAngle -= 2f*3.14f;
            }else{
                spriteAngle += 2f*3.14f;
            }
        }

        spriteAngleChange = anglePlayer-spriteAngle;

        if (spriteAngleChange > 0.02){
            spriteAngleChange = 0.02f;
        }else if (spriteAngleChange < -0.02){
            spriteAngleChange = -0.02f;
        }
        spriteAngle += spriteAngleChange;


        lastAnglePlayer = anglePlayer;

            

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
        animTime += msec;
        if (animTime >= 150){
            animTime = 0;
            if (anim < 2){
                anim += 1;
            }else{
                anim = 0;
            }
        }
    }

    @Override
    public void Render() {
        CubeTexture tex = (CubeTexture)(Ref.ResMan.LoadResource("data/enemy3.png").Data);

        float size = 12f;
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
