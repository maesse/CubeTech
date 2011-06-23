package cubetech.gfx;

/**
 *
 * @author mads
 */
public class SpriteManager {
    private static final int MAX_SPRITES = 10000;

    public Sprite[] Sprites = new Sprite[MAX_SPRITES];
    public int[] NormalSprites = new int[MAX_SPRITES];
    public int[] HUDSprites = new int[MAX_SPRITES];
    public int SpriteOffset = 0;
    public int HUDSpriteOffset = 0;
    public int NormalSpriteOffset = 0;

    public enum Type {
        GAME,
        HUD
    }

    public SpriteManager() {
        for (int i= 0; i < Sprites.length; i++) {
            Sprites[i] = new Sprite();
        }
    }

    public Sprite GetHUDSprite() {
        return GetSprite(Type.HUD);
    }

    public Sprite GetGameSprite() {
        return GetSprite(Type.GAME);
    }

    public Sprite GetSprite(Type type) {
        if(type == Type.GAME) {
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
        Sprite spr;
        for (int i= 0; i < NormalSpriteOffset; i++) {
            spr = Sprites[NormalSprites[i]];
                spr.Draw();
        }
    }

    public void DrawHUD() {
        for (int i= 0; i < HUDSpriteOffset; i++) {
            Sprites[HUDSprites[i]].Draw();
        }
    }

    public void Reset() {
        // Clear all sprite values, so stuff dont carry over to next frame
        for (int i= 0; i < SpriteOffset; i++) {
            Sprites[i].reset();
        }
        SpriteOffset = 0;
        HUDSpriteOffset = 0;
        NormalSpriteOffset = 0;
    }
}
