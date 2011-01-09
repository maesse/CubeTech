package cubetech.state;

import cubetech.World;
import cubetech.misc.Ref;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.input.Keyboard;


/**
 *  Simple game
 * @author mads
 */
public class HagserState implements IGameState {
    public World world = null;
    
    public void Enter() {
        if(world == null)
             world = new World();

        //Ref.soundMan.PlayBackgroundMusic(true);
    }

    public void Exit() {
        //Ref.soundMan.PlayBackgroundMusic(false);
//        world = null;
    }

    public void RunFrame(int msec) {
        try {
//            Ref.common.Frame();
            world.Render(msec);
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
