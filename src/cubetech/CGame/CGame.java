package cubetech.CGame;

import cubetech.Block;
import cubetech.Game.Game;
import cubetech.collision.BlockModel;
import cubetech.collision.ClipmapException;
import cubetech.collision.CollisionResult;
import cubetech.collision.CubeCollision;
import cubetech.collision.SingleCube;
import cubetech.common.*;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.CubeType;
import cubetech.gfx.Shader;
import cubetech.gfx.SkyBox;
import cubetech.gfx.SkyDome;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.SpriteManager.Type;
import cubetech.input.*;
import cubetech.iqm.IQMLoader;
import cubetech.iqm.IQMModel;
import cubetech.misc.Ref;
import cubetech.spatial.Bin;
import cubetech.spatial.SpatialQuery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CGame implements ITrace, KeyEventListener, MouseEventListener {
    public static final int PLAYER_LAYER = -20;
    CVar cg_nopredict = Ref.cvars.Get("cg_nopredict", "0", EnumSet.of(CVarFlags.TEMP));
    CVar cg_smoothclients = Ref.cvars.Get("cg_smoothclients", "0", EnumSet.of(CVarFlags.TEMP));
    CVar cg_errorDecay = Ref.cvars.Get("cg_errorDecay", "100", EnumSet.of(CVarFlags.TEMP));
    CVar cg_viewsize = Ref.cvars.Get("cg_viewsize", "100", EnumSet.of(CVarFlags.TEMP));
    CVar cg_fov = Ref.cvars.Get("cg_fov", "90", EnumSet.of(CVarFlags.ARCHIVE));
    CVar cg_chattime = Ref.cvars.Get("cg_chattime", "5000", EnumSet.of(CVarFlags.ARCHIVE)); // show text for this long
    CVar cg_chatfadetime = Ref.cvars.Get("cg_chatfadetime", "500", EnumSet.of(CVarFlags.ARCHIVE)); // + this time for fading out
    CVar cg_drawSolid = Ref.cvars.Get("cg_drawSolid", "0", EnumSet.of(CVarFlags.NONE));
    CVar cg_editmode = Ref.cvars.Get("cg_editmode", "0", EnumSet.of(CVarFlags.ROM));
    CVar cg_depthnear = Ref.cvars.Get("cg_depthnear", "1", EnumSet.of(CVarFlags.CHEAT));
    CVar cg_depthfar = Ref.cvars.Get("cg_depthfar", "3000", EnumSet.of(CVarFlags.CHEAT));
    CVar cg_viewmode = Ref.cvars.Get("cg_viewmode", "1", EnumSet.of(CVarFlags.NONE));
    CVar cg_drawentities = Ref.cvars.Get("cg_drawentities", "0", EnumSet.of(CVarFlags.ROM));
    CVar cg_drawbin = Ref.cvars.Get("cg_drawbin", "0", EnumSet.of(CVarFlags.NONE));

    CVar cg_viewheight = Ref.cvars.Get("cg_viewheight", "15", EnumSet.of(CVarFlags.ARCHIVE));

    // zoom to this fov
    CVar camera_maxfov = Ref.cvars.Get("camera_maxfov", "1500", EnumSet.of(CVarFlags.ARCHIVE));
    // at this speed
    CVar camera_maxspeed = Ref.cvars.Get("camera_maxspeed", "600", EnumSet.of(CVarFlags.ARCHIVE));

    // allow for smooth zooming
    CVar camera_zoomspeed = Ref.cvars.Get("camera_zoomspeed", "10", EnumSet.of(CVarFlags.ARCHIVE));

    // player centering
    CVar camera_hplayerpos = Ref.cvars.Get("camera_hplayerpos", "0.3", EnumSet.of(CVarFlags.ARCHIVE));
    CVar camera_vplayerpos = Ref.cvars.Get("camera_vplayerpos", "0.5", EnumSet.of(CVarFlags.ARCHIVE));
    
    // Move camera in vertical direction when vertical velocity > this
    CVar camera_vsnapmin = Ref.cvars.Get("camera_vsnapmin", "140", EnumSet.of(CVarFlags.ARCHIVE));
    CVar camera_vsnapmax = Ref.cvars.Get("camera_vsnapmax", "300", EnumSet.of(CVarFlags.ARCHIVE));

    CVar cg_tps = Ref.cvars.Get("cg_tps", "0", EnumSet.of(CVarFlags.NONE));

    public Vector3f rayStart = new Vector3f();
    public Vector3f rayEnd = new Vector3f();
    public int rayTime = 0;

    public CGameState cg;
    CGameStatic cgs;
    public CGameRender cgr;
    public LagOMeter lag;

    ChatLine[] chatLines = new ChatLine[8];
    int chatIndex = 0;
    public CEntity[] cg_entities;
    public int cg_numSolidEntities;
    public int cg_numTriggerEntities;
    public CEntity[] cg_solidEntities = new CEntity[256];
    public CEntity[] cg_triggerEntities = new CEntity[256];
    private HashMap<String, ICommand> commands = new HashMap<String, ICommand>();
    CubeTexture playerTexture;
    MapEditor mapEditor = null;
    public float speed;
    public float lastFov = cg_fov.fValue;
    public float lastVleft = camera_vplayerpos.fValue;

    SkyBox skyBox = new SkyBox("data/sky");

    


    /**
    *=================
    *CG_Init
    *
    *Called after every level change or subsystem restart
    *Will perform callbacks to make the loading info screen update.
    *=================
    **/
    public void Init(int serverMessageSequence, int serverCommandSequence, int ClientNum) {
        cgr = new CGameRender(this);
        lag = new LagOMeter();
        playerTexture = Ref.ResMan.LoadTexture("data/enemy1.png");
        commands.put("+scores", new Cmd_ScoresDown());
        commands.put("-scores", new Cmd_ScoresUp());
        Ref.cvars.Set2("cg_editmode", "0", true);

        for (int i= 0; i < chatLines.length; i++) {
            chatLines[i] = new ChatLine();
        }

        // Clear everything
        cgs = new CGameStatic(Ref.client.cl.GameState, serverMessageSequence, serverCommandSequence);
        cg = new CGameState(ClientNum);
        cg_entities = new CEntity[Common.MAX_GENTITIES];
        for (int i= 0; i < cg_entities.length; i++) {
            cg_entities[i] = new CEntity();
        }
        
        Ref.commands.AddCommand("say", null); // re-direct to server
        Ref.commands.AddCommand("block", null);

        LoadingString("Collision Map");
        if(!Ref.client.servername.equalsIgnoreCase("localhost")) {
            Ref.cm.EmptyCubeMap();
        }
//        try {
//            if(Ref.client.clc.mapdata != null)
//                Ref.cm.LoadBlockMap(Ref.client.clc.mapdata, true);
//            else
//                Ref.cm.LoadBlockMap(cgs.mapname, true);
//        } catch (ClipmapException ex) {
//            Ref.common.Error(Common.ErrorCode.DROP, "Couldn't load map:_" + Common.getExceptionString(ex));
//        }

        cg.loading = true; // Dont defer now

        InitConsoleCommands();

        LoadingString("Sounds");
        cgs.RegisterSound();
        cgs.media.Load();

        LoadingString("Graphics");
        cgs.RegisterGraphics();

        LoadingString("Clients");
        cgs.RegisterClients(ClientNum);

        cg.loading = false;

        InitLocalEntities();

        cg.infoScreenText = "";
        
        cgs.SetConfigValues();

        LoadingString("");

        Ref.Input.AddKeyEventListener(this, Input.KEYCATCH_CGAME);
        Ref.Input.AddMouseEventListener(this, Input.KEYCATCH_CGAME);
        
    }

    public void DrawActiveFrame(int serverTime) {
        // UpdateCVars
        cg.time = serverTime;
        // if we are only updating the screen as a loading
	// pacifier, don't even try to read snapshots
        if(!cg.infoScreenText.isEmpty()) {
            cgr.DrawInformation();
            return;
        }

        // set up cg.snap and possibly cg.nextSnap
        cg.ProcessSnapshots();

        // if we haven't received any snapshots yet, all
	// we can draw is the information screen
        if(cg.snap == null || (cg.snap.snapFlags & cubetech.client.CLSnapshot.SF_NOT_ACTIVE) > 0)
        {
            cgr.DrawInformation();
            return;
        }

        // this counter will be bumped for every valid scene we generate
        cg.clientframe++;
        cg.PredictPlayerState();

        CalcViewValues();
        //SkyDome.RenderDome(cg.refdef.Origin, 8000, false);

        // Highlight block
        if(Ref.cm.cubemap != null) {
            Vector3f dir = new Vector3f(cg.refdef.ViewAxis[0]);
            Helper.Normalize(dir);
            dir.scale(-1f);
            CubeCollision col = Ref.cm.cubemap.TraceRay(cg.refdef.Origin, dir, 6);
            if(col != null) {
                SingleCube cube = new SingleCube(col);
                cgr.lookingAtCube = cube;
            } else
            {
                cgr.lookingAtCube = null;
            }
        }

        if(Ref.cm.cubemap != null && cgr.lookingAtCube != null && cgr.lookingAtCube.highlightSide != 0) {
            if(Ref.Input.playerInput.Mouse1 && Ref.Input.playerInput.Mouse1Diff) {
                cgr.lookingAtCube.getHightlightside().putBlock(CubeType.GRASS);
                //Ref.cm.cubemap.putBlock(cgr.highlightCube, CubeType.GRASS);
            } else if(Ref.Input.playerInput.Mouse2 && Ref.Input.playerInput.Mouse2Diff) {
                cgr.lookingAtCube.removeBlock();
            }
//            Vector3f dir = new Vectoraf(cg.refdef.ViewAxis[0]);
//            Helper.Normalize(dir);
//            dir.scale(-1f);
//            Ref.cm.cubemap.TraceRay(cg.refdef.Origin, dir, 64);
//            rayTime = cg.time + 8000;
//            rayStart.set(cg.refdef.Origin);
//            rayEnd.set(dir);
//            rayEnd.scale(1000);
//            Vector3f.add(rayEnd, rayStart, rayEnd);
        }

        Ref.soundMan.Respatialize(cg.refdef.Origin, cg.predictedPlayerState.velocity);
//        AddLocalEntities();
        
//        cg.refdef.time = cg.time;
        cg.frametime = cg.time - cg.oldTime;
        if(cg.frametime < 0)
            cg.frametime = 0;
        cg.oldTime = cg.time;
        lag.AddFrameInfo();

        // Time to do some drawing
//        if(cg_editmode.iValue == 0)
//            cgr.RenderClouds();
        if(cg_editmode.iValue == 1 && mapEditor.isShowingAnimator())
            mapEditor.animEditor.SetView(); // Let mapeditor override viewparams

        if(cg_drawbin.iValue == 1) {
            cgr.DrawBin();
            return;
        }

        if(cg_editmode.iValue == 0 || !mapEditor.isShowingAnimator()) {
            // Normal render
            
            if(Ref.cm.cubemap != null) {
                //Shader shad = Ref.glRef.getShader("GroundFromAtmosphere");
                //Ref.glRef.PushShader(shad);
                //SkyDome.updateShader(shad, true);
//                if(cg.refdef.planes[0] != null) {
//                    cg.refdef.planes[0].DebugRender(cg.refdef.Origin);
//                }
                skyBox.Render(cg.refdef);
                Ref.cm.cubemap.Render(cg.refdef);
                
                //Ref.glRef.PopShader();
                
            }

            cgr.RenderClientEffects();
            
            //cgr.RenderScene(cg.refdef);
            AddPacketEntities();
            cgr.DrawEntities();
            
        }
        // UI
        cgr.Draw2D();
    }

    float getPullAccel() {
        try {
        float pullstep = Ref.cvars.Find("sv_pullstep").fValue;
        float accel = Ref.cvars.Find("sv_pullacceleration").fValue;
        float spd = Math.abs(cg.predictedPlayerState.velocity.x);
        if(spd > Ref.cvars.Find("sv_pull1").fValue)
            accel *= pullstep;
        if(spd > Ref.cvars.Find("sv_pull2").fValue)
            accel *= pullstep;
        if(spd > Ref.cvars.Find("sv_pull3").fValue)
            accel *= pullstep;
        if(spd > Ref.cvars.Find("sv_pull4").fValue)
            accel *= pullstep;
        if(spd > Ref.cvars.Find("sv_pull5").fValue)
            accel *= pullstep;
        if(spd > Ref.cvars.Find("sv_pull6").fValue)
            accel *= pullstep;
        return accel;
        } catch(NullPointerException ex) {
            return 10;
        }
    }

    private void CalcViewValues() {
        speed = (float) (speed * 0.8 + cg.predictedPlayerState.velocity.length() * 0.2f);
        if(cg.refdef == null)
        cg.refdef = new ViewParams();
        cg.refdef.CalcVRect();

        Helper.VectorCopy(cg.predictedPlayerState.origin, cg.refdef.Origin);
        
        Helper.VectorCopy(cg.predictedPlayerState.viewangles, cg.refdef.Angles);

        if(cg_errorDecay.fValue > 0f) {
            int t = cg.time - cg.predictedErrorTime;
            float f = (cg_errorDecay.fValue - t) / cg_errorDecay.fValue;
            if(f > 0 && f < 1)
                Helper.VectorMA(cg.refdef.Origin, f, cg.predictedError, cg.refdef.Origin);
            else
                cg.predictedErrorTime = 0;
        }
        
//        cg.refdef.Origin.z += cg_viewheight.fValue;

        if(cg_tps.isTrue()) {
            cg.refdef.offsetThirdPerson();
        } else {

        }

        cg.refdef.SetupProjection();
    }

    // Handle command from console
    public boolean GameCommands(String[] tokens) {
        String cmd = tokens[0];
        if(commands.containsKey(cmd)) {
            commands.get(cmd).RunCommand(tokens);
            return true;
        }

        return false;
    }

    private void InitConsoleCommands() {
        for (String key : commands.keySet()) {
            Ref.commands.AddCommand(key, commands.get(key));
        }

        //
	// the game server will interpret these commands, which will be automatically
	// forwarded to the server after they are not recognized locally
	//
        Ref.commands.AddCommand("kill", null);
        Ref.commands.AddCommand("say", null);
        Ref.commands.AddCommand("god", null);
        Ref.commands.AddCommand("give", null);
        Ref.commands.AddCommand("noclip", null);
        Ref.commands.AddCommand("notarget", null);
        Ref.commands.AddCommand("follow", null);
    }

    private void ParseScores(String[] tokens) {
        int nClients = Integer.parseInt(tokens[1]);
        cg.scores = new Score[nClients];
        for (int i= 0; i < nClients; i++) {
            cg.scores[i] = new Score();
            cg.scores[i].client = Integer.parseInt(tokens[3 * i + 2]);
            cg.scores[i].ping = Integer.parseInt(tokens[3 * i + 3]);
            cg.scores[i].time = Integer.parseInt(tokens[3 * i + 4]);
        }
    }

    private void AddPacketEntities() {
        if(cg.nextSnap != null) {
            int delta = cg.nextSnap.serverTime - cg.snap.serverTime;
            if(delta == 0)
                cg.frameInterpolation = 0;
            else
                cg.frameInterpolation = (float)(cg.time - cg.snap.serverTime)/delta;
        } else
            cg.frameInterpolation = 0;

        
        cg.autoAngle = (float) ((((cg.time>>1) & 1023) * Math.PI * 2) / 1024.0f);
        
        // generate and add the entity from the playerstate
        PlayerState ps = cg.predictedPlayerState;
        ps.ToEntityState(cg.predictedPlayerEntity.currentState, false);
        cgr.AddCEntity(cg.predictedPlayerEntity);

        // lerp the non-predicted value for lightning gun origins
        cg_entities[cg.snap.ps.clientNum].CalcLerpPosition();

        // add each entity sent over by the server
        for (int i= 0; i < cg.snap.numEntities; i++) {
            CEntity cent = cg_entities[cg.snap.entities[i].ClientNum];
            cgr.AddCEntity(cent);
        }

        // Draw bounding boxes for solid entities
        if(cg_drawSolid.iValue > 0) {
            for (int i = 0; i < cg_numSolidEntities; i++) {
                CEntity cent = cg_solidEntities[i];
                int x, y;
                int pack = cent.nextState.solid;
                if(pack == EntityState.SOLID_BMODEL && cg_drawSolid.iValue < 2) {
                    BlockModel model = Ref.cm.cm.getModel(cent.currentState.modelindex);

                    Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
                    spr.Set(model.mins, model.size, null, null, null);
                    spr.SetColor(255, 128, 0, 80);
                    for (Block block : model.blocks) {
                        if(!block.Collidable)
                            continue;
                        spr = Ref.SpriteMan.GetSprite(Type.GAME);
                        spr.Set(new Vector2f(block.GetCenter().x - block.getAbsExtent().x, block.GetCenter().y - block.getAbsExtent().y), block.getAbsSize(), null, null, null);
                        spr.SetColor(255, 255, 0, 128);
                        spr.SetDepth(block.getLayer()+1);
                    }
                } else if(pack != EntityState.SOLID_BMODEL) {
                    x = pack & 255;
                    y = (pack >> 8) & 255;

                    if(x == 0 || y == 0)
                        continue;
                    
                    Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
                    spr.Set(new Vector2f(cent.lerpOrigin.x - x, cent.lerpOrigin.y - y), new Vector2f(x*2, y*2), null, null, null);
                    spr.SetColor(0, 0, 255, 80);
                }
            }
        }
        if(cg_drawSolid.iValue > 1) {
            // Draw the solid blocks too
            SpatialQuery result = Ref.spatial.Query(cg.refdef.Origin.x - cg.refdef.FovX, cg.refdef.Origin.y - cg.refdef.FovY, cg.refdef.Origin.x + cg.refdef.FovX, cg.refdef.Origin.y + cg.refdef.FovY);
            int queryNum = result.getQueryNum();
            Object object;
            while((object = result.ReadNext()) != null) {
                if(object.getClass() != Block.class)
                    continue;
                Block block = (Block)object;
                if(block.LastQueryNum == queryNum)
                    continue; // duplicate
                block.LastQueryNum = queryNum;

                if(!block.Collidable)
                    continue;

    //            if(!BlockVisible(block))
    //                continue;

                Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
                spr.Set(block.getPosition(), block.getSize(), Ref.ResMan.getWhiteTexture(), block.Material.getTextureOffset(), block.Material.getTextureSize());
                spr.SetAngle(block.getAngle());
                spr.SetDepth(block.getLayer()+1);
                if(block.isModel())
                    spr.SetColor(255, 128, 0, 120);
                else
                    spr.SetColor(255, 255, 0, 100);
            }
        }

    }

    // Server says we need to update a block
    private void SetBlock(String[] tokens) {
        if(tokens.length != 3)
        {
            Common.Log("SetBlock: Invalid arg count");
            return;
        }

        int blockIndex = Integer.parseInt(tokens[1]);
        String blockCmd = tokens[2];
        if(blockCmd.isEmpty() || blockIndex < 0) {
            Common.Log("Invalid setblock args");
            return;
        }

        Block b = Ref.cm.cm.GetBlock(blockIndex);
        b.LoadString(blockCmd);
    }

    public void KeyPressed(KeyEvent evt) {
        if(mapEditor != null)
            mapEditor.KeyPressed(evt);
    }

    public void GotMouseEvent(MouseEvent evt) {
        if(mapEditor != null)
            mapEditor.GotMouseEvent(evt);
    }

    class ChatLine {
        public String str = "";
        //public String from;
        public int time;
    }

    // When set to a non-empty string, CGame wont run, only display the text
    public void LoadingString(String str) {
        if(str == null)
            str = "";
        cg.infoScreenText = str;
        Ref.client.UpdateScreen();
    }

    public void Shutdown() {
        Common.Log("--- CGAME SHUTDOWN ---");
        Ref.Input.RemoveKeyEventListener(this, Input.KEYCATCH_CGAME);
        Ref.Input.RemoveMouseEventListener(this, Input.KEYCATCH_CGAME);
    }


    /*
    ====================
    CG_BuildSolidList

    When a new cg.snap has been set, this function builds a sublist
    of the entities that are actually solid, to make for more
    efficient collision detection
    ====================
    */
    void BuildSolidList() {
        cg_numSolidEntities = 0;
        cg_numTriggerEntities = 0;

        Snapshot snap = null;
        if(cg.nextSnap != null && !cg.nextFrameTeleport && !cg.thisFrameTeleport)
            snap = cg.nextSnap;
        else
            snap = cg.snap;

        for (int i= 0; i < snap.numEntities; i++) {
            CEntity cent = cg_entities[snap.entities[i].ClientNum];
            EntityState ent = cent.currentState;

            if(ent.eType == EntityType.ITEM || ent.eType == EntityType.TRIGGER) {
                cg_triggerEntities[cg_numTriggerEntities++] = cent;
                continue;
            }

            if(cent.nextState.solid > 0) {
                cg_solidEntities[cg_numSolidEntities++] = cent;
                continue;
            }
        }
    }

    void ExecuteNewServerCommands(int serverCommandSequence) {
        while(cgs.serverCommandSequence < serverCommandSequence) {
            String cmd = Ref.client.GetServerCommand(++cgs.serverCommandSequence);
            if(cmd != null)
                ServerCommand(cmd);
        }
    }

    private void ServerCommand(String command) {
        String[] tokens = Commands.TokenizeString(command, false);
        String cmd = tokens[0];
        if(cmd == null || cmd.isEmpty())
            return; // server claimed the command

        if(cmd.equalsIgnoreCase("cp")) {
            // CenterPrint
            return;
        }

        if(cmd.equalsIgnoreCase("cs")) {
            ConfigStringModified(tokens);
            return;
        }

        if(cmd.equalsIgnoreCase("print")) {
            Ref.cgame.Print(Commands.Args(tokens));
            return;
        }

        if(cmd.equalsIgnoreCase("chat")) {
            // TODO: Chat text overlay in console
            Ref.cgame.Print(Commands.Args(tokens));
            return;
        }

        if(cmd.equalsIgnoreCase("scores")) {
            ParseScores(tokens);
            return;
        }

        if(cmd.equalsIgnoreCase("setblock")) {
            SetBlock(tokens);
            return;
        }

        if(cmd.equalsIgnoreCase("map_restart"))
        {
            MapRestart();
            return;
        }

        Common.Log("Unkown cgame command: " + cmd);
    }

    public void Print(String str) {
        ChatLine line = chatLines[chatIndex++ % chatLines.length];
        line.str = str;
        line.time = Ref.client.realtime;
        Common.Log(str);
    }

    private void ConfigStringModified(String[] tokens) {
        int num;
        try {
            num = Integer.parseInt(tokens[1]);
        } catch(NumberFormatException ex) {
            Common.LogDebug("ConfigStringModifies(): Couldn't parse number: " + tokens[1]);
            return;
        }

        // MAYBEFIX: Copy gamestate from client instead of sharing
        // trap_GetGameState( &cgs.gameState );

        // look up the individual string that was modified
        String str = cgs.ConfigString(num);
        if(num == CS.CS_SERVERINFO)
            cgs.ParseServerInfo();
        else if(num == CS.CS_LEVEL_START_TIME)
        { try {
            cgs.levelStartTime = Integer.parseInt(str);
            } catch(NumberFormatException ex) {
                Ref.common.Error(Common.ErrorCode.DROP, "ConfigStringModified(): Can't parse int for level start time: " + str);
            }
        }
        else if(num >= CS.CS_PLAYERS && num < CS.CS_PLAYERS + 64) {
            cgs.NewClientInfo(num-CS.CS_PLAYERS);
        }
    }

    /*
    ===============
    CG_MapRestart

    The server has issued a map_restart, so the next snapshot
    is completely new and should not be interpolated to.

    A tournement restart will clear everything, but doesn't
    require a reload of all the media
    ===============
    */
    private void MapRestart() {
        InitLocalEntities();
        cg.mapRestart = false;

        // Display "GO" message or something
    }

    private void InitLocalEntities() {
        // TODO
    }

    public CollisionResult Trace(Vector3f start, Vector3f end, Vector3f mins, Vector3f maxs, int tracemask, int passEntityNum) {
        if(mins == null)
            mins = new Vector3f();
        if(maxs == null)
            maxs = new Vector3f();

        Vector3f delta = new Vector3f();
        Vector3f.sub(end, start, delta);
        // clip to world
        // FIX FIX
        CollisionResult worldResult = Ref.collision.traceCubeMap(start, delta, mins, maxs);
        //CollisionResult worldResult = Ref.collision.TestMovement(new Vector2f(start), new Vector2f(delta), new Vector2f(maxs), tracemask);
        if(worldResult.frac == 0.0f) {
            return worldResult; // Blocked instantl by world
        }


        ClipMoveToEntities(new Vector2f(start), new Vector2f(end), new Vector2f(mins), new Vector2f(maxs), passEntityNum, tracemask, worldResult);

        return worldResult;
    }

    private void ClipMoveToEntities(Vector2f start, Vector2f end, Vector2f mins, Vector2f maxs, int passEntityNum, int tracemask, CollisionResult worldResult) {
        for (int i= 0; i < cg_numSolidEntities; i++) {
            CEntity cent = cg_solidEntities[i];
            EntityState ent = cent.currentState;

            if(ent.ClientNum == passEntityNum)
                continue;

            // Extract encoded bbox
            
            int pack = ent.solid;
            if(pack == EntityState.SOLID_BMODEL) {
                // special value for bmodel
                int index = Ref.cm.cm.InlineModel(ent.modelindex);
                
                Vector3f origin = new Vector3f();
                ent.pos.Evaluate(cg.physicsTime, origin);

                Ref.collision.SetSubModel(index, new Vector2f(origin.x, origin.y)); // FIXFIX
            } else {
                int x = pack & 255;
                int y = (pack >> 8) & 255;

                if(x == 0 || y == 0)
                    continue;
                
                Ref.collision.SetBoxModel(new Vector2f(x, y), new Vector2f(cent.lerpOrigin)); // FIX HAX
            }

            
            CollisionResult res = Ref.collision.TransformedBoxTrace(start, end, mins, maxs, tracemask);

            if(res.frac < worldResult.frac) {
                worldResult.entitynum = ent.ClientNum;
                worldResult.frac = res.frac;
                worldResult.hit = res.hit;
                worldResult.hitAxis = res.hitAxis;
                worldResult.hitmask = res.hitmask;

                if(res.frac < 0f)
                    res.frac = 0f;

                if(res.frac == 0f)
                    return;
            }
        }
    }

    // User wants to see the scoreboard
    private class Cmd_ScoresDown implements ICommand {
        public void RunCommand(String[] args) {
            if(cg.scoresRequestTime + 2000 < cg.time) {
                // the scores are more than two seconds out of data,
		// so request new ones
                cg.scoresRequestTime = cg.time;
                Ref.client.AddReliableCommand("score", false);

                // leave the current scores up if they were already
		// displayed, but if this is the first hit, clear them out
                if(!cg.showScores)
                {
                    cg.showScores = true;
//                    cg.scores = new Score[0]; // empty array
                }
            } else {
                // show the cached contents even if they just pressed if it
		// is within two seconds
                cg.showScores = true;
            }
        }
    }
    // Users doesnt want to see the scoreboard anymore
    private class Cmd_ScoresUp implements ICommand {
        public void RunCommand(String[] args) {
            if(cg.showScores) {
                cg.showScores = false;
            }
        }
    }

    public int GetCurrentPlayerEntityNum() {
        if(cg.predictedPlayerState != null)
            return cg.predictedPlayerState.clientNum;
        else
            return -1;
    }
}
