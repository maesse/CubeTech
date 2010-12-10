/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.SpriteManager.Type;
import cubetech.misc.Ref;
import org.jbox2d.collision.PolygonDef;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class Player {
    public Vector2f position;
    public Vector2f velocity;
    Body body;
    World world;

    public Player(World world, Vec2 spawn) {
        BodyDef bd = new BodyDef();
        bd.position.set(spawn);
        body = world.b2World.createBody(bd);
        this.world = world;
        PolygonDef sd = new PolygonDef();
        sd.setAsBox(16f/32f, 16f/32f);
        sd.density = 5*5f;
        sd.friction = 1f;
        
        body.createShape(sd);

        body.setMassFromShapes();
        
        //
    }

    public void Render() {
        Vec2 pos = body.getPosition();
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
        spr.Set(new Vector2f(pos.x*16f-16, pos.y*16f-16), new Vector2f(16f, 16f), null, new Vector2f(0, 0), new Vector2f(1, 1));
    }

    public void Update() {
        Vec2 dir = new Vec2();
        if(Ref.Input.playerInput.Down)
            dir.y -= 1f;
        if(Ref.Input.playerInput.Up)
            dir.y += 1f;
        if(Ref.Input.playerInput.Left)
            dir.x -= 1f;
        if(Ref.Input.playerInput.Right)
            dir.x += 1f;

        dir.x *= 10f;
        dir.y *= 10f;

        //body.setLinearVelocity(dir);
        //body.setBullet(true);
        
        body.wakeUp();
        //body.applyForce(dir, body.getPosition());
        body.applyImpulse(dir, body.getWorldCenter());
    }
}
