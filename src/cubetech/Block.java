/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.misc.Ref;

import org.lwjgl.util.vector.Vector2f;

/**
 * Static block
 * @author mads
 */
public class Block {
    public CubeTexture Texture;
    public Vector2f Position;
    public Vector2f Size;
    public int Handle;
    public float Angle;
    
    public Vector2f TexOffset;
    public Vector2f TexSize;

    public Block(int Handle, Vector2f Position, Vector2f Size) {
        
        this.Position = Position;
        this.Handle = Handle;
        this.Size = Size;
        this.Texture = (CubeTexture)(Ref.ResMan.LoadResource("data/tile.png").Data);
        this.TexOffset = new Vector2f(0, 0);
        this.TexSize = new Vector2f(1, 1);
    }

    public void Render() {
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
        spr.Set(Position, Size, Texture, TexOffset, TexSize);
        spr.Angle = Angle;
    }

    public boolean Intersects(Vector2f point) {
        // Scale to rect ints
        if(point.x >= Position.x && point.x <= Position.x + Size.x) // inside x coords
            if(point.y >= Position.y && point.y <= Position.y + Size.y) // inside y coords {
                return true;
        
        return false;
    }
}
