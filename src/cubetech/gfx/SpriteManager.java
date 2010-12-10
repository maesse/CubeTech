/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

/**
 *
 * @author mads
 */
public class SpriteManager {
    public Sprite[] Sprites = new Sprite[10000];
    public int[] NormalSprites = new int[10000];
    public int[] HUDSprites = new int[10000];
    public int SpriteOffset = 0;
    public int HUDSpriteOffset = 0;
    public int NormalSpriteOffset = 0;

    public enum Type {
        NORMAL,
        HUD
    }

    public SpriteManager() {
        for (int i= 0; i < Sprites.length; i++) {
            Sprites[i] = new Sprite();
        }
    }

    public Sprite GetSprite(Type type) {
        if(type == Type.NORMAL) {
            if(SpriteOffset >= Sprites.length) {
                System.err.print("SpriteManager: Sprite overflow");
                SpriteOffset = 0;
                }
                NormalSprites[NormalSpriteOffset++] = SpriteOffset;
            return Sprites[SpriteOffset++];
        } else {
            if(SpriteOffset >= Sprites.length) {
                System.err.print("SpriteManager: Sprite overflow");
                SpriteOffset = 0;
                }
            HUDSprites[HUDSpriteOffset++] = SpriteOffset;
            return Sprites[SpriteOffset++];
        }
    }

    public void DrawNormal() {
        
        for (int i= 0; i < NormalSpriteOffset; i++) {
            Sprites[NormalSprites[i]].Draw();
        }
        
    }
    public void DrawHUD() {
        for (int i= 0; i < HUDSpriteOffset; i++) {
            Sprites[HUDSprites[i]].Draw();
        }
    }

    public void Reset() {
        SpriteOffset = 0;
        HUDSpriteOffset = 0;
        NormalSpriteOffset = 0;
    }
}
