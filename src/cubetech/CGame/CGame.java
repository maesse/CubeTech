package cubetech.CGame;

import cubetech.common.Score;
import cubetech.Block;
import cubetech.Game.Gentity;
import cubetech.collision.*;
import cubetech.common.*;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.gfx.*;
import cubetech.gfx.SpriteManager.Type;
import cubetech.input.*;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import cubetech.spatial.SpatialQuery;
import java.util.EnumSet;
import java.util.HashMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
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
    CVar cg_showmiss = Ref.cvars.Get("cg_showmiss", "0", EnumSet.of(CVarFlags.TEMP));
    CVar cg_viewsize = Ref.cvars.Get("cg_viewsize", "100", EnumSet.of(CVarFlags.TEMP));
    CVar cg_fov = Ref.cvars.Get("cg_fov", "90", EnumSet.of(CVarFlags.ARCHIVE));
    CVar cg_chattime = Ref.cvars.Get("cg_chattime", "5000", EnumSet.of(CVarFlags.ARCHIVE)); // show text for this long
    CVar cg_chatfadetime = Ref.cvars.Get("cg_chatfadetime", "500", EnumSet.of(CVarFlags.ARCHIVE)); // + this time for fading out
    CVar cg_drawSolid = Ref.cvars.Get("cg_drawSolid", "0", EnumSet.of(CVarFlags.NONE));
    CVar cg_depthnear = Ref.cvars.Get("cg_depthnear", "1", EnumSet.of(CVarFlags.CHEAT));
    CVar cg_depthfar = Ref.cvars.Get("cg_depthfar", "8000", EnumSet.of(CVarFlags.CHEAT));
    CVar cg_viewmode = Ref.cvars.Get("cg_viewmode", "1", EnumSet.of(CVarFlags.NONE));
    CVar cg_drawentities = Ref.cvars.Get("cg_drawentities", "0", EnumSet.of(CVarFlags.ROM));
    CVar cg_drawbin = Ref.cvars.Get("cg_drawbin", "0", EnumSet.of(CVarFlags.NONE));
    CVar cg_swingspeed = Ref.cvars.Get("cg_swingspeed", "0.8", EnumSet.of(CVarFlags.CHEAT));
    CVar cg_freecam = Ref.cvars.Get("cg_freecam", "0", EnumSet.of(CVarFlags.CHEAT));

    CVar cg_tps = Ref.cvars.Get("cg_tps", "0", EnumSet.of(CVarFlags.NONE));

    public CGameState cg;
    CGameStatic cgs;
    public CGWeapons weapons = new CGWeapons();
    public ClientCubeMap map = new ClientCubeMap();
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
    public float speed;
    public Marks marks;

    SkyBox skyBox = new SkyBox("data/sky");
    private CVar cg_skybox;
    public ShadowManager shadowMan = new ShadowManager();
    public CGPhysics physics = new CGPhysics();
    
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
        ParticleSystem.init();
        marks = new Marks();
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

        cg_skybox = Ref.cvars.Get("cg_skybox", "1", EnumSet.of(CVarFlags.NONE));

