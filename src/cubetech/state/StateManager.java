/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.state;


import java.util.ArrayList;

/**
 *
 * @author mads
 */
public final class StateManager {
    ArrayList<IGameState> gamestates = new ArrayList<IGameState>();
    IGameState currentState = null;

    public StateManager() throws Exception {
        // register states we're going to use
        registerState(new IntroState());
        registerState(new MenuState());
        registerState(new HagserState());

        // Set initial state
        SetState("intro");
    }

    private void registerState(IGameState state) {
        gamestates.add(state);
    }

    // Runs a frame of the current state
    public void RunFrame(int msec) {
        currentState.RunFrame(msec);
    }

    /**
     * Returns the gamestate with
     * @param name
     * @return
     */
    public IGameState GetGameState(String name) {
        for (int i= 0; i < gamestates.size(); i++) {
            IGameState state = gamestates.get(i);
            
            if(state.GetName().equalsIgnoreCase(name))
                return state; // found it
        }
        return null;
    }

    /**
     * Change to this gamestate
     * @param newstate
     */
    public void SetState(IGameState newstate) {
        // Let the old state pause/exit
        if(currentState != null)
            currentState.Exit();

        // Set the new state and let it know it is now running
        currentState = newstate;
        newstate.Enter();
    }

    public void SetState(String statestr) throws Exception {
        IGameState state = GetGameState(statestr);
        if(state != null)
            SetState(state);
        else
            throw new Exception("FATAL: SetState(" + statestr + "): State doesn't exist.");
    }
}
