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
public interface Entity {
    public Vector2f GetPosition();
    public Vector2f GetSize();
    public void Update(int msec);
    public void Render();
    public void Collide(Entity other);
    public void Hurt(int damage);
    public int GetType();
    public boolean ToRemove();
}
