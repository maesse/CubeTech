package cubetech.entities;

import cubetech.Game.Gentity;
import cubetech.gfx.CubeTexture;
import cubetech.misc.Ref;


/**
 * Just holds a position. Doesn't actually spawn anything.
 * @author mads
 */
public class Info_Player_Spawn implements IEntity {
    public void init(Gentity ent) {
        // Doesn't do anything
    }

    public String getClassName() {
        return "info_player_spawn";
    }

    public CubeTexture getIcon() {
        return Ref.ResMan.LoadTexture("data/tool_spawn.png");
    }

}
