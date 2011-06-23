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
    public Vector2f getPosition();
    public Vector2f getSize();
    public void update(int msec);
    public void render();
    public void touches(Entity other); // called when the entity touches another entity
    public void hurt(int damage); // apply damage to an entity
    public Entities GetType();
    public boolean toRemove();
}