//        commands.put("+scores", new Cmd_ScoresDown());
//        commands.put("-scores", new Cmd_ScoresUp());
        commands.put("testgun", cg.cg_testgun_f);
        commands.put("testmodel", cg.cg_testmodel_f);
        commands.put("nextframe", cg.cg_testmodelNextFrame_f);
        commands.put("prevframe", cg.cg_testmodelPrevFrame_f);
        commands.put("weapon", CGameState.cg_SwitchWeapon_f);
        commands.put("firebox", physics.firebox);

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

        Ref.soundMan.clearLoopingSounds(true);
        
    }

    public void DrawActiveFrame(int serverTime, boolean isDemo) {
        // UpdateCVars
        cg.time = serverTime;
        cg.playingdemo = isDemo;
        // if we are only updating the screen as a loading
	// pacifier, don't even try to read snapshots
        if(!cg.infoScreenText.isEmpty()) {
            cgr.DrawInformation();
            return;
        }

        Ref.soundMan.clearLoopingSounds(false);

        // set up cg.snap and possibly cg.nextSnap
        cg.ProcessSnapshots();

        // if we haven't received any snapshots yet, all
	// we can draw is the information screen
        if(cg.snap == null || (cg.snap.snapFlags & cubetech.client.CLSnapshot.SF_NOT_ACTIVE) > 0)
        {
            cgr.DrawInformation();
            return;
        }

        Ref.client.setUserCommand(cg.weaponSelect, cg.zoomSensitivity);

        // this counter will be bumped for every valid scene we generate
        cg.clientframe++;
        cg.PredictPlayerState();
        cg.showScores = cg.predictedPlayerState.oldButtons[3];

        CalcViewValues();

        Ref.soundMan.Respatialize(cg.predictedPlayerState.clientNum, cg.refdef.Origin, cg.predictedPlayerState.velocity, cg.refdef.ViewAxis);
//        AddLocalEntities();
        
        cg.frametime = cg.time - cg.oldTime;
        if(cg.frametime < 0)
            cg.frametime = 0;
        cg.oldTime = cg.time;
        lag.AddFrameInfo();
        
        AddPacketEntities();
        cg.addTestModel();

        

        SecTag s = Profiler.EnterSection(Sec.PHYSICS);
        physics.stepPhysics();
        s.ExitSection();
        
        s = Profiler.EnterSection(Sec.RENDER);
        cgr.renderViewModel(cg.predictedPlayerState);
        marks.addMarks();

        physics.renderBodies();

        if(shadowMan.isEnabled()) {
            shadowMan.renderShadowCascades(cg.refdef, new IThinkMethod() {
                public void think(Gentity ent) {
                    if(map != null) {
                        map.Render(cg.refdef);
                        Ref.render.renderAll(true);
                    }
                }
            });
        }

        if(map != null) {
            if(cg_skybox.isTrue()) skyBox.Render(cg.refdef);
            map.Render(cg.refdef);
        }
        
        
        //cgr.RenderScene(cg.refdef);

        ParticleSystem.update();
        
//        cgr.DrawEntities();
        LocalEntities.addLocalEntities();

        drawSolid();
        
        // UI
        cgr.Draw2D();
        s.ExitSection();
    }

    private float WheelAccelerate(float velocity, float dir, float wishspeed, float accel) {
        float currentSpeed = velocity * dir;
        float addspeed = wishspeed - currentSpeed;
        if(addspeed <= 0) return velocity;
        float accelspeed = accel * wishspeed * (Ref.common.framemsec / 1000f);
        if(accelspeed > addspeed) accelspeed = addspeed;
        return velocity + dir * accelspeed;
    }

    private void MouseAccelerate(Vector2f velocity, Vector2f mouseDir, float wishspeed, float accel) {
        // Determine veer amount
        float currentSpeed = Vector2f.dot(velocity, mouseDir);

        // See how much to add
        float addSpeed = wishspeed  - currentSpeed;

        // If not adding any, done.
        if(addSpeed <= 0f)
            return;

        // Determine acceleration speed after acceleration
        float accelspeed = accel * (Ref.common.framemsec/1000f) * wishspeed;

        // Cap it
        if(accelspeed > addSpeed)
            accelspeed = addSpeed;

        // Adjust pmove vel.
        Helper.VectorMA(velocity, accelspeed, mouseDir, velocity);
    }

    private float WheelFriction(float wheelSpeed, float friction) {
        float wspeed = wheelSpeed;
        if(wspeed < 0) wspeed = -wspeed;
        if(wspeed < 0.1f) {
            wheelSpeed = 0f;
            return wheelSpeed;
        }

        float drop = wspeed * friction * (Ref.common.framemsec / 1000f);
        float newspeed = wspeed - drop;
        if(newspeed < 0) newspeed = 0;
        newspeed /= wspeed;
        return wheelSpeed * newspeed;
    }

    private void MouseFriction(Vector2f velocity, float friction) {
       float speed2 = velocity.length();
       if(speed2 < 0.1f) {
           velocity.set(0,0);
           return;
       }

       // Apply ground friction
       float drop = speed2 * friction * (Ref.common.framemsec / 1000f);


       float newspeed = speed2 - drop;
       if(newspeed < 0)
           newspeed = 0;

       newspeed /= speed2;

       velocity.scale(newspeed);
   }

    private void WeightedMouseMove(PlayerInput cmd) {
        // Cast to float // Multiply by sensitivity
        float sens = Ref.cvars.Find("sensitivity").fValue;
        float mx = cmd.MouseDelta[0] * sens;
        float my = cmd.MouseDelta[1] * sens;

        MouseFriction(cg.mouseVelocity, 4f);
        Vector2f mouseDir = new Vector2f(mx, my);
        float len = Helper.Normalize(mouseDir);
        if(len != 0) {
            MouseAccelerate(cg.mouseVelocity, mouseDir, len * 200, 2f);
        }

        float[] viewangles = cg.demoangles;
        float frameFrac = Ref.common.framemsec / 1000f;
        viewangles[Input.ANGLE_YAW] -= 0.022f * cg.mouseVelocity.x * frameFrac;
        viewangles[Input.ANGLE_PITCH] -= 0.022f * cg.mouseVelocity.y * frameFrac;

        if(viewangles[Input.ANGLE_PITCH] > 180f)
            viewangles[Input.ANGLE_PITCH] = 180f;
        else if(viewangles[Input.ANGLE_PITCH] < 0f)
            viewangles[Input.ANGLE_PITCH] = 0f;

        // Ensure angles have not been wrapped
        cmd.angles[0] = Helper.Angle2Short(viewangles[0]);
        cmd.angles[1] = Helper.Angle2Short(viewangles[1]);
        cmd.angles[2] = Helper.Angle2Short(viewangles[2]);

        cg.demofovVel = WheelFriction(cg.demofovVel, 5f);
        if(cmd.WheelDelta != 0) {
            float wDir = Math.signum(cmd.WheelDelta)*-1f;
            cg.demofovVel = WheelAccelerate(cg.demofovVel, wDir, 200f, 5f);
        }
        cg.demofov += cg.demofovVel * frameFrac;
        if(cg.demofov < 10) {
            cg.demofov = 10;
            cg.demofovVel = 0;
        } else if(cg.demofov > 220) {
            cg.demofov = 220;
            cg.demofovVel = 0;
        }

    }

    private void CalcViewValues() {
        speed = (float) (speed * 0.8 + cg.predictedPlayerState.velocity.length() * 0.2f);
        if(cg.refdef == null)
        cg.refdef = new ViewParams();
        cg.refdef.CalcVRect();

        cg.bobcycle = (cg.predictedPlayerState.bobcycle & 128) >> 7;
        cg.bobfracsin = (float) Math.abs(Math.sin((float)(cg.predictedPlayerState.bobcycle & 0xff) / 127f * Math.PI));
        cg.xyspeed = (float) Math.sqrt(cg.predictedPlayerState.velocity.x * cg.predictedPlayerState.velocity.x +
                                    cg.predictedPlayerState.velocity.y * cg.predictedPlayerState.velocity.y);

        if(cg_freecam.isTrue() && cg.playingdemo) {
            if(Ref.client.demo.track.playingTrack()) {
                Ref.client.demo.track.readFrame(cg.time, cg.refdef);
            } else {
                // Do a fake playerstate for freecamming in demos
                if(cg.demoPlayerState == null) {
                    cg.demoPlayerState = cg.predictedPlayerState.Clone(null);
                    cg.demoPlayerState.moveType = Move.MoveType.NOCLIP;
                    cg.demoPlayerState.velocity.set(0,0,0); // clear velocity
                }
                WeightedMouseMove(Ref.Input.playerInput);
                Ref.Input.playerInput.serverTime = Ref.common.framemsec;
                cg.demoPlayerState.UpdateViewAngle(Ref.Input.playerInput);
                cg.demoPlayerState.commandTime = 0;

                MoveQuery move = new MoveQuery(this);
                move.cmd = Ref.Input.playerInput;
                move.ps = cg.demoPlayerState;
                Move.Move(move);
                Helper.VectorCopy(cg.demoPlayerState.origin, cg.refdef.Origin);
                Helper.VectorCopy(cg.demoPlayerState.viewangles, cg.refdef.Angles);
                Ref.client.demo.track.recordFrame(cg.time, cg.refdef.Origin, cg.refdef.Angles, cg.demofov);
            }
        } else {

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

            cg.refdef.Origin.z += cg.predictedPlayerState.viewheight;

            if(cg_tps.isTrue()) {
                cg.refdef.offsetThirdPerson();
            } else {
                Vector3f.add(cg.refdef.Angles, cg.predictedPlayerState.punchangle, cg.refdef.Angles);
            }
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
        Ref.commands.AddCommand("block", null);
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

        

    }

    private void drawSolid() {
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

                    int zmin = (pack >> 24) & 127;
                    int zmax = ((pack >> 16) & 255)-zmin;

                    GL11.glDepthFunc(GL11.GL_ALWAYS);
                    Ref.glRef.PushShader(Ref.glRef.getShader("sprite"));
                    Helper.renderBBoxWireframe(cent.lerpOrigin.x-x, cent.lerpOrigin.y-y, cent.lerpOrigin.z-zmin,
                            cent.lerpOrigin.x + x, cent.lerpOrigin.y + y, cent.lerpOrigin.z+zmax,null);
                    Ref.glRef.PopShader();
                    GL11.glDepthFunc(GL11.GL_LEQUAL);
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

    public void KeyPressed(KeyEvent evt) {
        
    }

    public void GotMouseEvent(MouseEvent evt) {
        
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
        if(!Ref.client.demo.isPlaying()) {
            map.dispose();
            map = null;
        }
        Ref.ResMan.cleanupModels();
        System.gc();
        System.gc();
        System.gc();
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
            cg.scores.parseScores(tokens);
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
        marks = new Marks();
        cg.mapRestart = false;
        Ref.soundMan.clearLoopingSounds(true);
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
        CollisionResult worldResult = Ref.collision.traceCubeMap(start, delta, mins, maxs, false);
        //CollisionResult worldResult = Ref.collision.TestMovement(new Vector2f(start), new Vector2f(delta), new Vector2f(maxs), tracemask);
        if(worldResult.frac == 0.0f) {
            return worldResult; // Blocked instantl by world
        }


        ClipMoveToEntities(start, end, mins, maxs, passEntityNum, tracemask, worldResult);

        return worldResult;
    }

    private void ClipMoveToEntities(Vector3f start, Vector3f end, Vector3f mins, Vector3f maxs, int passEntityNum, int tracemask, CollisionResult worldResult) {
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

                Ref.collision.SetSubModel(index, origin);
            } else {
                int x = pack & 255;
                int y = (pack >> 8) & 255;
                int height = (pack >> 16) & 255;
                int zminLen = (pack >> 24) & 127;

                if(x == 0 || y == 0 || height == 0) {
                    Common.Log("Invalid entity bbox for unpacking ent:%d eType:%d", ent.ClientNum, ent.eType);
                    continue;
                }

                // zmin = 10, height = 30
                float z = height/2f;
                Vector3f origin = new Vector3f(cent.lerpOrigin);
                

                float leftover = ((height-zminLen)-zminLen) / 2f;
                //origin.z += leftover;
                Vector3f tmins = new Vector3f(-x,-y,-zminLen);
                Vector3f tmaxs = new Vector3f(x,y,height - zminLen);
                Ref.collision.SetBoxModel(tmins, tmaxs, origin);
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

//    // User wants to see the scoreboard
//    private class Cmd_ScoresDown implements ICommand {
//        public void RunCommand(String[] args) {
//            if(cg.scoresRequestTime + 2000 < cg.time) {
//                // the scores are more than two seconds out of data,
//		// so request new ones
//                cg.scoresRequestTime = cg.time;
//                Ref.client.AddReliableCommand("score", false);
//
//                // leave the current scores up if they were already
//		// displayed, but if this is the first hit, clear them out
//                if(!cg.showScores)
//                {
//                    cg.showScores = true;
////                    cg.scores = new Score[0]; // empty array
//                }
//            } else {
//                // show the cached contents even if they just pressed if it
//		// is within two seconds
//                cg.showScores = true;
//            }
//        }
//    }
//    // Users doesnt want to see the scoreboard anymore
//    private class Cmd_ScoresUp implements ICommand {
//        public void RunCommand(String[] args) {
//            if(cg.showScores) {
//                cg.showScores = false;
//            }
//        }
//    }

    public int GetCurrentPlayerEntityNum() {
        if(cg.predictedPlayerState != null)
            return cg.predictedPlayerState.clientNum;
        else
            return -1;
    }
}
