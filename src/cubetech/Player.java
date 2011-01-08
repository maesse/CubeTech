/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.collision.Collision;
import cubetech.collision.CollisionResult;
import cubetech.entities.Bullet;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.TextManager.Align;
import cubetech.misc.Ref;
import cubetech.spatial.SpatialQuery;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.openmali.FastMath;

/**
 *
 * @author mads
 */
public final class Player {

    final static int MINTAILTIME = 32;
    // Entity
    public Vector2f position;
    public Vector2f velocity;
    public Vector2f extent;

    // Constants
    float speed = 130f; // max speed
    float accel = 8f;
    float friction = 4f;
    float stopspeed = 15f;
    World world;
    
    // Player Info
    int lives;
    float health;
    int score;
    float energy = 0f;

    // Textures
    CubeTexture heart;
    CubeTexture healthBar;
    CubeTexture energyBar;
    

    // Animation info
    long damageTime;

    // FLow control
    long dieTime = 0;
    long nextTailTime = 0;
    boolean gameover;
    long countTime = 0;
    float TimeToNextTail = 300f;
    long lastConnect = 0;
    Vector2f LastDrop = new Vector2f();


    ArrayList<CircleSplotion> splotions = new ArrayList<CircleSplotion>();
    TailPart[] Tail = new TailPart[128];
    int NextTail = 0;

    public Player(World world, Vector2f spawn) {
        this.world = world;
        position = spawn;
        heart = (CubeTexture)(Ref.ResMan.LoadResource("data/heart.png").Data);
        healthBar = (CubeTexture)(Ref.ResMan.LoadResource("data/healthbar.png").Data);
        energyBar = (CubeTexture)(Ref.ResMan.LoadResource("data/energybar.png").Data);

        for (int i= 0; i < Tail.length; i++) {
            Tail[i] = new TailPart(heart);
        }
        ResetPlayer();
    }

    void SpawnTail() {
        Tail[NextTail].Position.x = position.x;
        Tail[NextTail].Position.y = position.y;
        Tail[NextTail].SetTime(4000);
        
        NextTail++;
        if(NextTail >= Tail.length)
            NextTail = 0;
    }

    void TailConnect(int collideIndex) {
        // Get center point
        int nParts;
        if(collideIndex > NextTail)
            nParts = Tail.length - collideIndex - 2 + NextTail;
        else
            nParts = NextTail - collideIndex;

        float x = 0, y = 0;
        for (int i= 0; i < nParts; i++) {
            int tailIndex = i + collideIndex;
            if(tailIndex >= Tail.length)
                tailIndex -= Tail.length;
            x += Tail[tailIndex].Position.x/(float)nParts;
            y += Tail[tailIndex].Position.y/(float)nParts;
        }

        Vector2f center = new Vector2f(x, y);

        // Get average distance to center
        Vector2f tempDist = new Vector2f();
        float avgLenght = 0f;
        for (int i= 0; i < nParts; i++) {
            int tailIndex = i + collideIndex;
            if(tailIndex >= Tail.length)
                tailIndex -= Tail.length;
            Vector2f.sub(center, Tail[tailIndex].Position, tempDist);
            float tmplen = FastMath.sqrt(tempDist.x * tempDist.x + tempDist.y * tempDist.y);
            avgLenght += tmplen/(float)nParts;
            Tail[tailIndex].SetPopTime(i*32);
        }

        splotions.add(new CircleSplotion(center, avgLenght));
        lastConnect = Ref.loop.time;
    }

    void Respawn() {
        if(dieTime == 0)
            return;

        if(lives == 0)
            return;

        dieTime = 0;
        int oldLife = lives;
        ResetPlayer();
        lives = oldLife;
       
        world.WorldUpdated(false);

         // Set at spawn position
         for (int i= 0; i < world.NextBlockHandle; i++) {
            Block block = world.Blocks[i];
            if(block == null || block.CustomVal != 1)
                continue;

            position.x = block.getPosition().x + block.getSize().x/2f;
            position.y = block.getPosition().y + block.getSize().y/2f;
            break;
        }
    }

    public void Update(int msec) {
        if(gameover)
            return;
       

        // Handle die
        if(dieTime != 0) {
            if(dieTime < Ref.loop.time) {
                // Player can spawn
                if(Ref.Input.playerInput.Mouse1 || Ref.Input.playerInput.Up ||
                        Ref.Input.playerInput.Down || Ref.Input.playerInput.Left ||
                        Ref.Input.playerInput.Right || Ref.Input.playerInput.Jump) {
                    Respawn();
                }
                
            }

            return;
        }


        // Direction player wants to move
        Vector2f wishdir = new Vector2f();
        if(Ref.Input.playerInput.Left)
            wishdir.x -= 1f;
        if(Ref.Input.playerInput.Right)
            wishdir.x += 1f;
        if(Ref.Input.playerInput.Up)
            wishdir.y += 1f;
        if(Ref.Input.playerInput.Down)
            wishdir.y -= 1f;

        float len = (float)Math.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);
        if(len != 0f) {
            wishdir.x /= len;
            wishdir.y /= len;
        }

