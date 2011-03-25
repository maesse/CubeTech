package cubetech.CGame;

import cubetech.Block;
import cubetech.Game.Game;
import cubetech.Game.Gentity;
import cubetech.collision.BlockModel;
import cubetech.collision.ClipmapException;
import cubetech.collision.CollisionResult;
import cubetech.common.CS;
import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.GItem;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.common.ITrace;
import cubetech.common.Move.MoveType;
import cubetech.common.PlayerState;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.input.Input;
import cubetech.input.KeyEvent;
import cubetech.input.KeyEventListener;
import cubetech.input.MouseEvent;
import cubetech.input.MouseEventListener;
import cubetech.misc.Ref;
import cubetech.spatial.SpatialQuery;
import java.util.EnumSet;
import java.util.HashMap;
import org.lwjgl.util.Color;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CGame implements ITrace, KeyEventListener, MouseEventListener {
    private static final int PLAYER_LAYER = -20;
    CVar cg_nopredict = Ref.cvars.Get("cg_nopredict", "0", EnumSet.of(CVarFlags.TEMP));
    CVar cg_smoothclients = Ref.cvars.Get("cg_smoothclients", "0", EnumSet.of(CVarFlags.TEMP));
    CVar cg_errorDecay = Ref.cvars.Get("cg_errorDecay", "100", EnumSet.of(CVarFlags.TEMP));
    CVar cg_viewsize = Ref.cvars.Get("cg_viewsize", "100", EnumSet.of(CVarFlags.TEMP));
    CVar cg_fov = Ref.cvars.Get("cg_fov", "180", EnumSet.of(CVarFlags.TEMP));
    CVar cg_chattime = Ref.cvars.Get("cg_chattime", "5000", EnumSet.of(CVarFlags.TEMP)); // show text for this long
    CVar cg_chatfadetime = Ref.cvars.Get("cg_chatfadetime", "500", EnumSet.of(CVarFlags.TEMP)); // + this time for fading out
    CVar cg_drawSolid = Ref.cvars.Get("cg_drawSolid", "0", EnumSet.of(CVarFlags.NONE));
    CVar cg_editmode = Ref.cvars.Get("cg_editmode", "0", EnumSet.of(CVarFlags.ROM));
    CVar cg_depthnear = Ref.cvars.Get("cg_depthnear", "1", EnumSet.of(CVarFlags.ROM));
    CVar cg_depthfar = Ref.cvars.Get("cg_depthfar", "1000", EnumSet.of(CVarFlags.ROM));
    CVar cg_viewmode = Ref.cvars.Get("cg_viewmode", "0", EnumSet.of(CVarFlags.NONE));
    CVar cg_drawentities = Ref.cvars.Get("cg_drawentities", "0", EnumSet.of(CVarFlags.ROM));

    CGameState cg;
    CGameStatic cgs;

    private ChatLine[] chatLines = new ChatLine[8];
    private int chatIndex = 0;
    public CEntity[] cg_entities;
    public int cg_numSolidEntities;
    public int cg_numTriggerEntities;
    public CEntity[] cg_solidEntities = new CEntity[256];
    public CEntity[] cg_triggerEntities = new CEntity[256];
    private HashMap<String, ICommand> commands = new HashMap<String, ICommand>();
    CubeTexture playerTexture;
    private MapEditor mapEditor = null;

//    private ColladaModel cm = ColladaModel.load("data/Dice.dae");


    /**
    *=================
    *CG_Init
    *
    *Called after every level change or subsystem restart
    *Will perform callbacks to make the loading info screen update.
    *=================
    **/
    public void Init(int serverMessageSequence, int serverCommandSequence, int ClientNum) {
        playerTexture = Ref.ResMan.LoadTexture("data/enemy1.png");
        commands.put("+scores", new Cmd_ScoresDown());
        commands.put("-scores", new Cmd_ScoresUp());

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
        try {
            if(Ref.client.clc.mapdata != null)
                Ref.cm.LoadMap(Ref.client.clc.mapdata, true);
            else
                Ref.cm.LoadMap(cgs.mapname, true);
        } catch (ClipmapException ex) {
            Ref.common.Error(Common.ErrorCode.DROP, "Couldn't load map:_" + ex);
        }

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
            DrawInformation();
            return;
        }

        // set up cg.snap and possibly cg.nextSnap
        cg.ProcessSnapshots();

        // if we haven't received any snapshots yet, all
	// we can draw is the information screen
        if(cg.snap == null || (cg.snap.snapFlags & cubetech.client.CLSnapshot.SF_NOT_ACTIVE) > 0)
        {
            DrawInformation();
            return;
        }

        // this counter will be bumped for every valid scene we generate
        cg.clientframe++;

        cg.PredictPlayerState();
        CalcViewValues();

        Ref.soundMan.Respatialize(cg.refdef.Origin, cg.predictedPlayerState.velocity);
        
//        AddLocalEntities();
//
//        cg.refdef.time = cg.time;
        cg.frametime = cg.time - cg.oldTime;
        if(cg.frametime < 0)
            cg.frametime = 0;
        cg.oldTime = cg.time;

        // Time to do some drawing
        RenderScene(cg.refdef);
        AddPacketEntities();
        DebugDraw();
        Draw2D();
//        cm.render(new Vector3f(0,0,-30));
    }

    private void DebugDraw() {
        if(cg_drawentities.iValue == 0 || Ref.game == null || Ref.game.g_entities == null)
            return;

        for (int i= 0; i < Ref.game.level.num_entities; i++) {
            Gentity ent = Ref.game.g_entities[i];
            if(!ent.inuse || ent.s.eType == EntityType.PLAYER)
                continue; // ignore players and unused entities

            if(!ent.r.linked)
                continue;

            Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
            Vector2f size = new Vector2f();
            Vector2f.sub(ent.r.absmax, ent.r.absmin, size);

            // Figure out what texture to display
            CubeTexture tex = null;
            switch(ent.s.eType) {
                case EntityType.ITEM:
                    tex = Ref.ResMan.LoadTexture("data/tool_item.png");
                    break;
                case EntityType.MOVER:
                    tex = Ref.ResMan.LoadTexture("data/tool_mover.png");
                    break;
                case EntityType.TRIGGER:
                    tex = Ref.ResMan.LoadTexture("data/tool_trigger.png");
                    break;
            }

            // Unclaimed by the switch, but has a trigger.
            if(tex == null && (ent.r.contents & Content.TRIGGER) == Content.TRIGGER)
                tex = Ref.ResMan.LoadTexture("data/tool_trigger.png");

            if(tex == null)
                Ref.ResMan.getWhiteTexture();
            
            spr.Set(ent.r.absmin, size, tex, null, null);
            spr.SetDepth(MapEditor.EDITOR_LAYER-1);
            spr.SetColor(0, 255, 255, 127);
            
            Vector2f center = new Vector2f(ent.r.absmin);
            size.scale(0.5f);
            Vector2f.add(center, size, center);

            MapEditor.renderHighlightBlock(center, size, MapEditor.EDITOR_LAYER-1, (Color) Color.GREEN);
        }
    }

    private void CalcViewValues() {
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

        cg.refdef.SetupProjection();
    }

    private void RenderScene(ViewParams refdef) {
        SpatialQuery result = Ref.spatial.Query(refdef.Origin.x - refdef.FovX, refdef.Origin.y - refdef.FovY, refdef.Origin.x + refdef.FovX, refdef.Origin.y + refdef.FovY);
        int queryNum = result.getQueryNum();
        Object object;
        
        int near = -cg_depthnear.iValue, far = -cg_depthfar.iValue;

        if(Ref.cvars.Find("cg_editmode").iValue == 1) {
            near = -Ref.cvars.Find("edit_nearlayer").iValue;
            far = -Ref.cvars.Find("edit_farlayer").iValue;
        }

        while((object = result.ReadNext()) != null) {
            if(object.getClass() != Block.class)
                continue;
            Block block = (Block)object;
            if(block.LastQueryNum == queryNum)
                continue; // duplicate
            block.LastQueryNum = queryNum;

//            if(!BlockVisible(block))
//                continue;

            int layer = block.getLayer();

            if(layer > near || layer < far)
                continue;
            block.Render();
        }
    }

    private void AddCEntity(CEntity cent) {
        // event-only entities will have been dealt with already
        if(cent.currentState.eType >= EntityType.EVENTS)
            return;

        // calculate the current origin
        cent.CalcLerpPosition();
        cent.Effects();
        switch(cent.currentState.eType) {
            case EntityType.PLAYER:
                Player(cent);
                break;
            case EntityType.ITEM:
                Item(cent);
                break;
            case EntityType.MOVER:
                Mover(cent);
                break;
        }
    }

    private void Mover(CEntity cent) {
//        cent.currentState.solid
//        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
//        Vector2f size = new Vector2f(40, 8);
//
//        spr.Set(new Vector2f(cent.lerpOrigin.x - size.x/2f, cent.lerpOrigin.y - size.y/2f), size, null, null,null);
//        spr.SetDepth(PLAYER_LAYER);

        RenderModel(cent.currentState.modelindex, cent.lerpOrigin, PLAYER_LAYER);
    }

    private void RenderModel(int index, Vector2f position, int layer) {
        index = Ref.cm.cm.InlineModel(index);

        // Get bounds
        BlockModel model = Ref.cm.cm.getModel(index);
        model.moveTo(position);
    }

    private void Item(CEntity cent) {
        EntityState es = cent.currentState;
        if(es.modelindex >= Ref.common.items.getItemCount())
            Ref.common.Error(Common.ErrorCode.DROP, "Bad item index " + es.modelindex + " on entity.");

        // if set to invisible, skip
        if(es.modelindex < 0 || (es.eFlags & EntityFlags.NODRAW) > 0) {
            return;
        }

        GItem item = Ref.common.items.getItem(es.modelindex);
        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
        spr.Set(cent.lerpOrigin, GItem.ITEM_RADIUS, item.icon);
        spr.SetDepth(PLAYER_LAYER-1);
        if(item.type == GItem.Type.HEALTH)
            spr.SetAngle(cg.autoAngle);
    }

    // Render a player
    private void Player(CEntity cent) {
        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
        spr.Set(new Vector2f(cent.lerpOrigin.x + Game.PlayerMins.x, cent.lerpOrigin.y + Game.PlayerMins.y), new Vector2f(Game.PlayerMaxs.x - Game.PlayerMins.x, Game.PlayerMaxs.y - Game.PlayerMins.y), null, null, null);
        
        spr.SetAngle((float) (Math.atan2(cent.lerpAngles.y, cent.lerpAngles.x) + Math.PI / 2f));

        float alpha = 0.5f;
        if(cent.interpolate)
            alpha = 0.75f;
        if(cent.currentState.ClientNum == Ref.client.cl.snap.ps.clientNum)
            spr.SetColor(0, 255, 0, (int)(alpha*255));
        else
            spr.SetColor(255, 0, 0, (int)(alpha*255));
        spr.SetDepth(PLAYER_LAYER);

        Ref.textMan.AddText(new Vector2f(cent.lerpOrigin.x, cent.lerpOrigin.y+Game.PlayerMaxs.y), "" + cgs.clientinfo[cent.currentState.ClientNum].name, Align.CENTER, Type.GAME, 1, PLAYER_LAYER+1);
    }

    private void Draw2D() {
        DrawChat();

        if(Ref.common.isDeveloper()) {
            Ref.textMan.AddText(new Vector2f(0, 0), "Position: " + cg.refdef.Origin, Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()), "Velocity: " + cg.predictedPlayerState.velocity, Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*2), "Ping: " + Ref.cgame.cg.snap.ps.ping, Align.LEFT, Type.HUD);
        }
        Ref.textMan.AddText(new Vector2f(0, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()), "HP: " + Ref.cgame.cg.snap.ps.stats.Health, Align.LEFT, Type.HUD);
