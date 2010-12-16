/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.TextManager;
import cubetech.misc.Ref;
import cubetech.input.KeyEventListener;
import cubetech.input.KeyEvent;
import cubetech.input.Key;
import java.util.ArrayList;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;


/**
 *
 * @author mads
 */
public class Console implements KeyEventListener {
    String cmdLine = "";
    boolean Visible = false;
    
    ArrayList<String> Log = new ArrayList<String>();
    public Console() {
        Ref.Input.AddKeyEventListener(this);

    }

    public void LogLine(String str) {
        if(str.trim().length() == 0)
            return; // Ignore empty line
        
        Log.add(str);
    }

    public void KeyPressed(KeyEvent evt) {
        Key key = (Key)evt.getSource();

        if(key.key == Keyboard.KEY_F10){
            Visible = !Visible;
            return;
        }

        if(!Visible)
            return;

        switch(key.key) {
            case Keyboard.KEY_BACK:
                if(cmdLine.length() > 0)
                    cmdLine = cmdLine.substring(0, cmdLine.length()-1);
                break;
            case Keyboard.KEY_RETURN:
                LogLine(cmdLine);
                cmdLine = "";
                break;
            default:
                if(key.Char != Keyboard.CHAR_NONE)
                    cmdLine += key.Char;
                break;
        }
    }

    public void Render() {
        if(!Visible)
            return;

        // Background
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(new Vector2f(0.5f,0.5f), 0.5f);
        spr.Color = new Vector4f(0,0,0,0.5f);

        // Commandline
        Ref.textMan.AddText(new Vector2f(0,0), cmdLine, TextManager.Align.LEFT);

        // Log
        int maxLines = Ref.loop.mode.getHeight()/32;
        int currLine = 1;
        for (int i = Log.size()-1; i >= 0; i--) {
            Ref.textMan.AddText(new Vector2f(0, (currLine*32f)/(float)Ref.loop.mode.getHeight()), Log.get(i), TextManager.Align.LEFT);

            
            
            if(currLine++ >= maxLines)
                break; // Can't see the rest
        }
    }
}
