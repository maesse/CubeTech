package cubetech;

import cubetech.collision.Collision;
import cubetech.collision.CollisionResult;
import cubetech.entities.Bombah;
import cubetech.entities.Drone;
import cubetech.entities.Entity;
import cubetech.entities.PlayerFinish;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.input.Key;
import cubetech.input.KeyEvent;
import cubetech.input.KeyEventListener;
import cubetech.misc.Ref;
import cubetech.spatial.SpatialQuery;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Current world (level) that is loaded, and it's state
 * @author mads
 */
public final class World implements KeyEventListener {
    Block[] Blocks = new Block[10000];
    int NextBlockHandle = 0;

    public ArrayList<Entity> Entities = new ArrayList<Entity>();
    Entity[] entToRemove = new Entity[30];
    int entToRemoveOffset = 0;

    public Camera camera;
    public Player player;
    WorldEditor worldEdit;
    Mode mode = Mode.Game; // Start in edit for now
    
    CubeTexture background;
    boolean EnableBackground = true;

    String[] maps;
    int currentMap = 0;
    boolean collisionDebug = false;
    int collisionDebugOffset = 0;

    public enum Mode {
        Game,
        Edit
    }

    public World() {
        Ref.world = this;
        background = (CubeTexture)(Ref.ResMan.LoadResource("data/horizont.png").Data);

        maps = new String[4];
        maps[0] = "map0.map";
        maps[1] = "map1.map";
        maps[2] = "map2.map";
        maps[3] = "map3.map";

        Ref.Input.AddKeyEventListener(this);

        worldEdit = new WorldEditor(this);
        LoadWorld(maps[0]);
    }

    public void KeyPressed(KeyEvent evt) {
        Key event = (Key) evt.getSource();
        // Check if user want to go to edit-mode
        if(event.key == Keyboard.KEY_F8) {
            if(mode == Mode.Edit)
                LoadGame();
            else
                LoadEdit();
        } else if(event.key == Keyboard.KEY_F7) {
            // Toggle collision debug
            collisionDebug = !collisionDebug;
            if(collisionDebug)
                SetCollisionDebug();
        }

        // Handle collision debug
        if(collisionDebug) {
            if(event.key == Keyboard.KEY_F5)
                collisionDebugOffset--;
            else if(event.key == Keyboard.KEY_F6)
                collisionDebugOffset++;
        }
    }

    public void SetCollisionDebug() {
        collisionDebug = true;
        collisionDebugOffset = Ref.collision.getBufferOffset();
    }

    void RenderCollisionDebug() {
        CollisionResult[] results = Ref.collision.getResultBuffer();
        CollisionResult actual = results[collisionDebugOffset & results.length-1];
        // Draw source rect
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        spr.Set(new Vector2f(actual.Start.x - actual.Extent.x,actual.Start.y - actual.Extent.y), new Vector2f(actual.Extent.x * 2f,actual.Extent.y * 2f), null, new Vector2f(), new Vector2f(1, 1));
        spr.SetColor(new Vector4f(0, 1, 0, 0.5f));
        //spr.Color = new Vector4f(0, 1, 0, 0.5f); // Green

        // Draw hit block if any
        if(actual.hitObject != null && actual.hitObject.getClass() == Block.class) {
            Block block = (Block)actual.hitObject;
            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
            spr.Set(new Vector2f(block.getPosition().x,block.getPosition().y), new Vector2f(block.getSize().x, block.getSize().y), null, new Vector2f(), new Vector2f(1, 1));
            spr.SetColor(new Vector4f(1, 0, 0, 0.5f));
            //spr.Color = new Vector4f(1, 0, 0, 0.5f); // Red
        }

        // Draw where player would be with direction vector applied to position
        spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        spr.Set(new Vector2f(actual.Start.x - actual.Extent.x + actual.Delta.x,actual.Start.y - actual.Extent.y+ actual.Delta.y), new Vector2f(actual.Extent.x * 2f,actual.Extent.y * 2f), null, new Vector2f(), new Vector2f(1, 1));
        spr.SetColor(new Vector4f(0,0, 1, 0.3f));
        //spr.Color = new Vector4f(0,0, 1, 0.3f); // Blue
    }

    public void Render(int msec) {
        if(mode == Mode.Edit) {
            worldEdit.RenderEdit(msec);
        } else {
            // disable entity updating - haxx
            if(collisionDebug)
                mode = Mode.Edit;
            
            RenderGame(msec);

            // enable entity updating again - haxx
            if(collisionDebug) {
                mode = Mode.Game;
                RenderCollisionDebug();
                }
        }
    }

