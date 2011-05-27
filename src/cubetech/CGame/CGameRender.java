/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.Block;
import cubetech.Game.Game;
import cubetech.Game.Gentity;
import cubetech.collision.BlockModel;
import cubetech.collision.CubeChunk;
import cubetech.collision.CubeMap;
import cubetech.collision.SingleCube;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.GItem;
import cubetech.common.Helper;
import cubetech.common.Move.MoveType;
import cubetech.common.PlayerState;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.gfx.CubeMaterial;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.CubeType;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.gfx.VBO;
import cubetech.input.Input;
import cubetech.iqm.IQMAdjacency;
import cubetech.iqm.IQMAnim;
import cubetech.misc.Ref;
import cubetech.spatial.Bin;
import cubetech.spatial.SpatialQuery;
import java.sql.NClob;
import java.util.ArrayList;
import java.util.Locale;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CGameRender {
    private CGame game;

    
    
    // Player
    private CubeMaterial c_head;
    private CubeMaterial c_arm;
    private CubeMaterial c_body;
    private CubeMaterial c_legs;
    // Sun effect
    public Vector2f sunPositionOnScreen = new Vector2f();
    public Color sunColor = (Color) Color.WHITE;
    // Cloud effect
    private Cloud[] clouds = new Cloud[16];
    SingleCube lookingAtCube;

    public CGameRender(CGame game) {
        this.game = game;

        try {
            c_head = CubeMaterial.Load("data/c_head.mat", true);
            c_arm = CubeMaterial.Load("data/c_arm.mat", true);
            c_legs = CubeMaterial.Load("data/c_legs.mat", true);
            c_body = CubeMaterial.Load("data/c_body.mat", true);
        } catch (Exception ex) {
            Common.Log(Common.getExceptionString(ex));
        }

        for (int i= 0; i < clouds.length; i++) {
            clouds[i] = new Cloud(new Vector2f());
        }
    }
    

    //
    // CMap
    //
    void RenderScene(ViewParams refdef) {
        SpatialQuery result = Ref.spatial.Query(refdef.Origin.x - refdef.FovX, refdef.Origin.y - refdef.FovY, refdef.Origin.x + refdef.FovX, refdef.Origin.y + refdef.FovY);
        int queryNum = result.getQueryNum();
        Object object;

        int near = -game.cg_depthnear.iValue, far = -game.cg_depthfar.iValue;

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


    //
    // Entities
    //
    void AddCEntity(CEntity cent) {
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

    private void Item(CEntity cent) {
        EntityState es = cent.currentState;
        if(es.modelindex >= Ref.common.items.getItemCount())
            Ref.common.Error(Common.ErrorCode.DROP, "Bad item index " + es.modelindex + " on entity.");

        // if set to invisible, skip
        if(es.modelindex < 0 || (es.eFlags & EntityFlags.NODRAW) > 0) {
            return;
        }


        float bounce = 0;

        GItem item = Ref.common.items.getItem(es.modelindex);
        if(item.type == GItem.Type.POWERUP)
            bounce = (float) Math.sin(game.cg.time/400f) * 2f;
        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
        spr.Set(cent.lerpOrigin.x, cent.lerpOrigin.y + bounce, GItem.ITEM_RADIUS, item.icon);
        spr.SetDepth(CGame.PLAYER_LAYER-1);
        if(item.type == GItem.Type.HEALTH)
            spr.SetAngle(game.cg.autoAngle);

    }

    // Render a player
    private void Player(CEntity cent) {
        RenderEntity ent = Ref.render.createEntity(REType.MODEL);
        ent.model = Ref.cm.cubemap.model;

        // set origin & angles
        ent.origin.set(cent.lerpOrigin);
        ent.origin.z += Game.PlayerMins.z;
        Vector3f deltaMove = cent.currentState.pos.delta;

        Vector3f angles;
        if(deltaMove.lengthSquared() > 0.1f) {
            angles = new Vector3f(deltaMove);
            angles.y = (float) (180/Math.PI * Math.atan2(-deltaMove.y, deltaMove.x) + Math.PI / 2f);
        }
        else {
            angles = new Vector3f(cent.lerpAngles);
        angles.y *= -1f;
        angles.x *= -1f;

        }

        angles.x = 0;
        angles.z = 0;
        ent.axis = Helper.AnglesToAxis(angles);
//        for (int i= 0; i < 3; i++) {
//            ent.axis[i].normalise();
//        }
//        ent.axis[0].scale(-1f);
//        ent.axis[1].scale(-1f);

        playerAnimation(cent, ent);
    }

    private void playerAnimation(CEntity cent, RenderEntity ent) {
        float speedScale = 1.0f;
        
        ClientInfo ci = Ref.cgame.cgs.clientinfo[cent.currentState.ClientNum];

        runLerpFrame(ci, cent.pe.torso, cent.currentState.frame, speedScale);

        ent.oldframe = cent.pe.torso.oldFrame;
        ent.frame = cent.pe.torso.frame;
        ent.backlerp = cent.pe.torso.backlerp;
    }

    private void Mover(CEntity cent) {
//        cent.currentState.solid
//        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
//        Vector2f size = new Vector2f(40, 8);
//
//        spr.Set(new Vector2f(cent.lerpOrigin.x - size.x/2f, cent.lerpOrigin.y - size.y/2f), size, null, null,null);
//        spr.SetDepth(CGame.PLAYER_LAYER);

        RenderModel(cent.currentState.modelindex, new Vector2f(cent.lerpOrigin), CGame.PLAYER_LAYER); // FIX
    }

    //
    // UI
    //
    void Draw2D() {
//        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
//        spr.Set(new Vector2f(), Ref.glRef.GetResolution(), Ref.ResMan.LoadTexture("data/hags.png"), null, null);

        // Render mapeditor if in editmode
        if(game.cg_editmode.iValue == 1) {
            game.mapEditor.Render();

        }

//        float speeds = Math.abs(game.cg.snap.ps.velocity.x);
//        float maxspeed = 600;
//        float frac = speeds / maxspeed;
//        if(game.cg_editmode.iValue == 0) {
//            spr = Ref.SpriteMan.GetSprite(Type.HUD);
//            Vector2f size = new Vector2f(Ref.glRef.GetResolution().x * 0.75f, 64);
//            Vector2f position = new Vector2f(Ref.glRef.GetResolution().x/2f - size.x / 2f , Ref.glRef.GetResolution().y - size.y - 32 );
//            size.x *= frac;
//            spr.Set(position, size, Ref.ResMan.LoadTexture("data/energybar.png"), null, new Vector2f(1, 1));
//        }


        DrawChat();


        game.lag.Draw();


        if(Ref.common.isDeveloper()) {
            Ref.textMan.AddText(new Vector2f(0, 0), "Position: " + game.cg.refdef.Origin, Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()), "Velocity: " + game.cg.predictedPlayerState.velocity, Align.LEFT, Type.HUD);
            //Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*2), "Ping: " + Ref.cgame.cg.snap.ps.ping, Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*2), "Pull accel: " + game.getPullAccel(), Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*2), "v: " + game.cg.predictedPlayerState.delta_angles[0], Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*2), "Total VBO size: " + VBO.TotalBytes/(1024*1024) + "mb", Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*3), "Quads: " + Ref.cm.cubemap.nSides + " (VBO: "+ (Ref.cm.cubemap.nSides * CubeChunk.PLANE_SIZE)/1024 +" kb)", Align.LEFT, Type.HUD);
            if(Ref.cm.cubemap.nChunks != 0)
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*4), 
                    String.format(Locale.ENGLISH,"Chunks: %d (avg. quads/chunk: %d (%.1fkb))", Ref.cm.cubemap.nChunks, (Ref.cm.cubemap.nSides)/Ref.cm.cubemap.nChunks, (Ref.cm.cubemap.nSides* CubeChunk.PLANE_SIZE )/Ref.cm.cubemap.nChunks/1024f), Align.LEFT, Type.HUD);
            if(Ref.cgame.cg.refdef.planes[0] != null)
                Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*5), "near plane: " + game.cg.refdef.planes[0], Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*4), "Vv: " + Ref.Input.viewangles[0], Align.LEFT, Type.HUD);

            if(game.rayTime > game.cg.time) {
                Ref.ResMan.getWhiteTexture().Bind();
                GL11.glBegin(GL11.GL_LINES);
                GL11.glColor3f(0, 1, 0);
                GL11.glVertex3f(game.rayStart.x, game.rayStart.y, game.rayStart.z);
                GL11.glVertex3f(game.rayEnd.x, game.rayEnd.y, game.rayEnd.z);
                GL11.glEnd();
            }
        }

        if(Ref.net.net_graph.iValue > 0) {
            DrawNetGraph();
        }

