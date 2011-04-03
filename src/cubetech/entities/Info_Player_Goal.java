package cubetech.entities;

import cubetech.Game.GameClient;
import cubetech.Game.Gentity;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.ITouchMethod;
import cubetech.gfx.CubeTexture;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;

/**
 * An entity spawner
 * @author mads
 */
public class Info_Player_Goal implements IEntity {
    private static Vector2f defaultHalfSize = new Vector2f(10,40);

    public void init(Gentity ent) {
        // Link the entity in as a trigger.
        ent.r.contents = Content.TRIGGER;
        ent.touch = player_touches_goal;

        // No size set by SpawnEntity, so use the default
        if(ent.r.mins.length() == 0) {
            ent.r.mins.set(-defaultHalfSize.x, -defaultHalfSize.y);
            ent.r.maxs.set(defaultHalfSize.x, defaultHalfSize.y);
        }
        ent.Link();
    }

    // Gets called whenever a player touches the goal
    private static ITouchMethod player_touches_goal = new ITouchMethod() {
        public void touch(Gentity self, Gentity other) {
            if(!other.isClient())
                return;

            GameClient cl = other.getClient();
            cl.reachedGoal(self);
        }
    };

    public String getClassName() {
        return "info_player_goal";
    }

    public CubeTexture getIcon() {
        return Ref.ResMan.LoadTexture("data/tool_goal.png");
    }

}
