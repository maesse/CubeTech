/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.misc;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.TextManager.Align;
import cubetech.input.KeyEventListener;
import org.lwjgl.util.Rectangle;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class Button {
    public String Text;
    public Rectangle Rect;
    public CubeTexture Texture;
    boolean Hover = false;
    

    public Button(String Text, Vector2f pos, Vector2f size, CubeTexture tex) {
        this.Text = Text;
        this.Texture = tex;
        this.Rect = new Rectangle((int)(pos.x*1000f), (int)(pos.y*1000f), (int)(size.x * 1000f), (int)(size.y*1000f));
    }

    public void Render() {
        // Background
        if(Hover) {
            

            // Render actual bg with a bit of alpha -- this will brighten up the texture due to the white background
            Sprite  spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.Set(new Vector2f(Rect.getX()/1000f, Rect.getY()/1000f), new Vector2f(Rect.getWidth()/1000f, Rect.getHeight()/1000f), Texture, new Vector2f(0,0), new Vector2f(1,1));
            

            // White background
            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.Set(new Vector2f(Rect.getX()/1000f, Rect.getY()/1000f), new Vector2f(Rect.getWidth()/1000f, Rect.getHeight()/1000f), null, new Vector2f(0,0), new Vector2f(1,1));
            spr.Color = new Vector4f(1,1,1,0.2f);
        } else {
            // no mouse over, just display background as normal
            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.Set(new Vector2f(Rect.getX()/1000f, Rect.getY()/1000f), new Vector2f(Rect.getWidth()/1000f, Rect.getHeight()/1000f), Texture, new Vector2f(0,0), new Vector2f(1,1));
        }

        Vector2f textCenter = new Vector2f(Rect.getX()+ Rect.getWidth()/2f, Rect.getY()+3);
        textCenter.x /= 1000f;
        textCenter.y /= 1000f;
        Ref.textMan.AddText(textCenter, Text, Align.CENTER);
        
        // Reset mouse over
        Hover = false;
    }

    // Mouse over this frame
    public void OnMouseOver() {
        Hover = true;
    }

    

    public boolean Intersects(Vector2f point) {
        // Scale to rect ints
        Vector2f test = new Vector2f(point.x * 1000f, point.y * 1000f);
        
        
        if(test.x >= Rect.getX() && test.x <= Rect.getX() + Rect.getWidth()) // inside x coords
            if(test.y >= Rect.getY() && test.y <= Rect.getY() + Rect.getHeight()) // inside y coords {
            {
                Hover = true;
                return true;
            }


        return false;
    }
}
