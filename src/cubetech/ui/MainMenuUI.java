package cubetech.ui;

import cubetech.common.Commands.ExecType;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.Input;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;
import cubetech.net.ConnectState;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class MainMenuUI implements IMenu {
    CubeTexture background;

    CButton continueButton;
    CContainer cont = null;

    public MainMenuUI() {
        CubeTexture buttonBg = Ref.ResMan.LoadTexture("data/textures/ui/menubutton.png");
        background = Ref.ResMan.LoadTexture("data/textures/ui/rightmenubar.png");

        cont = new CContainer(new FlowLayout(false, true, true));
                cont.addComponent(
                continueButton = new CButton("Continue",buttonBg, Align.CENTER, 1.5f, new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() & ~Input.KEYCATCH_UI);
            }
        }));
        continueButton.setVisible(false);
        cont.addComponent(new CButton("New Game",buttonBg, Align.CENTER, 1.5f, new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                Ref.commands.ExecuteText(ExecType.NOW, "map");
            }
        }));
        
//        cont.addComponent(new CButton("Servers",buttonBg, Align.CENTER, 1.5f, new ButtonEvent() {
//            public void buttonPressed(CComponent button, MouseEvent evt) {
//                Ref.ui.SetActiveMenu(UI.MENU.SERVERS);
//            }
//        }));
//        cont.addComponent(new CButton("Sup3 Long",buttonBg, Align.CENTER,1.5f));
        cont.addComponent(new CButton("Options",buttonBg, Align.CENTER,1.5f, new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                Ref.ui.SetActiveMenu(UI.MENU.OPTIONS);
            }
        }));
        cont.addComponent(new CButton("Quit",buttonBg, Align.CENTER,1.5f,new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                Ref.common.Shutdown();
            }
        }));
        cont.doLayout();
    }

    public void Update(int msec) {
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        Vector2f res = Ref.glRef.GetResolution();
        spr.Set(0, 0, res.x, res.y, background);
        if(Ref.cvars.Find("ui_fullscreen").iValue == 0)
            spr.SetColor(255,255,255,127);
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(res.x/2f - 200, res.y/2f + 100, 400, 200, Ref.ResMan.LoadTexture("data/textures/ui/logo.png"));
//        if(Ref.cvars.Find("ui_fullscreen").iValue == 0)
//            spr.SetColor(255,255,255,127);


        cont.setPosition(new Vector2f(Ref.glRef.GetResolution().x - cont.getSize().x, Ref.glRef.GetResolution().y - cont.getSize().y));
        cont.Render(new Vector2f());

    }

    public boolean IsFullscreen() {
        return (Ref.cvars.Find("ui_fullscreen").iValue == 1);
    }

    public void Show() {
        continueButton.setVisible(Ref.client.clc.state.ordinal() > ConnectState.DISCONNECTED.ordinal());
    }

    public void GotMouseEvent(MouseEvent evt) {
        // From 0-1 to 0-resolution space
        Vector2f absCoords = new Vector2f(Ref.glRef.GetResolution());
        absCoords.x *= evt.Position.x;
        absCoords.y *= 1-evt.Position.y;

        evt.Position = absCoords;

        cont.MouseEvent(evt);
    }

}
