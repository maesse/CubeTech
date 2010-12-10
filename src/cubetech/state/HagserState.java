/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.state;

import cubetech.World;

/**
 *  Simple game
 * @author mads
 */
public class HagserState implements IGameState {
    public World world;
    
    public void Enter() {
        world = new World();
    }

    public void Exit() {
        world = null;
    }

    public void RunFrame(int msec) {
        world.Render(msec);
    }

    public String GetName() {
        return "hagser";
    }
    
}
