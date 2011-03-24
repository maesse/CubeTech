///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package cubetech.state;
//
//
//import java.util.ArrayList;
//
///**
// *
// * @author mads
// */
//public final class StateManager {
//    ArrayList<IGameState> gamestates = new ArrayList<IGameState>();
//    IGameState currentState = null;
//
//    boolean initialized = false;
//
//    public StateManager() {
//
//    }
//
//    public void Init() {
//        if(initialized)
//            return;
//        initialized = true;
//        IGameState state = new IntroState();
//        gamestates.add(state);
//        SetState(state);
//        state = new MenuState();
//        gamestates.add(state);
//        gamestates.add(new HagserState());
//    }
//
//    // Runs a frame of the current state
//    public void RunFrame(int msec) {
//        if(!initialized)
//            return;
//        currentState.RunFrame(msec);
//    }
//
//    /**
//     * Returns the gamestate with
//     * @param name
//     * @return
//     */
//    public IGameState GetGameState(String name) {
//        for (int i= 0; i < gamestates.size(); i++) {
//            IGameState state = gamestates.get(i);
//
//            if(state.GetName().equalsIgnoreCase(name))
//                return state; // found it
//        }
//        return null;
//    }
//
//    /**
//     * Change to this gamestate
//     * @param newstate
//     */
//    public void SetState(IGameState newstate) {
//        if(!initialized)
//            return;
//        // Let the old state pause/exit
//        if(currentState != null)
//            currentState.Exit();
//
//        // Set the new state and let it know it is now running
//        currentState = newstate;
//        newstate.Enter();
//    }
//
//    public void SetState(String statestr) throws Exception {
//        if(!initialized)
//            return;
//        IGameState state = GetGameState(statestr);
//        if(state != null)
//            SetState(state);
//        else
//            throw new Exception("FATAL: SetState(" + statestr + "): State doesn't exist.");
//    }
//}
