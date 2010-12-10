package cubetech.state;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.TextManager.Align;
import cubetech.misc.Button;
import cubetech.misc.Ref;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Vector2f;

/**
 * Simple menu
 * TODO: Make nice for keyboard users?
 * @author mads
 */
public class MenuState implements IGameState {
    Button newGame;
    Button exit;


    public void Enter() {
        CubeTexture buttonBg = (CubeTexture)(Ref.ResMan.LoadResource("data/test.png").Data);
        newGame = new Button("New Game", new Vector2f(0.4f, 0.67f), new Vector2f(0.2f, 0.07f), buttonBg);
        exit = new Button("Exit", new Vector2f(0.4f, 0.6f), new Vector2f(0.2f, 0.07f), buttonBg);
    }

    public void Exit() {
        newGame = null;
        exit = null;
    }

    public void RunFrame(int msec) {
        Ref.textMan.AddText(new Vector2f(0.5f,0.8f), "Menu", Align.CENTER);
        Vector2f mousePos = Ref.Input.playerInput.MousePos;
        if(newGame.Intersects(mousePos) && Ref.Input.playerInput.Mouse1) {
            try {
                // Start new game
                Ref.StateMan.SetState("hagser");
                return;
            } catch (Exception ex) {
                Logger.getLogger(MenuState.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1);
            }
        }
        else if(exit.Intersects(mousePos) && Ref.Input.playerInput.Mouse1) {
            // Exit Cubetech
            System.exit(0);
        }
        newGame.Render();
        exit.Render();
    }

    public String GetName() {
        return "menu";
    }

}
