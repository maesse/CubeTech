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
    final static float BIG_SPEED = 100;
    final static float LITTLE_SPEED = 70;
    final static float BIG_JUMP = 2 * 270 * 45;
    final static float LITTLE_JUMP = 2 * 200 * 45;
    final static float OVERBOUNCE = 1.001f;
    final static float SV_GRAVITY = 300;
    float speed = 70f; // max speed
    float accel = 30f;
    float airaccel = 250f;
    float friction = 6f;
    float stopspeed = 15f;
    World world;
    
    // Player Info
    int lives;
    float health;
    int score;
    float energy = 0f;
    boolean bigguy = false;

    // Textures
    CubeTexture heart;
    CubeTexture healthBar;
    CubeTexture energyBar;
    CubeTexture[] runanim = new CubeTexture[10];
    CubeTexture[] jumpanim = new CubeTexture[2];
    CubeTexture[] shootanim = new CubeTexture[5];
    CubeTexture[] runanim2 = new CubeTexture[10];
    CubeTexture[] jumpanim2 = new CubeTexture[2];
    CubeTexture[] evolveanim = new CubeTexture[14];

    // Animation info
    long damageTime;
    boolean lookright = true;
    int runframe = 0;
    long nextanimTime = 0;

    // FLow control
    long jumpTime;
    boolean jumping = false;
    long weaponTime = 0;
    boolean transforming = false;
    boolean transformtobig = true;
    long dieTime = 0;
    long transformtime = 0;
    long goalTime = 0;
    boolean gameover;
    int BigGuyWeaponTime = 200;
    int LittleGuyWeaponTime = 400;
    boolean onGround = false;
    Vector2f groundNormal;
    long countTime = 0;

    public Player(World world, Vector2f spawn) {
        this.world = world;
        position = spawn;
        heart = (CubeTexture)(Ref.ResMan.LoadResource("data/heart.png").Data);
        healthBar = (CubeTexture)(Ref.ResMan.LoadResource("data/healthbar.png").Data);
        energyBar = (CubeTexture)(Ref.ResMan.LoadResource("data/energybar.png").Data);

        for (int i = 0; i < 10; i++) {
            runanim[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/ninjarun"+i+".png").Data));
            runanim2[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/funrun"+i+".png").Data));
        }
        for (int i = 0; i < 2; i++) {
            jumpanim[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/jump"+i+".png").Data));
            jumpanim2[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/funfall"+i+".png").Data));
        }
        for (int i = 0; i < 5; i++) {
            shootanim[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/shoot"+i+".png").Data));
        }
        for (int i = 0; i < 14; i++) {
            evolveanim[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/funfusion"+(19+i)+".png").Data));
        }
        ResetPlayer();
    }

    public void Goal() {
        // Add up points
        goalTime = Ref.loop.time;
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

    void NextMap() {
        // Save lives, energy and bigguy
        boolean diditwork = world.LoadNextMap(lives, energy, bigguy, score);
        if(!diditwork) {
            gameover = true;
        }
        
    }

    public void Update(int msec) {
        if(gameover)
            return;
        
//        extent = new Vector2f(5,6);
//        if(bigguy)
//            extent = new Vector2f(9,7*2);

        if(goalTime != 0) {
            if(goalTime + 1000 < Ref.loop.time) {
                // Player can spawn
                if(Ref.Input.playerInput.Mouse1 || Ref.Input.playerInput.Up ||
                        Ref.Input.playerInput.Down || Ref.Input.playerInput.Left ||
                        Ref.Input.playerInput.Right || Ref.Input.playerInput.Jump) {
                    NextMap();
                }

            }
            return;
        }

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
        if(transforming) {
            DontStuckTransform();

            // Increment animation and return
            int aniframe = 0;
            float frac = (float)(transformtime - Ref.loop.time) / 1500f;
            if(frac < 0)
                frac = 0;
            else if(frac > 1)
                frac = 1;
            frac *= 13;
            aniframe = (int)frac;
            if(transformtobig)
                aniframe = 13-aniframe;
            runframe = aniframe;

            if(transformtime < Ref.loop.time) {
                // Time to change form
                FinishTransform();
            }
            
            return;
        }

        // Handle energy
        //energy -= ((float)msec/1000f)*0.005f;
        if(bigguy) {
            energy -= ((float)msec/1000f)*0.05f;
        }
        if(energy <= 0f) {
            energy = 0f;
            if(bigguy && !transforming) {
                StartTransform(false);
            }
        }


        // Direction player wants to move
        Vector2f wishdir = new Vector2f();
        if(Ref.Input.playerInput.Left)
            wishdir.x -= 1f;
        if(Ref.Input.playerInput.Right)
            wishdir.x += 1f;
        wishdir.x *= speed; // speed;
        wishdir.y *= speed; // speed;

        
        
        // Gravity
        onGround = GroundTrace();
        if(!onGround)
            velocity.y -= SV_GRAVITY * (float)msec/1000f * 0.5f;

        // Handle jumping
        if(Ref.Input.playerInput.Up) {
            Jump(msec);
        }
        
        // Landed or jumped, reset animation
        if(jumping == onGround)
            runframe = 0;
        jumping = !onGround;

        // Move
        if(onGround) {
            velocity.y = 0;
            Friction(msec);
            WalkMove(wishdir, msec);
        } else
            AirMove(wishdir, msec);

        onGround = GroundTrace();

        
        velocity.y -= SV_GRAVITY * (float)msec/1000f * 0.5f;

        if(onGround)
            velocity.y = 0f;
        
        RunAnimation();
        HandleWeapon(msec);
    }

    // Resets the player for a new game
    void ResetPlayer() {
        extent = new Vector2f(6,8);
        velocity = new Vector2f();
        bigguy = false;
        health = 100;

        lives = 3;
        energy = 0f;
        runframe = 0;
        nextanimTime = 0;
        jumpTime = 0;
        jumping = false;
        weaponTime = 0;
        transforming = false;
        transformtobig = true;
        transformtime = 0;
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
        energy += 0.1f + Ref.rnd.nextFloat() * 0.1f;
        if(energy > 1f) {
            energy = 1f;
            if(!bigguy && !transforming)
                StartTransform(true);
        }

    }

    void StartTransform(boolean toBigGuy) {
        transforming = true;
        transformtime = Ref.loop.time + 1500; // Player gets control here
        transformtobig = toBigGuy;
        health += 50;
        if(health > 100)
            health = 100;
    }

    void FinishTransform() {
        transforming = false;
        transformtime = 0;
        bigguy = transformtobig;

        speed = bigguy?BIG_SPEED:LITTLE_SPEED;
        runframe = 0;
    }

    // Dont wanna get stuck when transforming to bigguy
    void DontStuckTransform() {
        if(!transforming || !transformtobig)
            return;

        CollisionResult coll = Ref.collision.TestPosition(position, new Vector2f(), new Vector2f(9,7*2), Collision.MASK_WORLD);
        if(coll.Hit) {
            position.y += 5f;
        }
    }

    public void HitByBullet(Bullet bullet) {
        TakeDamage(bullet.damage);
    }

    public void TakeDamage(float damage) {
        damage *= 1.5f;
        if(bigguy)
            damage /= 2f; // BigGuy only takes 50% dmg
        if(gameover || goalTime != 0)
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
        if(!transforming) {
            
            CubeTexture tex = GetAnimation(offset, size);

            if(bigguy) {
                
                spr.Set(new Vector2f(position.x-11, position.y-16-6), new Vector2f(11*2f, 18*2f), tex, offset, size);

//                spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
//                spr.Set(new Vector2f(position.x-extent.x, position.y-extent.y), new Vector2f(extent.x*2f, extent.y*2f), null, offset, size);
//                spr.Color = new Vector4f(1, 0, 0, 0.7f);
            } else {
                spr.Set(new Vector2f(position.x-8, position.y-7), new Vector2f(16f, 16f), tex, offset, size);

//                spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
//                spr.Set(new Vector2f(position.x-extent.x, position.y-extent.y), new Vector2f(extent.x*2f, extent.y*2f), null, offset, size);
//                spr.Color = new Vector4f(1, 0, 0, 0.7f);
            }
        } else {
            
                offset.x = 0.0f;
                size.x = 0.7f;
                offset.y = 0.0f;
                size.y = 0.7f;

            if(!lookright) {
                offset.x += size.x;
                //offset.y += size.y;
                size.x = -size.x;
                //size.y = -size.y;
            }
            // Custom transforming code
            CubeTexture tex = evolveanim[runframe];
            spr.Set(new Vector2f(position.x-17, position.y-27), new Vector2f(18*2f, 30*2f), tex, offset, size);
            //spr.Set(new Vector2f(position.x-11, position.y-16-6), new Vector2f(11*2f, 18*2f), tex, offset, size);
        }

        RenderFireWeapon();
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
            spr.Angle = block.getAngle();
            spr.Color = new Vector4f(1, 0, 0, 1f);
        }
    }
    
    void RenderPlayerHud() {
        // Black background
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(new Vector2f(0.05f, 0.9f), new Vector2f(0.3f, 0.05f), null, new Vector2f(), new Vector2f(1, 1));
        spr.Color = new Vector4f(0, 0, 0, 0.5f);

        // Healthbar
        if(health > 0) {
            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.Set(new Vector2f(0.055f, 0.905f), new Vector2f(0.29f * (float)health/100f, 0.042f), healthBar, new Vector2f(), new Vector2f((float)health/100, 1));
            //spr.Color = new Vector4f(0, 1, 0, 0.7f);
            if(damageTime + 300 > Ref.loop.time) {
                float frac = (float)(damageTime+300-Ref.loop.time)/300f;
                float invfrac = 1f-frac;
                spr.Color = new Vector4f(frac + invfrac, invfrac, invfrac, 0.7f);
            }
        }

        for (int i= 0; i < lives; i++) {
            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.Set(new Vector2f(0.4f + 0.045f*i, 0.925f), 0.025f, heart);
        }

        // Draw Energybar
        spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(new Vector2f(0.6f, 0.9f), new Vector2f(0.3f, 0.05f), null, new Vector2f(), new Vector2f(1, 1));
        spr.Color = new Vector4f(0, 0, 0, 0.5f);
        if(energy > 0f) {
            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.Set(new Vector2f(0.605f, 0.905f), new Vector2f(0.29f * energy, 0.042f), energyBar, new Vector2f(), new Vector2f(1, 1));
            spr.Color = new Vector4f(0, 1, 0, 0.7f);
        }

        Ref.textMan.AddText(new Vector2f(0.98f, 0.85f), "Score: " + score, Align.RIGHT);

        if(dieTime != 0) {
            Ref.textMan.AddText(new Vector2f(0.5f, 0.5f), "You died :(", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
            if(lives == 0) {
                Ref.textMan.AddText(new Vector2f(0.5f, 0.4f), "GAME OVER MAN!", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
            }
            
        } else {
            if(goalTime != 0 && !gameover) {
                Ref.textMan.AddText(new Vector2f(0.5f, 0.6f), "Level Complete", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
                Ref.textMan.AddText(new Vector2f(0.5f, 0.5f), "Click to continue", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
            } else if(gameover) {
                Ref.textMan.AddText(new Vector2f(0.5f, 0.6f), "Game Over -- You reached the end", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
                Ref.textMan.AddText(new Vector2f(0.5f, 0.5f), "But you can still play around with F8 :)", Align.CENTER,new Vector4f(1, 0.8f, 0.8f, 1));
            }
        }
    }

    void RunAnimation() {
        if(velocity.x > 3f)
            lookright = true;
        else if(velocity.x < -3f)
            lookright = false;

        if(nextanimTime < Ref.loop.time )
        {
            float invFrac = (100f - (float)Math.abs(velocity.x))/100f;
            if(invFrac < 0f)
                invFrac = 0f;
            if(invFrac > 1f)
                invFrac = 1f;
            nextanimTime = Ref.loop.time + 50 + (int)(100f*invFrac);
            runframe++;
            if(runframe >= 10)
                runframe = 0;
        }
        if((float)Math.abs(velocity.x) < 4f && runframe > 6 && !jumping) {
            runframe = 6;

            } else if(velocity.x == 0f && !jumping)
            runframe = 6;
    }

    void HandleWeapon(int msec) {
        if(!Ref.Input.playerInput.Jump)
            return;

        if(weaponTime > Ref.loop.time)
            return; // not time yet

        if(bigguy) {
            weaponTime = Ref.loop.time + BigGuyWeaponTime;
            Ref.soundMan.playEffect(Ref.soundMan.addSound("data/hurt2.wav"));
            }
        else {
            weaponTime = Ref.loop.time + LittleGuyWeaponTime;
            Ref.soundMan.playEffect(Ref.soundMan.addSound("data/hurt.wav"));
            }
        Vector2f bulletpos = new Vector2f(position.x, position.y-extent.y*0.35f);
        if(bigguy)
            bulletpos.y = position.y;
        Vector2f bulletvel = new Vector2f(150f,0);

        if(lookright)
            bulletpos.x += extent.x + 2;
        else {
            bulletpos.x -= extent.x + 2;
            bulletvel.x *= -1f;
        }

        bulletvel.x += velocity.x /4f;
       // bulletvel.y += velocity.y /2f;

        Bullet bullet = new Bullet(bulletpos, bulletvel, 10, Collision.MASK_ENEMIES | Collision.MASK_WORLD);
        world.Entities.add(bullet);
    }

    CubeTexture GetAnimation(Vector2f offset, Vector2f size) {
        CubeTexture tex;
        if(jumping) {
            if(bigguy) {
                tex = jumpanim2[runframe%2];
                if(velocity.y > 0) {
                    //offset.y = 0.25f;
                    size.x = 0.3f;
                    size.y = 0.80f;
                    offset.y = 0.1f;
                    //offset.x = 0.05f;
                    //size.y = 0.25f;
                } else {
                    size.y = 0.80f;
                    offset.y = 0.05f;
                    offset.x = 0.36f;
                    //offset.y = 0.25f;
                    size.x = 0.28f;
                    //size.y = 0.25f;
                }

            } else {
                tex = jumpanim[runframe%2];
                if(velocity.y > 0) {
                    offset.y = 0.26f;
                    size.x = 0.42f;
                    size.y = 0.24f;
                } else {
                    offset.x = 0.40f;
                    offset.y = 0.26f;
                    size.x = 0.42f;
                    size.y = 0.24f;
                }
            }

            if(!lookright) {
                offset.x += size.x;
                //offset.y += size.y;
                size.x = -size.x;
                //size.y = -size.y;
            }
        } else
        {
            if(bigguy)
                tex = runanim2[runframe];
            else
                tex = runanim[runframe];

            if(bigguy) {
                offset.x = 0.08f;
                size.x = 0.6f;
                offset.y = 0.1f;
                size.y = 0.81f;

                
            }

            if(!lookright) {
                offset.x += size.x;
                //offset.y += size.y;
                size.x = -size.x;
                //size.y = -size.y;
            }
        }
        return tex;
    }

    void Jump(int msec) {
        onGround = GroundTrace();
        if(onGround) {
            velocity.y = (float)Math.sqrt(bigguy?BIG_JUMP : LITTLE_JUMP);
            Ref.soundMan.playEffect(Ref.soundMan.addSound("data/jump.wav"));
            jumpTime = Ref.loop.time;
        } else if(jumpTime + 300 > Ref.loop.time) {
            velocity.y += 200f * (float)msec/1000f;
        }
        
    }

    void TryMove(int msec, boolean grav) {
        SlideMove(grav, msec);
//        position.x += velocity.x * (float)msec/1000f;
//        position.y += velocity.y * (float)msec/1000f;
//        Vector2f newmove = new Vector2f(velocity.x * (float)msec/1000f, velocity.y * (float)msec/1000f);
//        CollisionResult coll;
//        int mask = Collision.MASK_WORLD;
//        coll = Ref.collision.TestPosition(position, newmove, extent, mask);
//
//        if(!coll.Hit)
//            Vector2f.add(newmove, position, position);
//        else {
//            Vector2f up = new Vector2f(0, 6f);
//            boolean stepped = false;
//
////            // Only step when colliding with world
////            if(coll.hitmask == Collision.MASK_WORLD) {
////                coll = Ref.collision.TestPosition(position, up, extent, mask);
////                if(!coll.Hit && newmove.y == 0f && velocity.y == 0f) {
////                    up.x += position.x;
////                    up.y += position.y;
////                    coll = Ref.collision.TestPosition(up, newmove, extent, mask);
////                    if(!coll.Hit) {
////                        // push down
////                        stepped = true;
////                        up.x += newmove.x;
////                        up.y += newmove.y;
////                        Vector2f down = new Vector2f(0, -5f);
////                        coll = Ref.collision.TestPosition(up, down, extent, mask);
////                        if(!coll.Hit) {
////                            position.x = up.x + down.x * coll.frac;
////                            position.y = up.y + down.y * coll.frac;
////                        }
////                    }
////                }
////            }
//
//            if(!stepped) {
//                coll = Ref.collision.TestPosition(position, new Vector2f(newmove.x, 0f), extent, mask);
//                if(!coll.Hit) {
//                    Vector2f.add(new Vector2f(newmove.x, 0f), position, position);
//                    velocity.y = 0f;
//                    }
//                else if(!(coll = Ref.collision.TestPosition(position, new Vector2f(0f, newmove.y), extent, mask)).Hit) {
//                    Vector2f.add(new Vector2f(0f, newmove.y), position, position);
//                    velocity.x = 0f;
//                    }
//                else {
//                    if(coll.hitmask == Collision.MASK_WORLD) {
////                        position.x += newmove.x * coll.frac;
//                        position.y += newmove.y * coll.frac;
//                    }
//                }
//            }
//        }
        

    }

    

    void Friction(int msec) {
        float speed2 = (float)Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
        if(speed2 < 0.1f)
            return;

        
//        boolean hit = Ref.collision.TestPlayer(position, new Vector2f(position.x+(velocity.x/speed)*16f, position.y+(velocity.y/speed)*16f-40f), extent, new Vector2f());
        float fric = friction;
//        if(!hit)
//            fric *= 2f;

        float control = (speed2 < stopspeed ? stopspeed : speed2);
        float drop = control * fric * (float)msec/1000f;

        float newspeed = speed2 - drop;
        if(newspeed < 0)
            newspeed = 0;

        newspeed /= speed2;

        velocity.x *= newspeed;
        velocity.y *= newspeed;
    }

    void AirMove(Vector2f wishvel, int msec) {
        float wishspeed = (float)Math.sqrt(wishvel.x * wishvel.x + wishvel.y * wishvel.y);
        Vector2f wishdir = new Vector2f(wishvel.x, wishvel.y);
        if(wishspeed != 0)  {
            wishdir.x /= wishspeed;
            wishdir.y /= wishspeed;
        }

        if(wishspeed > speed) {
            wishvel.x *= (speed / wishspeed);
            wishvel.y *= (speed / wishspeed);
            wishspeed = speed;
        }

        AirAccelerate(wishdir, wishspeed, airaccel, msec);

//        if(onGround) {
//            ClipVelocity(velocity, groundNormal, velocity, OVERBOUNCE);
//        }
        TryMove(msec,!GroundTrace());
    }

    int SlideMove(boolean gravity, int msec) {
        int numbumps = 4;
        Vector2f pri_vel = new Vector2f(velocity.x, velocity.y);
        Vector2f planes[] = new Vector2f[5];
        Vector2f new_velocity = new Vector2f();
        float time_left = (float)msec/1000f;
        int numplanes = 0;
        int bumpcount;
        float allFraction = 0f;
        Vector2f org_vel = new Vector2f(velocity.x, velocity.y);
        
        int blocked = 0;
        for (bumpcount= 0; bumpcount < numbumps; bumpcount++) {
            // calculate position we are trying to move to
            Vector2f end = new Vector2f(time_left * velocity.x, time_left * velocity.y);

            // see if we can make it there
            CollisionResult res = Ref.collision.TestPosition(position, extent, end, Collision.MASK_WORLD);
//            if(res.Hit == true && res.frac <= Collision.EPSILON) {
//                // entity is completely trapped in another solid
//                velocity.y = 0; // don't build up falling damage, but allow sideways acceleration
//                return true;
//            }
            allFraction += res.frac;

            if(res.frac > 0.0f) {
                // actually covered some distance
                position.x += end.x * res.frac;
                position.y += end.y * res.frac;
                org_vel = new Vector2f(velocity.x, velocity.y);
                numplanes = 0;
            }

            if(res.frac == 1.0f)
                break; // moved the entire distance

            if(res.HitAxis.x > 0.7f)
                blocked |= 1; // blocked by floor
            if(res.HitAxis.x == 0.0f)
                blocked |= 2; // step/wall

            time_left -= time_left * res.frac;

            if(numplanes >= 5) {
                // this shouldn't really happen
                velocity.x = velocity.y = 0;
                break;
            }

//            //
//            // if this is the same plane we hit before, nudge velocity
//            // out along it, which fixes some epsilon issues with
//            // non-axial planes
//            //
//            int i;
//            for (i= 0; i < numplanes; i++) {
//                if(Vector2f.dot(res.HitAxis, planes[i]) > 0.99f) {
//                    Vector2f.add(res.HitAxis, velocity, velocity);
//                    break;
//                }
//            }
//            if(i < numplanes)
//                continue;

            planes[numplanes++] = res.HitAxis;
            if(numplanes == 1 && !onGround) {
                if(planes[0].x > 0.7f) {
                    // Floor or slope
                    ClipVelocity(org_vel, planes[0], new_velocity, 1f);
                    org_vel = new_velocity;
                } else
                    ClipVelocity(org_vel, planes[0], new_velocity, 1f);

                velocity = new Vector2f(new_velocity.x, new_velocity.y);
                org_vel = new_velocity;
            } else {
                int i;
                for (i= 0; i < numplanes; i++) {
                    ClipVelocity(org_vel, planes[i], velocity, 1f);
                    int j;
                    for (j= 0; j < numplanes; j++) {
                        if(j == i)
                            continue;
                        // Are we now moving against this plane?
                        if(Vector2f.dot(velocity, planes[j]) < 0f)
                            break; // not ok
                    }
                    if(j == numplanes) // didn't clip
                        break;
                }
                if(i != numplanes) {

                } else {
                    // go along the crease
                    if(numplanes != 2) {
                        velocity = new Vector2f();
                        break;
                    }
//                    Vector2f dir =
                }

                float vdiff = Vector2f.dot(velocity, pri_vel);
                if(vdiff <= 0)
                {
                    velocity = new Vector2f();
                    break;
                }

            }
            
        }
//        position = origin;

        if(allFraction == 0f)
            velocity = new Vector2f();

        return blocked;
    }

    void AirAccelerate(Vector2f wishdir, float wishspeed, float acceleration, int msec) {
        float wishspd = wishspeed;
        if(wishspd > 40f)
            wishspd = 40f;

        float currentspeed = Vector2f.dot(velocity, wishdir);
        float addspeed = wishspd - currentspeed;
        if(addspeed <= 0f)
            return;

        float accelspeed = acceleration * (float)msec/1000f * wishspeed;
        if(accelspeed > addspeed)
            accelspeed = addspeed;

        velocity.x += accelspeed * wishdir.x;
        velocity.y += accelspeed * wishdir.y;
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

        //ClipVelocity(wishdir, groundNormal, wishdir, OVERBOUNCE);
        velocity.y = 0;

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

        velocity.y = 0;
        
        float speed2 = (float)Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
        if(speed2 < 1f)
        {
            velocity.x = 0f;
            velocity.y = 0f;
            return;
        }
        

//        ClipVelocity(velocity, groundNormal, velocity, OVERBOUNCE);
//        float norm = FastMath.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
//        velocity.x /= norm;
//        velocity.y /= norm;
//        velocity.x *= speed2;
//        velocity.y *= speed2;


        boolean oldOnground = onGround;
        Vector2f moveVec = new Vector2f(velocity.x * (float)msec/1000f, velocity.y * (float)msec/1000f);
        CollisionResult res = Ref.collision.TestPosition(position, extent, moveVec, Collision.MASK_WORLD);
        if(!res.Hit) {
            position.x += moveVec.x;
            position.y += moveVec.y;
            return;
        }

        // Don't walk up stairs if not on ground.
        if(!oldOnground)
            return;

        TryMove(msec, false);


    }

    void ClipVelocity(Vector2f in, Vector2f normal, Vector2f out, float overbounce) {
        float backoff = Vector2f.dot(in, normal);
        if(backoff < 0)
            backoff *= overbounce;
        else
            backoff /= overbounce;

        float change = normal.x * backoff;
        out.x = in.x - change;
        change = normal.y * backoff;
        out.y = in.y - change;
    }

    boolean GroundTrace() {
        CollisionResult result = Ref.collision.TestPosition(position, new Vector2f(0, -2f), extent, Collision.MASK_WORLD);
        if(result.Hit) {
            groundNormal = result.HitAxis;
            if(result.frac != 0.0f)
                position.y -= 2f * result.frac;
        }
        return result.Hit;
    }

    private void RenderFireWeapon() {
        Sprite spr;
        if(weaponTime > Ref.loop.time) {
            float frac  = (float)(weaponTime - Ref.loop.time)/(float)(bigguy?BigGuyWeaponTime:LittleGuyWeaponTime);
            frac *= 4;
            int fireFrame = (int)frac;
            if(fireFrame > 4)
                fireFrame = 4;
            if(fireFrame < 0)
                fireFrame = 0;
            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            //spr.Set(position, 1f, shootanim[fi]);
            Vector2f offset2 = new Vector2f();
            Vector2f size2 = new Vector2f(1, 1);
            if(!lookright) {
                offset2.x = 1;
                size2.x = -1;
            }
            if(!bigguy)
                spr.Set(new Vector2f(position.x + (lookright?4:(-extent.x*3f-4)), position.y-extent.y*1.5f),
                    new Vector2f(16, 16), shootanim[fireFrame],offset2, size2);
            else
                spr.Set(new Vector2f(position.x + (lookright?8:(-extent.x*2f-6)), position.y-9),
                    new Vector2f(16, 16), shootanim[fireFrame],offset2, size2);
        }
    }

    

    
}
