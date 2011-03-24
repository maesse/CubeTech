package cubetech.entities;

import cubetech.Game.Gentity;


/**
 *
 * @author mads
 */
public interface IEntity {
    public void init(Gentity ent);
    public String getClassName();
}
