package cubetech.game;


import cubetech.entities.Entity;
import cubetech.entities.PlayerEntity;
import cubetech.gfx.Camera;
import cubetech.gfx.Graphics;
import cubetech.state.IGameState;
import java.util.ArrayList;
import java.util.Iterator;


/**
 *  Simple game
 * @author mads
 */
public class HagserState implements IGameState {
    ArrayList<Entity> entities = new ArrayList<Entity>();

    // Initialize game state
    private void initialize() {
        // Setup camera
        Camera gameCam = Graphics.getCamera();
        gameCam.centered = true;

        // Create a Player Entity
        entities.add(new PlayerEntity());
    }

    public void RunFrame(int msec) {
        // Loop over all the entities
        // Start with update
        Iterator<Entity> it = entities.iterator();
        while(it.hasNext()) {
            Entity ent = it.next();

            // Run entity.Update
            ent.update(msec);

            // Entity is dead and wan't to be deleted
            if(ent.toRemove()) {
                it.remove();
            }
        }

        // render now
        it = entities.iterator();
        while(it.hasNext()) {
            Entity ent = it.next();
            ent.render();
        }
    }

    public String GetName() {
        return "hagser";
    }

    private boolean initialized = false;
    public void Enter() {
        if(initialized) {
            return;
        }
        initialize();
        initialized = true;
    }

    public void Exit() {
    }
    
}
