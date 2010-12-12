/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.entities;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class PlayerStart implements Entity {
    public Vector2f Position;
    Vector2f size = new Vector2f(1, 1);

    public PlayerStart(Vector2f Position) {
        
    }

    public Vector2f GetPosition() {
        return Position;
    }

    public Vector2f GetSize() {
        return size;
    }

    public void Update(int msec) {
        
    }

    public void Render() {
        
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
