/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;
import org.openmali.FastMath;

/**
 *
 * @author mads
 */
public class TestEnemy extends EnemyBase {
    int nextThinkTime = 0;
    int killtime = 0;

    public TestEnemy(int x, int y) {
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
            wishdir.x *= 30;
            wishdir.y *= 30;
            setVelocity(wishdir);

            nextThinkTime = 500; // dumb fuck
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
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        spr.Set(getPosition(), 10);
    }

    @Override
    public void Die() {
        killtime = 50; // remove totally in 50 msec
    }

}
