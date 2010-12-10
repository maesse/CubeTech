/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.TextManager.Align;
import cubetech.input.Key;
import cubetech.input.KeyEvent;
import cubetech.input.KeyEventListener;

import cubetech.misc.Button;
import cubetech.misc.Ref;
import java.util.ArrayList;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.CircleDef;
import org.jbox2d.collision.PolygonDef;
import org.jbox2d.collision.PolygonShape;
import org.jbox2d.collision.ShapeDef;
import org.jbox2d.collision.ShapeType;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.common.XForm;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.DebugDraw;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Current world (level) that is loaded, and it's state
 * @author mads
 */
public class World implements KeyEventListener {
    Block[] Blocks = new Block[10000];
    //ArrayList<Block> Blocks = new ArrayList<Block>();
    Mode mode = Mode.Edit; // Start in edit for now
    public Camera camera;
    int NextBlockHandle = 0;

    CubeTexture scaleTex;
    CubeTexture background;
    CubeTexture dims;
    CubeTexture toFront;
    CubeTexture grid;
    CubeTexture open;
    CubeTexture save;
    CubeTexture newfile;

    boolean EnableBackground = true;

    org.jbox2d.dynamics.World b2World;

    // Edit mode
    int selectedHandle = -1;
    boolean editMouseLock = false;
    Corner editCorder = Corner.BOTTOMLEFT;
    EditFunc editFunc = EditFunc.NONE;
    Vector2f editMouseOrigin;
    Vector2f texScaleStart;
    Button toFrontButton;
    Button gridButton;
    Button openButton;
    Button saveButton;
    Button newButton;
    float editAngle;
    boolean gridEnabled = false;
    EditFunc oldFunc = EditFunc.NONE;

    ArrayList<CubeTexture> editTextures = new ArrayList<CubeTexture>();
    int editTextureSelected;

    Player player;
    DebugDraw  dd;

    public void KeyPressed(KeyEvent evt) {
        Key event = (Key) evt.getSource();
        if(event.key == Keyboard.KEY_F8) {
            if(mode == Mode.Edit)
                LoadGame();
            else
                LoadEdit();
        }
    }

    void LoadGame() {
        mode = Mode.Game;
        AABB worldaabb = new AABB(new Vec2(0, 0), new Vec2(1000, 100));
        b2World = new org.jbox2d.dynamics.World(worldaabb, new Vec2(0, -16), false);

        for (int i= 0; i < NextBlockHandle; i++) {
            Block block = Blocks[i];
            if(block == null)
                continue;

            PolygonDef sd = new PolygonDef();
            sd.setAsBox(BoxScale(block.Size.x/2f), BoxScale(block.Size.y/2f));
            //sd.setAsBox(block.Size.x, block.Size.y, new Vec2(block.Size.x, block.Size.y), block.Angle);
            sd.density = 0f;
            
            BodyDef def = new BodyDef();
            def.position.set(BoxScale(block.Position.x+block.Size.x/2f), BoxScale(block.Position.y+block.Size.y/2f));
            //def.angle = block.Angle;
            def.fixedRotation = true;
            
            Body body = b2World.createBody(def);
            body.createShape(sd);
            body.setMassFromShapes();
            
        }
        player = new Player(this, new Vec2(BoxScale(camera.Position.x + camera.VisibleSize.x/2f), BoxScale(camera.Position.y + camera.VisibleSize.y/2f)));
        dd = new DebugDraw() {

            @Override
            public void drawPolygon(Vec2[] vec2s, int i, Color3f clrf) {
                GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_LINE);
                GL11.glPolygonMode(GL11.GL_BACK, GL11.GL_LINE);
                 GL11.glBegin(GL11.GL_POLYGON);
                    {
                     GL11.glColor3f(clrf.x, clrf.y, clrf.z);
                for (int j= 0; j < i; j++) {
                    
//                        GL11.glTexCoord2f(TexOffset.x, TexOffset.y+TexSize.y);
                        GL11.glVertex2f(vec2s[j].x*16f, vec2s[j].y*16f);


                    }
                    
                }
                 GL11.glEnd();
                 GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_FILL);
                GL11.glPolygonMode(GL11.GL_BACK, GL11.GL_FILL);
            }