//        Ref.textMan.AddText(new Vector2f(0, 0), "Time: " + game.cg.snap.ps.maptime, Align.LEFT, Type.HUD);
//        Ref.textMan.AddText(new Vector2f(0, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()), "HP: " + Ref.cgame.cg.snap.ps.stats.Health, Align.LEFT, Type.HUD);
//        Ref.textMan.AddText(new Vector2f(0, 0.75f), "Interp: " + Ref.cgame.cg.frameInterpolation, Align.LEFT, Type.HUD);

        if(game.cg.predictedPlayerState.stats.Health <= 0) {
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x / 2f, Ref.glRef.GetResolution().y /2f  - Ref.textMan.GetCharHeight()*2), "^5You are dead", Align.CENTER, Type.HUD);
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x / 2f, Ref.glRef.GetResolution().y/2f  - Ref.textMan.GetCharHeight()), "Click mouse to spawn", Align.CENTER, Type.HUD);
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x / 2f, Ref.glRef.GetResolution().y/2f), "ESC for menu", Align.CENTER, Type.HUD);
        }

        // Draw powerups
        PlayerState ps = game.cg.snap.ps;
        for (int i= 0; i < PlayerState.NUM_POWERUPS; i++) {
            if(ps.powerups[i] <= 0)
                continue;

            int ms = ps.powerups[i];
            int sec = ms / 1000;


            GItem item = Ref.common.items.findItemByClassname("item_boots");
            if(item == null) {
                Common.LogDebug("CGame.Draw2D: Can't find item " + i);
                continue;
            }
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            Vector2f res = Ref.glRef.GetResolution();
            float radius = 32;
            spr.Set(res.x - radius * 2, res.y - ( res.y * 0.7f + i * radius * 2) , radius, item.icon);
            Ref.textMan.AddText(new Vector2f(res.x - radius * 2 , res.y * 0.7f + i * radius * 2 +5), ""+sec, Align.CENTER, Type.HUD);
        }

        // Handle changes from server
        if(game.cg.snap.ps.moveType == MoveType.EDITMODE && game.cg_editmode.iValue == 0) {
            // Turn on editmode
            Ref.cvars.Set2("cg_editmode", "1", true);
        } else if(game.cg.snap.ps.moveType != MoveType.EDITMODE && game.cg_editmode.iValue == 1) {
            // Turn off editmode
            Ref.cvars.Set2("cg_editmode", "0", true);
        }

        // Handle changes to cvar
        if(game.cg_editmode.modified) {
            game.cg_editmode.modified = false;
            if(game.cg_editmode.iValue == 0) {
                Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() & ~Input.KEYCATCH_CGAME);
                game.mapEditor = null;
            } else {
                game.mapEditor = new MapEditor();
            }
        }

        if(game.cg.showScores)
            DrawScoreboard();

        if(Ref.common.cl_paused.iValue == 1) {
            Vector2f pos = new Vector2f(Ref.glRef.GetResolution());
            pos.scale(0.5f);
            Ref.textMan.AddText(pos, "^3PAUSED", Align.CENTER, Type.HUD, 2,0);
        }
    }

    private void DrawScoreboard() {
        // Request update if scoreboard info is too old
        if(game.cg.scoresRequestTime + 2000 < game.cg.time) {
            game.cg.scoresRequestTime =game.cg.time;
            Ref.client.AddReliableCommand("score", false);
        }

        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(0.1f, 0.1f), new Vector2f(0.8f, 0.8f), null, null, null);
        spr.SetColor(0, 0, 0, 127);
        float yOffset = 100;
        float lineHeight = Ref.textMan.GetCharHeight();
        for (int i= 0; i < game.cg.scores.length; i++) {
            Score score = game.cg.scores[i];


            DrawClientScore(yOffset - lineHeight * i, score);
        }
    }

    private void DrawClientScore(float yOffset, Score score) {
        if(score.client < 0 || score.client >= game.cgs.clientinfo.length) {
            Ref.cgame.Print("DrawClientScore: Invalid ci index: " + score.client);
            return;
        }

        ClientInfo ci = game.cgs.clientinfo[score.client];

        if(ci == null || ci.name == null) {
            int test = 2;
        }

        if(score.client == game.cg.snap.ps.clientNum) {
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

    private void DrawChat() {
        int time = Ref.client.realtime;
        int lineIndex = 1;
        float lineHeight = Ref.textMan.GetCharHeight();
        for (int i= 0; i < game.chatLines.length; i++) {
            CGame.ChatLine line = game.chatLines[(game.chatIndex+7-i) % 8];
            if(time - line.time > game.cg_chattime.iValue + game.cg_chatfadetime.iValue)
                continue;

            Color color = new Color(255,255,255,255);

            if(time - line.time >= game.cg_chattime.iValue) {
                color.setAlpha((int)((1-((time - line.time - game.cg_chattime.iValue)/game.cg_chatfadetime.fValue))*255));
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

    void DrawInformation() {
        if(!game.cg.infoScreenText.isEmpty())
            Ref.textMan.AddText(new Vector2f(0.5f*Ref.glRef.GetResolution().x, Ref.glRef.GetResolution().y-20), String.format("Loading... %s", game.cg.infoScreenText), Align.CENTER, Type.HUD);
        else
            Ref.textMan.AddText(new Vector2f(0.5f*Ref.glRef.GetResolution().x, Ref.glRef.GetResolution().y-20), "Awaiting snapshot", Align.CENTER, Type.HUD);
    }

    void DrawNetGraph() {
        if(Ref.net.net_graph.iValue > 1)
            game.lag.Draw();

        int ping = Ref.cgame.cg.snap.ps.ping;
        int fps = Ref.client.currentFPS;

        int in = Ref.net.clLastBytesIn;
        int out = Ref.net.clLastBytesOut;
        int inAvgBytes = Ref.net.clAvgBytesIn;
        int outAvgBytes = Ref.net.clAvgBytesOut;
        int inRate = Ref.net.clAvgPacketsIn;
        int outRate = Ref.net.clAvgPacketsOut;

        int loss = 0; // Lost snapshot %
        int choke = 0; // Percentage of packets rate-delayed

        String graph_text = String.format(
                  "fps:%4d  ping: %d ms\n"
                + "in :%4d  %.2f k/s  %d/s\n"
                + "out:%4d  %.2f k/s  %d/s\n"
                + "loss:%3d  choke: %d",
                fps, ping, in, inAvgBytes/1024f, inRate, out, outAvgBytes/1024f, outRate, loss, choke);

        Vector2f size = Ref.textMan.GetStringSize(graph_text, null, null, 1.0f, Type.HUD);
//        Common.Log(graph_text);
        Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x - size.x, Ref.glRef.GetResolution().y-size.y), graph_text, Align.LEFT, Type.HUD);
    }


    //
    // Effects
    //
    void RenderClouds() {
        float time = Ref.client.frametime/1000f;
        ViewParams view = game.cg.refdef;
        for (int i= 0; i < clouds.length; i++) {
            if(clouds[i].isOutOfView(view.xmin + view.Origin.x, view.Origin.y))
                clouds[i].position(new Vector2f(view.xmax + view.Origin.x, view.Origin.y));

            clouds[i].Render(time);
        }
    }

    public void DrawSun() {
        Ref.ResMan.LoadTexture("data/particle.png").Bind();

        ViewParams view = game.cg.refdef;
        float yfrac;
        try {
            yfrac = view.Origin.y / Ref.cvars.Find("g_killheight").fValue;
        } catch(NullPointerException ex) {
            yfrac = 0;
        }
        if(yfrac < -1f)
            yfrac = -1f;
        if(yfrac > 1)
            yfrac = 1f;

        Vector2f position = new Vector2f(view.Origin.x + view.xmin + view.w * 0.2f
                ,view.Origin.y + view.ymin + view.h * 0.9f + view.h* yfrac * 0.1f);

        sunPositionOnScreen.set(Ref.glRef.GetResolution());
        sunPositionOnScreen.x *= 0.2f;
        sunPositionOnScreen.y *= 0.9f + 0.1f * yfrac;
        Vector2f TexOffset = new Vector2f();
        Vector2f TexSize = new Vector2f(1,1);
        Color color = sunColor;
        //color.set((byte)182, (byte)126, (byte)91, (byte)30); // sunrise / sunset
        //color.set((byte)192, (byte)191, (byte)173, (byte)30); // noon
        color.set((byte)189, (byte)190, (byte)192, (byte)30); // cloud/haze
        //color.set((byte)174, (byte)183, (byte)190, (byte)30); // overcast

        float scale = view.w / view.h;
        Vector2f Extent = new Vector2f(0.05f * view.w,0.05f *view.h * scale);
        float depth = -100;

        GL11.glPushMatrix();
        GL11.glTranslatef(position.x, position.y, 0);

        GL11.glBegin(GL11.GL_QUADS);
        {
            GL20.glVertexAttrib2f(2, TexOffset.x, TexOffset.y);
            GL20.glVertexAttrib4Nub(1, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
            GL20.glVertexAttrib3f(0, -Extent.x, -Extent.y, depth);

            GL20.glVertexAttrib2f(2, TexOffset.x+TexSize.x, TexOffset.y);
            GL20.glVertexAttrib4Nub(1, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
            GL20.glVertexAttrib3f(0, Extent.x, -Extent.y, depth);

            GL20.glVertexAttrib2f(2, TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
            GL20.glVertexAttrib4Nub(1, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
            GL20.glVertexAttrib3f(0, Extent.x, Extent.y, depth);

            GL20.glVertexAttrib2f(2, TexOffset.x, TexOffset.y+TexSize.y);
            GL20.glVertexAttrib4Nub(1, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
            GL20.glVertexAttrib3f(0, -Extent.x, Extent.y, depth);
        }
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    public void RenderBackground() {
        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);

        ViewParams view = game.cg.refdef;

        float yfrac;
        try {
            yfrac = view.Origin.y / Ref.cvars.Find("g_killheight").fValue;
        } catch(NullPointerException ex) {
            yfrac = 0f;
        }
        if(yfrac < -1f)
            yfrac = -1f;
        if(yfrac > 1)
            yfrac = 1f;

        float xfrac = view.Origin.x / 5000f;

        Vector2f texSize = new Vector2f(1, 0.8f);
        Vector2f texoffset = new Vector2f(0+xfrac,0.1f + yfrac * -0.1f);

        spr.Set(new Vector2f(view.Origin.x + view.xmin, view.Origin.y + view.ymin),
                new Vector2f(view.w, view.h), Ref.ResMan.LoadTexture("data/background_1.png"), texoffset, texSize);
        spr.SetDepth(CGame.PLAYER_LAYER - 200);
    }


    //
    // Misc.
    //
    void DrawEntities() {
        if(game.cg_drawentities.iValue == 0 || Ref.game == null || Ref.game.g_entities == null)
            return;

        for (int i= 0; i < Ref.game.level.num_entities; i++) {
            Gentity ent = Ref.game.g_entities[i];
            if(!ent.inuse || ent.s.eType == EntityType.PLAYER)
                continue; // ignore players and unused entities

            if(!ent.r.linked)
                continue;

            Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
            Vector2f size = new Vector2f();
            // FIX
            size.x = ent.r.absmax.x - ent.r.absmin.x;
            size.y = ent.r.absmax.y - ent.r.absmin.y;
            

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

            spr.Set(new Vector2f(ent.r.absmin.x, ent.r.absmin.y), size, tex, null, null);
            spr.SetDepth(MapEditor.EDITOR_LAYER-1);
            spr.SetColor(0, 255, 255, 127);

            Vector2f center = new Vector2f(ent.r.absmin);
            size.scale(0.5f);
            Vector2f.add(center, size, center);

            MapEditor.renderHighlightBlock(center, size, MapEditor.EDITOR_LAYER-1, (Color) Color.GREEN);
        }
    }

    private void RenderModel(int index, Vector2f position, int layer) {
        index = Ref.cm.cm.InlineModel(index);

        // Get bounds
        BlockModel model = Ref.cm.cm.getModel(index);
        model.moveTo(position);
    }

    void DrawBin() {
        ViewParams view = game.cg.refdef;
        ArrayList<Bin> bins = Ref.spatial.getBins(view.Origin.x + Game.PlayerMins.x, view.Origin.y + Game.PlayerMins.y, view.Origin.x + Game.PlayerMaxs.x, view.Origin.y + Game.PlayerMaxs.y);
        ArrayList<Block> blocks = new ArrayList<Block>();
        for (int i= 0; i < bins.size(); i++) {
            Bin bin = bins.get(i);
            bin.getBlocks(blocks);
        }

        for (Block block : blocks) {
            block.Render();
        }
    }

    void RenderClientEffects() {
        if(lookingAtCube != null) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glLineWidth(1f);
            GL11.glDisable(GL11.GL_CULL_FACE);
            //GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            Ref.textMan.AddText(new Vector2f(10, 200), ""+lookingAtCube.highlightSide, Align.LEFT, Type.HUD);
            lookingAtCube.chunk.renderSingleWireframe(lookingAtCube.x, lookingAtCube.y, lookingAtCube.z, CubeType.DIRT);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
            //GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

        }
    }

    private void runLerpFrame(ClientInfo ci, LerpFrame lf, int newanimation, float speedScale) {

        // see if the animation sequence is switching
        if(newanimation != lf.animationNumber || lf.animation == null) {
            setLerpFrameAnimation(ci, lf, newanimation);
        }

        // if we have passed the current frame, move it to
	// oldFrame and calculate a new frame
        if(Ref.cgame.cg.time >= lf.frametime) {
            lf.oldFrame = lf.frame;
            lf.oldFrameTime = lf.frametime;

            // get the next frame based on the animation
            IQMAnim anim = lf.animation;
            if(anim.frameLerp == 0) return; // shouldn't happen

            if(Ref.cgame.cg.time < lf.animationTime) {
                lf.frametime = lf.animationTime;
            } else {
                lf.frametime = lf.oldFrameTime + anim.frameLerp;
            }
            float f = (lf.frametime - lf.animationTime) / anim.frameLerp;
            f *= speedScale;

            int numFrames = anim.num_frames;
            if(f >= numFrames) {
                f -= numFrames;
                if(anim.loopFrames != 0) {
                    f %= anim.loopFrames;
                    f += anim.num_frames - anim.loopFrames;
                } else {
                    f = numFrames - 1;
                    // the animation is stuck at the end, so it
                    // can immediately transition to another sequence
                    lf.frametime = Ref.cgame.cg.time;
                }
            }

            if(f < 0) f = -f;
            lf.frame = (int) (anim.first_frame + f);
            if(Ref.cgame.cg.time > lf.frametime) {
                lf.frametime = Ref.cgame.cg.time;

            }
        }

        if(lf.frametime > Ref.cgame.cg.time + 200) lf.frametime = Ref.cgame.cg.time;
        if(lf.oldFrameTime > Ref.cgame.cg.time) lf.oldFrameTime = Ref.cgame.cg.time;

        // calculate current lerp value
        if(lf.frametime == lf.oldFrameTime) {
            lf.backlerp = 0;
        } else {
            lf.backlerp = 1f - (float)(Ref.cgame.cg.time - lf.oldFrameTime) / (lf.frametime - lf.oldFrameTime);
        }

    }

    private void setLerpFrameAnimation(ClientInfo ci, LerpFrame lf, int newanimation) {
        lf.animationNumber = newanimation;
        newanimation &= ~128;
        if(newanimation < 0 || newanimation > Ref.cm.cubemap.model.anims.length) {
            Ref.common.Error(Common.ErrorCode.DROP, "Bad animation number " + newanimation);
        }

        IQMAnim anim = Ref.cm.cubemap.model.anims[newanimation];
        lf.animation = anim;
        lf.animationTime = lf.frametime + anim.initialLerp;

    }



}