    public void RenderGame(int msec) {
        // Update entities
        if(mode == Mode.Game) {
            if(!player.transforming) {
                entToRemoveOffset = 0;
                for (int i= 0; i < Entities.size(); i++) {
                    Entity ent = Entities.get(i);

                    // Check distance to player..
                    float xdiff = Math.abs(ent.GetPosition().x - Ref.world.player.position.x);
                    if(xdiff > 200f && ent.GetType() != Collision.MASK_BULLETS)
                        continue; // out of range

                    ent.Update(msec);
                    if(ent.ToRemove()) {
                        entToRemove[entToRemoveOffset++] = ent;
                    }
                }
            }

            // Update player
            player.Update(msec);
        }

        if(EnableBackground) {
            RenderBackground();
        }

        // Render all visible blocks
        SpatialQuery result = Ref.spatial.Query(camera.Position.x, camera.Position.y, camera.Position.x + camera.VisibleSize.x, camera.Position.y + camera.VisibleSize.y);
        int queryNum = result.getQueryNum();
        Object object;
        while((object = result.ReadNext()) != null) {
            if(object.getClass() != Block.class)
                continue;
            Block block = (Block)object;
            if(block.LastQueryNum == queryNum)
                continue; // duplicate
            block.LastQueryNum = queryNum;

            if(!BlockVisible(block))
                continue;
            
            block.Render();
        }

        // Render all entities
        for (int i= 0; i < Entities.size(); i++) {
            Entity ent = Entities.get(i);
            ent.Render();
        }

        // Render player
        if(mode == Mode.Game) {
            player.Render();

            // Move camera center towards player
            Vector2f camPos = new Vector2f(player.position.x - 192/2, player.position.y - 70);
            float ms = (float)msec/200f;
            float invms = 1f-ms;
            camera.Position.x = camera.Position.x * invms + camPos.x * ms;
            camera.Position.y = camera.Position.y * invms + camPos.y * ms;
        }
        
        // Update camera position
        camera.PositionCamera();

        // Remove dead entities
        for (int i= 0; i < entToRemoveOffset; i++) {
            Entities.remove(entToRemove[i]);
        }
    }

    // Clears and loads entities defined in the map.
    // Used for resetting a map during load/death/finished-edit
    public void WorldUpdated(boolean commingFromEditor) {
        // Load entities
        Entities.clear();
        for (int i= 0; i < NextBlockHandle; i++) {
            Block block = Blocks[i];
            if(block == null || block.CustomVal == 0)
                continue;

            switch(block.CustomVal) {
                case 1:
                    if(!commingFromEditor)
                    {
                        // Position player at spawn
                        player.position = new Vector2f(block.getPosition().x + block.getSize().x /2f, block.getPosition().y + block.getSize().y/2f);
                    }
                    break;
                case 2:
                    PlayerFinish finish = new PlayerFinish(block.getPosition());
                     Entities.add(finish);
                    // Add goal
                    break;
                case 3:
                    Drone drone = new Drone(new Vector2f(block.getPosition().x + block.getSize().x /2f, block.getPosition().y + block.getSize().y/2f));
                    Entities.add(drone);
                    break;
                case 4:
                    Bombah bombah = new Bombah(new Vector2f(block.getPosition().x + block.getSize().x /2f, block.getPosition().y + block.getSize().y/2f));
                    Entities.add(bombah);
                    break;
            }
        }
    }

    // Determines if a block is visible in the current camera view
    boolean BlockVisible(Block block) {
        if(mode == Mode.Game && block.CustomVal != 0)
            return false;

        float dist = block.getPosition().x - camera.Position.x;
        if(dist > 192f || dist < -10f -block.getSize().x)
            return false;
        
        dist = block.getPosition().y - camera.Position.y + block.getSize().y;
        if(dist > 150f + block.getSize().y || dist < -10f-block.getSize().y)
            return false;

        return true;
    }

    void LoadEdit() {
        mode = Mode.Edit;
    }

    // Reset game. If last mode was Edit, don't spawn player at the playerStart entitity
    void LoadGame() {
        boolean fromEdit = (mode == Mode.Edit);
        mode = Mode.Game;
        player = new Player(this, new Vector2f(camera.Position.x + camera.VisibleSize.x/2f, camera.Position.y + camera.VisibleSize.y/2f));

        // Update collision data
        WorldUpdated(fromEdit);
    }

    // Load next map, remembering important playerstate-stuff
    boolean LoadNextMap(int lives, float energy, boolean bigguy, int score) {
        currentMap++;

        if(maps.length <= currentMap) {
            // No more maps!
            currentMap = 0;
            return false;
        }

        String map = maps[currentMap];

        boolean result = LoadWorld(map);
        player.ResetPlayer();
        player.lives = lives;
        player.energy = energy;
        player.bigguy = bigguy;
        player.score = score;
        return result;
    }

