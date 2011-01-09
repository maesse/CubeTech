package cubetech;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.misc.Ref;
import cubetech.spatial.Cell;
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
    // lower left corner
    private Vector2f Position;
    // center of block
    private Vector2f Center = new Vector2f(); // auto-set when position is set
    // direction vectors
    private Vector2f[] Axis;
    // block size
    private Vector2f Size = new Vector2f();
    // block half-size
    private Vector2f Extent = new Vector2f(); // auto-set when size is set
    // block absolute half-size
    private Vector2f AbsExtent = new Vector2f();
    // block handle -- id
    public int Handle;
    // current angle (changes Axis[])
    private float Angle;

    // Texture info
    public Vector2f TexOffset;
    public Vector2f TexSize;

    // Collidable?
    public boolean Collidable;
    SpatialHandle spaceHandle = null; // this blocks handle to the spatial system
    public int LastQueryNum = 0; // spatial optimization

    // Custom value, used for monsters, spawn, etc..
    public int CustomVal;

    
    // constructor
    public Block(int Handle, Vector2f Position, Vector2f Size, boolean hookupSpatial) {
        Axis = new Vector2f[2];
        Axis[0] = new Vector2f(1, 0);
        Axis[1] = new Vector2f(0, 1);
        this.Collidable = true;
        SetPosition(Position);
        this.Handle = Handle;
        this.SetSize(Size);
        this.Texture = (CubeTexture)(Ref.ResMan.LoadResource("data/tile.png").Data);
        this.TexOffset = new Vector2f(0, 0);
        this.TexSize = new Vector2f(1, 1);

        if(hookupSpatial) {
            // We start off with no angle, so this is fine
            spaceHandle = Ref.spatial.Create(Position.x, Position.y, Position.x + Size.x, Position.y + Size.y, this);
        }
    }

    public void SpatialHandleChanged(Cell cell, int newIndex) {
        spaceHandle.CellIndexChanged(cell, newIndex);
    }

    

    private void UpdateSpatial() {
        AbsExtent.x = Extent.x * Math.abs(Axis[0].x) + Extent.y * Math.abs(Axis[1].x);
        AbsExtent.y = Extent.x * Math.abs(Axis[0].y) + Extent.y * Math.abs(Axis[1].y);
        
        if(spaceHandle == null)
            return;

        Ref.spatial.Update(spaceHandle, Center.x - AbsExtent.x, Center.y - AbsExtent.y, Center.x + AbsExtent.x, Center.y + AbsExtent.y);
    }

    public void Set(Vector2f position, Vector2f size) {
        // Doesnt call SetPosition and SetSize to save a few updateSpatial calls
        this.Position = position;
        Size.x = size.x;
        Size.y = size.y;
        this.Extent.x = size.x/2f;
        this.Extent.y = size.y/2f;
        this.Center.x = Position.x + Size.x/2f;
        this.Center.y = Position.y + Size.y/2f;
        UpdateSpatial();
    }

    public void Set(Vector2f position, Vector2f size, float angle) {
        // Doesnt call SetPosition and SetSize to save a few updateSpatial calls
        this.Position = position;
        Size.x = size.x;
        Size.y = size.y;
        this.Extent.x = size.x/2f;
        this.Extent.y = size.y/2f;
        this.Center.x = Position.x + Size.x/2f;
        this.Center.y = Position.y + Size.y/2f;
        SetAngle(angle);
        //UpdateSpatial(); // setangle already does this
    }

    public void Render() {
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        spr.Set(Position, Size, Texture, TexOffset, TexSize);
        spr.SetAngle(Angle);

        // Non-collidables are slightly transparent
        if(!Collidable) {
            spr.SetColor(new Vector4f(1, 1, 1, 0.7f));
        }
    }

    // Important to call this, or the block will be removed but still colliable
    public void Remove() {
        if(spaceHandle != null)
            Ref.spatial.Remove(spaceHandle);
    }

    public final void SetPosition(Vector2f position) {
        this.Position = position;
        this.Center.x = position.x + Size.x/2f;
        this.Center.y = position.y + Size.y/2f;
        UpdateSpatial();
    }

    // Calculate non-normalized direction vectors
    public void SetAngle(float angle) {
        Angle = angle;
        Axis[0].x = FastMath.cos(angle);
        Axis[0].y = FastMath.sin(angle);
        Axis[1].x = FastMath.cos(angle+FastMath.PI_HALF);
        Axis[1].y = FastMath.sin(angle+FastMath.PI_HALF);
        UpdateSpatial();
    }

    public final void SetSize(Vector2f size) {
        Size.x = size.x;
        Size.y = size.y;
        this.Extent.x = size.x/2f;
        this.Extent.y = size.y/2f;
        this.Center.x = Position.x + Size.x/2f;
        this.Center.y = Position.y + Size.y/2f;
        UpdateSpatial();
    }

    // NOTE: Outdated, doesn't take angle into account
    public boolean Intersects(Vector2f point) {
        // Scale to rect ints
        if(point.x >= Position.x && point.x <= Position.x + Size.x) // inside x coords
            if(point.y >= Position.y && point.y <= Position.y + Size.y) // inside y coords {
                return true;
        
        return false;
    }

    public Vector2f GetCenter() {
        return Center;
    }

    public Vector2f GetExtents() {
        return Extent;
    }

    public Vector2f[] GetAxis() {
        return Axis;
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
