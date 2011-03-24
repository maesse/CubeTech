//package cubetech;
//
//import cubetech.collision.CMap;
//import cubetech.collision.Collision;
//import cubetech.collision.CollisionResult;
//import cubetech.common.Common.ErrorCode;
//import cubetech.entities.Entity;
//import cubetech.gfx.CubeTexture;
//import cubetech.gfx.ResourceManager;
//import cubetech.gfx.Sprite;
//import cubetech.gfx.SpriteManager;
//import cubetech.input.Key;
//import cubetech.input.KeyEvent;
//import cubetech.input.KeyEventListener;
//import cubetech.misc.Ref;
//import cubetech.net.NetBuffer;
//import cubetech.spatial.SpatialQuery;
//import java.io.BufferedReader;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//import org.lwjgl.input.Keyboard;
//import org.lwjgl.util.vector.Vector2f;
//import org.lwjgl.util.vector.Vector4f;
//
///**
// * Current world (level) that is loaded, and it's state
// * @author mads
// */
//public final class World implements KeyEventListener {
//    Block[] Blocks = new Block[10000];
//    int NextBlockHandle = 0;
//
//    public ArrayList<Entity> Entities = new ArrayList<Entity>();
//    Entity[] entToRemove = new Entity[30];
//    int entToRemoveOffset = 0;
//
//    public Camera camera;
//    public Player player;
//    WorldEditor worldEdit;
//    Mode mode = Mode.Game; // Start in edit for now
//
//    CubeTexture background;
//    boolean EnableBackground = true;
//
//    String[] maps;
//    int currentMap = 0;
//    boolean collisionDebug = false;
//    int collisionDebugOffset = 0;
//
//    public Vector2f WorldMins = new Vector2f();
//    public Vector2f WorldMaxs = new Vector2f(300,300);
//
//    public enum Mode {
//        Game,
//        Edit
//    }
//
//    public World() {
//        Ref.world = this;
//        background = Ref.ResMan.LoadTexture("data/horizont.png");
////        Ref.Input.AddKeyEventListener(this);
//        worldEdit = new WorldEditor(this);
//        StartNewEmptyGame();
//    }
//
//    public void KeyPressed(KeyEvent evt) {
//        Key event = (Key) evt.getSource();
//        // Check if user want to go to edit-mode
//        if(event.key == Keyboard.KEY_F8) {
//            if(mode == Mode.Edit)
//                LoadGame();
//            else
//                LoadEdit();
//        } else if(event.key == Keyboard.KEY_F7) {
//            // Toggle collision debug
//            collisionDebug = !collisionDebug;
//            if(collisionDebug)
//                SetCollisionDebug();
//        }
//
//        // Handle collision debug
//        if(collisionDebug) {
//            if(event.key == Keyboard.KEY_F5)
//                collisionDebugOffset--;
//            else if(event.key == Keyboard.KEY_F6)
//                collisionDebugOffset++;
//        }
//
//        if(mode == Mode.Game) {
//            player.HandleKey(event);
//        }
//    }
//
//    public void SetCollisionDebug() {
//        collisionDebug = true;
//        collisionDebugOffset = Ref.collision.getBufferOffset();
//    }
//
//    void RenderCollisionDebug() {
//        CollisionResult[] results = Ref.collision.getResultBuffer();
//        CollisionResult actual = results[collisionDebugOffset & results.length-1];
//        // Draw source rect
//        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
//        spr.Set(new Vector2f(actual.Start.x - actual.Extent.x,actual.Start.y - actual.Extent.y), new Vector2f(actual.Extent.x * 2f,actual.Extent.y * 2f), null, new Vector2f(), new Vector2f(1, 1));
//        spr.SetColor(0,255,0,127);
//        //spr.Color = new Vector4f(0, 1, 0, 0.5f); // Green
//
//        // Draw hit block if any
////        if(actual.hitObject != null && actual.hitObject.getClass() == Block.class) {
////            Block block = (Block)actual.hitObject;
////            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
////            spr.Set(new Vector2f(block.getPosition().x,block.getPosition().y), new Vector2f(block.getSize().x, block.getSize().y), null, new Vector2f(), new Vector2f(1, 1));
////            spr.SetColor(1, 0, 0, 0.5f);
////            //spr.Color = new Vector4f(1, 0, 0, 0.5f); // Red
////        }
//
//        // Draw where player would be with direction vector applied to position
//        spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
//        spr.Set(new Vector2f(actual.Start.x - actual.Extent.x + actual.Delta.x,actual.Start.y - actual.Extent.y+ actual.Delta.y), new Vector2f(actual.Extent.x * 2f,actual.Extent.y * 2f), null, new Vector2f(), new Vector2f(1, 1));
//        spr.SetColor(0,0, 255, 80);
//        //spr.Color = new Vector4f(0,0, 1, 0.3f); // Blue
//    }
//
//    public void Render(int msec) {
//        if(mode == Mode.Edit) {
//            worldEdit.RenderEdit(msec);
//        } else {
//            // disable entity updating - haxx
//            if(collisionDebug)
//                mode = Mode.Edit;
//
//            RenderGame(msec);
//
//            // enable entity updating again - haxx
//            if(collisionDebug) {
//                mode = Mode.Game;
//                RenderCollisionDebug();
//            }
//        }
//    }
//
//    public void RenderGame(int msec) {
//        // Update entities
//        if(mode == Mode.Game) {
//            entToRemoveOffset = 0;
//            for (int i= 0; i < Entities.size(); i++) {
//                Entity ent = Entities.get(i);
//
//                // Check distance to player..
//                float xdiff = Math.abs(ent.GetPosition().x - Ref.world.player.position.x);
//                if(xdiff > 200f && ent.GetType() != Collision.MASK_BULLETS)
//                    continue; // out of range
//
//                ent.Update(msec);
//                if(ent.ToRemove()) {
//                    entToRemove[entToRemoveOffset++] = ent;
//                }
//            }
//
//            // Update player
//            player.Update(msec);
//        }
//
//        if(EnableBackground) {
//            RenderBackground();
//        }
//
//        // Render all visible blocks
//        SpatialQuery result = Ref.spatial.Query(camera.Position.x, camera.Position.y, camera.Position.x + camera.VisibleSize.x, camera.Position.y + camera.VisibleSize.y);
//        int queryNum = result.getQueryNum();
//        Object object;
//        while((object = result.ReadNext()) != null) {
//            if(object.getClass() != Block.class)
//                continue;
//            Block block = (Block)object;
////            if(block.LastQueryNum == queryNum)
////                continue; // duplicate
////            block.LastQueryNum = queryNum;
//
//            if(!BlockVisible(block))
//                continue;
//
//            block.Render();
//        }
//
//        // Render all entities
//        for (int i= 0; i < Entities.size(); i++) {
//            Entity ent = Entities.get(i);
//            ent.Render();
//        }
//
//        // Render player
//        if(mode == Mode.Game) {
//            player.Render();
//
//            if(player.health > 0) {
//                // Move camera center towards player
//                Vector2f camPos = new Vector2f(player.position.x - 192/2, player.position.y - 70);
//                float ms = (float)msec/200f;
//                float invms = 1f-ms;
//                camera.Position.x = camera.Position.x * invms + camPos.x * ms;
//                camera.Position.y = camera.Position.y * invms + camPos.y * ms;
//            }
//        }
//
//        // Update camera position
//        camera.PositionCamera();
//
//        // Remove dead entities
//        for (int i= 0; i < entToRemoveOffset; i++) {
//            Entities.remove(entToRemove[i]);
//        }
//    }
//
//    // Clears and loads entities defined in the map.
//    // Used for resetting a map during load/death/finished-edit
//    public void WorldUpdated(boolean commingFromEditor) {
//        // Load entities
//        Entities.clear();
//        for (int i= 0; i < NextBlockHandle; i++) {
//            Block block = Blocks[i];
//            if(block == null || block.CustomVal == 0)
//                continue;
//
//            switch(block.CustomVal) {
//                case 1:
//                    if(!commingFromEditor)
//                    {
//                        // Position player at spawn
//                        player.position = new Vector2f(block.getPosition().x + block.getSize().x /2f, block.getPosition().y + block.getSize().y/2f);
//                    }
//                    break;
//                case 2:
////                    PlayerFinish finish = new PlayerFinish(block.getPosition());
////                     Entities.add(finish);
//                    System.out.println("PlayerFinish not supported any more");
//                    // Add goal
//                    break;
//                case 3:
////                    Drone drone = new Drone(new Vector2f(block.getPosition().x + block.getSize().x /2f, block.getPosition().y + block.getSize().y/2f));
////                    Entities.add(drone);
//                    System.out.println("Drone not supported any more");
//                    break;
//                case 4:
////                    Bombah bombah = new Bombah(new Vector2f(block.getPosition().x + block.getSize().x /2f, block.getPosition().y + block.getSize().y/2f));
////                    Entities.add(bombah);
//                    System.out.println("Bombah not supported any more");
//                    break;
//            }
//        }
//    }
//
//    // Determines if a block is visible in the current camera view
//    boolean BlockVisible(Block block) {
//        if(mode == Mode.Game && block.CustomVal != 0)
//            return false;
//
//        float dist = block.getPosition().x - camera.Position.x;
//        if(dist > 192f || dist < -10f -block.getSize().x)
//            return false;
//
//        dist = block.getPosition().y - camera.Position.y + block.getSize().y;
//        if(dist > 150f + block.getSize().y || dist < -10f-block.getSize().y)
//            return false;
//
//        return true;
//    }
//
//    void LoadEdit() {
//        mode = Mode.Edit;
//    }
//
//    // Reset game. If last mode was Edit, don't spawn player at the playerStart entitity
//    void LoadGame() {
//        boolean fromEdit = (mode == Mode.Edit);
//        mode = Mode.Game;
//        player = new Player(this, new Vector2f(camera.Position.x + camera.VisibleSize.x/2f, camera.Position.y + camera.VisibleSize.y/2f));
//
//        // Update collision data
//        WorldUpdated(fromEdit);
//    }
//
//    // Load next map, remembering important playerstate-stuff
//    boolean LoadNextMap(int lives, float energy, boolean bigguy, int score) {
//        currentMap++;
//
//        if(maps.length <= currentMap) {
//            // No more maps!
//            currentMap = 0;
//            return false;
//        }
//
//        String map = maps[currentMap];
//
//        boolean result = LoadWorld(map);
//        player.ResetPlayer();
//        player.lives = lives;
//        player.energy = energy;
//        //player.bigguy = bigguy;
//        player.score = score;
//        return result;
//    }
//
//    // Create a new world, filled with a default border
//     public void StartNewEmptyGame() {
//         ClearWorld();
//         NextBlockHandle = 0;
//        camera = new Camera(new Vector2f(), 192);
//
//        String mapname = "map";
//        int num = Ref.rnd.nextInt(4)+1;
//        if(num >= 4)
//            num = 3;
//        mapname += ""+num;
//
//        switch(num) {
//            default:
//            case 1:
//                WorldMins.x = -230;
//                WorldMins.y = -128;
//                WorldMaxs.x = 225;
//                WorldMaxs.y = 228;
//                break;
//            case 2:
//                WorldMins.x = -160;
//                WorldMins.y = -239;
//                WorldMaxs.x = 170;
//                WorldMaxs.y = 343;
//                break;
//            case 3:
//                WorldMins.x = -221;
//                WorldMins.y = -190;
//                WorldMaxs.x = 232;
//                WorldMaxs.y = 232;
//                break;
//        }
//
//        LoadWorld(mapname);
//
////       int width = 30; //i
////        int height = 30; //j
////        for (int i= 0; i < width; i++) {
////            for (int j= 0; j < height; j++) {
////
////                Blocks[NextBlockHandle] = new Block(NextBlockHandle, new Vector2f(-16*height/2+16f*i,-16*height/2+16f*j), new Vector2f(16,16), true);
////               CubeTexture tex = (CubeTexture)(Ref.ResMan.LoadResource("data/Cave04.png").Data);
////               if (j < 6){
////                    if (i == 0){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+0f,32f/1024f*18f-32f/1024f*j);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }else if (i > 0 && i != width-1){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+1f/8f,32f/1024f*18f-32f/1024f*j);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }else if (i == width-1){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+2f/8f,32f/1024f*18f-32f/1024f*j);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }
////                }else if (j == 6){
////                    if (i == 0){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+5f/8f,32f/1024f*6);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }else if (i > 0 && i != width-1){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+6f/8f,32f/1024f*6);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }else if (i == width-1){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+7f/8f,32f/1024f*6);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }
////                }else if(j > 0 && j != height-1){
////                    if (i == 0){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+5f/8f,32f/1024f*5f);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }else if (i > 0 && i != width-1){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+6f/8f,32f/1024f*5f);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }else if (i == width-1){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+7f/8f,32f/1024f*5f);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }
////                }else if (j == height-1){
////                    if (i == 0){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+5f/8f,32f/1024f*4f);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }else if (i > 0 && i != width-1){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+6f/8f,32f/1024f*4f);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }else if (i == width-1){
////                    Blocks[NextBlockHandle].Texture = tex;
////                    Blocks[NextBlockHandle].TexOffset = new Vector2f(0.5f/256f+7f/8f,32f/1024f*4f);
////                    Blocks[NextBlockHandle].TexSize = new Vector2f(1/8f,31.5f/1024f);
////                    }
////                }
////                NextBlockHandle++;
////           }
////        }
//
//        player = new Player(this, new Vector2f(camera.Position.x + camera.VisibleSize.x/2f, camera.Position.y + camera.VisibleSize.y/2f));
//        WorldUpdated(false);
//    }
//
//     // Clear the current world. Including collision/spatial information.
//    public void ClearWorld() {
//        NextBlockHandle = 0;
//        camera = new Camera(new Vector2f(0, 0), 192);
//        worldEdit.ClearEditor();
//        Ref.spatial.Clear();
//    }
//
//    public boolean LoadWorld(String filename) {
//        // Let Resource manager load the file for us
//        NetBuffer buf;
//        try {
//            buf = ResourceManager.OpenFileAsNetBuffer(filename, false).getKey();
//        } catch (IOException ex) {
//            System.out.println("LoadWorld: Couldn't load file: " + filename);
//            return false;
//        }
//
//        NextBlockHandle = 0;
//        camera = new Camera(new Vector2f(), 192);
//        ClearWorld();
//        int magic = buf.ReadInt();
//        if(magic != CMap.MAPMAGIC) {
//            System.out.println("LoadWorld: Map is not a valid cubetech map");
//            return false;
//        }
//        int version = buf.ReadInt();
//        if(version != CMap.MAPVERSION) {
//            System.out.println("LoadWorld: Map uses a different version");
//            return false;
//        }
//
//        try {
//            String parse = buf.ReadString().trim();
//            int nTex = Integer.parseInt(parse);
//            CubeTexture[] newTexs = null;
//            if(nTex > 0) {
//                newTexs = new CubeTexture[nTex];
//                for (int i= 0; i < nTex; i++) {
//                    parse = buf.ReadString().trim();
//                    newTexs[i] = Ref.ResMan.LoadTexture(parse);
//                }
//            }
//            parse = buf.ReadString().trim();
//            int nBlocks = Integer.parseInt(parse);
//            if(nBlocks > 0) {
//
//                for (int i = 0; i < nBlocks; i++) {
//
//                    parse = buf.ReadString().trim();
//                    String[] splitted = parse.split("(:)");
//                    if(splitted.length != 12) {
//                        Ref.common.Error(ErrorCode.DROP, "Cannot load map: Invalid map file");
//                    }
//                    Vector2f loadPos = new Vector2f(Float.parseFloat(splitted[0]), Float.parseFloat(splitted[1]));
//                    Vector2f loadSize = new Vector2f(Float.parseFloat(splitted[2]), Float.parseFloat(splitted[3]));
//                    float loadAngle = Float.parseFloat(splitted[4]);
//                    Block block = new Block(NextBlockHandle, loadPos, loadSize, true);
//                    block.SetAngle(loadAngle);
//                    block.TexOffset = new Vector2f(Float.parseFloat(splitted[5]), Float.parseFloat(splitted[6]));
//                    block.TexSize = new Vector2f(Float.parseFloat(splitted[7]), Float.parseFloat(splitted[8]));
//                    int texid = Integer.parseInt(splitted[9]);
//                    if(texid < 0 || texid >= newTexs.length) {
//                        Ref.common.Error(ErrorCode.DROP, "Cannot load map: Unknown texture");
//                    }
//                    block.Texture = newTexs[texid];
//                    block.Collidable = Integer.parseInt(splitted[10])==1?true:false;
//                    block.CustomVal = Integer.parseInt(splitted[11]);
//                    Blocks[NextBlockHandle] = block;
//                    NextBlockHandle++;
//                }
//            }
//            player = new Player(this, new Vector2f(camera.Position.x + camera.VisibleSize.x/2f, camera.Position.y + camera.VisibleSize.y/2f));
//            WorldUpdated(false);
//
//        } catch (NumberFormatException ex) {
//            Logger.getLogger(World.class.getName()).log(Level.SEVERE, null, ex);
//            return false;
//        }
//
//        return true;
//    }
//
//    private void RenderBackground() {
//        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
//
//        float yoffset = (400) / 650f;
//        if(yoffset > 1f)
//            yoffset = 1f;
//        yoffset = 0.62f - yoffset * 0.38f;
//
//        spr.Set(new Vector2f(camera.Position.x - 30, camera.Position.y - 30), new Vector2f(camera.VisibleSize.x + 60, camera.VisibleSize.y + 60), background, new Vector2f(0, yoffset), new Vector2f(1, yoffset-0.33f));
//    }
//
//    // From 0-1 viewspace to worldspace
//    public Vector2f MousePosToWorldPos(Vector2f mousePos) {
//        return new Vector2f(camera.Position.x + mousePos.x * 192f, camera.Position.y + mousePos.y * camera.VisibleSize.y);
//    }
//}
