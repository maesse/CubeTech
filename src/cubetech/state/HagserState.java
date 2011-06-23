package cubetech.state;


import cubetech.misc.Ref;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.input.Keyboard;


/**
 *  Simple game
 * @author mads
 */
public class HagserState implements IGameState {
    
    
    public void Enter() {

        
    }

    public void Exit() {
//        world = null;
    }

    public void RunFrame(int msec) {
        try {
            
            if (Ref.Input.GetKey(Keyboard.KEY_ESCAPE).Changed && Ref.Input.GetKey(Keyboard.KEY_ESCAPE).Pressed) {
                Ref.StateMan.SetState("menu");
            }
        } catch (Exception ex) {
            Logger.getLogger(HagserState.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String GetName() {
        return "hagser";
    }
    
}
