/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.entities;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class PlayerFinish implements Entity {
    public Vector2f Position;
    Vector2f size = new Vector2f(8, 6);
    CubeTexture chest[] = new CubeTexture[2];
    CubeTexture gold[] = new CubeTexture[5];

    boolean PlayerTouch = false;
    long touchTime = 0;
    int goldanim = 0;
    int animationmsec = 100;

    public PlayerFinish(Vector2f pos) {
        this.Position = pos;
        chest[0] = (CubeTexture)(Ref.ResMan.LoadResource("data/chest0.png").Data);
        chest[1] = (CubeTexture)(Ref.ResMan.LoadResource("data/chest1.png").Data);
        gold[0] = (CubeTexture)(Ref.ResMan.LoadResource("data/gold0.png").Data);
        gold[1] = (CubeTexture)(Ref.ResMan.LoadResource("data/gold1.png").Data);
        gold[2] = (CubeTexture)(Ref.ResMan.LoadResource("data/gold2.png").Data);
        gold[3] = (CubeTexture)(Ref.ResMan.LoadResource("data/gold3.png").Data);
        gold[4] = (CubeTexture)(Ref.ResMan.LoadResource("data/gold4.png").Data);
    }
    
    public Vector2f GetPosition() {
        return Position;
    }

    public Vector2f GetSize() {
        return size;
    }

    void ReachGoal() {
        PlayerTouch = true;
        touchTime = Ref.loop.time + animationmsec;
        Ref.soundMan.playEffect(Ref.soundMan.addSound("data/powerup.wav"));
        Ref.world.player.Goal();
    }

    public void Update(int msec) {
        if(PlayerTouch) {
            if(touchTime < Ref.loop.time) {
                touchTime = Ref.loop.time + animationmsec;
                goldanim++;
                if(goldanim == 5)
                    goldanim = 3;
            }
        } else {
        Vector2f playerPos = Ref.world.player.position;
        Vector2f extent = Ref.world.player.extent;
        if(playerPos.x >= Position.x - extent.x && playerPos.x <= Position.x + size.x + extent.x) // inside x coords
            if(playerPos.y >= Position.y - extent.y && playerPos.y <= Position.y + size.y + extent.y) {
                ReachGoal();
            }
        }
    }

    public void Render() {
        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
        if(PlayerTouch) {
            spr.Set(Position, size, chest[1], new Vector2f(), new Vector2f(1, 1));
        } else {
            spr.Set(Position, size, chest[0], new Vector2f(), new Vector2f(1, 1));
        }

        if(touchTime != 0) {
            spr = Ref.SpriteMan.GetSprite(Type.GAME);
            spr.Set(Position, size, gold[goldanim], new Vector2f(), new Vector2f(1, 1));
        }
    }

    public void Collide(Entity other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void Hurt(int damage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int GetType() {
        return 0;
    }

    public boolean ToRemove() {
        return false;
    }
    
}