//        Ref.textMan.AddText(new Vector2f(0, 0.75f), "Interp: " + Ref.cgame.cg.frameInterpolation, Align.LEFT, Type.HUD);

        if(cg.predictedPlayerState.stats.Health <= 0) {
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x / 2f, Ref.glRef.GetResolution().y /2f  - Ref.textMan.GetCharHeight()*2), "^5You are dead", Align.CENTER, Type.HUD);
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x / 2f, Ref.glRef.GetResolution().y/2f  - Ref.textMan.GetCharHeight()), "Click mouse to spawn", Align.CENTER, Type.HUD);
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x / 2f, Ref.glRef.GetResolution().y/2f), "ESC for menu", Align.CENTER, Type.HUD);
        }

        // Handle changes from server
        if(cg.predictedPlayerState.moveType == MoveType.EDITMODE && cg_editmode.iValue == 0) {
            // Turn on editmode
            Ref.cvars.Set2("cg_editmode", "1", true);
        } else if(cg.predictedPlayerState.moveType != MoveType.EDITMODE && cg_editmode.iValue == 1) {
            // Turn off editmode
            Ref.cvars.Set2("cg_editmode", "0", true);
        }

        // Handle changes to cvar
        if(cg_editmode.modified) {
            cg_editmode.modified = false;
            if(cg_editmode.iValue == 0) {
                Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() & ~Input.KEYCATCH_CGAME);
                mapEditor = null;
            } else {
                mapEditor = new MapEditor();
            }
        }

        // Render mapeditor if in editmode
        if(cg_editmode.iValue == 1) {
            mapEditor.Render();
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x / 2f, 0.0f), "^3Edit Mode", Align.CENTER, Type.HUD);
        }

        if(cg.showScores)
            DrawScoreboard();
    }

    private void DrawScoreboard() {
        // Request update if scoreboard info is too old
        if(cg.scoresRequestTime + 2000 < cg.time) {
            cg.scoresRequestTime = cg.time;
            Ref.client.AddReliableCommand("score", false);
        }

        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(0.1f, 0.1f), new Vector2f(0.8f, 0.8f), null, null, null);
        spr.SetColor(0, 0, 0, 127);
        float yOffset = 100;
        float lineHeight = Ref.textMan.GetCharHeight();
        for (int i= 0; i < cg.scores.length; i++) {
            Score score = cg.scores[i];
            

            DrawClientScore(yOffset - lineHeight * i, score);
        }
    }

    private void DrawClientScore(float yOffset, Score score) {
        if(score.client < 0 || score.client >= cgs.clientinfo.length) {
            Ref.cgame.Print("DrawClientScore: Invalid ci index: " + score.client);
            return;
        }
        
        ClientInfo ci = cgs.clientinfo[score.client];

        if(ci == null || ci.name == null) {
            int test = 2;
        }

        if(score.client == cg.snap.ps.clientNum) {
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(0.1f, yOffset-0.01f), new Vector2f(0.8f, 0.07f), null, null, null);
            spr.SetColor(255,255,255,80);
            // Highlight self
            // TODO: Sprite bg
        }

        String extra = "";
        if(score.ping == -1)
            extra = "(Connecting...)";
        Vector2f res = Ref.glRef.GetResolution();
        Ref.textMan.AddText(new Vector2f(0.1f * res.x, yOffset), String.format("%s%s", ci.name, extra), Align.LEFT, Type.HUD);
        Ref.textMan.AddText(new Vector2f(0.9f * res.y, yOffset), String.format("ping:%d time:%d", score.ping, score.time), Align.RIGHT, Type.HUD);
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
        AddCEntity(cg.predictedPlayerEntity);

        // lerp the non-predicted value for lightning gun origins
        cg_entities[cg.snap.ps.clientNum].CalcLerpPosition();

        // add each entity sent over by the server
        for (int i= 0; i < cg.snap.numEntities; i++) {
            CEntity cent = cg_entities[cg.snap.entities[i].ClientNum];
            AddCEntity(cent);
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

    private class ChatLine {
        public String str = "";
        //public String from;
        public int time;
    }

    private void DrawChat() {
        int time = Ref.client.realtime;
        int lineIndex = 1;
        float lineHeight = Ref.textMan.GetCharHeight();
        for (int i= 0; i < chatLines.length; i++) {
            ChatLine line = chatLines[(chatIndex+7-i) % 8];
            if(time - line.time > cg_chattime.iValue + cg_chatfadetime.iValue)
                continue;

            Color color = new Color(255,255,255,255);

            if(time - line.time >= cg_chattime.iValue) {
                color.setAlpha((int)((1-((time - line.time - cg_chattime.iValue)/cg_chatfadetime.fValue))*255));
            }

            float height = Ref.textMan.AddText(new Vector2f(10, Ref.glRef.GetResolution().y - (200 + lineHeight * lineIndex)), line.str, Align.LEFT, color, null, Type.HUD,1).y;
            //height /= Ref.textMan.GetCharHeight();

            lineIndex ++;
        }

        // Draw message field if active
        if(Ref.client.message.isChatMessageUp()) {
            Ref.client.message.getCurrentMessage().Render();
        }
    }

    // When set to a non-empty string, CGame wont run, only display the text
    public void LoadingString(String str) {
        if(str == null)
            str = "";
        cg.infoScreenText = str;
        Ref.client.UpdateScreen();
    }

    private void DrawInformation() {
        if(!cg.infoScreenText.isEmpty())
            Ref.textMan.AddText(new Vector2f(0.5f*Ref.glRef.GetResolution().x, Ref.glRef.GetResolution().y-20), String.format("Loading... %s", cg.infoScreenText), Align.CENTER, Type.HUD);
        else
            Ref.textMan.AddText(new Vector2f(0.5f*Ref.glRef.GetResolution().x, Ref.glRef.GetResolution().y-20), "Awaiting snapshot", Align.CENTER, Type.HUD);
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

    public CollisionResult Trace(Vector2f start, Vector2f end, Vector2f mins, Vector2f maxs, int tracemask, int passEntityNum) {
        if(mins == null)
            mins = new Vector2f();
        if(maxs == null)
            maxs = new Vector2f();

        Vector2f delta = new Vector2f();
        Vector2f.sub(end, start, delta);
        // clip to world
        CollisionResult worldResult = Ref.collision.TestPosition(start, delta, maxs, tracemask);
        if(worldResult.frac == 0.0f) {
            return worldResult; // Blocked instantl by world
        }


        ClipMoveToEntities(start, end, mins, maxs, passEntityNum, tracemask, worldResult);

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
                
                Vector2f origin = new Vector2f();
                ent.pos.Evaluate(cg.physicsTime, origin);

                Ref.collision.SetSubModel(index, origin);
            } else {
                int x = pack & 255;
                int y = (pack >> 8) & 255;

                if(x == 0 || y == 0)
                    continue;
                
                Ref.collision.SetBoxModel(new Vector2f(x, y), cent.lerpOrigin);
            }

            
            CollisionResult res = Ref.collision.TransformedBoxTrace(start, end, mins, maxs, tracemask);

            if(res.frac < worldResult.frac) {
                worldResult.entitynum = ent.ClientNum;
                worldResult.frac = res.frac;
                worldResult.Hit = res.Hit;
                worldResult.HitAxis = res.HitAxis;
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
