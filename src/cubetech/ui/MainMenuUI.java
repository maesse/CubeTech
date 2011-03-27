package cubetech.ui;

import cubetech.common.Commands.ExecType;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.MouseEvent;
import cubetech.misc.Button;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class MainMenuUI implements IMenu {
    Button newGame;
    Button optionsButton;
    Button serverButton;
    Button exit;
    Button contGame;

    Vector2f menuStartPos = new Vector2f(0.75f, 0.55f);
    float menuSpacing = 0.08f;
    Vector2f menuSize = new Vector2f(0.2f, 0.07f);
    CubeTexture background;

    CContainer cont = null;

    public MainMenuUI() {
        CubeTexture buttonBg = Ref.ResMan.LoadTexture("data/menubutton.png");
        background = Ref.ResMan.LoadTexture("data/rightmenubar.png");

        cont = new CContainer(new FlowLayout(false, true, true));
        cont.addComponent(new CButton("New Game",buttonBg, Align.CENTER, 1.5f, new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                Ref.commands.ExecuteText(ExecType.NOW, "map data/themap16");
                Ref.commands.ExecuteText(ExecType.NOW, "start");
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
        spr.Set(new Vector2f(0, 0), Ref.glRef.GetResolution(), background, new Vector2f(), new Vector2f(1, 1));
        if(Ref.cvars.Find("ui_fullscreen").iValue == 0)
            spr.SetColor(255,255,255,127);
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(Ref.glRef.GetResolution().x/2f - 200, Ref.glRef.GetResolution().y/2f + 100), new Vector2f(400, 200), Ref.ResMan.LoadTexture("data/logo.png"), new Vector2f(), new Vector2f(1, 1));
//        if(Ref.cvars.Find("ui_fullscreen").iValue == 0)
//            spr.SetColor(255,255,255,127);


        cont.setPosition(new Vector2f(Ref.glRef.GetResolution().x - cont.getSize().x, Ref.glRef.GetResolution().y - cont.getSize().y));
        cont.Render(new Vector2f());
    }

    public boolean IsFullscreen() {
        return (Ref.cvars.Find("ui_fullscreen").iValue == 1);
    }

    public void Show() {
        
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
