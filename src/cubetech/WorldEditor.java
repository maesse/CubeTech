/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.TextManager.Align;
import cubetech.misc.Button;
import cubetech.misc.Ref;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class WorldEditor {
    World world;
    
    CubeTexture scaleTex;
    CubeTexture dims;
    CubeTexture toFront;
    CubeTexture grid;
    CubeTexture open;
    CubeTexture save;
    CubeTexture newfile;

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
    CubeTexture[] customTextures = new CubeTexture[9];
    int editTextureSelected;

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

    public WorldEditor(World world) {
        this.world = world;

        scaleTex = (CubeTexture)(Ref.ResMan.LoadResource("data/scale.png").Data);

        dims = (CubeTexture)(Ref.ResMan.LoadResource("data/dims.png").Data);
        toFront = (CubeTexture)(Ref.ResMan.LoadResource("data/tofront.png").Data);
        grid = (CubeTexture)(Ref.ResMan.LoadResource("data/grid.png").Data);

        open = (CubeTexture)(Ref.ResMan.LoadResource("data/open.png").Data);
        save = (CubeTexture)(Ref.ResMan.LoadResource("data/save.png").Data);
        newfile = (CubeTexture)(Ref.ResMan.LoadResource("data/new.png").Data);

        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/tile.png").Data));
        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/dims.png").Data));
        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/horizont.png").Data));
        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/grass.png").Data));
        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/grass2.png").Data));
        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/Tiles_grass_set.png").Data));
        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/Tiles_snow_set.png").Data));
        editTextures.add((CubeTexture)(Ref.ResMan.LoadResource("data/Tiles_lava_set.png").Data));

        customTextures[0] = (CubeTexture)(Ref.ResMan.LoadResource("data/tile.png").Data);
        customTextures[1] = (CubeTexture)(Ref.ResMan.LoadResource("data/playerstart.png").Data);
        customTextures[2] = (CubeTexture)(Ref.ResMan.LoadResource("data/chest0.png").Data);
        customTextures[3] = (CubeTexture)(Ref.ResMan.LoadResource("data/dronehover0.png").Data);
        customTextures[4] = (CubeTexture)(Ref.ResMan.LoadResource("data/bombahwalk0.png").Data);

        newButton = new Button(" ", new Vector2f(0.925f, 0.85f),  new Vector2f(0.04f, 0.04f), newfile);
        openButton = new Button(" ", new Vector2f(0.925f, 0.8f),  new Vector2f(0.04f, 0.04f), open);
        saveButton = new Button(" ", new Vector2f(0.925f, 0.75f),  new Vector2f(0.04f, 0.04f), save);

        toFrontButton = new Button(" ", new Vector2f(0.925f, 0.7f),  new Vector2f(0.04f, 0.04f), toFront);
        gridButton = new Button(" ", new Vector2f(0.925f, 0.65f),  new Vector2f(0.04f, 0.04f), grid);
    }

    // Called when a new map is being loaded
    public void ClearEditor() {
        editFunc = EditFunc.NONE;
        editMouseLock = false;
        selectedHandle = -1;
    }

    private void HandleSaveButton() {
        String filename = File.separator+"map";

        JFileChooser fc = new JFileChooser(new File(filename));
        fc.showSaveDialog(null);
        File selFile = fc.getSelectedFile();

        String name = selFile.getAbsolutePath();
        SaveWorld(name);
    }

    private void HandleOpenButton() {
        String filename = File.separator+"map";

        JFileChooser fc = new JFileChooser(new File(filename));
        fc.showOpenDialog(null);
        File selFile = fc.getSelectedFile();

        String name = selFile.getAbsolutePath();
        world.LoadWorld(name);
    }

    public void SaveWorld(String filename) {
        try {
            FileWriter fs;
            fs = new FileWriter(filename);
            
            BufferedWriter out = new BufferedWriter(fs);
            int count = 0;
            for (int i = 0; i < world.NextBlockHandle; i++) {
                if (world.Blocks[i] != null) {
                    count++;
                }
            }

            // Assemble textures
            out.write(editTextures.size()+"\n");
            for (int i= 0; i < editTextures.size(); i++) {
                out.write(editTextures.get(i).name + "\n");
            }

            
            
            out.write(count + "\n");
            for (int i = 0; i < world.NextBlockHandle; i++) {
                if (world.Blocks[i] != null) {
                    Block b = world.Blocks[i];
                    int selectedTexId = b.Texture.GetID();
                    int selectedIndex = 0;
                    for (int j= 0; j < editTextures.size(); j++) {
                        if(editTextures.get(j).GetID() == selectedTexId) {
                            selectedIndex = j;
                            break;
                        }
                    }
                    out.write(String.format("%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s\n", b.getPosition().x, b.getPosition().y, b.getSize().x, b.getSize().y,
                                                    b.getAngle(), b.TexOffset.x, b.TexOffset.y, b.TexSize.x, b.TexSize.y, selectedIndex, b.Collidable?1:0,b.CustomVal));
                }
            }
            out.close();
            
        } catch (IOException ex) {
            Logger.getLogger(WorldEditor.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

    }

    public void RenderEdit(int msec) {
        if(Ref.Input.IsKeyPressed(Keyboard.KEY_NUMPAD6)) {
            world.camera.Position.x += 2f;
        }
        if(Ref.Input.IsKeyPressed(Keyboard.KEY_NUMPAD4)) {
            world.camera.Position.x -= 2f;
        }
        if(Ref.Input.IsKeyPressed(Keyboard.KEY_NUMPAD8)) {
            world.camera.Position.y += 2f;
        }
        if(Ref.Input.IsKeyPressed(Keyboard.KEY_NUMPAD2)) {
            world.camera.Position.y -= 2f;
        }

        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        float xoffset = world.camera.Position.x % 64;
        float yoffset = world.camera.Position.y % 64;
        Vector2f pos = new Vector2f(world.camera.Position.x - xoffset, world.camera.Position.y - yoffset);
        spr.Set(pos, new Vector2f(192+64, 192+64), scaleTex, new Vector2f(0, 0), new Vector2f(4, 4));

        world.EnableBackground = false;
        world.RenderGame(msec);
        world.EnableBackground = true;

        HandleEditMode();


        Ref.textMan.AddText(new Vector2f(0.5f, 0.9f), "Edit Mode", Align.CENTER);
    }

    void HandleSetEntity(Block block, int keyinput) {
        block.CustomVal = keyinput;
        block.Texture = customTextures[keyinput];
        if(keyinput == 0)
            block.Collidable = true;
        else
            block.Collidable = false;
    }

    void HandleEditMode() {

        Vector2f mouseWorldPos = world.MousePosToWorldPos(Ref.Input.playerInput.MousePos);

        if(editFunc == EditFunc.NONE || editFunc == EditFunc.SELECT) {
            if(Ref.Input.playerInput.Mouse3) {
                oldFunc = editFunc;
                editFunc = EditFunc.CAMERAMOVE;
                editMouseOrigin = mouseWorldPos;
            }
        } else if(editFunc == EditFunc.CAMERAMOVE) {
            Vector2f diff = new Vector2f(mouseWorldPos.x - editMouseOrigin.x, mouseWorldPos.y - editMouseOrigin.y);
            world.camera.Position.x += diff.x * 0.5f;
            world.camera.Position.y += diff.y * 0.5f;
            editMouseOrigin = mouseWorldPos;
            if(!Ref.Input.playerInput.Mouse3)
                editFunc = oldFunc;
        }

        EditModeHUD();

        // Check if user is trying to select a block
        int newSelect = -2;
        if(Ref.Input.playerInput.Mouse1 && !editMouseLock && Ref.Input.playerInput.Mouse1Diff) {
            boolean found = false;
            for (int i= world.NextBlockHandle-1; i >=0 ; i--)
            {
                Block block = world.Blocks[i];
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
            if(editFunc == EditFunc.NONE) {
                editFunc = EditFunc.SELECT;
                editMouseLock = false;
                }
            Block block = world.Blocks[selectedHandle];

            switch(editFunc) {
                case NONE:
                    break;
                case SELECT:
                    editMouseLock = false;
                    if(Ref.Input.GetKey(Keyboard.KEY_TAB).Changed && Ref.Input.GetKey(Keyboard.KEY_TAB).Pressed)
                        block.Collidable = !block.Collidable;

                    if(Ref.Input.GetKey(Keyboard.KEY_0).Pressed && Ref.Input.GetKey(Keyboard.KEY_0).Changed)
                        HandleSetEntity(block, 0);
                    if(Ref.Input.GetKey(Keyboard.KEY_1).Pressed && Ref.Input.GetKey(Keyboard.KEY_1).Changed)
                        HandleSetEntity(block, 1);
                    if(Ref.Input.GetKey(Keyboard.KEY_2).Pressed && Ref.Input.GetKey(Keyboard.KEY_2).Changed)
                        HandleSetEntity(block, 2);
                    if(Ref.Input.GetKey(Keyboard.KEY_3).Pressed && Ref.Input.GetKey(Keyboard.KEY_3).Changed)
                        HandleSetEntity(block, 3);
                    if(Ref.Input.GetKey(Keyboard.KEY_4).Pressed && Ref.Input.GetKey(Keyboard.KEY_4).Changed)
                        HandleSetEntity(block, 4);
                    if(Ref.Input.GetKey(Keyboard.KEY_5).Pressed && Ref.Input.GetKey(Keyboard.KEY_5).Changed)
                        HandleSetEntity(block, 5);
                    if(Ref.Input.GetKey(Keyboard.KEY_6).Pressed && Ref.Input.GetKey(Keyboard.KEY_6).Changed)
                        HandleSetEntity(block, 6);
                    if(Ref.Input.GetKey(Keyboard.KEY_7).Pressed && Ref.Input.GetKey(Keyboard.KEY_7).Changed)
                        HandleSetEntity(block, 7);
                    if(Ref.Input.GetKey(Keyboard.KEY_8).Pressed && Ref.Input.GetKey(Keyboard.KEY_8).Changed)
                        HandleSetEntity(block, 8);
                    if(Ref.Input.GetKey(Keyboard.KEY_9).Pressed && Ref.Input.GetKey(Keyboard.KEY_9).Changed)
                        HandleSetEntity(block, 9);



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
                                editMouseOrigin.x -= block.getPosition().x + block.getSize().x / 2f;
                                editMouseOrigin.y -= block.getPosition().y + block.getSize().y / 2f;
                                float lenght = 1/(float)Math.sqrt(editMouseOrigin.x * editMouseOrigin.x + editMouseOrigin.y * editMouseOrigin.y);
                                editMouseOrigin.x *= lenght;
                                editMouseOrigin.y *= lenght;
                                editAngle = block.getAngle();
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
                        block.Remove();
                        world.Blocks[selectedHandle] = null;
                        selectedHandle = -1;
                        newSelect = -1;
                        editFunc = editFunc.NONE;
                    }
                    if(Ref.Input.IsKeyPressed(Keyboard.KEY_LCONTROL) && Ref.Input.IsKeyPressed(Keyboard.KEY_SPACE))
                        DrawEditBlockTexEdit(block.getPosition(), block.getSize());
                    else
                        DrawEditBlock(block.getPosition(), block.getSize());

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
                    DrawEditBlockTexEdit(block.getPosition(), block.getSize());
                    break;
                case TEXSCALE:
                    Vector2f pos = new Vector2f();
                    Vector2f size = new Vector2f();
                    GetCornerTempBlock(block, mouseWorldPos, pos, size);
                    if(Ref.Input.playerInput.Mouse1) {
                        if(editCorder == Corner.BOTTOMRIGHT) {
                            block.TexSize = new Vector2f(texScaleStart.x, texScaleStart.y);
                            block.TexSize.x *= 1f/(size.x / block.getSize().x);
                            block.TexSize.y *= 1f/(size.y / block.getSize().y);
                        }

                    } else {
                        block.TexSize = new Vector2f(texScaleStart.x, texScaleStart.y);
                        block.TexSize.x *= 1f/(size.x / block.getSize().x);
                        block.TexSize.y *= 1f/(size.y / block.getSize().y);
                        editMouseLock = false;
                        editFunc = EditFunc.SELECT;
                    }
                    DrawEditBlockTexEdit(block.getPosition(), block.getSize());
                    break;
                case ROTATE:
                    if(Ref.Input.playerInput.Mouse1) {
                        Vector2f newdir = new Vector2f(mouseWorldPos.x, mouseWorldPos.y);
                        newdir.x -= block.getPosition().x + block.getSize().x / 2f;
                        newdir.y -= block.getPosition().y + block.getSize().y / 2f;

                        float lenght = 1/(float)Math.sqrt(newdir.x * newdir.x + newdir.y * newdir.y);
                        newdir.x *= lenght;
                        newdir.y *= lenght;

                        float angle =  editAngle + (float)(Math.atan2(newdir.y, newdir.x) - Math.atan2(editMouseOrigin.y, editMouseOrigin.x));

                        //float angle = (float)Math.acos(Vector2f.dot(newdir, editMouseOrigin));
                        System.out.println(""+angle);
                        block.SetAngle(angle);

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
                        block = new Block(-2, pos, size, false);
                    } else {
                        pos.x = (int)pos.x;
                        pos.y = (int)pos.y;
                        size.x = (int)size.x;
                        size.y = (int)size.y;
                        block.Set(pos, size);
                        editMouseLock = false;
                        editFunc = EditFunc.SELECT;
                    }
                    DrawEditBlock(block.getPosition(), block.getSize());
                    break;
                case MOVE:
                    diff = new Vector2f(mouseWorldPos.x - editMouseOrigin.x, mouseWorldPos.y - editMouseOrigin.y);
                    if(Ref.Input.playerInput.Mouse1) {
                        block = new Block(-2, new Vector2f((int)(block.getPosition().x + diff.x), (int)(block.getPosition().y + diff.y)), block.getSize(), false);
                    } else {

                        Vector2f poss = block.getPosition();
                        poss.x += diff.x;
                        poss.y += diff.y;
                        block.SetPosition(poss);

                        editFunc = editFunc.SELECT;
                        editMouseLock = false;

                    }
                    DrawEditBlock(block.getPosition(), block.getSize());
                    break;
                case COPY:
                    diff = new Vector2f(mouseWorldPos.x - editMouseOrigin.x, mouseWorldPos.y - editMouseOrigin.y);
                    if(Ref.Input.playerInput.Mouse1) {
                        // Display "ghost"
                        block = new Block(-2, new Vector2f((int)(block.getPosition().x + diff.x), (int)(block.getPosition().y + diff.y)), block.getSize(), false);
                        if(Ref.Input.playerInput.Mouse2) {
                            // Rightclick while copying aborts
                            editFunc = EditFunc.SELECT;
                            editMouseLock = false;
                        }
                    } else {
                        // Create copy of block
                        Block newblock = new Block(world.NextBlockHandle, new Vector2f(block.getPosition().x, block.getPosition().y), new Vector2f(block.getSize().x, block.getSize().y), true);
                        newblock.Texture = block.Texture;
                        newblock.SetAngle(block.getAngle());
                        newblock.TexOffset = new Vector2f(block.TexOffset.x, block.TexOffset.y);
                        newblock.TexSize = new Vector2f(block.TexSize.x, block.TexSize.y);
                        world.Blocks[world.NextBlockHandle++] = newblock;

                        Vector2f poss = block.getPosition();
                        poss.x += diff.x;
                        poss.y += diff.y;
                        block.SetPosition(poss);
                        
                        newblock.CustomVal = block.CustomVal;
                        newblock.Collidable = block.Collidable;
                        editFunc = editFunc.SELECT;
                        selectedHandle = newblock.Handle;
                        editMouseLock = false;
                    }
                    DrawEditBlock(block.getPosition(), block.getSize());
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
                    int selectedTexId = world.Blocks[selectedHandle].Texture.GetID();
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

            world.Blocks[selectedHandle].Texture = editTextures.get(editTextureSelected);

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

    void HandleEditMenuButtons() {
        Vector2f mouseLocalPos = Ref.Input.playerInput.MousePos;

        if(newButton.Intersects(mouseLocalPos) && Ref.Input.playerInput.Mouse1 && Ref.Input.playerInput.Mouse1Diff) {
            // New
            world.StartNewEmptyGame();
        }

        if(openButton.Intersects(mouseLocalPos) && Ref.Input.playerInput.Mouse1 && Ref.Input.playerInput.Mouse1Diff) {
            // Open
            HandleOpenButton();
        }

        if(saveButton.Intersects(mouseLocalPos) && Ref.Input.playerInput.Mouse1 && Ref.Input.playerInput.Mouse1Diff) {
            // Save
            HandleSaveButton();
        }

        if(toFrontButton.Intersects(mouseLocalPos) && Ref.Input.playerInput.Mouse1 && Ref.Input.playerInput.Mouse1Diff) {
            // Do it
            if(selectedHandle > 0) {
                SendToFront(world.Blocks[selectedHandle]);
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
        Block newblock = new Block(world.NextBlockHandle, new Vector2f(block.getPosition().x, block.getPosition().y), new Vector2f(block.getSize().x, block.getSize().y), true);
        newblock.Texture = block.Texture;
        newblock.SetAngle(block.getAngle());
        newblock.TexOffset = new Vector2f(block.TexOffset.x, block.TexOffset.y);
        newblock.TexSize = new Vector2f(block.TexSize.x, block.TexSize.y);
        newblock.CustomVal = block.CustomVal;
        newblock.Collidable = block.Collidable;
        selectedHandle = world.NextBlockHandle;
        world.Blocks[world.NextBlockHandle++] = newblock;
        block.Remove();
        world.Blocks[block.Handle] = null;

    }

    private void GetCornerTempBlock(Block block, Vector2f mouseWorldPos, Vector2f pos, Vector2f size) {
        Vector2f oppositeCorner = new Vector2f();
        switch(editCorder) {
            case BOTTOMLEFT:
                oppositeCorner = new Vector2f(block.getPosition().x + block.getSize().x, block.getPosition().y + block.getSize().y);
                break;
            case BOTTOMRIGHT:
                oppositeCorner = new Vector2f(block.getPosition().x, block.getPosition().y+block.getSize().y);
                break;
            case TOPLEFT:
                oppositeCorner = new Vector2f(block.getPosition().x+block.getSize().x, block.getPosition().y);
                break;
            case TOPRIGHT:
                oppositeCorner = new Vector2f(block.getPosition().x, block.getPosition().y);
                break;
        }
        pos.x = oppositeCorner.x<mouseWorldPos.x?oppositeCorner.x:mouseWorldPos.x;
        pos.y = oppositeCorner.y<mouseWorldPos.y?oppositeCorner.y:mouseWorldPos.y;
        size.x = (oppositeCorner.x>mouseWorldPos.x?oppositeCorner.x:mouseWorldPos.x)-pos.x;
        size.y = (oppositeCorner.y>mouseWorldPos.y?oppositeCorner.y:mouseWorldPos.y)-pos.y;
    }

    private boolean CheckBoundsHit(Vector2f mouseWorldPos, Block block)
    {
        if(mouseWorldPos.x >= block.getPosition().x && mouseWorldPos.x <= block.getPosition().x + block.getSize().x) // inside x coords
            if(mouseWorldPos.y >= block.getPosition().y&& mouseWorldPos.y <= block.getPosition().y + block.getSize().y) {
                return true;
            }
        return false;
    }

    private boolean CheckCornerHit(Vector2f mouseWorldPos, Block block) {
        boolean hit = false;
        float dimsSize = 2f;
        // Bottom left
        if(mouseWorldPos.x >= block.getPosition().x-dimsSize && mouseWorldPos.x <= block.getPosition().x + dimsSize) // inside x coords
            if(mouseWorldPos.y >= block.getPosition().y-dimsSize && mouseWorldPos.y <= block.getPosition().y + dimsSize) {
                editMouseLock = true;
                editCorder = Corner.BOTTOMLEFT;
                hit = true;
            }
        // Bottom right
        if(mouseWorldPos.x >= block.getPosition().x-dimsSize+block.getSize().x && mouseWorldPos.x <= block.getPosition().x + dimsSize+block.getSize().x) // inside x coords
            if(mouseWorldPos.y >= block.getPosition().y-1f && mouseWorldPos.y <= block.getPosition().y + 1f) {
                editMouseLock = true;
                editCorder = Corner.BOTTOMRIGHT;
                hit = true;
            }
        // top left
        if(mouseWorldPos.x >= block.getPosition().x-dimsSize && mouseWorldPos.x <= block.getPosition().x + dimsSize) // inside x coords
            if(mouseWorldPos.y >= block.getPosition().y-dimsSize+block.getSize().y && mouseWorldPos.y <= block.getPosition().y + dimsSize+block.getSize().y) {
                editMouseLock = true;
                editCorder = Corner.TOPLEFT;
                hit = true;
            }
        // top right
        if(mouseWorldPos.x >= block.getPosition().x-dimsSize+block.getSize().x && mouseWorldPos.x <= block.getPosition().x + dimsSize+block.getSize().x) // inside x coords
            if(mouseWorldPos.y >= block.getPosition().y-dimsSize+block.getSize().y && mouseWorldPos.y <= block.getPosition().y + dimsSize+block.getSize().y) {
                editMouseLock = true;
                editCorder = Corner.TOPRIGHT;
                hit = true;
            }

        return hit;
    }

    private void DrawEditBlock(Vector2f pos, Vector2f size) {
            // Hightlight selected
            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            spr.Set(pos, size, null, new Vector2f(0,0), new Vector2f(1, 1));
            spr.Color = new Vector4f(1,1,1,0.2f);

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            spr.Set(pos, 1f, dims);

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            spr.Set(new Vector2f(pos.x + size.x, pos.y), 1f, dims);

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            spr.Set(new Vector2f(pos.x + size.x, pos.y + size.y), 1f, dims);

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            spr.Set(new Vector2f(pos.x, pos.y + size.y), 1f, dims);
    }

    private void DrawEditBlockTexEdit(Vector2f pos, Vector2f size) {
            // Hightlight selected
            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            spr.Set(pos, size, null, new Vector2f(0,0), new Vector2f(1, 1));
            spr.Color = new Vector4f(1,1,1,0.2f);

            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            spr.Set(new Vector2f(pos.x + size.x, pos.y), 1f, dims);
    }

    
}
