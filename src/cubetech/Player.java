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
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.openmali.FastMath;

/**
 *
 * @author mads
 */
public final class Player {
    // Entity
    public Vector2f position;
    public Vector2f velocity;
    public Vector2f extent;

    // Constants
    final static float LITTLE_SPEED = 70;
    float speed = 70f; // max speed
    float accel = 30f;
    float friction = 6f;
    float stopspeed = 15f;
    World world;
    
    // Player Info
    int lives;
    float health;
    int score;
    float energy = 0f;
//    boolean bigguy = false;

    // Textures
    CubeTexture heart;
    CubeTexture healthBar;
    CubeTexture energyBar;
    //CubeTexture[] runanim = new CubeTexture[10];
    //CubeTexture[] jumpanim = new CubeTexture[2];
    //CubeTexture[] shootanim = new CubeTexture[5];
    //CubeTexture[] runanim2 = new CubeTexture[10];
    //CubeTexture[] jumpanim2 = new CubeTexture[2];
    //CubeTexture[] evolveanim = new CubeTexture[14];

    // Animation info
    long damageTime;
    //boolean lookright = true;
    //int runframe = 0;
    //long nextanimTime = 0;

    // FLow control
    //long jumpTime;
    //boolean jumping = false;
    //long weaponTime = 0;
    //boolean transforming = false;
    //boolean transformtobig = true;
    long dieTime = 0;
    //long transformtime = 0;
    //long goalTime = 0;
    boolean gameover;
    //int BigGuyWeaponTime = 200;
    //int LittleGuyWeaponTime = 400;
    //boolean onGround = false;
    //Vector2f groundNormal;
    long countTime = 0;

    public Player(World world, Vector2f spawn) {
        this.world = world;
        position = spawn;
        heart = (CubeTexture)(Ref.ResMan.LoadResource("data/heart.png").Data);
        healthBar = (CubeTexture)(Ref.ResMan.LoadResource("data/healthbar.png").Data);
        energyBar = (CubeTexture)(Ref.ResMan.LoadResource("data/energybar.png").Data);

//        for (int i = 0; i < 10; i++) {
//            runanim[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/ninjarun"+i+".png").Data));
//            runanim2[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/funrun"+i+".png").Data));
//        }
//        for (int i = 0; i < 2; i++) {
//            jumpanim[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/jump"+i+".png").Data));
//            jumpanim2[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/funfall"+i+".png").Data));
//        }
//        for (int i = 0; i < 5; i++) {
//            shootanim[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/shoot"+i+".png").Data));
//        }
//        for (int i = 0; i < 14; i++) {
//            evolveanim[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/funfusion"+(19+i)+".png").Data));
//        }
        ResetPlayer();
    }

//    public void Goal() {
//        // Add up points
//        goalTime = Ref.loop.time;
//    }

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

//    void NextMap() {
//        // Save lives, energy and bigguy
//        boolean diditwork = world.LoadNextMap(lives, energy, bigguy, score);
//        if(!diditwork) {
//            gameover = true;
//        }
//
//    }

    public void Update(int msec) {
        if(gameover)
            return;
        
//        extent = new Vector2f(5,6);
//        if(bigguy)
//            extent = new Vector2f(9,7*2);

//        if(goalTime != 0) {
//            if(goalTime + 1000 < Ref.loop.time) {
//                // Player can spawn
//                if(Ref.Input.playerInput.Mouse1 || Ref.Input.playerInput.Up ||
//                        Ref.Input.playerInput.Down || Ref.Input.playerInput.Left ||
//                        Ref.Input.playerInput.Right || Ref.Input.playerInput.Jump) {
//                    NextMap();
//                }
//
//            }
//            return;
//        }

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

        // Handle transform
//        if(transforming) {
//            DontStuckTransform();
//
//            // Increment animation and return
//            int aniframe = 0;
//            float frac = (float)(transformtime - Ref.loop.time) / 1500f;
//            if(frac < 0)
//                frac = 0;
//            else if(frac > 1)
//                frac = 1;
//            frac *= 13;
//            aniframe = (int)frac;
//            if(transformtobig)
//                aniframe = 13-aniframe;
//            runframe = aniframe;
//
//            if(transformtime < Ref.loop.time) {
//                // Time to change form
//                FinishTransform();
//            }
//
//            return;
//        }

        // Handle energy
        //energy -= ((float)msec/1000f)*0.005f;
//        if(bigguy) {
//            energy -= ((float)msec/1000f)*0.05f;
//        }
//        if(energy <= 0f) {
//            energy = 0f;
//            if(bigguy && !transforming) {
//                StartTransform(false);
//            }
//        }


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
        wishdir.x *= speed; // speed;
        wishdir.y *= speed; // speed;

        

       // Friction(msec);
       
    }

