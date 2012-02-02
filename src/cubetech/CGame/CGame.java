package cubetech.CGame;

import cubetech.Game.Gentity;
import cubetech.collision.*;
import cubetech.common.*;
import cubetech.common.items.IItem;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.gfx.*;
import cubetech.input.*;
import cubetech.iqm.IQMModel;
import cubetech.iqm.RigidBoneMesh;
import cubetech.misc.Profiler;
import cubetech.misc.Profiler.Sec;
import cubetech.misc.Profiler.SecTag;
import cubetech.misc.Ref;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import nbullet.collision.shapes.CollisionShape;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CGame implements ITrace, KeyEventListener, MouseEventListener {
    public static final int PLAYER_LAYER = -20;
    public CVar cg_nopredict = Ref.cvars.Get("cg_nopredict", "0", EnumSet.of(CVarFlags.TEMP));
    CVar cg_smoothclients = Ref.cvars.Get("cg_smoothclients", "0", EnumSet.of(CVarFlags.TEMP));
    public CVar cg_errorDecay = Ref.cvars.Get("cg_errorDecay", "100", EnumSet.of(CVarFlags.TEMP));
    public CVar cg_showmiss = Ref.cvars.Get("cg_showmiss", "0", EnumSet.of(CVarFlags.TEMP));
    CVar cg_viewsize = Ref.cvars.Get("cg_viewsize", "100", EnumSet.of(CVarFlags.TEMP));
    CVar cg_fov = Ref.cvars.Get("cg_fov", "90", EnumSet.of(CVarFlags.ARCHIVE));
    CVar cg_chattime = Ref.cvars.Get("cg_chattime", "5000", EnumSet.of(CVarFlags.ARCHIVE)); // show text for this long
    CVar cg_chatfadetime = Ref.cvars.Get("cg_chatfadetime", "500", EnumSet.of(CVarFlags.ARCHIVE)); // + this time for fading out
    CVar cg_drawSolid = Ref.cvars.Get("cg_drawSolid", "0", EnumSet.of(CVarFlags.NONE));
    CVar cg_depthnear = Ref.cvars.Get("cg_depthnear", "1", EnumSet.of(CVarFlags.CHEAT));
    CVar cg_depthfar = Ref.cvars.Get("cg_depthfar", "8000", EnumSet.of(CVarFlags.CHEAT));
    CVar cg_viewmode = Ref.cvars.Get("cg_viewmode", "1", EnumSet.of(CVarFlags.NONE));
    CVar cg_swingspeed = Ref.cvars.Get("cg_swingspeed", "0.8", EnumSet.of(CVarFlags.CHEAT));
    CVar cg_freecam = Ref.cvars.Get("cg_freecam", "0", EnumSet.of(CVarFlags.CHEAT));
    public CVar cg_drawprofiler = Ref.cvars.Get("cg_drawprofiler", "0", EnumSet.of(CVarFlags.NONE));
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
    
    // These CEntities have created something in the physics system
    // and need some cleanup after they stop appearing in the snapshots
    public ArrayList<CEntity> centitiesWithPhysics = new ArrayList<CEntity>(32);
    
    public ArrayList<CEntity> cg_solidEntities = new ArrayList<CEntity>(256);
    public ArrayList<CEntity> cg_triggerEntities = new ArrayList<CEntity>(256);
    private HashMap<String, ICommand> commands = new HashMap<String, ICommand>();
    public float speed;
    public Marks marks;

    SkyBox skyBox = new SkyBox("data/textures/skybox/sky");
    private CVar cg_skybox;
    
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
        
        SecTag s = Profiler.EnterSection(Sec.CG_RENDER);
        // this counter will be bumped for every valid scene we generate
        cg.clientframe++;
        cg.frametime = cg.time - cg.oldTime;
        if(cg.frametime < 0) cg.frametime = 0;
        cg.oldTime = cg.time;
        lag.AddFrameInfo();
        
        SecTag s2 = Profiler.EnterSection(Sec.PHYSICS);
        physics.stepPhysics();
        s2.ExitSection();
        marks.updateMarks();
        
        cg.nViewports = 0;
        boolean[] renderClientViewport = new boolean[4];
        for (int i = 0; i < 4; i++) {
            if(cg.snap.lcIndex[i] == -1) {
                renderClientViewport[i] = false;
                continue;
            }
            cg.cur_localClientNum = i;
            cg.cur_lc = cg.localClients[i];
            cg.cur_ps = cg.snap.pss[cg.snap.lcIndex[i]];
            
            cg.nViewports++;
            renderClientViewport[i] = true;
            
            Ref.client.setUserCommand(cg.cur_lc.weaponSelect, cg.cur_lc.zoomSensitivity, cg.cur_localClientNum);
            cg.cur_lc.PredictPlayerState();
            cg.showScores = cg.cur_lc.predictedPlayerState.oldButtons[3];
        }
        
        
        
        if(cg.nViewports == 3) cg.nViewports = 4;
        cg.cur_viewport = 0;
        for (int i = 0; i < 4; i++) {
            if(!renderClientViewport[i]) continue;
            cg.cur_localClientNum = i;
            cg.cur_lc = cg.localClients[i];
            cg.cur_ps = cg.snap.pss[cg.snap.lcIndex[i]];
            
            renderForClient();
            
            if(i == 0) {
                Ref.soundMan.Respatialize(cg.cur_lc.predictedPlayerState.clientNum, cg.refdef.Origin, cg.cur_lc.predictedPlayerState.velocity, cg.refdef.ViewAxis);
            }
            cg.cur_viewport++;
        }
        
        for (int i = 0; i < 4; i++) {
            if(!renderClientViewport[i]) continue;
            cg.cur_localClientNum = i;
            cg.cur_lc = cg.localClients[i];
            cg.cur_ps = cg.snap.pss[cg.snap.lcIndex[i]];
            cg.cur_lc.wasDead = cg.cur_ps.stats.Health <= 0;
        }
        
        s.ExitSection();
    }
    
    private void renderForClient() {
        CalcViewValues();
        if(map != null) {
            if(cg_skybox.isTrue()) {
                RenderEntity ent = Ref.render.createEntity(REType.SKYBOX);
                ent.flags = RenderEntity.FLAG_NOSHADOW | RenderEntity.FLAG_NOLIGHT;
                ent.renderObject = skyBox;
                Ref.render.addRefEntity(ent);
            }
            
            map.Render(cg.refdef);
        }
        AddPacketEntities();
        cg.addTestModel();
        cgr.player.renderViewModel(cg.cur_lc.predictedPlayerState);
        marks.addMarks();

        physics.renderBodies();
        Light.skylight.setDirection(new Vector3f(1,0,-1));
        Light.skylight.setDiffuse(new Vector3f(1.0f, 1.0f, 1.0f));
        Light.skylight.setSpecular(new Vector3f(1.0f,1.0f,1.0f));
        Light.skylight.enableShadowCasting(true);
        cg.refdef.lights.add(Light.skylight);
        

        
        
        ParticleSystem.update();
        LocalEntities.addLocalEntities();

        if(cg_drawSolid.isTrue()) {
            
            for (CEntity cEntity : cg_solidEntities) {
                // Try to extract the bounds
                Vector3f[] bounds = Helper.solidToBounds(cEntity.currentState.solid);
                Vector3f[] axis = null;
                
                if(cEntity.nextState.solid == EntityState.SOLID_BMODEL
                        && cEntity.currentState.modelindex != 0) {
                    IQMModel model = Ref.ResMan.loadModel(
                            Ref.client.cl.GameState.get(
                            CS.CS_MODELS-1+cEntity.currentState.modelindex));
                    
                    if(model != null) {
                        bounds = new Vector3f[2];
                        bounds[0] = model.getMins();
                        bounds[1] = model.getMaxs();
                    }
                }
                if(cEntity.currentState.apos.type == Trajectory.QUATERNION) {
                    axis = cEntity.lerpAnglesQ.quatToMatrix(axis);
                }

                if(bounds != null) {
                    drawEntityBBox(bounds, cEntity.lerpOrigin, axis);
                }
            }
            for (CEntity cEntity : cg_triggerEntities) {
                // Try to extract the bounds
                Vector3f[] bounds = Helper.solidToBounds(cEntity.currentState.solid);
                Vector3f[] axis = null;

                if(cEntity.nextState.solid == EntityState.SOLID_BMODEL
                        && cEntity.currentState.modelindex != 0) {
                    bounds = new Vector3f[2];
                    IQMModel model = Ref.ResMan.loadModel(
                            Ref.client.cl.GameState.get(
                            CS.CS_MODELS-1+cEntity.currentState.modelindex));
                    bounds[0] = model.getMins();
                    bounds[1] = model.getMaxs();
                } else if(cEntity.currentState.eType == EntityType.ITEM) {
                    bounds = new Vector3f[] {new Vector3f(), new Vector3f()};
                    IItem item = Ref.common.items.getItem(cEntity.currentState.modelindex);
                    
                    item.getBounds(bounds[0], bounds[1]);
                    axis = cEntity.lerpAnglesQ.quatToMatrix(axis);
                }

                if(bounds != null) {
                    drawEntityBBox(bounds, cEntity.lerpOrigin, axis);
                }
            }
        }
        
        // UI
        cgr.Draw2D();
        
        Ref.render.assignAndClear(Ref.cgame.cg.refdef);
    }
    
    private void drawEntityBBox(Vector3f[] bounds, Vector3f origin, Vector3f[] axis) {
        RenderEntity ent = Ref.render.createEntity(REType.BBOX);
        ent.flags = RenderEntity.FLAG_NOLIGHT;
        ent.origin.set(origin);
        //Vector3f.add(origin, bounds[0], ent.origin);
        ent.oldOrigin.set(bounds[0]);
        ent.oldOrigin2.set(bounds[1]);
//        Vector3f.sub(bounds[1], bounds[0], ent.oldOrigin);
////        axis = null;
//        ent.oldOrigin.scale(0.5f);
//        Vector3f offset = Vector3f.sub(origin, origin, origin)
        if(axis != null) {
            ent.flags |= RenderEntity.FLAG_SPRITE_AXIS;
            for (int i = 0; i < 3; i++) {
                ent.axis[i].set(axis[i]);
            }
        }

        Ref.render.addRefEntity(ent);
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
        speed = (float) (speed * 0.8 + cg.cur_lc.predictedPlayerState.velocity.length() * 0.2f);
        cg.refdef = new ViewParams();
        
        ViewParams.SliceType slice = ViewParams.SliceType.NONE;
        switch(cg.nViewports) {
            case 2:
                slice = ViewParams.SliceType.HORIZONAL;
                break;
            case 4:
                slice = ViewParams.SliceType.BOTH;
                break;
        }
        
        cg.refdef.CalcVRect(slice,cg.cur_viewport,cg_viewsize.iValue);

        cg.cur_lc.bobcycle = (cg.cur_lc.predictedPlayerState.bobcycle & 128) >> 7;
        cg.cur_lc.bobfracsin = (float) Math.abs(Math.sin((float)(cg.cur_lc.predictedPlayerState.bobcycle & 0xff) / 127f * Math.PI));
        cg.cur_lc.xyspeed = (float) Math.sqrt(cg.cur_lc.predictedPlayerState.velocity.x * cg.cur_lc.predictedPlayerState.velocity.x +
                                    cg.cur_lc.predictedPlayerState.velocity.y * cg.cur_lc.predictedPlayerState.velocity.y);

        if(cg_freecam.isTrue() && cg.playingdemo) {
            if(Ref.client.demo.track.playingTrack()) {
                Ref.client.demo.track.readFrame(cg.time, cg.refdef);
            } else {
                // Do a fake playerstate for freecamming in demos
                if(cg.demoPlayerState == null) {
                    cg.demoPlayerState = cg.cur_lc.predictedPlayerState.Clone(null);
                    cg.demoPlayerState.moveType = Move.MoveType.NOCLIP;
                    cg.demoPlayerState.velocity.set(0,0,0); // clear velocity
                }
                PlayerInput input = Ref.Input.getKeyboardInput();
                WeightedMouseMove(input);
                input.serverTime = Ref.common.framemsec;
                cg.demoPlayerState.UpdateViewAngle(input);
                cg.demoPlayerState.commandTime = 0;

                MoveQuery move = new MoveQuery(this);
                move.cmd = input;
                move.ps = cg.demoPlayerState;
                Move.Move(move);
                Helper.VectorCopy(cg.demoPlayerState.origin, cg.refdef.Origin);
                Helper.VectorCopy(cg.demoPlayerState.viewangles, cg.refdef.Angles);
                Ref.client.demo.track.recordFrame(cg.time, cg.refdef.Origin, cg.refdef.Angles, cg.demofov);
            }
        } else {

            Helper.VectorCopy(cg.cur_lc.predictedPlayerState.origin, cg.refdef.Origin);

            Helper.VectorCopy(cg.cur_lc.predictedPlayerState.viewangles, cg.refdef.Angles);

            if(cg_errorDecay.fValue > 0f) {
                int t = cg.time - cg.cur_lc.predictedErrorTime;
                float f = (cg_errorDecay.fValue - t) / cg_errorDecay.fValue;
                if(f > 0 && f < 1)
                    Helper.VectorMA(cg.refdef.Origin, f, cg.cur_lc.predictedError, cg.refdef.Origin);
                else
                    cg.cur_lc.predictedErrorTime = 0;
            }

            cg.refdef.Origin.z += cg.cur_lc.predictedPlayerState.viewheight;

            if(cg_tps.isTrue()) {
                cg.refdef.offsetThirdPerson();
            } else {
                Vector3f.add(cg.refdef.Angles, cg.cur_lc.predictedPlayerState.punchangle, cg.refdef.Angles);
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

        
        cg.cur_lc.autoAngle = (float) ((((cg.time>>1) & 1023) * Math.PI * 2) / 1024.0f);
        
        // generate and add the entity from the playerstate
        PlayerState ps = cg.cur_lc.predictedPlayerState;
        ps.ToEntityState(cg.cur_lc.predictedPlayerEntity.currentState, false);
        cgr.AddCEntity(cg.cur_lc.predictedPlayerEntity);

        // lerp the non-predicted value for lightning gun origins
        cg_entities[cg.cur_ps.clientNum].CalcLerpPosition();

        // add each entity sent over by the server
        for (int i= 0; i < cg.snap.numEntities; i++) {
            CEntity cent = cg_entities[cg.snap.entities[i].number];
            cgr.AddCEntity(cent);
        }

        

    }


    public boolean KeyPressed(KeyEvent evt) {
        return false;
    }

    public void GotMouseEvent(MouseEvent evt) {
        
    }

    // Cleans up playerentity
    public void cleanPhysicsFromCEntity(CEntity physcent) {
        if(physcent.pe.boneMeshModel == null) return;
        
        for (RigidBoneMesh bmesh : physcent.pe.boneMeshes) {
            if(bmesh.rigidBody != null) {
                CollisionShape shape = bmesh.rigidBody.getCollisionShape();
                physics.deleteBody(bmesh.rigidBody);
                bmesh.rigidBody = null;
                shape.destroy();
            }
        }
        
        physcent.pe.boneMeshModel = null;
        physcent.pe.boneMeshes = null;
        Common.LogDebug("Cleaned physics from centity");
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
        Ref.client.clRender.UpdateScreen();
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
        cg_solidEntities.clear();
        cg_triggerEntities.clear();

        Snapshot snap = cg.snap;
        if(cg.nextSnap != null) {
            snap = cg.nextSnap;
        }

        for (int i= 0; i < snap.numEntities; i++) {
            CEntity cent = cg_entities[snap.entities[i].number];
            EntityState ent = cent.currentState;

            if(ent.eType == EntityType.ITEM || ent.eType == EntityType.TRIGGER) {
                cg_triggerEntities.add(cent);
                continue;
            }

            if(cent.nextState.solid != 0) {
                cg_solidEntities.add(cent);
                continue;
            }
        }
    }

    void ExecuteNewServerCommands(int serverCommandSequence) {
        while(cgs.serverCommandSequence < serverCommandSequence) {
            String cmd = Ref.client.clc.GetServerCommand(++cgs.serverCommandSequence);
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

    private void ClipMoveToEntities(Vector3f start, Vector3f end, Vector3f mins, Vector3f maxs,
            int passEntityNum, int tracemask, CollisionResult worldResult) {
        for (CEntity cent : cg_solidEntities) {
            EntityState ent = cent.nextState;

            if(ent.number == passEntityNum)
                continue;

            // Extract encoded bbox
            Vector3f[] bounds = Helper.solidToBounds(ent.solid);
            if(bounds != null) {
                Ref.collision.SetBoxModel(bounds[0], bounds[1], cent.lerpOrigin);
            } else if(ent.solid == EntityState.SOLID_BMODEL) {
                String modelname = Ref.client.cl.GameState.get(CS.CS_MODELS+ent.modelindex-1);
                IQMModel model = Ref.ResMan.loadModel(modelname);
                bounds = new Vector3f[2];
                bounds[0] = model.getMins();
                bounds[1] = model.getMaxs();
                Ref.collision.SetBoxModel(bounds[0], bounds[1], cent.lerpOrigin);
            }

            CollisionResult res = Ref.collision.TransformedBoxTrace(start, end, mins, maxs, tracemask);

            if(res.frac < worldResult.frac) {
                worldResult.entitynum = ent.number;
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


    public int GetCurrentPlayerEntityNum() {
        if(cg.cur_lc.predictedPlayerState != null)
            return cg.cur_lc.predictedPlayerState.clientNum;
        else
            return -1;
    }
}