    // Create a new world, filled with a default border
     public void StartNewEmptyGame() {
         ClearWorld();
         NextBlockHandle = 0;
        camera = new Camera(new Vector2f(), 192);

        int numx = 80;
        int numy = 5;
        int height = 40;

        for (int i= 0; i < numy; i++) {
            for (int j= 0; j < numx; j++) {
                Blocks[NextBlockHandle] = new Block(NextBlockHandle, new Vector2f(16f*j,-16f*i), new Vector2f(16,16), true);
                Blocks[NextBlockHandle].Texture = (CubeTexture)(Ref.ResMan.LoadResource("data/tile.png").Data);

                NextBlockHandle++;
            }
        }
        for (int i= 1; i < height; i++) {
             Blocks[NextBlockHandle] = new Block(NextBlockHandle, new Vector2f(0,16f*i), new Vector2f(16,16), true);
                Blocks[NextBlockHandle].Texture = (CubeTexture)(Ref.ResMan.LoadResource("data/tile.png").Data);

                NextBlockHandle++;
         }
        for (int i= 1; i < height; i++) {
             Blocks[NextBlockHandle] = new Block(NextBlockHandle, new Vector2f(16f*(numx-1),16f*i), new Vector2f(16,16), true);
                Blocks[NextBlockHandle].Texture = (CubeTexture)(Ref.ResMan.LoadResource("data/tile.png").Data);

                NextBlockHandle++;
         }
        for (int i= 1; i < numx; i++) {
             Blocks[NextBlockHandle] = new Block(NextBlockHandle, new Vector2f(16f*i,16f*(height-1)), new Vector2f(16,16), true);
                Blocks[NextBlockHandle].Texture = (CubeTexture)(Ref.ResMan.LoadResource("data/tile.png").Data);

                NextBlockHandle++;
         }
        player = new Player(this, new Vector2f(camera.Position.x + camera.VisibleSize.x/2f, camera.Position.y + camera.VisibleSize.y/2f));
        WorldUpdated(false);
    }

     // Clear the current world. Including collision/spatial information.
    public void ClearWorld() {
        NextBlockHandle = 0;
        camera = new Camera(new Vector2f(0, 0), 192);
        worldEdit.ClearEditor();
        Ref.spatial.Clear();
    }

    public boolean LoadWorld(String filename) {
        InputStream is = World.class.getResourceAsStream("data/"+filename);

        NextBlockHandle = 0;
        camera = new Camera(new Vector2f(), 192);
        ClearWorld();

        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        try {
            String parse = in.readLine();
            int nTex = Integer.parseInt(parse);
            CubeTexture[] newTexs = null;
            if(nTex > 0) {
                newTexs = new CubeTexture[nTex];
                for (int i= 0; i < nTex; i++) {
                    parse =in.readLine();
                    newTexs[i] = (CubeTexture)(Ref.ResMan.LoadResource(parse).Data);
                }
            }
            parse = in.readLine();
            int nBlocks = Integer.parseInt(parse);
            if(nBlocks > 0) {

                for (int i = 0; i < nBlocks; i++) {

                    parse = in.readLine();
                    String[] splitted = parse.split("(:)");
                    if(splitted.length != 12)
                        throw new IOException("Failed loading");
                    Vector2f loadPos = new Vector2f(Float.parseFloat(splitted[0]), Float.parseFloat(splitted[1]));
                    Vector2f loadSize = new Vector2f(Float.parseFloat(splitted[2]), Float.parseFloat(splitted[3]));
                    float loadAngle = Float.parseFloat(splitted[4]);
                    Block block = new Block(-1, loadPos, loadSize, true);
                    block.SetAngle(loadAngle);
                    block.TexOffset = new Vector2f(Float.parseFloat(splitted[5]), Float.parseFloat(splitted[6]));
                    block.TexSize = new Vector2f(Float.parseFloat(splitted[7]), Float.parseFloat(splitted[8]));
                    int texid = Integer.parseInt(splitted[9]);
                    if(texid < 0 || texid >= newTexs.length)
                        throw new IOException("Failed loading -- texture doesn't exist");
                    block.Texture = newTexs[texid];
                    block.Collidable = Integer.parseInt(splitted[10])==1?true:false;
                    block.CustomVal = Integer.parseInt(splitted[11]);
                    block.Handle = NextBlockHandle;
                    Blocks[NextBlockHandle] = block;
                    NextBlockHandle++;
                }
            }
            player = new Player(this, new Vector2f(camera.Position.x + camera.VisibleSize.x/2f, camera.Position.y + camera.VisibleSize.y/2f));
            WorldUpdated(false);
            in.close();
            is.close();
        } catch (IOException ex) {
            Logger.getLogger(World.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    private void RenderBackground() {
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
        
        float yoffset = (camera.Position.y) / 650f;
        if(yoffset > 1f)
            yoffset = 1f;
        yoffset = 0.62f - yoffset * 0.38f;
        
        spr.Set(camera.Position, camera.VisibleSize, background, new Vector2f(0, yoffset), new Vector2f(1, yoffset-0.33f));
    }

    // From 0-1 viewspace to worldspace
    public Vector2f MousePosToWorldPos(Vector2f mousePos) {
        return new Vector2f(camera.Position.x + mousePos.x * 192f, camera.Position.y + mousePos.y * camera.VisibleSize.y);
    }
}
