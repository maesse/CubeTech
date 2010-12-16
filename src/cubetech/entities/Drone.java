/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.entities;

import cubetech.collision.Collision;
import cubetech.collision.CollisionResult;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class Drone implements Entity {
    public Vector2f Position;
    public Vector2f Size = new Vector2f(6, 8);
    public Vector2f Velocity = new Vector2f();
    public int Hitmask = Collision.MASK_ALL;
    public int damage = 10;
    public boolean ToRemove = false;

    float speed = 45f; // max speed
    float accel = 30f;
    float airaccel = 10f;
    float friction = 6f;
    float stopspeed = 15f;

    boolean lookright = true;
    boolean jumping = false;
    int runframe = 0;
    long nextanimTime = 0;
    long jumpTime;
    long damageTime = 0;
    int health = 35;
    int DroneScore = 60;
    long suicideTime = 0;

    CubeTexture[] walkAnim = new CubeTexture[3];
    CubeTexture[] explode = new CubeTexture[10];

    long weaponTime = 0;

    public Drone(Vector2f spawn) {
        for (int i = 0; i < walkAnim.length; i++) {
            walkAnim[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/dronehover"+i+".png").Data));
        }
        for (int i = 0; i < explode.length; i++) {
            explode[i] = (CubeTexture)((Ref.ResMan.LoadResource("data/dronedie"+i+".png").Data));
        }
        Position = spawn;

    }

    public void Update(int msec) {
        Vector2f wishdir = new Vector2f(0, (float)Math.sin(Ref.loop.time/500f));
        
        wishdir.x *= -1f;
        wishdir.y *= -1f;

        Vector2f distdir = new Vector2f(Position.x - Ref.world.player.position.x, Position.y - Ref.world.player.position.y);
        wishdir.x *= -1f;
        wishdir.y *= -1f;

        float distance = (float)Math.sqrt(distdir.x * distdir.x + distdir.y * distdir.y);

        if(distance < 10f && health > 0) {
            // Apply damage to player
            Ref.world.player.TakeDamage((20 * (float)msec/1000f));
        }

        float lenght = (float)Math.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);
        if(lenght != 0f) {
            wishdir.x /= lenght;
            wishdir.y /= lenght;
        }

        float speedcap = speed;
        if(lenght < 20f)
            speedcap *= 0.5f;

        wishdir.x *= speedcap;
        wishdir.y *= 1000;
        wishdir.y *= 1f/speed;
        
        boolean onGround = GroundTrace();
        if(health <= 0)
        {
            if(!onGround)
                Velocity.y -= 300f * (float)msec/1000f;
            else
                Velocity.y = 0;
        } else
            Velocity = wishdir;
        TryMove(msec);

        FireWeapon();

        RunAnimation();
    }

    void FireWeapon() {
        if(weaponTime > Ref.loop.time)
            return;

        if(health <= 0)
            return;

        // Check distance
        
        float xdiff = Math.abs(Position.x - Ref.world.player.position.x);
        if(xdiff > 100f)
            return;

        weaponTime = Ref.loop.time + 1000 + (int)(Ref.rnd.nextFloat()*500);
        Vector2f bulletpos = new Vector2f(Position.x, Position.y-Size.y*0.35f);
        Vector2f bulletvel = new Vector2f(50f,0);

        if(lookright)
            bulletpos.x += Size.x + 2;
        else {
            bulletpos.x -= Size.x + 2;
            bulletvel.x *= -1f;
        }

        
       // bulletvel.y += velocity.y /2f;

        Bullet bullet = new Bullet(bulletpos, bulletvel, 5, Collision.MASK_ENEMIES | Collision.MASK_WORLD | Collision.MASK_PLAYER);
        bullet.Size = new Vector2f(0.5f, 0.5f);
        Ref.world.Entities.add(bullet);
    }

    void DoSuicide() {
        health = -100;
        //damageTime = Ref.loop.time;
        Vector2f wishdir = new Vector2f(Position.x - Ref.world.player.position.x, Position.y - Ref.world.player.position.y);
        wishdir.x *= -1f;
        wishdir.y *= -1f;

        float distance = (float)Math.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);

        if(distance > 60f)
            return;
        int damage2 = 5;
        float invfrac = 1f-(distance/60f);
        damage2 += (int)(damage*invfrac);
        Ref.world.player.TakeDamage(damage2);
    }

    void Suicide() {
        suicideTime = Ref.loop.time + 500;
        damageTime = Ref.loop.time;
        nextanimTime = Ref.loop.time + 500;
    }

    void Friction(int msec) {
        float speed2 = (float)Math.sqrt(Velocity.x * Velocity.x + Velocity.y * Velocity.y);
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

        Velocity.x *= newspeed;
        Velocity.y *= newspeed;
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

        float currentSpeed = Vector2f.dot(Velocity, wishdir);
        float addSpeed = wishspeed  - currentSpeed;
        if(addSpeed > 0f)
        {
            float accelspeed = accel * (float)msec/1000f * wishspeed;
            if(accelspeed > addSpeed)
                accelspeed = addSpeed;

            Velocity.x += accelspeed * wishdir.x;
            Velocity.y += accelspeed * wishdir.y;
        }

        float speed2 = (float)Math.sqrt(Velocity.x * Velocity.x + Velocity.y * Velocity.y);
        if(speed2 < 1f)
        {
            Velocity.x = 0f;
            Velocity.y = 0f;
            return;
        }

        TryMove(msec);


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
        TryMove(msec);
    }

    void AirAccelerate(Vector2f wishdir, float wishspeed, float acceleration, int msec) {
        float wishspd = wishspeed;
        if(wishspd > 40f)
            wishspd = 40f;

        float currentspeed = Vector2f.dot(Velocity, wishdir);
        float addspeed = wishspd - currentspeed;
        if(addspeed <= 0f)
            return;

        float accelspeed = acceleration * (float)msec/1000f * wishspeed;
        if(accelspeed > addspeed)
            accelspeed = addspeed;

        Velocity.x += accelspeed * wishdir.x;
        Velocity.y += accelspeed * wishdir.y;
    }

    void TryMove(int msec) {
        Vector2f newmove = new Vector2f(Velocity.x * (float)msec/1000f, Velocity.y * (float)msec/1000f);
        CollisionResult coll;
        int mask = Collision.MASK_WORLD;
        coll = Ref.collision.TestPosition(Position, newmove, Size, mask);

        if(!coll.Hit)
            Vector2f.add(newmove, Position, Position);
        else {
            Vector2f up = new Vector2f(0, 6f);
            boolean stepped = false;

            // Only step when colliding with world
            if(coll.hitmask == Collision.MASK_WORLD) {
                coll = Ref.collision.TestPosition(Position, up, Size, mask);
                if(!coll.Hit && newmove.y == 0f && Velocity.y == 0f) {
                    up.x += Position.x;
                    up.y += Position.y;
                    coll = Ref.collision.TestPosition(up, newmove, Size, mask);
                    if(!coll.Hit) {
                        // push down
                        stepped = true;
                        up.x += newmove.x;
                        up.y += newmove.y;
                        Vector2f down = new Vector2f(0, -8f);
                        Ref.collision.TestPosition(up, down, Size, mask);
                        Position.x = up.x + down.x * coll.frac;
                        Position.y = up.y + down.y * coll.frac;
                    }
                }
            }

            if(!stepped) {
                coll = Ref.collision.TestPosition(Position, new Vector2f(newmove.x, 0f), Size, mask);
                if(!coll.Hit) {
                    Vector2f.add(new Vector2f(newmove.x, 0f), Position, Position);
                    Velocity.y = 0f;
                    }
                else if(!(coll = Ref.collision.TestPosition(Position, new Vector2f(0f, newmove.y), Size, mask)).Hit) {
                    Vector2f.add(new Vector2f(0f, newmove.y),Position, Position);
                    Velocity.x = 0f;
                    }
                else {
                    if(coll.hitmask == Collision.MASK_WORLD) {
                        Position.x += newmove.x * coll.frac;
                        Position.y += newmove.y * coll.frac;
                    }
                }
            }
        }


    }

    void Jump(int msec) {
        if(jumping)
            return;

        boolean onGround = GroundTrace();
        if(onGround) {
            Velocity.y = (float)Math.sqrt(2 * 150 * 45);
            jumpTime = Ref.loop.time;
        } else if(jumpTime + 300 > Ref.loop.time) {
            Velocity.y += 200f * (float)msec/1000f;
        }
    }

    void RunAnimation() {
        float xdiff = Position.x - Ref.world.player.position.x;
        lookright = xdiff < 0;


        if(nextanimTime < Ref.loop.time )
        {
            nextanimTime = Ref.loop.time + 50;

            runframe++;

            if(health > 0) {
                if(runframe >= 3)
                    runframe = 0;
            } else if(runframe >= 9)
                runframe = 9;
        }
    }

    boolean GroundTrace() {
        CollisionResult result = Ref.collision.TestPosition(Position, new Vector2f(0, -1f), Size, Collision.MASK_WORLD);
//        if(result.frac != 0f) {
//            Position.y -= 1f * result.frac;
//        }
        return result.Hit;
    }

    public void Render() {
        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
        CubeTexture tex;
        if(health > 0)
            tex = walkAnim[runframe];
        else
            tex = explode[runframe];
        Vector2f offset = new Vector2f(0.05f,0.00f);
        Vector2f size = new Vector2f(0.6f, 0.65f);
        if(!lookright) {
            offset.x += size.x;
            size.x = -size.x;
        }
        spr.Set(new Vector2f(Position.x-Size.x, Position.y-Size.y), new Vector2f(Size.x*2f, Size.y*2f), tex, offset, size);

//        spr = Ref.SpriteMan.GetSprite(Type.GAME);
//        spr.Set(new Vector2f(Position.x-Size.x, Position.y-Size.y), new Vector2f(Size.x*2f, Size.y*2f), null, offset, size);
//        spr.Color = new Vector4f(1, 0, 0, 0.5f);
        
        if(damageTime + 100 > Ref.loop.time) {
           float frac = (float)(damageTime+100-Ref.loop.time)/100f;
           spr.Color = new Vector4f(1, 0, 0, 0.5f + 0.5f*frac);
        }

        if(runframe == 9)
            ToRemove = true;
    }

    public void Collide(Entity other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void Hurt(int damage) {
        if(health > 0 && health - damage <= 0)
            Ref.world.player.AddScore(DroneScore);
        health -= damage;
        damageTime = Ref.loop.time;
    }

    public Vector2f GetPosition() {
        return Position;
    }

    public Vector2f GetSize() {
        return Size;
    }

    public int GetType() {
        return Collision.MASK_BOMBAH;
    }

    public boolean ToRemove() {
        return ToRemove;
    }
}