    // Resets the player for a new game
    void ResetPlayer() {
        extent = new Vector2f(6,8);
        velocity = new Vector2f();
        //bigguy = false;
        health = 100;

        lives = 3;
        energy = 0f;
//        runframe = 0;
//        nextanimTime = 0;
//        jumpTime = 0;
//        jumping = false;
//        weaponTime = 0;
//        transforming = false;
//        transformtobig = true;
//        transformtime = 0;
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
//        energy += 0.1f + Ref.rnd.nextFloat() * 0.1f;
//        if(energy > 1f) {
//            energy = 1f;
//            if(!bigguy && !transforming)
//                StartTransform(true);
//        }

    }

//    void StartTransform(boolean toBigGuy) {
//        transforming = true;
//        transformtime = Ref.loop.time + 1500; // Player gets control here
//        transformtobig = toBigGuy;
//        health += 50;
//        if(health > 100)
//            health = 100;
//    }
//
//    void FinishTransform() {
//        transforming = false;
//        transformtime = 0;
//        bigguy = transformtobig;
//
//        speed = bigguy?BIG_SPEED:LITTLE_SPEED;
//        runframe = 0;
//    }

    // Dont wanna get stuck when transforming to bigguy
//    void DontStuckTransform() {
//        if(!transforming || !transformtobig)
//            return;
//
//        CollisionResult coll = Ref.collision.TestPosition(position, new Vector2f(), new Vector2f(9,7*2), Collision.MASK_WORLD);
//        if(coll.Hit) {
//            position.y += 5f;
//        }
//    }

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
        if(countTime != 0) {
            countTime--;
            Ref.soundMan.playEffect(Ref.soundMan.addSound("data/coin.wav"));

        }
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        Vector2f offset = new Vector2f(0.01f,0.01f);
        Vector2f size = new Vector2f(0.98f,0.98f);
        spr.Set(new Vector2f(position.x-8, position.y-7), new Vector2f(16f, 16f), null, offset, size);
        spr.SetColor(new Vector4f(1, 0, 0, 1));
//        if(!transforming) {
//
//            CubeTexture tex = GetAnimation(offset, size);
//
//            if(bigguy) {
//
//                spr.Set(new Vector2f(position.x-11, position.y-16-6), new Vector2f(11*2f, 18*2f), tex, offset, size);
//
////                spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
////                spr.Set(new Vector2f(position.x-extent.x, position.y-extent.y), new Vector2f(extent.x*2f, extent.y*2f), null, offset, size);
////                spr.Color = new Vector4f(1, 0, 0, 0.7f);
//            } else {
//
//
////                spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
////                spr.Set(new Vector2f(position.x-extent.x, position.y-extent.y), new Vector2f(extent.x*2f, extent.y*2f), null, offset, size);
////                spr.Color = new Vector4f(1, 0, 0, 0.7f);
//            }
//        } else {
//
//                offset.x = 0.0f;
//                size.x = 0.7f;
//                offset.y = 0.0f;
//                size.y = 0.7f;
//
//            if(!lookright) {
//                offset.x += size.x;
//                //offset.y += size.y;
//                size.x = -size.x;
//                //size.y = -size.y;
//            }
//            // Custom transforming code
//            CubeTexture tex = evolveanim[runframe];
//            spr.Set(new Vector2f(position.x-17, position.y-27), new Vector2f(18*2f, 30*2f), tex, offset, size);
//            //spr.Set(new Vector2f(position.x-11, position.y-16-6), new Vector2f(11*2f, 18*2f), tex, offset, size);
//        }

        //RenderFireWeapon();
        RenderPlayerHud();

        RenderSpatialDebug();
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

        // Healthbar
//        if(health > 0) {
//            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
//            spr.Set(new Vector2f(0.055f, 0.905f), new Vector2f(0.29f * (float)health/100f, 0.042f), healthBar, new Vector2f(), new Vector2f((float)health/100, 1));
//            //spr.Color = new Vector4f(0, 1, 0, 0.7f);
//            if(damageTime + 300 > Ref.loop.time) {
//                float frac = (float)(damageTime+300-Ref.loop.time)/300f;
//                float invfrac = 1f-frac;
//                spr.SetColor(new Vector4f(frac + invfrac, invfrac, invfrac, 0.7f));
//                //spr.Color = new Vector4f(frac + invfrac, invfrac, invfrac, 0.7f);
//            }
//        }

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
