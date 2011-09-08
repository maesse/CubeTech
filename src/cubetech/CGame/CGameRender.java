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
import cubetech.collision.CubeCollision;
import cubetech.collision.CubeMap;
import cubetech.collision.SingleCube;
import cubetech.common.Animations;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.Move.MoveType;
import cubetech.common.PlayerState;
import cubetech.common.items.IItem;
import cubetech.common.items.ItemType;
import cubetech.common.items.Weapon;
import cubetech.common.items.WeaponInfo;
import cubetech.common.items.WeaponItem;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.gfx.CubeMaterial;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.CubeType;
import cubetech.gfx.Shader;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.gfx.VBO;
import cubetech.input.Input;
import cubetech.iqm.IQMAdjacency;
import cubetech.iqm.IQMAnim;
import cubetech.iqm.IQMModel;
import cubetech.misc.Profiler;
import cubetech.misc.Ref;
import cubetech.spatial.Bin;
import java.util.ArrayList;
import java.util.Locale;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CGameRender {
    private CGame game;

    

    SingleCube lookingAtCube;

    public CGameRender(CGame game) {
        this.game = game;

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
        if(cent == Ref.cgame.cg.predictedPlayerEntity && !Ref.cgame.cg_tps.isTrue() && (!Ref.cgame.cg_freecam.isTrue() || !Ref.cgame.cg.playingdemo)) return;
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
            case EntityType.MISSILE:
                missile(cent);
                break;
        }
    }

    private void missile(CEntity cent) {
        WeaponItem w = WeaponItem.get(cent.currentState.weapon);
        cent.lerpAngles.set(cent.currentState.Angles);

        

        // add trails
        Vector3f o = cent.lerpOrigin;
//        Vector3f min = cent.currentState.

        // convert direction of travel into axis

        RenderEntity ent = Ref.render.createEntity(REType.MODEL);

        ent.axis[0].set(cent.currentState.pos.delta);
        if(Helper.Normalize(ent.axis[0]) == 0) {
            ent.axis[0].z = 1;
        }
//        ent.axis[0].z *= -1;

        WeaponInfo wi = w.getWeaponInfo();
        if(wi != null) {
            Helper.rotateAroundDirection(ent.axis, game.cg.time/5f);

            ent.model = wi.missileModel;
            ent.origin.set(cent.lerpOrigin);
    //        ent.axis = Helper.AnglesToAxis(ent.axis[0]);
            Ref.render.addRefEntity(ent);

            if(wi.missileSound != null && !wi.missileSound.isEmpty()) {
                Vector3f vel = cent.currentState.pos.delta;
                
                Ref.soundMan.addLoopingSound(cent.currentState.ClientNum, cent.lerpOrigin, vel, Ref.soundMan.AddWavSound(wi.missileSound));
            }

            wi.missileTrailFunc.run(cent);

//            if(wi.missileSound != null && !wi.missileSound.isEmpty()) {
//                int buffer = Ref.soundMan.AddWavSound(wi.missileSound);
//                Ref.soundMan.playEntityEffectLoop(cent.currentState.ClientNum, buffer, 1.0f);
//            }
        }



        
        //Helper.renderBBoxWireframe(o.x - 10, o.y - 10, o.z - 10, o.x + 10, o.y + 10, o.z + 10);
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

        IItem item = Ref.common.items.getItem(es.modelindex);
        if(item.getType() == ItemType.POWERUP)
            bounce = (float) Math.sin(game.cg.time/400f) * 2f;
        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
        Vector3f min = new Vector3f(), max = new Vector3f();
        item.getBounds(min, max);
        float radius = (float) Math.sqrt((max.lengthSquared() - min.lengthSquared())*0.5);

        spr.Set(cent.lerpOrigin.x, cent.lerpOrigin.y + bounce, radius, Ref.ResMan.LoadTexture(item.getIconName()));
        spr.SetDepth(CGame.PLAYER_LAYER-1);
        if(item.getType() == ItemType.HEALTH)
            spr.SetAngle(game.cg.autoAngle);

    }

    public void renderViewModel(PlayerState ps) {
        // no gun if in third person view or a camera is active
        if (Ref.cgame.cg_tps.isTrue() || (Ref.cgame.cg_freecam.isTrue() && Ref.cgame.cg.playingdemo)) return;

        // don't draw if testing a gun model
        if(Ref.cgame.cg.testGun) return;

        RenderEntity ent = Ref.render.createEntity(REType.MODEL);
        // get clientinfo for animation map
        WeaponItem wi = Ref.common.items.getWeapon(ps.weapon);
        if(wi == null) return;
        
        // Let weapon render any effects
        wi.renderClientEffects();

        
        WeaponInfo winfo = wi.getWeaponInfo();
        if(winfo == null || winfo.viewModel == null) return;
        ent.model = winfo.viewModel;

        Vector3f angles = new Vector3f();
        CalculateWeaponPosition(ent.origin, angles);
        ent.axis = Helper.AnglesToAxis(angles, ent.axis);

        Helper.VectorMA(ent.origin, game.cg.cg_gun_x.fValue, ent.axis[0], ent.origin);
        Helper.VectorMA(ent.origin, game.cg.cg_gun_y.fValue, ent.axis[1], ent.origin);
        Helper.VectorMA(ent.origin, game.cg.cg_gun_z.fValue, ent.axis[2], ent.origin);
        
        if(ent.model.anims != null && ent.model.anims.length > 0) {
            int max = ent.model.anims[0].num_frames;
            float left = (Ref.cgame.cg.time/50f) - (Ref.cgame.cg.time/50);
            ent.backlerp = 1f-left;
            ent.frame = (Ref.cgame.cg.time/50) % max;
            ent.oldframe = ent.frame - 1;
            if(ent.oldframe < 0) ent.oldframe = max-1;
        }

        Ref.render.addRefEntity(ent);

        
    }

    private void CalculateWeaponPosition(Vector3f position, Vector3f angles) {
        position.set(game.cg.refdef.Origin);
        angles.set(game.cg.refdef.Angles);

        
    }

    // Render a player
    private void Player(CEntity cent) {
        ClientInfo ci = game.cgs.clientinfo[cent.currentState.ClientNum];
        if(!ci.infoValid) return;
        RenderEntity ent = Ref.render.createEntity(REType.MODEL);

        ent.model = Ref.ResMan.loadModel(ci.modelName);

        // set origin & angles
        ent.origin.set(cent.lerpOrigin);
        ent.origin.z += Game.PlayerMins.z;
//        Vector3f deltaMove = cent.currentState.pos.delta;

        Vector3f angles = new Vector3f();
//        if(deltaMove.lengthSquared() > 0.1f) {
//            angles = new Vector3f(deltaMove);
//            angles.y = (float) (180/Math.PI * Math.atan2(-deltaMove.y, deltaMove.x) + Math.PI / 2f);
//        }
//        else {
//            angles = new Vector3f(cent.lerpAngles);
//            angles.y *= -1f;
//            angles.x *= -1f;
//        }
//
//        Vector3f velNorm = new Vector3f(deltaMove);
//        if(Helper.Normalize(velNorm) == 0) {
//            velNorm.set(0,0,1);
//        }
//        angles.x = 0;
//        angles.z = 0;
//        angles.set(0,90,0);
//        ent.axis[0].set(1,0,0);
//        Helper.rotateAroundDirection(ent.axis, game.cg.time/10f);

        angles.set(cent.lerpAngles);
        if(angles.x > 1) angles.x = 1;
        if(angles.x < -25) angles.x = -25;
        ent.axis = Helper.AnglesToAxis(angles, ent.axis);

        playerAngles(cent, ent);
        
        //ent.axis = Helper.AnglesToAxis(angles);
//        for (int i= 0; i < 3; i++) {
//            ent.axis[i].normalise();
//        }
//        ent.axis[0].scale(-1f);
//        ent.axis[1].scale(-1f);

        playerAnimation(cent, ent);

        // Fade out when close to camera
        Vector3f cameraOrigin = Ref.cgame.cg.refdef.Origin;
        Vector3f entityOrigin = ent.origin;

        float distance = Helper.VectorDistance(cameraOrigin, entityOrigin);
        float maxdist = 60; // where fadeout starts
        float mindist = 30; // where model is completely transparent
        if(distance < maxdist) {
            distance -= mindist;
            if(distance < 2) distance = 2;
            distance /= maxdist-mindist;
        }

        ent.color = new Vector4f(255, 255, 255, 255f*distance);
        Ref.render.addRefEntity(ent);
//        LocalEntity.smokePuff(cent.lerpOrigin, new Vector3f(), 32, 1, 1, 1, 1, 500, Ref.cgame.cg.time, 0, 0, Ref.ResMan.LoadTexture("data/smokepuff.png").asMaterial());
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
        DrawChat();
        game.lag.Draw();


        if(Ref.common.isDeveloper()) {
            Ref.textMan.AddText(new Vector2f(0, 0), "Position: " + game.cg.refdef.Origin, Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()), "Velocity: " + game.cg.predictedPlayerState.velocity, Align.LEFT, Type.HUD);
            //Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*2), "Ping: " + Ref.cgame.cg.snap.ps.ping, Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*2), "Pull accel: " + game.getPullAccel(), Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*2), "v: " + game.cg.predictedPlayerState.delta_angles[0], Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*2), "Total VBO size: " + VBO.TotalBytes/(1024*1024) + "mb", Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*3), "cl_chunks/s: " + game.map.chunkPerSecond, Align.LEFT, Type.HUD);
            if(Ref.cm != null && Ref.cm.cubemap != null && Ref.cm.cubemap.nChunks != 0)
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*4), 
                    String.format(Locale.ENGLISH,"Chunks: %d (avg. quads/chunk: %d (%.1fkb))", Ref.cm.cubemap.nChunks, (Ref.cm.cubemap.nSides)/Ref.cm.cubemap.nChunks, (Ref.cm.cubemap.nSides* CubeChunk.PLANE_SIZE )/Ref.cm.cubemap.nChunks/1024f), Align.LEFT, Type.HUD);
            //if(Ref.cgame.cg.refdef.planes[0] != null)
                //Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*5), "near plane: " + game.cg.refdef.planes[0], Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*4), "Vv: " + Ref.Input.viewangles[0], Align.LEFT, Type.HUD);

            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            Vector2f res = Ref.glRef.GetResolution();
            float width = 14;
            spr.setLine(new Vector2f(res.x/2f - width / 2f, res.y/2f), new Vector2f(res.x/2f + width / 2f, res.y/2f), 2f);
            spr.SetColor(255, 0, 0, 255);
            spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.setLine(new Vector2f(res.x/2f , res.y/2f - width / 2f), new Vector2f(res.x/2f , res.y/2f + width / 2f), 2f);
            spr.SetColor(255, 0, 0, 255);
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
        PlayerState ps = game.cg.snap.ps;
        

        Ref.textMan.AddText(new Vector2f(0, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()*2), "HP: " + ps.stats.Health, Align.LEFT, Type.HUD,2f);

        if(ps.weapon != Weapon.NONE) {
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()*2), "Ammo: " + ps.stats.getAmmo(ps.weapon), Align.RIGHT, Type.HUD,2f);
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x/2f, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()*2), ""+ps.weapon , Align.CENTER, Type.HUD);
            Ref.textMan.AddText(new Vector2f(Ref.glRef.GetResolution().x/2f, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()*1), "(" + ps.weaponState + ")", Align.CENTER, Type.HUD);
        }
        
        // Draw powerups
        