            @Override
            public void drawSolidPolygon(Vec2[] vec2s, int i, Color3f clrf) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void drawCircle(Vec2 vec2, float f, Color3f clrf) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void drawSolidCircle(Vec2 vec2, float f, Vec2 vec21, Color3f clrf) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void drawPoint(Vec2 vec2, float f, Color3f clrf) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void drawSegment(Vec2 vec2, Vec2 vec21, Color3f clrf) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void drawXForm(XForm xform) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void drawString(float f, float f1, String string, Color3f clrf) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
        b2World.setDebugDraw(dd);
        
    }

    void LoadEdit() {
        mode = Mode.Edit;
    }

    public enum Mode {
        Game,
        Edit
    }

    public enum EditFunc {
        NONE,
        SELECT,
        CORNER,
        MOVE,
        COPY,
        CAMERAMOVE,
        TEXSCALE,
        TEXMOVE,
        ROTATE
    }

    public enum Corner {
        BOTTOMLEFT,
        BOTTOMRIGHT,
        TOPLEFT,
        TOPRIGHT
    }

    public World() {
        scaleTex = (CubeTexture)(Ref.ResMan.LoadResource("data/scale.png").Data);
        background = (CubeTexture)(Ref.ResMan.LoadResource("data/horizont.png").Data);
        dims = (CubeTexture)(Ref.ResMan.LoadResource("data/dims.png").Data);
        toFront = (CubeTexture)(Ref.ResMan.LoadResource("data/tofront.png").Data);
        grid = (CubeTexture)(Ref.ResMan.LoadResource("data/grid.png").Data);

        open = (CubeTexture)(Ref.ResMan.LoadResource("data/open.png").Data);
        save = (CubeTexture)(Ref.ResMan.LoadResource("data/save.png").Data);
        newfile = (CubeTexture)(Ref.ResMan.LoadResource("data/new.png").Data);

        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/tile.png").Data));
        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/dims.png").Data));
        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/horizont.png").Data));

        newButton = new Button(" ", new Vector2f(0.925f, 0.85f),  new Vector2f(0.04f, 0.04f), newfile);
        openButton = new Button(" ", new Vector2f(0.925f, 0.8f),  new Vector2f(0.04f, 0.04f), open);
        saveButton = new Button(" ", new Vector2f(0.925f, 0.75f),  new Vector2f(0.04f, 0.04f), save);
        
        toFrontButton = new Button(" ", new Vector2f(0.925f, 0.7f),  new Vector2f(0.04f, 0.04f), toFront);
        gridButton = new Button(" ", new Vector2f(0.925f, 0.65f),  new Vector2f(0.04f, 0.04f), grid);

        camera = new Camera(new Vector2f(), 192);
        Blocks[NextBlockHandle] = new Block(NextBlockHandle++, new Vector2f(0,0), new Vector2f(16,16));
        Blocks[NextBlockHandle] = new Block(NextBlockHandle++, new Vector2f(16,0), new Vector2f(16,16));
        Blocks[NextBlockHandle] = new Block(NextBlockHandle++, new Vector2f(32,0), new Vector2f(16,16));
        Blocks[NextBlockHandle] = new Block(NextBlockHandle++, new Vector2f(64,0), new Vector2f(16,16));
        Blocks[NextBlockHandle] = new Block(NextBlockHandle++, new Vector2f(64,16), new Vector2f(16,16));
        Blocks[NextBlockHandle] = new Block(NextBlockHandle++, new Vector2f(64,32), new Vector2f(16,16));
        Blocks[NextBlockHandle] = new Block(NextBlockHandle++, new Vector2f(96,0), new Vector2f(16,16));
        Blocks[NextBlockHandle] = new Block(NextBlockHandle++, new Vector2f(128,96), new Vector2f(16,16));
        Ref.Input.AddKeyEventListener(this);
    }

    public void Render(int msec) {
        if(mode == Mode.Edit) {
            RenderEdit(msec);
        } else {
            RenderGame(msec);
        }
    }

    private float BoxScale(float f) {
        return f/16f;
    }

    private void RenderGame(int msec) {
        float step = (float)1f/60f;

        if(mode == Mode.Game)
            b2World.step(step, 10);
        

        camera.PositionCamera();
        if(EnableBackground) {
            RenderBackground();
        }
        for (int i= 0; i < NextBlockHandle; i++) {
            if(Blocks[i] != null)
                Blocks[i].Render();
        }

        if(mode == Mode.Game) {
            player.Update();
            player.Render();
//            b2World.drawDebugData();
            }

    }

    private void RenderBackground() {
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
        
        float yoffset = (camera.Position.y) / 650f;
        if(yoffset > 1f)
            yoffset = 1f;
        yoffset = 0.62f - yoffset * 0.38f;
        
        spr.Set(camera.Position, camera.VisibleSize, background, new Vector2f(0, yoffset), new Vector2f(1, yoffset-0.33f));
    }

    Vector2f MousePosToWorldPos(Vector2f mousePos) {
        return new Vector2f(camera.Position.x + mousePos.x * 192f, camera.Position.y + mousePos.y * camera.VisibleSize.y);
    }

    boolean EditModeHUD() {
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(new Vector2f(0.92f, 0.1f), new Vector2f(0.05f, 0.8f), null, new Vector2f(0, 0), new Vector2f(0, 0));
        spr.Color = new Vector4f(0, 0, 0, 0.4f);


        // Check if mouse is hovering the hud
        boolean hit = false;
        Vector2f mouseLocalPos = Ref.Input.playerInput.MousePos;
        if(mouseLocalPos.x >= 0.92f && mouseLocalPos.x <= 0.97f) // inside x coords
            if(mouseLocalPos.y >= 0.1f && mouseLocalPos.y <= 0.9f)
                hit = true;




        HandleEditMenuButtons();

        return hit;
    }

    void HandleEditMenuButtons() {
        Vector2f mouseLocalPos = Ref.Input.playerInput.MousePos;

        if(newButton.Intersects(mouseLocalPos) && Ref.Input.playerInput.Mouse1 && Ref.Input.playerInput.Mouse1Diff) {
            // New
        }

        if(openButton.Intersects(mouseLocalPos) && Ref.Input.playerInput.Mouse1 && Ref.Input.playerInput.Mouse1Diff) {
            // Open
        }
        
        if(saveButton.Intersects(mouseLocalPos) && Ref.Input.playerInput.Mouse1 && Ref.Input.playerInput.Mouse1Diff) {
            // Save
        }

        if(toFrontButton.Intersects(mouseLocalPos) && Ref.Input.playerInput.Mouse1 && Ref.Input.playerInput.Mouse1Diff) {
            // Do it
            if(selectedHandle > 0) {
                SendToFront(Blocks[selectedHandle]);
            }
        }

        if(gridButton.Intersects(mouseLocalPos) && Ref.Input.playerInput.Mouse1&& Ref.Input.playerInput.Mouse1Diff) {
            gridEnabled = !gridEnabled;
            System.out.println("Diff");
        }
        else if(gridEnabled)
            gridButton.OnMouseOver();

        saveButton.Render();
        newButton.Render();
        openButton.Render();
        toFrontButton.Render();
        gridButton.Render();
    }

    void SendToFront(Block block) {
        Block newblock = new Block(NextBlockHandle, new Vector2f(block.Position.x, block.Position.y), new Vector2f(block.Size.x, block.Size.y));
        newblock.Texture = block.Texture;
        newblock.Angle = block.Angle;
        newblock.TexOffset = new Vector2f(block.TexOffset.x, block.TexOffset.y);
        newblock.TexSize = new Vector2f(block.TexSize.x, block.TexSize.y);
        selectedHandle = NextBlockHandle;
        Blocks[NextBlockHandle++] = newblock;
        Blocks[block.Handle] = null;
        
    }

    void DrawTextureBar() {
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(new Vector2f(0.05f, 0.0f), new Vector2f(0.9f, 0.22f), null, new Vector2f(0,0),new Vector2f(1,1));
        spr.Color = new Vector4f(0,0,0,0.4f);

        if(editTextureSelected < 0)
            return;

        // Take input
        if(Ref.Input.playerInput.WheelDelta != 0) {
            if(Ref.Input.playerInput.WheelDelta> 0)
                editTextureSelected++;
            else
                editTextureSelected--;

            // Wrap
            if(editTextureSelected >= editTextures.size())
                editTextureSelected = editTextureSelected - editTextures.size();
            else if(editTextureSelected < 0)
                editTextureSelected = editTextures.size()+editTextureSelected;

            Blocks[selectedHandle].Texture = editTextures.get(editTextureSelected);
            
        }

        spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(new Vector2f(0.5f, 0.1f), 0.1f, editTextures.get(editTextureSelected));

        int numright = editTextures.size()-editTextureSelected-1;
        if(numright > 4)
            numright = 4;
        
        for (int i = 0; i < numright; i++) {
            float invIFrac = (float)(numright - i)/(float)numright;

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.Set(new Vector2f(0.5f+i*0.1f+0.12f, 0.1f), 0.05f, editTextures.get(editTextureSelected+1+i));
            spr.Color = new Vector4f(1, 1, 1, 0.3f + 0.7f * invIFrac);
        }

        int numleft = editTextureSelected;
        if(numleft > 4)
            numleft = 4;
        for (int i = 0; i < numleft; i++) {
            float invIFrac = (float)(numleft - i)/(float)numleft;

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.Set(new Vector2f(0.5f-i*0.1f-0.12f, 0.1f), 0.05f, editTextures.get(editTextureSelected-1-i));
            spr.Color = new Vector4f(1, 1, 1, 0.4f + 0.6f * invIFrac);
        }

    }

    void HandleEditMode() {

        Vector2f mouseWorldPos = MousePosToWorldPos(Ref.Input.playerInput.MousePos);

        if(editFunc == EditFunc.NONE || editFunc == EditFunc.SELECT) {
            if(Ref.Input.playerInput.Mouse3) {
                oldFunc = editFunc;
                editFunc = EditFunc.CAMERAMOVE;
                editMouseOrigin = mouseWorldPos;
            }
        } else if(editFunc == EditFunc.CAMERAMOVE) {
            Vector2f diff = new Vector2f(mouseWorldPos.x - editMouseOrigin.x, mouseWorldPos.y - editMouseOrigin.y);
            camera.Position.x += diff.x * 0.5f;
            camera.Position.y += diff.y * 0.5f;
            editMouseOrigin = mouseWorldPos;
            if(!Ref.Input.playerInput.Mouse3)
                editFunc = oldFunc;
        }

        EditModeHUD();

        // Check if user is trying to select a block
        int newSelect = -2;
        if(Ref.Input.playerInput.Mouse1 && !editMouseLock && Ref.Input.playerInput.Mouse1Diff) {
            boolean found = false;
            for (int i= NextBlockHandle-1; i >=0 ; i--)
            {
                Block block = Blocks[i];
                if(block == null)
                    continue;
                if(block.Intersects(mouseWorldPos)) {
                    // Select block in next frame
                    newSelect = block.Handle;
                    found = true;
                    break;
                }
            }
            if(!found)
                newSelect = -1;
        }
        
        // A block is selected
        if(selectedHandle >= 0) {
            if(editFunc == EditFunc.NONE)
                editFunc = EditFunc.SELECT;
            Block block = Blocks[selectedHandle];
            
            switch(editFunc) {
                case NONE:
                    break;
                case SELECT:
                    if(Ref.Input.playerInput.Mouse1 && !editMouseLock && Ref.Input.playerInput.Mouse1Diff) {
                        if(CheckCornerHit(mouseWorldPos, block) && !Ref.Input.IsKeyPressed(Keyboard.KEY_LCONTROL) && !Ref.Input.IsKeyPressed(Keyboard.KEY_SPACE)) {
                            editFunc = EditFunc.CORNER;
                        } else if(CheckBoundsHit(mouseWorldPos, block)) {
                            if(Ref.Input.IsKeyPressed(Keyboard.KEY_LSHIFT)){
                                editFunc = EditFunc.COPY;
                                editMouseOrigin = new Vector2f(mouseWorldPos.x, mouseWorldPos.y);
                                editMouseLock = true;
                            } else if(Ref.Input.IsKeyPressed(Keyboard.KEY_LCONTROL) && !Ref.Input.IsKeyPressed(Keyboard.KEY_SPACE)) {
                                editFunc = EditFunc.ROTATE;
                                editMouseOrigin = new Vector2f(mouseWorldPos.x, mouseWorldPos.y);
                                editMouseOrigin.x -= block.Position.x + block.Size.x / 2f;
                                editMouseOrigin.y -= block.Position.y + block.Size.y / 2f;
                                float lenght = 1/(float)Math.sqrt(editMouseOrigin.x * editMouseOrigin.x + editMouseOrigin.y * editMouseOrigin.y);
                                editMouseOrigin.x *= lenght;
                                editMouseOrigin.y *= lenght;
                                editAngle = block.Angle;
                                editMouseLock = true;
                            
                                
                            } else if(!Ref.Input.IsKeyPressed(Keyboard.KEY_LCONTROL) && !Ref.Input.IsKeyPressed(Keyboard.KEY_SPACE)) {
                                editFunc = EditFunc.MOVE;
                                editMouseOrigin = new Vector2f(mouseWorldPos.x, mouseWorldPos.y);
                                editMouseLock = true;
                            }
                        }
                        
                        if(Ref.Input.IsKeyPressed(Keyboard.KEY_LCONTROL) && Ref.Input.IsKeyPressed(Keyboard.KEY_SPACE)) {
                            if(CheckCornerHit(mouseWorldPos, block) && editCorder == Corner.BOTTOMRIGHT) {
                                // Texscale
                                editFunc = EditFunc.TEXSCALE;
                                editMouseOrigin = new Vector2f(mouseWorldPos.x, mouseWorldPos.y);
                                editMouseLock = true;
                                texScaleStart = block.TexSize;
                                    
                            } else if(CheckBoundsHit(mouseWorldPos, block)){
                                editFunc = EditFunc.TEXMOVE;
                                editMouseOrigin = new Vector2f(mouseWorldPos.x, mouseWorldPos.y);
                                editMouseLock = true;
                                texScaleStart = block.TexOffset;
                            }
                        }
                    } else if(Ref.Input.IsKeyPressed(Keyboard.KEY_DELETE)) {
                        // Remove this block
                        Blocks[selectedHandle] = null;
                        selectedHandle = -1;
                        newSelect = -1;
                        editFunc = editFunc.NONE;
                    }
                    if(Ref.Input.IsKeyPressed(Keyboard.KEY_LCONTROL) && Ref.Input.IsKeyPressed(Keyboard.KEY_SPACE))
                        DrawEditBlock2(block.Position, block.Size);
                    else
                        DrawEditBlock(block.Position, block.Size);
                    
                    if(editFunc == EditFunc.SELECT) {
                        // Show texturebar
                        DrawTextureBar();
                    }
                    break;
                case TEXMOVE:
                    Vector2f diff = new Vector2f(mouseWorldPos.x - editMouseOrigin.x, mouseWorldPos.y - editMouseOrigin.y);
                    
                    if(Ref.Input.playerInput.Mouse1) {
                        block.TexOffset = new Vector2f(texScaleStart.x, texScaleStart.y);
                        block.TexOffset.x -= diff.x*0.01f;
                        block.TexOffset.y += diff.y*0.01f;
                    } else {
                        block.TexOffset = new Vector2f(texScaleStart.x, texScaleStart.y);
                        block.TexOffset.x -= diff.x*0.01f;
                        block.TexOffset.y += diff.y*0.01f;
                        editMouseLock = false;
                        editFunc = EditFunc.SELECT;
                    }
                    DrawEditBlock2(block.Position, block.Size);
                    break;
                case TEXSCALE:
                    Vector2f pos = new Vector2f();
                    Vector2f size = new Vector2f();
                    GetCornerTempBlock(block, mouseWorldPos, pos, size);
                    if(Ref.Input.playerInput.Mouse1) {
                        if(editCorder == Corner.BOTTOMRIGHT) {
                            block.TexSize = new Vector2f(texScaleStart.x, texScaleStart.y);
                            block.TexSize.x *= 1f/(size.x / block.Size.x);
                            block.TexSize.y *= 1f/(size.y / block.Size.y);
                        }

                    } else {
                        block.TexSize = new Vector2f(texScaleStart.x, texScaleStart.y);
                        block.TexSize.x *= 1f/(size.x / block.Size.x);
                        block.TexSize.y *= 1f/(size.y / block.Size.y);
                        editMouseLock = false;
                        editFunc = EditFunc.SELECT;
                    }
                    DrawEditBlock2(block.Position, block.Size);
                    break;
                case ROTATE:
                    if(Ref.Input.playerInput.Mouse1) {
                        Vector2f newdir = new Vector2f(mouseWorldPos.x, mouseWorldPos.y);
                        newdir.x -= block.Position.x + block.Size.x / 2f;
                        newdir.y -= block.Position.y + block.Size.y / 2f;

                        float lenght = 1/(float)Math.sqrt(newdir.x * newdir.x + newdir.y * newdir.y);
                        newdir.x *= lenght;
                        newdir.y *= lenght;

                        float angle =  editAngle + (float)(Math.atan2(newdir.y, newdir.x) - Math.atan2(editMouseOrigin.y, editMouseOrigin.x));
                        
                        //float angle = (float)Math.acos(Vector2f.dot(newdir, editMouseOrigin));
                        block.Angle = angle;

                    } else {
                        editMouseLock = false;
                        editFunc = EditFunc.SELECT;
                    }
                    break;
                case CORNER:
                    pos = new Vector2f();
                    size = new Vector2f();
                    GetCornerTempBlock(block, mouseWorldPos, pos, size);
                    if(Ref.Input.playerInput.Mouse1) {
                        pos.x = (int)pos.x;
                        pos.y = (int)pos.y;
                        size.x = (int)size.x;
                        size.y = (int)size.y;
                        block = new Block(-2, pos, size);
                    } else {
                        pos.x = (int)pos.x;
                        pos.y = (int)pos.y;
                        size.x = (int)size.x;
                        size.y = (int)size.y;
                        block.Position = pos;
                        block.Size = size;
                        editMouseLock = false;
                        editFunc = EditFunc.SELECT;
                    }
                    DrawEditBlock(block.Position, block.Size);
                    break;
                case MOVE:
                    diff = new Vector2f(mouseWorldPos.x - editMouseOrigin.x, mouseWorldPos.y - editMouseOrigin.y);
                    if(Ref.Input.playerInput.Mouse1) {
                        block = new Block(-2, new Vector2f((int)(block.Position.x + diff.x), (int)(block.Position.y + diff.y)), block.Size);
                    } else {
                        block.Position.x += diff.x;
                        block.Position.y += diff.y;
                        block.Position.x = (int)block.Position.x;
                        block.Position.y = (int)block.Position.y;
                        
                        editFunc = editFunc.SELECT;
                        editMouseLock = false;
                        
                    }
                    DrawEditBlock(block.Position, block.Size);
                    break;
                case COPY:
                    diff = new Vector2f(mouseWorldPos.x - editMouseOrigin.x, mouseWorldPos.y - editMouseOrigin.y);
                    if(Ref.Input.playerInput.Mouse1) {
                        // Display "ghost"
                        block = new Block(-2, new Vector2f((int)(block.Position.x + diff.x), (int)(block.Position.y + diff.y)), block.Size);
                        if(Ref.Input.playerInput.Mouse2) {
                            // Rightclick while copying aborts
                            editFunc = EditFunc.SELECT;
                            editMouseLock = false;
                        }
                    } else {
                        // Create copy of block
                        Block newblock = new Block(NextBlockHandle, new Vector2f(block.Position.x, block.Position.y), new Vector2f(block.Size.x, block.Size.y));
                        newblock.Texture = block.Texture;
                        newblock.Angle = block.Angle;
                        newblock.TexOffset = new Vector2f(block.TexOffset.x, block.TexOffset.y);
                        newblock.TexSize = new Vector2f(block.TexSize.x, block.TexSize.y);
                        Blocks[NextBlockHandle++] = newblock;
                        
                        newblock.Position.x += diff.x;
                        newblock.Position.y += diff.y;
                        block.Position.x = (int)block.Position.x;
                        block.Position.y = (int)block.Position.y;
                        editFunc = editFunc.SELECT;
                        selectedHandle = newblock.Handle;
                        editMouseLock = false;
                    }
                    DrawEditBlock(block.Position, block.Size);
                    break;
            }

        } else if(editFunc != EditFunc.CAMERAMOVE)
            editFunc = EditFunc.NONE;

        if(!editMouseLock && (newSelect >= 0 || newSelect == -1)) {
                selectedHandle = newSelect;
                if(selectedHandle == -1)
                    editFunc = editFunc.NONE;
                else {
                    //editTextureSelected
                    int selectedTexId = Blocks[selectedHandle].Texture.GetID();
                    int selectedIndex = 0;
                    for (int i= 0; i < editTextures.size(); i++) {
                        if(editTextures.get(i).GetID() == selectedTexId) {
                            selectedIndex = i;
                            break;
                        }
                    }
                    editTextureSelected = selectedIndex;
                }
            }
    }

    private void GetCornerTempBlock(Block block, Vector2f mouseWorldPos, Vector2f pos, Vector2f size) {
        Vector2f oppositeCorner = new Vector2f();
        switch(editCorder) {
            case BOTTOMLEFT:
                oppositeCorner = new Vector2f(block.Position.x + block.Size.x, block.Position.y + block.Size.y);
                break;
            case BOTTOMRIGHT:
                oppositeCorner = new Vector2f(block.Position.x, block.Position.y+block.Size.y);
                break;
            case TOPLEFT:
                oppositeCorner = new Vector2f(block.Position.x+block.Size.x, block.Position.y);
                break;
            case TOPRIGHT:
                oppositeCorner = new Vector2f(block.Position.x, block.Position.y);
                break;
        }
        pos.x = oppositeCorner.x<mouseWorldPos.x?oppositeCorner.x:mouseWorldPos.x;
        pos.y = oppositeCorner.y<mouseWorldPos.y?oppositeCorner.y:mouseWorldPos.y;
        size.x = (oppositeCorner.x>mouseWorldPos.x?oppositeCorner.x:mouseWorldPos.x)-pos.x;
        size.y = (oppositeCorner.y>mouseWorldPos.y?oppositeCorner.y:mouseWorldPos.y)-pos.y;
    }

    private boolean CheckBoundsHit(Vector2f mouseWorldPos, Block block)
    {
        if(mouseWorldPos.x >= block.Position.x && mouseWorldPos.x <= block.Position.x + block.Size.x) // inside x coords
            if(mouseWorldPos.y >= block.Position.y&& mouseWorldPos.y <= block.Position.y + block.Size.y) {
                return true;
            }
        return false;
    }

    private boolean CheckCornerHit(Vector2f mouseWorldPos, Block block) {
        boolean hit = false;
        float dimsSize = 2f;
        // Bottom left
        if(mouseWorldPos.x >= block.Position.x-dimsSize && mouseWorldPos.x <= block.Position.x + dimsSize) // inside x coords
            if(mouseWorldPos.y >= block.Position.y-dimsSize && mouseWorldPos.y <= block.Position.y + dimsSize) {
                editMouseLock = true;
                editCorder = Corner.BOTTOMLEFT;
                hit = true;
            }
        // Bottom right
        if(mouseWorldPos.x >= block.Position.x-dimsSize+block.Size.x && mouseWorldPos.x <= block.Position.x + dimsSize+block.Size.x) // inside x coords
            if(mouseWorldPos.y >= block.Position.y-1f && mouseWorldPos.y <= block.Position.y + 1f) {
                editMouseLock = true;
                editCorder = Corner.BOTTOMRIGHT;
                hit = true;
            }
        // top left
        if(mouseWorldPos.x >= block.Position.x-dimsSize && mouseWorldPos.x <= block.Position.x + dimsSize) // inside x coords
            if(mouseWorldPos.y >= block.Position.y-dimsSize+block.Size.y && mouseWorldPos.y <= block.Position.y + dimsSize+block.Size.y) {
                editMouseLock = true;
                editCorder = Corner.TOPLEFT;
                hit = true;
            }
        // top right
        if(mouseWorldPos.x >= block.Position.x-dimsSize+block.Size.x && mouseWorldPos.x <= block.Position.x + dimsSize+block.Size.x) // inside x coords
            if(mouseWorldPos.y >= block.Position.y-dimsSize+block.Size.y && mouseWorldPos.y <= block.Position.y + dimsSize+block.Size.y) {
                editMouseLock = true;
                editCorder = Corner.TOPRIGHT;
                hit = true;
            }

        return hit;
    }

    private void DrawEditBlock(Vector2f pos, Vector2f size) {
            // Hightlight selected
            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
            spr.Set(pos, size, null, new Vector2f(0,0), new Vector2f(1, 1));
            spr.Color = new Vector4f(1,1,1,0.2f);

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
            spr.Set(pos, 1f, dims);

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
            spr.Set(new Vector2f(pos.x + size.x, pos.y), 1f, dims);

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
            spr.Set(new Vector2f(pos.x + size.x, pos.y + size.y), 1f, dims);

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
            spr.Set(new Vector2f(pos.x, pos.y + size.y), 1f, dims);
    }

    private void DrawEditBlock2(Vector2f pos, Vector2f size) {
            // Hightlight selected
            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
            spr.Set(pos, size, null, new Vector2f(0,0), new Vector2f(1, 1));
            spr.Color = new Vector4f(1,1,1,0.2f);

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
            spr.Set(new Vector2f(pos.x + size.x, pos.y), 1f, dims);
    }

    private void RenderEdit(int msec) {
        if(Ref.Input.IsKeyPressed(Keyboard.KEY_NUMPAD6)) {
            camera.Position.x += 2f;
        }
        if(Ref.Input.IsKeyPressed(Keyboard.KEY_NUMPAD4)) {
            camera.Position.x -= 2f;
        }
        if(Ref.Input.IsKeyPressed(Keyboard.KEY_NUMPAD8)) {
            camera.Position.y += 2f;
        }
        if(Ref.Input.IsKeyPressed(Keyboard.KEY_NUMPAD2)) {
            camera.Position.y -= 2f;
        }
        
        // Game editing / debug mode
        RenderBackground();
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.NORMAL);
        float xoffset = camera.Position.x % 64;
        float yoffset = camera.Position.y % 64;
        Vector2f pos = new Vector2f(camera.Position.x - xoffset, camera.Position.y - yoffset);
        spr.Set(pos, new Vector2f(192+64, 192+64), scaleTex, new Vector2f(0, 0), new Vector2f(4, 4));
        
        EnableBackground = false;
        RenderGame(msec);
        EnableBackground = true;

        HandleEditMode();
        

        Ref.textMan.AddText(new Vector2f(0.5f, 0.9f), "Edit Mode", Align.CENTER);
    }
}