        wishdir.x *= speed; // speed;
        wishdir.y *= speed; // speed;

        Friction(msec);

        WalkMove(wishdir, msec);
        nextTailTime -= msec;
        if(nextTailTime < 0)
            nextTailTime = 0;

        int toRemove = -1;

        for (int i= 0; i < splotions.size(); i++) {
            splotions.get(i).Update(msec);
            if(toRemove == -1 && splotions.get(i).time == 0)
                toRemove = i;
        }

        if(toRemove != -1)
            splotions.remove(toRemove);

        // Do the tail
        Vector2f dropDist = new Vector2f();
        if(velocity.x != 0 || velocity.y != 0) {
            // Check dist to LastDrop
            Vector2f.sub(LastDrop, position, dropDist);
            float dist = FastMath.sqrt(dropDist.x * dropDist.x + dropDist.y * dropDist.y);
            if(dist > 7) {
                
            //}
            //if(nextTailTime == 0) {
//                float currspeed = (float)Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
//                float frac = currspeed/(speed/2f);
//                if(frac > 1f)
//                    frac = 1f;
//                frac = 1-frac;
                SpawnTail();
                LastDrop.x = position.x;
                LastDrop.y = position.y ;
                //nextTailTime = (long)(TimeToNextTail * frac);
                //if(nextTailTime < MINTAILTIME)
                //    nextTailTime = MINTAILTIME;
            }
        }

        boolean collided = false;
        Vector2f hags = new Vector2f();
        int collideIndex = 0;
        int minPart = NextTail - 15;
        if(minPart < 0)
            minPart += Tail.length;
        
        for (int i= 0; i < Tail.length; i++) {
            Tail[i].Update(msec); // let it fade out
            
            if(collided || Tail[i].time == 0)
                continue;

            if((NextTail-i < 10 && NextTail-i >= 0) || (NextTail-i < 0 && NextTail+Tail.length-i < 10))
                continue;

            // Check for collision
            Vector2f.sub(Tail[i].Position, position, hags);
            float lens = (float)Math.sqrt(hags.x * hags.x + hags.y * hags.y);
            if(lens < (8 + Tail[i].GetRadius())) {
                collided = true;
                collideIndex = i;
            }
        }

        if(collided && Tail[collideIndex].Age >= 500) {
            // from collideIndex to NextTail is connecting
            if(lastConnect + 1000 < Ref.loop.time)
                TailConnect(collideIndex);
        }
    }

    void WalkMove(Vector2f wishdir, int msec) {
       // normalize
       float wishspeed = (float)Math.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);
       if(wishspeed > 0) {
           wishdir.x /= wishspeed;
           wishdir.y /= wishspeed;

       }
       if(wishspeed > speed)
       {
           wishdir.x *= (speed/wishspeed);
           wishdir.y *= (speed/wishspeed);
           wishspeed = speed;
       }


       float currentSpeed = Vector2f.dot(velocity, wishdir);
       float addSpeed = wishspeed  - currentSpeed;
       if(addSpeed > 0f)
       {
           float accelspeed = accel * (float)msec/1000f * wishspeed;
           if(accelspeed > addSpeed)
               accelspeed = addSpeed;

           velocity.x += accelspeed * wishdir.x;
           velocity.y += accelspeed * wishdir.y;
       }

