package cubetech.ui;

import cubetech.common.Commands.ExecType;
import cubetech.common.Helper;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.MouseEvent;
import cubetech.misc.Ref;
import cubetech.ui.CContainer.Direction;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.vector.Vector2f;


/**
 *
 * @author mads
 */
public class OptionsUI implements IMenu {
    private CContainer mainCont;
    private CTextField nameField;
    private CContainer popupContainer = null;
    private CScrollPane resScroll = null;
    private CLabel resolutionLabel = null;

    private int time;

    public OptionsUI() {
        initUI();

    }

    Vector2f oldResolution = null;
    public void Update(int msec) {
        if(!Helper.Equals(oldResolution, Ref.glRef.GetResolution())) {
            initUI();

            return;
        }

        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(0, 0), Ref.glRef.GetResolution(), Ref.ResMan.getWhiteTexture(), new Vector2f(), new Vector2f(1, 1));
        
        spr.SetColor(0,0,0,127);

        // Timer tool
        time += msec;
        float scale = 50f;
        
        Vector2f delta = new Vector2f();
        delta.x = (float)Math.sin(time/150f)*scale + 100;
        delta.y = (float)Math.cos(time/150f)*scale +  Ref.glRef.GetResolution().y /2f;

        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(delta, 50f, Ref.ResMan.LoadTexture("data/textures/tile.png"));
        
        // Main container
        mainCont.Render(new Vector2f());

        // Popup
        if(popupContainer != null)
            popupContainer.Render(new Vector2f());
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

        // Handle popup
        if(popupContainer != null) {
            if(popupContainer.containsPoint(absCoords) || evt.Button < 0) {
                popupContainer.MouseEvent(evt);
                return;
            }


            if(evt.Button >= 0 && evt.Pressed)
                popupContainer = null;

        }

