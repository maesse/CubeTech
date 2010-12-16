/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.misc.Ref;
import cubetech.spatial.SpatialHandle;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;
import org.openmali.FastMath;

/**
 * Static block
 * @author mads
 */
public class Block {
    public CubeTexture Texture;
    private Vector2f Position;

    private Vector2f[] Axis;

    private Vector2f Size;
    public int Handle;
    private float Angle;
    
    public Vector2f TexOffset;
    public Vector2f TexSize;
    public boolean Collidable;

    public int CustomVal;

    SpatialHandle spaceHandle = null;


    public int LastQueryNum = 0;

    public Block(int Handle, Vector2f Position, Vector2f Size, boolean hookupSpatial) {
        Axis = new Vector2f[2];
        Axis[0] = new Vector2f(1, 0);
        Axis[1] = new Vector2f(0, 1);
        this.Collidable = true;
        this.Position = Position;
        this.Handle = Handle;
        this.Size = Size;
        this.Texture = (CubeTexture)(Ref.ResMan.LoadResource("data/tile.png").Data);
        this.TexOffset = new Vector2f(0, 0);
        this.TexSize = new Vector2f(1, 1);
        if(hookupSpatial)
            spaceHandle = Ref.spatial.Create(Position.x, Position.y, Position.x + Size.x, Position.y + Size.y, this);
    }

    public void Remove() {
        if(spaceHandle != null)
            Ref.spatial.Remove(spaceHandle);
    }

    public void SetPosition(Vector2f position) {
        this.Position = position;
        if(spaceHandle != null)
            Ref.spatial.Update(spaceHandle, position.x, position.y, position.x + Size.x, position.y + Size.y);
    }
    public void SetAngle(float angle) {
        Angle = angle;
        Axis[0].x = FastMath.cos(angle);
        Axis[0].y = FastMath.sin(angle);
        Axis[1].x = FastMath.cos(angle+FastMath.PI_HALF);
        Axis[1].y = FastMath.sin(angle+FastMath.PI_HALF);
    }

    public void SetSize(Vector2f size) {
        this.Size = size;
        if(spaceHandle != null)
            Ref.spatial.Update(spaceHandle, Position.x, Position.y, Position.x + Size.x, Position.y + Size.y);
    }

    public void Set(Vector2f position, Vector2f size) {
        this.Position = position;
        this.Size = size;
        if(spaceHandle != null)
            Ref.spatial.Update(spaceHandle, position.x, position.y, position.x + size.x, position.y + size.y);
    }

    public void Set(Vector2f position, Vector2f size, float angle) {
        this.Position = position;
        this.Size = size;
        this.Angle = angle;
        if(spaceHandle != null)
            Ref.spatial.Update(spaceHandle, position.x, position.y, position.x + size.x, position.y + size.y);
    }

    public void Render() {
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        spr.Set(Position, Size, Texture, TexOffset, TexSize);
        spr.Angle = Angle;
        if(!Collidable)
            spr.Color = new Vector4f(1, 1, 1, 0.7f);
    }

    public boolean Intersects(Vector2f point) {
        // Scale to rect ints
        if(point.x >= Position.x && point.x <= Position.x + Size.x) // inside x coords
            if(point.y >= Position.y && point.y <= Position.y + Size.y) // inside y coords {
                return true;
        
        return false;
    }

    public float getAngle() {
        return Angle;
    }

    public Vector2f getPosition() {
        return Position;
    }

    public Vector2f getSize() {
        return Size;
    }
}
