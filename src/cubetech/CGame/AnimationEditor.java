package cubetech.CGame;

import cubetech.gfx.AniModel;
import cubetech.gfx.Bone;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.Key;
import cubetech.input.KeyEvent;
import cubetech.input.KeyEventListener;
import cubetech.input.MouseEvent;
import cubetech.input.MouseEventListener;
import cubetech.misc.Ref;
import cubetech.ui.ButtonEvent;
import cubetech.ui.CButton;
import cubetech.ui.CComponent;
import cubetech.ui.CContainer;
import cubetech.ui.CLabel;
import cubetech.ui.CScrollPane;
import cubetech.ui.FlowLayout;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class AnimationEditor implements KeyEventListener, MouseEventListener {
    MapEditor mapEditor;
    CContainer menupanel;
    CScrollPane animateScroll;
    CContainer animateMenu;

    int FovX = 120;

    AniModel model = new AniModel();

    enum SelectType {
        BONE
    }

    SelectType select_type = SelectType.BONE;
    boolean isSelected = false;
    Bone select_bone = null;

    Vector2f lastMouseCoords = new Vector2f();

    enum Action {
        NONE,
        MOVEBONE,
        MOVEBONE2 // don't rotate children
    }

    Action action = Action.NONE;

    public AnimationEditor(MapEditor mapEditor) {
        this.mapEditor = mapEditor;
        initUI();
    }

    ButtonEvent exitAnimationEditor = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
             mapEditor.hideAnimator();
        }
    };

    private void initUI() {
        // Menu
        menupanel = new CContainer(new FlowLayout(true, true, false));
        menupanel.setBackground(Ref.ResMan.LoadTexture("data/topmenubar.png"));
        menupanel.setSize(new Vector2f(512, 64));
        menupanel.addComponent(new CButton(Ref.ResMan.LoadTexture("data/edit_game.png"), new Vector2f(32,32), exitAnimationEditor));

        menupanel.doLayout();

        animateMenu = new CContainer(new FlowLayout(true, true, false));
        animateMenu.setBackground(Ref.ResMan.LoadTexture("data/topmenubar.png"));
        animateMenu.setSize(new Vector2f(256, 45));
        animateMenu.addComponent(new CButton(Ref.ResMan.LoadTexture("data/arrow.png"), new Vector2f(24,24), exitAnimationEditor));
        Vector2f res = Ref.glRef.GetResolution();
        animateMenu.setPosition(new Vector2f(res.x/2f - 128, res.y - 45 - 140));
        animateMenu.doLayout();

        animateScroll = new CScrollPane(CContainer.Direction.HORIZONTAL);
        animateScroll.setResizeToChildren(CContainer.Direction.NONE);
        animateScroll.setBackground(Ref.ResMan.LoadTexture("data/rightmenubar.png"));
        
        CContainer animCont = new CContainer(new FlowLayout(true, false, true));
        animCont.setBackground(Ref.ResMan.LoadTexture("data/rightmenubar.png"));
        for (int i = 0; i < 10; i++) {
            animCont.addComponent(new CLabel("Hello thar"));
        }
        

        animCont.setResizeToChildren(CContainer.Direction.HORIZONTAL);
        animCont.setSize2(new Vector2f(0,120));
        animCont.doLayout();
        
        animateScroll.addComponent(animCont);
        animateScroll.setPosition(new Vector2f(0, Ref.glRef.GetResolution().y - 140));
        animateScroll.setSize2(new Vector2f(Ref.glRef.GetResolution().x, 140));
        animateScroll.doLayout();
    }

    public void SetView() {
        float aspect = Ref.glRef.GetResolution().y / Ref.glRef.GetResolution().x;

        // Set view
        ViewParams view = Ref.cgame.cg.refdef;
        view.FovX = view.w = FovX;
        view.h = (FovX * aspect);
        view.FovY = (int)view.h;
        view.Origin.set(0,0);
        view.xmin = -FovX * 0.5f;
        view.ymin = -view.h * 0.5f;
        view.xmax = FovX * 0.5f;
        view.ymax = view.h * 0.5f;

        // Apply view
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(view.xmin, view.xmax, view.ymin, view.ymax, 1,1000);
        GLU.gluLookAt(0, 0, 2, 0, 0, 0, 0, 1, 0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    public void Render() {
        Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x - 40, 0.0f), "^3Animation Editor", Align.RIGHT, Type.HUD);

        menupanel.Render(new Vector2f());
        animateMenu.Render(new Vector2f());
        animateScroll.Render(new Vector2f());

        mapEditor.renderGrid();

        if(isSelected && select_type == SelectType.BONE)
            select_bone.selected = true;
        model.Render(new Vector2f());

        RenderInformation();
    }

    private void RenderInformation() {
        // Draw cursor position
        Vector2f mouseCoords = mapEditor.OrgCoordsToGameCoords(lastMouseCoords);
        Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()),
                "Cursor: [" + (int)mouseCoords.x + "," + (int)mouseCoords.y + "]", Align.RIGHT, (Color) Color.GREY, null, Type.HUD, 1);

        // Draw grid scale
        float scale = Ref.glRef.GetResolution().x / FovX;
        float lineThickness = 3;
        float width = mapEditor.gridSpacing * scale;
        // Add text
        Vector2f size = Ref.textMan.AddText(new Vector2f(3,Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()),
                (int)mapEditor.gridSpacing + "u", Align.LEFT, null, null, Type.HUD,1 );

        Vector2f position = new Vector2f(size);
        position.x += 4;
        position.y *= 0.5f;
        position.y -= lineThickness * 0.5f;

        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(position, new Vector2f(width, lineThickness), Ref.ResMan.getWhiteTexture(), null, null);
        spr.SetColor(200, 200, 200, 200);
        spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        position.x -= lineThickness;
        position.y -= 5;
        spr.Set(position, new Vector2f(lineThickness, 12), Ref.ResMan.getWhiteTexture(), null, null);
        spr.SetColor(200, 200, 200, 200);
        position.x += width + lineThickness;
        spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(position, new Vector2f(lineThickness, 12), Ref.ResMan.getWhiteTexture(), null, null);
        spr.SetColor(200, 200, 200, 200);
    }

    public void KeyPressed(KeyEvent evt) {
        Key key = (Key) evt.getSource();

        if(key.Pressed) {
            if(key.key == Keyboard.KEY_ADD) {
                // Zoom in on grid
                mapEditor.setGridSpacing(mapEditor.gridSpacing/2);
                return;
            } else if(key.key == Keyboard.KEY_SUBTRACT) {
                // Zoom out on grid
                mapEditor.setGridSpacing(mapEditor.gridSpacing*2);
                return;
            } else if(key.key == Keyboard.KEY_DELETE) {
                if(isSelected && select_type == SelectType.BONE && action == Action.NONE) {
                    select_bone.RemoveBone();
                    SelectBone(null);
                    return;
                }
            }

        }
    }

    private void SelectBone(Bone bone) {
        if(bone == null) {
            // de-select
            isSelected = false;
            select_bone = null;
            action = Action.NONE;
            return;
        }
        isSelected = true;
        select_bone = bone;
        select_type = SelectType.BONE;
        action = Action.NONE;
    }

    public void GotMouseEvent(MouseEvent evt) {
        // From 0-1 to 0-resolution space
        Vector2f absCoords = mapEditor.OrgCoordsToPixelCoords(evt.Position); // HUD-space
        Vector2f gameCoords = mapEditor.OrgCoordsToGameCoords(evt.Position); // Game-space

        lastMouseCoords.set(evt.Position);
        if(evt.WheelDelta != 0 && Ref.Input.IsKeyPressed(Keyboard.KEY_LSHIFT)) {
            FovX += evt.WheelDelta * -10;
            if(FovX < 1)
                FovX = 1;
            return;
        }
        
        evt.Position = absCoords; // UI's use MouseEvent's, so just set pixel coords

        if(menupanel.containsPoint(absCoords))
            menupanel.MouseEvent(evt);
        else if(animateScroll.containsPoint(absCoords)) {
            animateScroll.MouseEvent(evt);
        } else if(animateMenu.containsPoint(absCoords)) {
            animateMenu.MouseEvent(evt);
        }
        else {

            // Currently moveing a bone around
            if(action == Action.MOVEBONE || action == Action.MOVEBONE2) {
                select_bone.SetBoneEndPoint(gameCoords, action == Action.MOVEBONE);
                // On button-release
                if(evt.Button >= 0 && !evt.Pressed) {
                    // We're done.
                    action = Action.NONE;
                }

                return;
            }

            boolean mousePress = evt.Button >= 0 && evt.Pressed;
            if(!mousePress)
                return;
            

            // Select on button press, when not doing anything fancy
            if(action == Action.NONE) {
                if(evt.Button == 1) { // Right Click
                    if(isSelected && select_type == SelectType.BONE) {
                        if(select_bone.TraceAgainstEnd(gameCoords, Ref.cgame.cg.refdef.w/100f)) {
                            // Create a new bone, and start moving it
                            Bone bone = new Bone();
                            select_bone.AttachBone(bone);
                            SelectBone(bone);
                            action = Action.MOVEBONE;
                            select_bone.SetBoneEndPoint(gameCoords, true);
                            return;
                        }
                    }
                }
                else if(evt.Button == 0) { // Left click
                    // A bone is selected, check if we're initiating a dragging
                    // of the endpoint
                    if(isSelected && select_type == SelectType.BONE) {
                        if(select_bone.TraceAgainstEnd(gameCoords, Ref.cgame.cg.refdef.w/100f)) {
                            action = Action.MOVEBONE;
                            if(Ref.Input.IsKeyPressed(Keyboard.KEY_LSHIFT))
                                action = Action.MOVEBONE2;
                            select_bone.SetBoneEndPoint(gameCoords, action == Action.MOVEBONE);
                            return;
                        }
                    }

                    Bone bone_hit = model.TraceForBone(gameCoords, Ref.cgame.cg.refdef.w/100f);
                    if(bone_hit != null) {
                        SelectBone(bone_hit);
                    }
                }
            }
        }
        
    }

}
