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
public class Bullet implements Entity {
    public Vector2f Position;
    public Vector2f Size;
    public Vector2f Velocity;
    public int Hitmask = Collision.MASK_ALL;
    public int damage = 10;
    public boolean ToRemove = false;
    CubeTexture tex;

    public Bullet(Vector2f position, Vector2f velocity, int damage, int hitmask) {
        this.Position = position;
        this.Velocity = velocity;
        this.Size = new Vector2f(2, 1);
        this.Hitmask = hitmask;
        this.damage = damage;
        tex = (CubeTexture)(Ref.ResMan.LoadResource("data/pellet.png").Data);
    }
    
    public void Update(int msec) {
        Vector2f wishdir = new Vector2f(Position.x - Ref.world.player.position.x, Position.y - Ref.world.player.position.y);
        wishdir.x *= -1f;
        wishdir.y *= -1f;

        // Kill bullets when they get out of range
        float distance = (float)Math.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);
        if(distance > 300f)
            ToRemove = true;

        Vector2f movement = new Vector2f(Velocity.x*(float)msec/1000f, Velocity.y*(float)msec/1000f);
        CollisionResult res =  Ref.collision.TestPosition(Position,movement , Size, Hitmask);
        if(res.Hit) {
            // Todo: explode
            if(res.hitmask != 0) {
                ToRemove = true;
                type = 0;
            }
            // Deal damage
            switch(res.hitmask) {
                case Collision.MASK_PLAYER:
                    Ref.world.player.HitByBullet(this);
                    Ref.soundMan.playEffect(Ref.soundMan.addSound("data/powerdown.wav"));
                    break;
                case Collision.MASK_ENEMIES:
                    Entity ent = (Entity)res.hitObject;
                    Ref.soundMan.playEffect(Ref.soundMan.addSound("data/lazer.wav"));
                    ent.Hurt(damage);
                    break;
            }
            
        } else {
            Position.x += Velocity.x * (float)msec/1000f;
            Position.y += Velocity.y * (float)msec/1000f;
        }
    }

    public void Render() {
        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);

        float sizex = Size.x;
        float sizey = Size.y;
        if(sizex < 1)
            sizex = 1;
        if(sizey < 1)
            sizey = 1;

        spr.Set(new Vector2f(Position.x - sizex, Position.y - sizey), new Vector2f(sizex *2f, sizey*2f), tex, new Vector2f(), new Vector2f(1, 1));
        //spr.Color = new Vector4f(1, 0, 0, 1);
    }

    public void Collide(Entity other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void Hurt(int damage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Vector2f GetPosition() {
        return Position;
    }

    public Vector2f GetSize() {
        return Size;
    }
    int type =  Collision.MASK_BULLETS;
    public int GetType() {
        return type;
    }

    public boolean ToRemove() {
        return ToRemove;
    }


}
