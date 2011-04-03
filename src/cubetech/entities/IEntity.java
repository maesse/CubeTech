package cubetech.entities;

import cubetech.Game.Gentity;
import cubetech.gfx.CubeTexture;


/**
 *
 * @author mads
 */
public interface IEntity {
    public void init(Gentity ent);
    public CubeTexture getIcon(); // icon for editor
    public String getClassName();
}
