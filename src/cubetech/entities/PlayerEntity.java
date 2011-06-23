package cubetech.entities;

import cubetech.Helper;
import cubetech.game.Move;
import cubetech.gfx.Camera;
import cubetech.gfx.Graphics;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.PlayerInput;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class PlayerEntity implements Entity {
    // constants
    private static final Vector2f playerExtent = new Vector2f(20,20); // Player half-size
    private static final float STOPSPEED = 10f; // when under this speed, apply more friction
    private static final float MAXSPEED = 80f; // when under this speed, apply more friction
    private static final float FRICTION = 4f; // when under this speed, apply more friction
    private static final float ACCELERATION = 8f; // when under this speed, apply more friction

    // Current player state
    private Vector2f position = new Vector2f();
    private Vector2f velocity = new Vector2f();

    

    // Take player input and apply it to the playerstate, move the player forward..
    public void update(int msec) {
        // Grab the latest input and create movement vector
        PlayerInput in = Ref.Input.playerInput;
        Vector2f inMove = new Vector2f((in.Left?-1:0)+(in.Right?1:0),
                                       (in.Up?1:0)+(in.Down?-1:0));
        Helper.Normalize(inMove);

        // Apply physics to the velocity vector
        Move.Friction(velocity, FRICTION, STOPSPEED);
        Move.Accelerate(velocity, inMove, MAXSPEED, ACCELERATION);

        // Apply velocity to position
        Helper.VectorMA(position, msec * 0.001f, velocity, position); // a + b*c

        // Center camera on player
        Camera gameCam = Graphics.getCamera();
        //gameCam.setPosition(position);
    }

    // Draw the player
    public void render() {
        Sprite spr = Ref.SpriteMan.GetGameSprite();
        spr.setFromCenter(position, playerExtent, null);

        // HUD
        Ref.textMan.AddText(new Vector2f(0, Graphics.getHeight()-Ref.textMan.GetCharHeight()),

                String.format("Position: (%d,%d)", (int)position.x, (int)position.y), Align.LEFT, Type.HUD);
        Ref.textMan.AddText(new Vector2f(0, Graphics.getHeight()-Ref.textMan.GetCharHeight() * 2),
                String.format("Velocity: (%d,%d)", (int)velocity.x, (int)velocity.y), Align.LEFT, Type.HUD);
    }

    public void touches(Entity other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void hurt(int damage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // Used to check for collisions
    public Vector2f getPosition() {
        return position;
    }
    // Used to check for collisions
    public Vector2f getSize() {
        return playerExtent;
    }

    public Entities GetType() {
        return Entities.PLAYER;
    }

    public boolean toRemove() {
        return false; // never remove a player
    }


}
