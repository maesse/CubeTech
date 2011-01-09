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
public class EnemyCar extends EnemyBase {
    int nextThinkTime = 0;
    int killtime = 0;
    int anim = 0;
    int animTime = 0;
    float spriteAngle = 0;
    float Angle;
    int Time;
    int runTime = 0;
    Vector2f spritePosition = new Vector2f();;
    static final float KILLSCORE = 10;
    Vector2f direction = new Vector2f();
    boolean goingForward = true;
    boolean goingBackward = false;
    boolean setTime = false;

    public EnemyCar(int x, int y, float angle, int time) {
        super(new Vector2f(x, y));
        Angle=angle;
        Time=time;
    }
    
    @Override
    public void Update(int msec, Vector2f PlayerPosition) {
        if (goingForward){
            Vector2f vel = getVelocity();
            float speed = FastMath.sqrt(vel.x * vel.x + vel.y * vel.y);
            if (speed < 60){
                vel.x += FastMath.sin(Angle);
                vel.y += -FastMath.cos(Angle);
            }else if (speed >= 60 && runTime >= 0){
                if (!setTime){
                    runTime = Time;
                    setTime = true;
                }
                runTime -= msec;
            }else if (runTime <= 0){
                vel.x -= 2*FastMath.sin(Angle);
                vel.y -= -2*FastMath.cos(Angle);
                goingForward = false;
                goingBackward = true;
                setTime = false;
                runTime = 0;
            }
            
        }else if (goingBackward){
            Vector2f vel = getVelocity();
            float speed = FastMath.sqrt(vel.x * vel.x + vel.y * vel.y);
            if (speed < 60){
                vel.x -= FastMath.sin(Angle);
                vel.y -= -FastMath.cos(Angle);
            }else if (speed >= 60 && runTime >= 0){
                if (!setTime){
                    runTime = Time;
                    setTime = true;
                }
                runTime -= msec;
            }else if (runTime <= 0){
                vel.x += 2*FastMath.sin(Angle);
                vel.y += -2*FastMath.cos(Angle);
                goingForward = true;
                goingBackward = false;
                setTime = false;
                runTime = 0;
            }
        }
        

        animTime += msec;
        if (animTime >= 125){
            animTime = 0;
            if (anim < 2){
                anim += 1;
            }else{
                anim = 0;
            }
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
        CubeTexture tex = (CubeTexture)(Ref.ResMan.LoadResource("data/enemy6.png").Data);
        float size = 18;
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        Vector2f pos = getPosition();

        spritePosition.x = pos.x - size/2f;
        spritePosition.y = pos.y - size/2f;

        if (goingBackward){
            spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(1f/4f*anim, 0f), new Vector2f(1f/4f, -1f));
        }else if (goingForward){
            spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(2/4f-1f/4f*anim, 0f), new Vector2f(1f/4f, -1f));
        }
        spr.SetAngle(Angle);
    }

    @Override
    public void Die() {
        killtime = 200; // remove totally in 50 msec
        Ref.world.player.score += KILLSCORE;
    }

}
