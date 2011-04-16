package cubetech.CGame;

import cubetech.gfx.AniModel;
import cubetech.gfx.Animation;
import cubetech.gfx.Bone;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.KeyFrame;
import cubetech.gfx.KeyFrameBone;
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
import cubetech.ui.*;
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

    // timeline, etc..
    CScrollPane animateScroll;
    boolean showAnimateScroll = true;
    float scrollHeight = 0f;

    // Menu above timeline
    CContainer animateMenu;
    CButton toggleAnimateScrollButton;

    // Popup-menus
    CContainer popup = null;
    CContainer animationListMenu;


    int FovX = 120;

    public AniModel model = new AniModel();

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

    ButtonEvent recordSnapshotEvent = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            KeyFrameBone snapshot = model.TakeSnapshot();
            Animation currAnim = model.GetCurrentAnimation();
            currAnim.InsertFrame(snapshot);
        }
    };

    ButtonEvent openAnimationSelection = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            
            setPopup(createAnimationList(), mapEditor.OrgCoordsToPixelCoords(Ref.Input.playerInput.MousePos));
        }
    };

    ButtonEvent seekToNextKeyframe = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            int time = model.GetCurrentAnimation().getTime();
            KeyFrame frame = model.GetCurrentAnimation().LocateKeyframe(time);
            if(frame != null && frame.next != null) {
                model.AnimateToTime(frame.next.time);
            }
        }
    };

    ButtonEvent seekToPrevKeyframe = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
            int time = model.GetCurrentAnimation().getTime()-1;
            KeyFrame frame = model.GetCurrentAnimation().LocateKeyframe(time);
            if(frame != null) {
                model.AnimateToTime(frame.time);
            }
        }
    };

    ButtonEvent toggleAnimationScrollEvent = new ButtonEvent() {
        public void buttonPressed(CComponent button, MouseEvent evt) {
           toggleAnimationScroll();
        }
    };

    private void toggleAnimationScroll() {
        showAnimateScroll = !showAnimateScroll;

        // Set texture on togglebutton
        CubeTexture tex = null;
        if(showAnimateScroll)
            tex = Ref.ResMan.LoadTexture("data/arrow.png");
        else
            tex = Ref.ResMan.LoadTexture("data/uparrow.png");
        ((CImage)(toggleAnimateScrollButton).getComponent(0)).setTex(tex);

        // Toggle animateScroll visibility
        if(showAnimateScroll) {
            scrollHeight = animateScroll.getSize().y;
            animateMenu.getPosition().y = Ref.glRef.GetResolution().y - animateMenu.getSize().y - scrollHeight;
        } else {
            animateMenu.getPosition().y = Ref.glRef.GetResolution().y - animateMenu.getSize().y;
            scrollHeight = 0;
        }
    }

    private void initUI() {
        // Menu
        menupanel = new CContainer(new FlowLayout(true, true, false));
        menupanel.setBackground(Ref.ResMan.LoadTexture("data/topmenubar.png"));
        menupanel.setSize(new Vector2f(512, 64));
        menupanel.addComponent(new CButton(Ref.ResMan.LoadTexture("data/edit_game.png"), new Vector2f(32,32), exitAnimationEditor));
        menupanel.doLayout();

        CButton but = null;

        // Menu on top of scroll
        animateMenu = new CContainer(new FlowLayout(true, true, true));
        animateMenu.setBackground(Ref.ResMan.LoadTexture("data/topmenubar.png"));
        animateMenu.setSize(new Vector2f(256, 45));
        animateMenu.addComponent(toggleAnimateScrollButton = new CButton(Ref.ResMan.LoadTexture("data/arrow.png"), new Vector2f(24,24), toggleAnimationScrollEvent));
        animateMenu.addComponent(new CButton(Ref.ResMan.LoadTexture("data/control_start.png"), new Vector2f(24,24), seekToPrevKeyframe));
        animateMenu.addComponent(but = new CButton(Ref.ResMan.LoadTexture("data/control_play.png"), new Vector2f(24,24)));
        but.isToggleButton = true;
        but.toggledTexture = Ref.ResMan.LoadTexture("data/control_stop.png");
        animateMenu.addComponent(new CButton(Ref.ResMan.LoadTexture("data/control_end.png"), new Vector2f(24,24), seekToNextKeyframe));
        animateMenu.addComponent(but = new CButton(Ref.ResMan.LoadTexture("data/lock_open.png"), new Vector2f(24,24)));
        but.isToggleButton = true;
        but.toggledTexture = Ref.ResMan.LoadTexture("data/lock.png");
        animateMenu.addComponent(new CButton(Ref.ResMan.LoadTexture("data/edit_record.png"), new Vector2f(24,24),recordSnapshotEvent));
        String animName = model.GetCurrentAnimation().getName();
        animateMenu.addComponent(new CButton(animName,0.5f, openAnimationSelection));
        Vector2f res = Ref.glRef.GetResolution();
        animateMenu.setPosition(new Vector2f(res.x/2f - 128, res.y - 45 - 100));
        animateMenu.doLayout();

        // Scroll
        animateScroll = new CScrollPane(CContainer.Direction.HORIZONTAL);
        animateScroll.setResizeToChildren(CContainer.Direction.NONE);
        animateScroll.setBackground(Ref.ResMan.LoadTexture("data/rightmenubar.png"));
        CTimeline animTimeline = new CTimeline(80, this);
        animateScroll.setSize2(new Vector2f(Ref.glRef.GetResolution().x, 100));
        animateScroll.addComponent(animTimeline);
        animateScroll.setPosition(new Vector2f(0, Ref.glRef.GetResolution().y - 100));
        scrollHeight = 100;
        animateScroll.doLayout();

        
    }

    private CContainer createAnimationList() {
        // Animation selection popup ui
        animationListMenu = new CContainer(new FlowLayout(false, true, true));
        animationListMenu.setBackground(Ref.ResMan.LoadTexture("data/rightmenubar.png"));
        animationListMenu.addComponent(new CLabel("Select Animation:", Align.LEFT, 0.75f));

        for (String string : model.GetAnimationNames()) {
            animationListMenu.addComponent(new CButton(string, 0.5f));
        }
        animationListMenu.addComponent(new CPanel(new Vector2f(5, 5)));
        animationListMenu.addComponent(new CButton("^3+ Add new",null, Align.CENTER, 0.6f));
        animationListMenu.doLayout();

        return animationListMenu;
    }

    private void setPopup(CContainer cont, Vector2f position) {
        if(cont != null) {
            Vector2f res = Ref.glRef.GetResolution();
            Vector2f popupSize = cont.getLayoutSize();
            Vector2f finalpos = new Vector2f(position);
            if(position.x + popupSize.x > res.x) {
                finalpos.x = res.x - popupSize.x;
            }
            if (position.y + popupSize.y > res.y) {
                finalpos.y = res.y - popupSize.y;
            }
            cont.setPosition(finalpos);
            cont.border = 2;
        }
        popup = cont;
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

        // UI
        menupanel.Render(new Vector2f());
        animateMenu.Render(new Vector2f());
        if(showAnimateScroll)
            animateScroll.Render(new Vector2f());
        if(popup != null)
            popup.Render(new Vector2f());

        mapEditor.renderGrid();

        if(isSelected && select_type == SelectType.BONE)
            select_bone.selected = true;
        model.Render(new Vector2f());

        RenderInformation();
    }

    private void RenderInformation() {
        // Draw cursor position
        Vector2f mouseCoords = mapEditor.OrgCoordsToGameCoords(lastMouseCoords);
        Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight() - scrollHeight),
                "Cursor: [" + (int)mouseCoords.x + "," + (int)mouseCoords.y + "]", Align.RIGHT, (Color) Color.GREY, null, Type.HUD, 1);

        // Draw grid scale
        float scale = Ref.glRef.GetResolution().x / FovX;
        float lineThickness = 3;
        float width = mapEditor.gridSpacing * scale;
        // Add text
        Vector2f size = Ref.textMan.AddText(new Vector2f(3,Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight() - scrollHeight),
                (int)mapEditor.gridSpacing + "u", Align.LEFT, null, null, Type.HUD,1 );

        Vector2f position = new Vector2f(size);
        position.x += 4;
        position.y *= 0.5f;
        position.y -= lineThickness * 0.5f - scrollHeight;

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
        
        
        evt.Position = absCoords; // UI's use MouseEvent's, so just set pixel coords

        if(popup != null && popup.containsPoint(absCoords)) {
            popup.MouseEvent(evt);
        } else if(popup != null && evt.Button == 0 && evt.Pressed) {
            popup = null;
        } else if(menupanel.containsPoint(absCoords))
            menupanel.MouseEvent(evt);
        else if(animateScroll.containsPoint(absCoords) && showAnimateScroll) {
            animateScroll.MouseEvent(evt);
        } else if(animateMenu.containsPoint(absCoords)) {
            animateMenu.MouseEvent(evt);
        }
        else {
            // Handle zoom
            if(evt.WheelDelta != 0 && Ref.Input.IsKeyPressed(Keyboard.KEY_LSHIFT)) {
                FovX += evt.WheelDelta * -10;
                if(FovX < 1)
                    FovX = 1;
                return;
            }
            
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
                            Bone bone = model.CreateBone();
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