//        for (int i= 0; i < PlayerState.NUM_POWERUPS; i++) {
//            if(ps.powerups[i] <= 0)
//                continue;
//
//            int ms = ps.powerups[i];
//            int sec = ms / 1000;
//
//
//            IItem item = Ref.common.items.findItemByClassname("item_boots");
//            if(item == null) {
//                Common.LogDebug("CGame.Draw2D: Can't find item " + i);
//                continue;
//            }
//            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
//            Vector2f res = Ref.glRef.GetResolution();
//            float radius = 32;
//            spr.Set(res.x - radius * 2, res.y - ( res.y * 0.7f + i * radius * 2) , radius, item.icon);
//            Ref.textMan.AddText(new Vector2f(res.x - radius * 2 , res.y * 0.7f + i * radius * 2 +5), ""+sec, Align.CENTER, Type.HUD);
//        }

        // Handle changes from server

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
        drawprofiler();
    }

    private void drawprofiler() {
        float[] times = Profiler.getTimes();
        float[] percentage = new float[times.length];

        float totalTime = Ref.common.framemsec; // this will be 100% usage
        for (int i= 0; i < times.length; i++) {
            percentage[i] = Helper.Clamp(times[i] / totalTime, 0, 1);
        }
        
        float spacing = 220;
        Vector2f res = Ref.glRef.GetResolution();
        float x = 100;
        float y = res.y - 100;
        if(totalTime > 25) {
            Common.Log("Frame took %d ms. Sub-times:", (int)totalTime);
        }
        for (int i= 0; i < times.length; i++) {
            String section = Profiler.Sec.values()[i].toString();
            String secTime = String.format("%.2f", times[i]);

            if(totalTime > 25) {
                // Had a frame-hitch.. print out the times
                Common.Log("%s: %s (%d%%)", section, secTime, (int)(percentage[i]*100f));
            }
            Vector2f position = new Vector2f(x, y);

            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(position.x+1, res.y - position.y - Ref.textMan.GetCharHeight()),
                    new Vector2f((spacing-2)*percentage[i], Ref.textMan.GetCharHeight()), null, null, null);
            spr.SetColor(50,50,200,127);

            Vector2f textSize = Ref.textMan.AddText(position, section, Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(x+textSize.x, y), secTime, Align.LEFT, Type.HUD);

            x += spacing;
            if(x > res.x - spacing) {
                x = 100;
                y += textSize.y + 1;
            }
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
            spr.SetDepth(CGame.PLAYER_LAYER-1);
            spr.SetColor(0, 255, 255, 127);
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

    private void runLerpFrame(ClientInfo ci, LerpFrame lf, int newanimation, float speedScale) {
        // see if the animation sequence is switching
        if(newanimation != lf.animationNumber || lf.animation == null) {
            setLerpFrameAnimation(ci, lf, newanimation);
        }

        if(lf.animation == null) {
            return; // no animation for you!
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
        
        // Figure out what animation was requested
        newanimation &= ~128;
        if(newanimation < 0 || newanimation >= Animations.values().length) {
            Common.LogDebug("Invalid animation %d", newanimation);
            return;
        }
        Animations animation = Animations.values()[newanimation];


        // Try to load the model
        IQMModel model = Ref.ResMan.loadModel(ci.modelName);
        if(model == null) {
            Common.LogDebug("Can't find model %s", ci.modelName);
            return;
        }

        // Check if we have a valid animation
        IQMAnim anim = model.getAnimation(animation);
        if(anim == null) {
            //Common.LogDebug("Can't find animation %s in model %s", animation.toString(), ci.modelName);
            if(model.anims == null) return;

            // Don't have the right animation, just grab the first
            anim = model.anims[0];
        }

        lf.animation = anim;
        lf.animationTime = lf.frametime + anim.initialLerp;

    }

    private static final int[] moveOffsets = new int[] {0,22,45,-22,0,22,-45,-22};
    private void playerAngles(CEntity cent, RenderEntity rent) {
        Vector3f headAngle = new Vector3f(cent.lerpAngles);
        headAngle.y = Helper.AngleMod(headAngle.y);

        int dir = (int)cent.currentState.Angles2.y;
        if(dir < 0 || dir > 7) {
            Common.LogDebug("Invalid movedirection %d", dir);
            return;
        }
        Vector3f torsoAngle = new Vector3f();
        cent.pe.torso.yawing = true;
        cent.pe.torso.pitching = true;
        // yaw
        torsoAngle.y = headAngle.y + 0.5f * moveOffsets[dir];
        swingAngles(torsoAngle.y, 25, 90, game.cg_swingspeed.fValue, cent.pe.torso, true); // yaw
        torsoAngle.y = cent.pe.torso.yawAngle;

        // pitch
        float dest;
        if(headAngle.x < 0 ) {
            dest = (headAngle.x) * 0.75f;
            if(dest < -30) dest = -30;
        } else {
            dest = headAngle.x * 0.75f;
            if(dest> 30) dest = 30;
        }
        swingAngles(dest, 15, 30, 0.1f, cent.pe.torso, false);
        torsoAngle.x = cent.pe.torso.pitchAngle;
        
        rent.axis = Helper.AnglesToAxis(torsoAngle, rent.axis);
    }

    private void swingAngles(float dest, int swingTolerance, int clampTolerance,
            float speed, LerpFrame out, boolean isYaw) {
        boolean swinging = isYaw?out.yawing:out.pitching;
        float angle = isYaw?out.yawAngle:out.pitchAngle;
        float swing;
        if(!swinging) {
            // see if a swing should be started
            swing = Helper.AngleSubtract(angle, dest);
            if(swing > swingTolerance || swing < -swingTolerance) {
                swinging = true;
            }
        }

        if(!swinging) {
            return;
        }

        // modify the speed depending on the delta
	// so it doesn't seem so linear
        swing = Helper.AngleSubtract(dest, angle);
        float scale = Math.abs(swing);
        scale /= swingTolerance;
        if(scale < 0.05) scale = 0.05f;
        if(scale > 2.0) scale = 2.0f;

        // swing towards the destination angle
        float move;
        if(swing >=0) {
            move = game.cg.frametime * scale * speed;
            if(move >= swing) {
                move = swing;
                swinging = false;
            }
            angle = Helper.AngleMod(angle + move);
        } else if(swing < 0) {
            move = game.cg.frametime * scale * -speed;
            if(move <= swing) {
                move = swing;
                swinging = false;
            }
            angle = Helper.AngleMod(angle + move);
        }

        // clamp to no more than tolerance
        swing = Helper.AngleSubtract(dest, angle);
        if(swing > clampTolerance) {
            angle = Helper.AngleMod(dest - (clampTolerance -1));
        } else if(swing < -clampTolerance) {
            angle = Helper.AngleMod(dest + (clampTolerance-1));
        }

        if(isYaw) {
            out.yawing = swinging;
            out.yawAngle = angle;
        } else {
            out.pitching = swinging;
            out.pitchAngle = angle;
        }
    }



}
