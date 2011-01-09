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
public class EnemyBlaster extends EnemyBase {
    static final float KILLSCORE = 8;
    int nextThinkTime = 0;
    int killtime = 0;
    int anim = 0;
    int animTime = 0;
    int boostTime = 0;
    int boost = 0; // 0 = no move , 1 = small vel , 2 = high vel

    float spriteAngle = 0;
    float anglePlayer = 0;
    float lastAnglePlayer = 0;
    float spriteAngleChange = 0;
    boolean startBoost = false;
    Vector2f spritePosition = new Vector2f();;
    Vector2f direction = new Vector2f();
    boolean boostSet = false;
    Vector2f wishdir = new Vector2f();
    Vector2f vectorAdd = new Vector2f();
    public EnemyBlaster(int x, int y) {
        super(new Vector2f(x, y));
    }

    @Override
    public void Update(int msec, Vector2f PlayerPosition) {

        Vector2f vel = getVelocity();
        
        float speed = FastMath.sqrt(vel.x * vel.x + vel.y * vel.y);
       // System.out.println("SPEED: "+speed);
        if (boost > 0){
            if (speed > 35 && boost < 3){
                boost = 2;
            }
            if (!boostSet){
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
                // Set speed in units per second
                wishdir.x *= 0.5f;
                wishdir.y *= 0.5f;
                boostSet = true;
            }else{
                if (boost < 3 && speed < 200){
                    if (speed < 50){
                        wishdir.x *= 1.15f;
                        wishdir.y *= 1.15f;
                    }else if (speed >= 50){
                        wishdir.x *= 1.06f;
                        wishdir.y *= 1.06f;
                    }
                    Vector2f.add(vel, wishdir, vectorAdd);
                    setVelocity(vectorAdd);
                }else if (boost == 3){
                    vectorAdd.x *= 0.95f;
                    vectorAdd.y *= 0.95f;
                    setVelocity(vectorAdd);
                }

            }




            
            

            
        }else if (boost == 0){
        

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

            if (Math.abs(spriteAngleChange) < 0.019){
                startBoost = true;
            }

            if (!startBoost){
                animTime += msec;
                if (animTime >= 150){
                    animTime = 0;
                    if (anim < 2){
                        anim += 1;
                    }else{
                        anim = 0;
                    }
                }
            }else{
                animTime += msec;
                boostTime += msec;
                if (boostTime < 550){
                    if (animTime >= 75){
                        animTime = 0;
                        if (anim < 2){
                            anim += 1;
                        }else{
                            anim = 0;
                        }
                    }
                }else if (boostTime >= 650 && boostTime < 850){
                    if (animTime >= 50){
                        animTime = 0;
                        if (anim < 2){
                            anim += 1;
                        }else{
                            anim = 0;
                        }
                    }
                }else if (boostTime >= 850 && boostTime < 1000){
                    if (animTime >= 30){
                        animTime = 0;
                        if (anim < 2){
                            anim += 1;
                        }else{
                            anim = 0;
                        }
                    }
                }else if (boostTime >= 1000){
                    boost = 1;
                }
                }
                lastAnglePlayer = anglePlayer;

                //System.out.println(anglePlayer+"   "+spriteAngle);
            }

        if (boost == 2){
            boostTime += msec;
            //System.out.println(boostTime+"");
            if (boostTime > 2000){
                boost = 3;
            }
        }

        if (boost == 3 && speed < 0.5){
            vectorAdd = new Vector2f(0,0);
            setVelocity(vectorAdd);
            boost = 0;
            boostTime = 0;
            animTime = 0;
            speed = 0;
            boostSet = false;
            startBoost = false;
        }

        //System.out.println(boost);

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
        CubeTexture tex = (CubeTexture)(Ref.ResMan.LoadResource("data/enemy2.png").Data);

        float size = 16f;
        if (boost == 0){
            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            Vector2f pos = getPosition();
            spritePosition.x = pos.x - size/2f;
            spritePosition.y = pos.y - size/2f;
            spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(1f/8f*anim, 0f), new Vector2f(1f/8f, -1f));
            spr.SetAngle(spriteAngle);
        }else if (boost == 1 || boost == 3){
                Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
                Vector2f pos = getPosition();
                spritePosition.x = pos.x - size/2f;
                spritePosition.y = pos.y - size/2f;
                spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(3f/8f, 0f), new Vector2f(1f/8f, -1f));
                spr.SetAngle(spriteAngle);
        }else if (boost == 2){
                Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
                Vector2f pos = getPosition();
                spritePosition.x = pos.x - size/2f;
                spritePosition.y = pos.y - size/2f;
                spr.Set(spritePosition, new Vector2f(size, size), tex, new Vector2f(4f/8f, 0f), new Vector2f(1f/8f, -1f));
                spr.SetAngle(spriteAngle);
            }
        }
    

    @Override
    public void Die() {
        killtime = 200; // remove totally in 50 msec
        Ref.world.player.score += KILLSCORE;
    }

}
