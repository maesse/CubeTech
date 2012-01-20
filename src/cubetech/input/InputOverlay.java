package cubetech.input;

import cubetech.CGame.ViewParams;
import cubetech.common.Helper;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.IRenderCallback;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.misc.Ref;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;


/**
 *
 * @author Mads
 */
public class InputOverlay implements KeyEventListener, MouseEventListener, IRenderCallback {
    private boolean visible = false;
    private float fade = 0;
    private float fadeScale = 4.0f;
    
    public InputOverlay(Input input) {
        input.AddKeyEventListener((KeyEventListener)this, Input.KEYCATCH_OVERLAY);
        input.AddMouseEventListener((MouseEventListener)this, Input.KEYCATCH_OVERLAY);
    }
    
    
    public void setVisible(boolean visible) {
        if(visible == this.visible) return;
        this.visible = visible;
        if(visible) Ref.Input.addKeyCatcher(Input.KEYCATCH_OVERLAY);
        else Ref.Input.removeKeyCatcher(Input.KEYCATCH_OVERLAY);
    }
    
    public void toggleVisible() {
        setVisible(!visible);
    }
    
    public boolean KeyPressed(KeyEvent evt) {
        if(!visible) return true;
        Key k = (Key)evt.getSource();
        if(k.Pressed) {
            switch(k.key) {
                case Keyboard.KEY_LEFT:
                case Keyboard.KEY_A:
                    int newcl = Helper.Clamp(Ref.Input.getKeyboardClient()-1, 0, 3);
                    Ref.Input.setKeyboardClient(newcl);
                    break;
                case Keyboard.KEY_RIGHT:
                case Keyboard.KEY_D:
                    newcl = Helper.Clamp(Ref.Input.getKeyboardClient()+1, 0, 3);
                    Ref.Input.setKeyboardClient(newcl);
                    break;
            }
        }
        return true;
    }

    public void GotMouseEvent(MouseEvent evt) {
        
    }
    
    private void updateFade() {
        float msec = Ref.client.frametime/1000f;
        if(visible && fade < 1.0) {
            fade += msec * fadeScale;
        } else if(!visible && fade > 0.0) {
            fade -= msec * fadeScale;
        }
        fade = Helper.Clamp(fade, 0, 1);
    }
    
    private float getXoffsetForPlayer(int client) {
        Vector2f res = Ref.glRef.GetResolution();
        float border = res.x * 0.2f;
        float resx = res.x -  border;
        float resx1 = resx * 0.25f;
        float tocenter = resx1 * 0.5f;
        return border*0.5f + client * resx1 + tocenter;
    }
    
    private void updateControllers() {
        if(!visible) return;
        for (int i = 0; i < 4; i++) {
            Joystick joy = Ref.Input.getJoystick(i);
            if(joy == null) continue;
            
            // Check for left/right input
            int sideAxis = Ref.Input.j_side_axis.iValue;
            if(joy.baxisChanged[sideAxis]) {
                int direction = joy.baxisValue[sideAxis];
                moveJoystickClient(joy, direction);
            }
        }
    }
    
    private void moveJoystickClient(Joystick joy, int direction) {
        if(direction == 0) return;
        int current = Ref.Input.getJoystickMapping(joy.getIndex());
        int newcl = Helper.Clamp(current+direction, 0, 3);
        Ref.Input.setJoystickClient(joy, newcl);
    }
 
    public void render(ViewParams view) {
        updateFade();
        if(!visible && fade <= 0.0) return;
        updateControllers();
        int fadebyte = (int)(fade * 255f);
        Color color = new Color(255,255,255,fadebyte);
        Vector2f res = Ref.glRef.GetResolution();
        
        // Background
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(10, 10, res.x-20, res.y-20, null, null, null);
        spr.SetColor(0, 0, 0, fadebyte);
        
        Vector2f pos = new Vector2f(res.x*0.5f, res.y*0.05f);
        Ref.textMan.AddText(pos, "Controller Setup", Align.CENTER, color, null, Type.HUD, 3f);
        
        for (int i = 0; i < 4; i++) {
            pos = new Vector2f(getXoffsetForPlayer(i), res.y*0.15f);
            Ref.textMan.AddText(pos, "P" + (i+1), Align.CENTER, color, null, Type.HUD, 3f);
        }
        
        // Draw keyboard
        CubeTexture keyboard = Ref.ResMan.LoadTexture("data/textures/keyboard.png");
        int kbClient = Ref.Input.getKeyboardClient();
        drawController(kbClient, 0, keyboard, "KB/Mouse", color);
        
        // Draw controllers
        CubeTexture controller = Ref.ResMan.LoadTexture("data/textures/controller.png");
        for (int i = 0; i < 4; i++) {
            Joystick j = Ref.Input.getJoystick(i);
            if(j == null) continue;
            drawController(Ref.Input.getJoystickMapping(i), i+1, controller, "PAD" + (i+1), color);
        }
    }
    
    private void drawController(int playerindex, int row, CubeTexture texture, String text, Color color) {
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        Vector2f res = Ref.glRef.GetResolution();
        float xoffset = getXoffsetForPlayer(playerindex);
        float ybase = 0.15f;
        
        float yoffset = 120f + (64 + 30) * row;
        
        float imgRadius = 64f;
        spr.Set(xoffset, res.y * (1f - ybase) - yoffset, imgRadius, texture);
        spr.SetColor(color);
        
        if(text != null) {
            Vector2f pos = new Vector2f(xoffset, res.y*(ybase) + yoffset + imgRadius * 0.3f);
            Ref.textMan.AddText(pos, text, Align.CENTER, color, null, Type.HUD, 1.3f);
        }
        
    }
    
}