        mainCont.MouseEvent(evt);
    }

    private void initUI() {
        mainCont = new CContainer(new FlowLayout(false, false, true));
        CContainer cont = new CContainer(new FlowLayout(true, true, false));

        // Name
        cont.addComponent(new CLabel("Name:"));
        nameField = new CTextField(20,"UnknownCube", setName);
        cont.addComponent(nameField);
        cont.doLayout();
        mainCont.addComponent(cont);

        // V-Sync
        cont = new CContainer(new FlowLayout(true, true, false));
        CCheckbox cbox = new CCheckbox("V-Sync", setVsync);
        cbox.setSelectedDontFire(Ref.cvars.Find("r_vsync").iValue==1);
        cont.addComponent(cbox);
        cont.doLayout();
        mainCont.addComponent(cont);

        // LWJGL Timer
        cont = new CContainer(new FlowLayout(true, true, false));
        cbox = new CCheckbox("LWJGL Timer", setTimer);
        cbox.setSelectedDontFire(Ref.cvars.Find("com_timer").iValue==1);
        cont.addComponent(cbox);
        cont.doLayout();
        mainCont.addComponent(cont);

        // Volume
        cont = new CContainer(new FlowLayout(true, true, false));
        cont.addComponent(new CLabel("Volume: "));
        CSlider slider = new CSlider(new Vector2f(250, Ref.textMan.GetCharHeight()), setVolume);
        slider.setValueDontFire(Ref.cvars.Find("volume").fValue);
        cont.addComponent(slider);
        cont.doLayout();
        mainCont.addComponent(cont);

        // Music
//        cont = new CContainer(new FlowLayout(true, true, false));
//        cbox = new CCheckbox("Music", setMusic);
//        cbox.setSelectedDontFire(Ref.soundMan.playmusic);
//        cont.addComponent(cbox);
//        cont.doLayout();
//        mainCont.addComponent(cont);

        // Resolution
        cont = new CContainer(new FlowLayout(true, true, false));
        cont.addComponent(new CLabel("Resolution: "));
        CButton but = new CButton(String.format("%dx%d", (int)Ref.glRef.GetResolution().x, (int)Ref.glRef.GetResolution().y), null, Align.LEFT, 1, selectResolution);
        resolutionLabel = (CLabel)but.getComponent(0);
        cont.addComponent(but);
        cont.doLayout();
        mainCont.addComponent(cont);

        // Fullscreen
        cont = new CContainer(new FlowLayout(true, true, false));
        cbox = new CCheckbox("Fullscreen", setFullscreen);
        cbox.setSelectedDontFire(Ref.cvars.Find("r_fullscreen").iValue==1);
        cont.addComponent(cbox);
        cont.doLayout();
        mainCont.addComponent(cont);

        // Show FPS
        cont = new CContainer(new FlowLayout(true, true, false));
        cbox = new CCheckbox("Show FPS", setShowFPS);
        cbox.setSelectedDontFire(Ref.cvars.Find("cl_showfps").iValue==1);
        cont.addComponent(cbox);
        cont.doLayout();
        mainCont.addComponent(cont);

        // Back button
        cont = new CContainer(new FlowLayout(true, true, false));
        cont.addComponent(new CButton("Back", null, Align.CENTER, 1, backEvent));
        cont.doLayout();
        mainCont.addComponent(cont);

        mainCont.doLayout();
        mainCont.doLayout();

        // Center maincont
        Vector2f size = mainCont.getLayoutSize();
        Vector2f maxsize = Ref.glRef.GetResolution();

        size.scale(0.5f);
        Vector2f position = new Vector2f(maxsize);
        position.scale(0.5f);
        Vector2f.sub(position, size, position);

        mainCont.setPosition(position);


        // Init resolution container
        resScroll = new CScrollPane(CContainer.Direction.VERTICAL);
        resScroll.setResizeToChildren(Direction.NONE);
        resScroll.setBackground(Ref.ResMan.getWhiteTexture());

        CContainer resCont = new CContainer(new FlowLayout(false, false, true));
        resCont.setResizeToChildren(Direction.VERTICAL);
        DisplayMode[] modes = Ref.glRef.getNiceModes();
        for (DisplayMode mode : modes) {
            String str = String.format("%dx%d", mode.getWidth(), mode.getHeight());
            CButton resBut = new CButton(str, null, Align.LEFT, 1, setResolution);
            resBut.tag = mode;
            resCont.addComponent(resBut);
        }
        resCont.doLayout();

        resScroll.addComponent(resCont);
        resScroll.doLayout();

        resScroll.setSize(new Vector2f(300, Ref.glRef.GetResolution().y * 0.7f));
        resScroll.setPosition(new Vector2f(Ref.glRef.GetResolution().x - 320, Ref.glRef.GetResolution().y * 0.15f));
        oldResolution = new Vector2f(Ref.glRef.GetResolution());
    }

    private ButtonEvent setResolution = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            if(button.tag != null && button.tag instanceof DisplayMode) {
                DisplayMode mode = (DisplayMode)button.tag;
                String str = String.format("%dx%d", mode.getWidth(), mode.getHeight());
                Ref.cvars.Set2("r_mode", str, true);
                popupContainer = null;
                resolutionLabel.setText(str);
                
            }
            
        }
    };

    private ButtonEvent setVolume = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            float vol = ((CSlider)button).getValue();
            Ref.cvars.Set2("volume", ""+vol, true);
        }
    };

    private ButtonEvent selectResolution = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            popupContainer = resScroll;
        }
    };


    private ButtonEvent setName = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            String name = ((CTextField)button).getText();
            Ref.commands.ExecuteText(ExecType.NOW, "name " + name);
        }
    };

    private ButtonEvent setTimer = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            boolean value = ((CCheckbox)button).isSelected();
            Ref.cvars.Set2("com_timer", ""+(value?1:2), true);
        }
    };

    private ButtonEvent backEvent = new ButtonEvent() {
            public void buttonPressed(CComponent button, MouseEvent evt) {
                nameField.removeFocus(); // Remove focus to trigger a text-change event
                Ref.ui.SetActiveMenu(UI.MENU.MAINMENU);
            }
        };

    private ButtonEvent setVsync = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            boolean value = ((CCheckbox)button).isSelected();
            Ref.cvars.Set2("r_vsync", ""+(value?1:0), true);
        }
    };

    private ButtonEvent setFullscreen = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            boolean value = ((CCheckbox)button).isSelected();
            Ref.cvars.Set2("r_fullscreen", ""+(value?1:0), true);
        }
    };

    private ButtonEvent setShowFPS = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            boolean value = ((CCheckbox)button).isSelected();
            Ref.cvars.Set2("cl_showfps", ""+(value?1:0), true);
        }
    };

    

    

}
