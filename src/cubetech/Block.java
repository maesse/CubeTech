package cubetech;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.misc.Ref;
import cubetech.spatial.Cell;
import cubetech.spatial.SpatialHandle;
import org.lwjgl.util.vector.Vector2f;

/**
 * Static blockdmi
 * @author mads
 */
public class Block {
    private static final int DEPTH_MULTIPLIER = 1;
    public CubeTexture tex;
    // lower left corner
    private Vector2f Position = new Vector2f();
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
    public final int Handle;
    // current angle (changes Axis[])
    private float Angle;

    // Texture info
//    public Vector2f TexOffset;
//    public Vector2f TexSize;

    // Collidable?
    public boolean Collidable;
    SpatialHandle spaceHandle = null; // this blocks handle to the spatial system
    public int LastQueryNum = 0; // spatial optimization

    private boolean removed = false;

    @Override
    public Block clone() {
        Block b = new Block(Handle, Position, Size, false);
        b.SetAngle(Angle);
        b.Collidable = Collidable;
        b.tex = tex;
//        b.Texture = Texture;
//        b.TexOffset = TexOffset;
//        b.TexSize = TexSize;
        
        return b;
    }


    public Vector2f getAbsExtent() {
        return AbsExtent;
    }

    public boolean equals(Block other, boolean ignorePosition) {
        if(Angle == other.Angle && Helper.Equals(Size, Size) 
                && Collidable == other.Collidable && (ignorePosition || Helper.Equals(Position, Position))
                && tex == other.tex)
            return true;
        return false;
        
    }

    // constructor
    public Block(int Handle, Vector2f Position, Vector2f Size, boolean hookupSpatial) {
        Axis = new Vector2f[2];
        Axis[0] = new Vector2f(1, 0);
        Axis[1] = new Vector2f(0, 1);
        this.Collidable = true;
        SetPosition(Position);
        this.Handle = Handle;
        this.SetSize(Size);
        tex = Ref.ResMan.LoadTexture("data/tile.png");

        if(hookupSpatial) {
            // We start off with no angle, so this is fine
            spaceHandle = Ref.spatial.Create(Position.x, Position.y, Position.x + Size.x, Position.y + Size.y, this);
        }
    }


    public void SpatialHandleChanged(Cell cell, int newIndex) {
        spaceHandle.CellIndexChanged(cell, newIndex);
    }

    public Vector2f getAbsSize() {
        float sin = (float)Math.abs(Math.sin(Angle));
        float cos = (float)Math.abs(Math.cos(Angle));
        Vector2f absSize = new Vector2f(cos * Size.x + sin * Size.y, cos * Size.y + sin * Size.x);
        return absSize;
    }

    private void UpdateSpatial() {
        AbsExtent.x = Extent.x * Math.abs(Axis[0].x) + Extent.y * Math.abs(Axis[1].x);
        AbsExtent.y = Extent.x * Math.abs(Axis[0].y) + Extent.y * Math.abs(Axis[1].y);
        
        if(spaceHandle == null)
            return;

        Ref.spatial.Update(spaceHandle, Center.x - AbsExtent.x, Center.y - AbsExtent.y, Center.x + AbsExtent.x, Center.y + AbsExtent.y);
    }

    public void SetCentered(Vector2f center, Vector2f extents) {
        this.Center = new Vector2f(center);
        this.Extent = new Vector2f(extents);
        Size.x = Extent.x*2f;
        Size.y = Extent.y*2f;
        Position.x = center.x - extents.x;
        Position.y = center.y - extents.y;
        UpdateSpatial();
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
        if(removed)
            return;
        Sprite spr = Ref.SpriteMan.GetGameSprite();
        spr.setFromCenter(Position, Size, tex);
        spr.setAngle(Angle);

        // Non-collidables are slightly transparent
        if(!Collidable) {
            spr.setColor(255,255,255,200);
        }
        
    }

    public boolean isLinked() {
        return spaceHandle != null;
    }

    // Important to call this, or the block will be removed but still colliable
    public void Remove() {
        if(spaceHandle != null)
            Ref.spatial.Remove(spaceHandle);
        removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

    public final void SetPosition(Vector2f position) {
        this.Position.set(position);
        this.Center.x = position.x + Size.x/2f;
        this.Center.y = position.y + Size.y/2f;
        UpdateSpatial();
    }

    // Calculate non-normalized direction vectors
    public void SetAngle(float angle) {
        Angle = angle;
        Axis[0].x = (float) Math.cos(angle);
        Axis[0].y = (float) Math.sin(angle);
        Helper.Normalize(Axis[0]);
        Axis[1].x = (float) Math.cos(angle+Math.PI/2f);
        Axis[1].y = (float) Math.sin(angle+Math.PI/2f);
        Helper.Normalize(Axis[1]);
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
        if(removed)
            return false;
        
        if(point.x >= Center.x - AbsExtent.x && point.x <= Center.x + AbsExtent.x ) // inside x coords
            if(point.y >= Center.y - AbsExtent.y && point.y <= Center.y + AbsExtent.y ) // inside y coords {
                return true;
        
        return false;
    }

    public Vector2f GetCenter() {
        return Center;
    }

    public Vector2f GetExtents() {
        return Extent;
    }

    public void setHalfWidth(float hw) {
        Extent.x = hw;
        Size.x = hw * 2f;
        Position.x = Center.x - Extent.x;
         UpdateSpatial();
    }

    public void setHalfHeight(float hh) {
        Extent.y = hh;
        Size.y = hh * 2f;
        Position.y = Center.y - Extent.y;
        UpdateSpatial();
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