//       velocity.y = 0;

       float speed2 = (float)Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
       if(speed2 < 1f)
       {
           velocity.x = 0f;
           velocity.y = 0f;
           return;
       }

       position.x += velocity.x * (float)msec/1000f;
       position.y += velocity.y * (float)msec/1000f;




   }

    void Friction(int msec) {
       float speed2 = (float)Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
       if(speed2 < 0.1f)
           return;

       float fric = friction;

       float control = (speed2 < stopspeed ? stopspeed : speed2);
       float drop = control * fric * (float)msec/1000f;

       float newspeed = speed2 - drop;
       if(newspeed < 0)
           newspeed = 0;

       newspeed /= speed2;

       velocity.x *= newspeed;
       velocity.y *= newspeed;
   }

    // Resets the player for a new game
    void ResetPlayer() {
        extent = new Vector2f(6,8);
        velocity = new Vector2f();
        //bigguy = false;
        health = 100;

        lives = 3;
        energy = 0f;
    }

    // Called when something dies
    public void AddScore(int score) {
        if(this.score % 1000 > (this.score + score) % 1000)
            lives++;
        this.score+= score;
        GotKill();
        countTime = 3;
    }

    // Kills adds energy
    void GotKill() {


    }



    public void HitByBullet(Bullet bullet) {
        TakeDamage(bullet.damage);
    }

    public void TakeDamage(float damage) {
        damage *= 1.5f;
       //if(bigguy)
       //     damage /= 2f; // BigGuy only takes 50% dmg
        if(gameover)
            return; // Cant take dmg when not playing
        damageTime = Ref.loop.time;
        this.health -= damage;
        if(health <= 0f)
            Die();
    }

    void Die() {
        // Implement
        if(dieTime == 0) {
            dieTime = Ref.loop.time + 1000; // When to start again
            lives--;
            }
    }

    public void Render() {
        for (int i= 0; i < splotions.size(); i++) {
            splotions.get(i).Render();
        }

        for (int i= 0; i < Tail.length; i++) {
            int index = NextTail + i + 1;
            if(index >= Tail.length)
                index -= Tail.length;
            Tail[index].Render();
        }




        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        Vector2f offset = new Vector2f(0.01f,0.01f);
        Vector2f size = new Vector2f(0.98f,0.98f);
        spr.Set(new Vector2f(position.x-8, position.y-7), new Vector2f(16f, 16f), null, offset, size);
        spr.SetColor(new Vector4f(1, 0, 0, 1));


        //RenderFireWeapon();
        RenderPlayerHud();

       // RenderSpatialDebug();
    }

    
    void RenderSpatialDebug() {
        SpatialQuery result = Ref.spatial.Query(position.x - extent.x, position.y - extent.y, position.x + extent.x, position.y + extent.y);
        int queryNum = result.getQueryNum();
        Object object;
        while((object = result.ReadNext()) != null) {
            if(object.getClass() != Block.class)
                continue;
            Block block = (Block)object;
            if(block.LastQueryNum == queryNum)
                continue; // duplicate
            block.LastQueryNum = queryNum;

            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            spr.Set(block.getPosition(), block.getSize(), block.Texture, block.TexOffset, block.TexSize);
            spr.SetAngle(block.getAngle());
            //spr.Angle = block.getAngle();
            spr.SetColor(new Vector4f(1, 0, 0, 1f));
            //spr.Color = new Vector4f(1, 0, 0, 1f);
        }
    }
    
    void RenderPlayerHud() {
        // Black background
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(new Vector2f(0.05f, 0.9f), new Vector2f(0.3f, 0.05f), null, new Vector2f(), new Vector2f(1, 1));
        spr.SetColor(new Vector4f(0, 0, 0, 0.5f));
        //spr.Color = new Vector4f(0, 0, 0, 0.5f);



        for (int i= 0; i < lives; i++) {
            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.Set(new Vector2f(0.4f + 0.045f*i, 0.925f), 0.025f, heart);
        }

        // Draw Energybar
//        spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
//        spr.Set(new Vector2f(0.6f, 0.9f), new Vector2f(0.3f, 0.05f), null, new Vector2f(), new Vector2f(1, 1));
//        spr.SetColor(new Vector4f(0, 0, 0, 0.5f));
//        //spr.Color = new Vector4f(0, 0, 0, 0.5f);
//        if(energy > 0f) {
//            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
//            spr.Set(new Vector2f(0.605f, 0.905f), new Vector2f(0.29f * energy, 0.042f), energyBar, new Vector2f(), new Vector2f(1, 1));
//            spr.SetColor(new Vector4f(0, 1, 0, 0.7f));
//            //spr.Color = new Vector4f(0, 1, 0, 0.7f);
//        }

        Ref.textMan.AddText(new Vector2f(0.98f, 0.85f), "Score: " + score, Align.RIGHT);

        if(dieTime != 0) {
            Ref.textMan.AddText(new Vector2f(0.5f, 0.5f), "You died :(", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
            if(lives == 0) {
                Ref.textMan.AddText(new Vector2f(0.5f, 0.4f), "GAME OVER MAN!", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
            }
            
        } else {
//            if(goalTime != 0 && !gameover) {
//                Ref.textMan.AddText(new Vector2f(0.5f, 0.6f), "Level Complete", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
//                Ref.textMan.AddText(new Vector2f(0.5f, 0.5f), "Click to continue", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
//            } else
                if(gameover) {
                Ref.textMan.AddText(new Vector2f(0.5f, 0.6f), "Game Over -- You reached the end", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
                Ref.textMan.AddText(new Vector2f(0.5f, 0.5f), "But you can still play around with F8 :)", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
            }
        }
    }

    

    
    

//    private void RenderFireWeapon() {
//        Sprite spr;
//        if(weaponTime > Ref.loop.time) {
//            float frac  = (float)(weaponTime - Ref.loop.time)/(float)(bigguy?BigGuyWeaponTime:LittleGuyWeaponTime);
//            frac *= 4;
//            int fireFrame = (int)frac;
//            if(fireFrame > 4)
//                fireFrame = 4;
//            if(fireFrame < 0)
//                fireFrame = 0;
//            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
//            //spr.Set(position, 1f, shootanim[fi]);
//            Vector2f offset2 = new Vector2f();
//            Vector2f size2 = new Vector2f(1, 1);
//            if(!lookright) {
//                offset2.x = 1;
//                size2.x = -1;
//            }
//            if(!bigguy)
//                spr.Set(new Vector2f(position.x + (lookright?4:(-extent.x*3f-4)), position.y-extent.y*1.5f),
//                    new Vector2f(16, 16), shootanim[fireFrame],offset2, size2);
//            else
//                spr.Set(new Vector2f(position.x + (lookright?8:(-extent.x*2f-6)), position.y-9),
//                    new Vector2f(16, 16), shootanim[fireFrame],offset2, size2);
//        }
//    }

    

    
}
