package cubetech.CGame;

import cubetech.collision.ClientCubeMap;
import cubetech.Game.Gentity;
import cubetech.Game.GentityFilter;
import cubetech.collision.CubeChunk;
import cubetech.collision.DefaultPhysics;
import cubetech.common.*;
import cubetech.common.items.*;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.entities.Vehicle;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.gfx.VBOPool;
import cubetech.gfx.VideoManager;
import cubetech.iqm.IQMModel;
import cubetech.misc.Profiler;
import cubetech.misc.Ref;
import cubetech.snd.SoundHandle;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Locale;
import nbullet.vehicle.DefaultVehicleRaycaster;
import nbullet.vehicle.RaycastVehicle;
import nbullet.vehicle.VehicleRaycasterResult;
import nbullet.vehicle.WheelInfo;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CGameRender {
    private CGame game;
    protected PlayerRender player;

    public CGameRender(CGame game) {
        this.game = game;
        this.player = new PlayerRender(game);
    }

    //
    // Entities
    //
    void AddCEntity(CEntity cent, boolean noLerp) {
        // event-only entities will have been dealt with already
        if(cent.currentState.eType >= EntityType.EVENTS)
            return;

        // calculate the current origin
        if(!noLerp) {
            cent.CalcLerpPosition();
        }
        cent.Effects();
        if(cent == Ref.cgame.cg.cur_lc.predictedPlayerEntity && !Ref.cgame.cg_tps.isTrue() 
                && (!Ref.cgame.cg_freecam.isTrue() || !Ref.cgame.cg.playingdemo) ) {
            // Don't render local playermodel
            if(cent.pe.boneMeshModel != null && Ref.cgame.cg.nViewports <= 1) {
                Ref.cgame.centitiesWithPhysics.remove(cent);
                Ref.cgame.cleanPhysicsFromCEntity(cent);
            }
            return;
        }
        switch(cent.currentState.eType) {
            case EntityType.PLAYER:
                player.renderPlayer(cent);
                break;
            case EntityType.ITEM:
                Item(cent);
                break;
            case EntityType.MISSILE:
                missile(cent);
                break;
            case EntityType.GENERAL:
                general(cent);
                break;
        }
    }

    private void general(CEntity cent) {
        EntityState es = cent.currentState;
        if(es.modelindex <= 0)  {
            return;
        }

        RenderEntity ref = Ref.render.createEntity(REType.MODEL);
        String modelname = Ref.client.cl.GameState.get(CS.CS_MODELS+es.modelindex-1);
        if(modelname == null) {
            Ref.common.Error(Common.ErrorCode.DROP, "Model not found: " + (es.modelindex-1));
        }
        IQMModel model = Ref.ResMan.loadModel(modelname);
        ref.model = model.buildFrame(0, 0, 0, null);
        ref.origin.set(cent.lerpOrigin);
        cent.lerpAnglesQ.quatToMatrix(ref.axis);
        Ref.render.addRefEntity(ref);
    }

    private void missile(CEntity cent) {
        WeaponItem w = WeaponItem.get(cent.currentState.weapon);
        cent.lerpAngles.set(cent.currentState.Angles);

        // convert direction of travel into axis
        RenderEntity ent = Ref.render.createEntity(REType.MODEL);

        ent.axis[0].set(cent.currentState.pos.delta);
        if(Helper.Normalize(ent.axis[0]) == 0) {
            ent.axis[0].z = 1;
        }

        WeaponInfo wi = w.getWeaponInfo();
        if(wi != null) {
            Helper.rotateAroundDirection(ent.axis, game.cg.time/2f);

            ent.model = wi.missileModel.buildFrame(0, 0, 0, null);
            ent.origin.set(cent.lerpOrigin);
            Ref.render.addRefEntity(ent);

            if(wi.missileSound != null && !wi.missileSound.isEmpty()) {
                Vector3f vel = cent.currentState.pos.delta;
                
                Ref.soundMan.addLoopingSound(cent.currentState.number, cent.lerpOrigin, vel, Ref.soundMan.AddWavSound(wi.missileSound));
            }

            wi.missileTrailFunc.run(cent);
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

        IItem item = Ref.common.items.getItem(es.modelindex);
        if(item.getType() == ItemType.WEAPON && item instanceof WeaponItem) {
            WeaponItem wi = (WeaponItem)item;
            IQMModel wModel = wi.getWeaponInfo().worldModel;
            if(wModel != null) {
                RenderEntity ent = Ref.render.createEntity(REType.MODEL);
                ent.model = wModel.buildFrame(0, 0, 0, null);
                ent.origin.set(cent.lerpOrigin);
                cent.lerpAnglesQ.quatToMatrix(ent.axis);
                Ref.render.addRefEntity(ent);
                return;
            }
        }
        
        float bounce = 0;
        if(item.getType() == ItemType.POWERUP) bounce = (float) Math.sin(game.cg.time/400f) * 2f;

        Sprite spr = Ref.SpriteMan.GetSprite(Type.GAME);
        Vector3f min = new Vector3f(), max = new Vector3f();
        item.getBounds(min, max);
        float radius = (float) Math.sqrt((max.lengthSquared() - min.lengthSquared())*0.5);
        spr.Set(cent.lerpOrigin.x, cent.lerpOrigin.y + bounce, radius, Ref.ResMan.LoadTexture(item.getIconName()));
        spr.SetDepth(CGame.PLAYER_LAYER-1);
        if(item.getType() == ItemType.HEALTH) spr.SetAngle(game.cg.cur_lc.autoAngle);
    }
    
    private void drawCrosshair(Vector2f offset, Vector2f res) {
        // Draw crosshair
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        
        float width = 14;
        spr.setLine(new Vector2f(offset.x + res.x/2f - width / 2f, offset.y + res.y/2f), new Vector2f(offset.x +res.x/2f + width / 2f, offset.y +res.y/2f), 2f);
        spr.SetColor(255, 0, 0, 255);
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.setLine(new Vector2f(offset.x +res.x/2f , offset.y + res.y/2f - width / 2f), new Vector2f(offset.x +res.x/2f , offset.y +res.y/2f + width / 2f), 2f);
        spr.SetColor(255, 0, 0, 255);
    }
    
    private void drawDamage(Vector2f offset, Vector2f res) {
        float dmg = game.cg.cur_lc.predictedPlayerState.dmgTime / 200f;
        dmg = Helper.Clamp(dmg, 0, 1);
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        
        spr.Set(offset.x, offset.y, res.x, res.y, null);
        spr.SetColor(255, 0, 0, (int)(255*dmg));
    }
    
    private void getPlayerMinMax(Vector2f offset, Vector2f res) {
        offset.set(game.cg.refdef.ViewportX, game.cg.refdef.ViewportY);
        res.set(game.cg.refdef.ViewportWidth, game.cg.refdef.ViewportHeight);
    }
    
    private void drawPlayerHUD() {
        Vector2f offset = new Vector2f(), res = new Vector2f();
        getPlayerMinMax(offset, res);
        
        
        drawDamage(offset, res);
        
        // Death info
        if(game.cg.cur_lc.predictedPlayerState.stats.Health <= 0) {
            Ref.textMan.AddText(new Vector2f(offset.x + res.x / 2f, Ref.glRef.GetResolution().y - (offset.y + res.y /2f  + Ref.textMan.GetCharHeight()*2)), "^5You are dead", Align.CENTER, Type.HUD);
            Ref.textMan.AddText(new Vector2f(offset.x + res.x / 2f, Ref.glRef.GetResolution().y - (offset.y + res.y/2f  + Ref.textMan.GetCharHeight())), "Shoot to spawn", Align.CENTER, Type.HUD);
            Ref.textMan.AddText(new Vector2f(offset.x + res.x / 2f, Ref.glRef.GetResolution().y - (offset.y + res.y/2f)), "ESC for menu", Align.CENTER, Type.HUD);
        } else {
            float textScale = 2f;
            if(game.cg.nViewports > 2) textScale = 1.25f;
            drawCrosshair(offset, res);
            // Health
            PlayerState ps = game.cg.cur_ps;
            Ref.textMan.AddText(new Vector2f(offset.x, Ref.glRef.GetResolution().y - (offset.y + Ref.textMan.GetCharHeight()*textScale)), "HP: " + ps.stats.Health, Align.LEFT, Type.HUD,textScale);
            // Ammo
            if(ps.weapon != Weapon.NONE) {
                Ref.textMan.AddText(new Vector2f(offset.x + res.x, Ref.glRef.GetResolution().y - (offset.y + Ref.textMan.GetCharHeight()*textScale)), "Ammo: " + ps.stats.getAmmo(ps.weapon), Align.RIGHT, Type.HUD,textScale);
                String col = "";
                if(ps.weaponState == WeaponState.FIRING) col = "^5";
                if(ps.weaponState == WeaponState.RAISING || ps.weaponState == WeaponState.DROPPING) col = "^2";
                Ref.textMan.AddText(new Vector2f(offset.x + res.x/2f, Ref.glRef.GetResolution().y - (offset.y + Ref.textMan.GetCharHeight()*1)), col+ps.weapon , Align.CENTER, Type.HUD);
            }
        }
        
        
    }
    
    //
    // UI
    //
    void Draw2D() {
//        if(Ref.cgame.videoManager != null) {
//            Dimension dim = VideoManager.dim;
//            int w = 512, h = 512;
//            if(dim != null) {
//                w = dim.width;
//                h = dim.height;
//            }
//            float hoffset = Ref.glRef.GetResolution().y / 2f - h / 2;
//            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
//            spr.Set(0, hoffset, w,h, Ref.cgame.videoManager.getTexture(), new Vector2f(0,1), new Vector2f(1, -1));
//        }
        
        drawPlayerHUD();
        if(game.cg.cur_viewport != 0) return;
        
        DrawChat();
        game.lag.Draw();

        if(Ref.common.isDeveloper() && game.cg.nViewports <= 1) {
            Ref.textMan.AddText(new Vector2f(0, 0),
                    "Position: " + game.cg.refdef.Origin, Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()),
                    "Velocity: " + game.cg.cur_lc.predictedPlayerState.velocity, Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*2),
                    "cl_chunks/s: " + game.map.chunkPerSecond, Align.LEFT, Type.HUD);
            if(Ref.cm != null && Ref.cm.cubemap != null && Ref.cm.cubemap.nChunks != 0)
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*3),
                    String.format(Locale.ENGLISH,"Chunks: %d (avg. quads/chunk: %d (%.1fkb))", 
                    Ref.cm.cubemap.nChunks, (Ref.cm.cubemap.nSides)/Ref.cm.cubemap.nChunks,
                    (Ref.cm.cubemap.nSides* CubeChunk.PLANE_SIZE )/Ref.cm.cubemap.nChunks/1024f), Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*4),
                    "VBOPool: " + VBOPool.Global.getFreeCount() + " free, " + VBOPool.Global.getBusyCount() + " in use", Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()*5),
                    "Chunks: " + ClientCubeMap.nChunks + " visible. " + ClientCubeMap.nSides + " quads. " + ClientCubeMap.nVBOthisFrame + " vbo's filled this frame", Align.LEFT, Type.HUD);
        }

        

        if(Ref.net.net_graph.iValue > 0) {
            DrawNetGraph();
        }

        
        
        // Handle changes from server
        if(game.cg.showScores) {
            DrawScoreboard();
        }
        
        if(Ref.common.developer.isTrue()) {
            Profiler.setForceGLFinish(false);
            if(game.cg_drawprofiler.isTrue()) drawprofiler();
        }
        

        if(Ref.common.cl_paused.iValue == 1) {
            Vector2f pos = new Vector2f(Ref.glRef.GetResolution());
            pos.scale(0.5f);
            Ref.textMan.AddText(pos, "^3PAUSED", Align.CENTER, Type.HUD, 2,0);
        }

        if(Ref.client.clc.servername.equals("localhost")) {
            Gentity ent = Ref.game.Find(null, GentityFilter.CLASSNAME, "vehicle");
            if(ent != null) {
                Vehicle v = (Vehicle)ent;
                ArrayList<String> vehicleInfo = v.getInfoStrings();
                float x = Ref.glRef.GetResolution().x * 0.8f;
                Vector2f pos = new Vector2f(x, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()*1);
                for (int i = 0; i < vehicleInfo.size(); i++) {
                    pos.y = Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()*(1+i);
                    Ref.textMan.AddText(pos, vehicleInfo.get(i), Align.LEFT, Type.HUD);
                }
                
                RaycastVehicle rv = v.getRaycastVehicle();
                for (int i = 0; i < rv.getWheelCount(); i++) {
                    WheelInfo wi = rv.getWheelInfo(i);
                    Vector3f rayStart = wi.getRayHardPoint();
                    Vector3f rayEnd = wi.getRayContactPoint();
                    rayStart.scale(DefaultPhysics.INV_SCALE_FACTOR);
                    rayEnd.scale(DefaultPhysics.INV_SCALE_FACTOR);
                    
                    RenderEntity rent = Ref.render.createEntity(REType.BBOX);
                    rent.flags = RenderEntity.FLAG_NOLIGHT;
                    rent.origin.set(rayEnd);
                    boolean contact = wi.getRayIsInContact();
                    if(contact) rent.outcolor.set(0, 255, 0, 255);
                    else rent.outcolor.set(255, 0, 0, 255);
                    
                    Ref.render.addRefEntity(rent);
                    
                    rent = Ref.render.createEntity(REType.BBOX);
                    rent.flags = RenderEntity.FLAG_NOLIGHT;
                    rent.origin.set(rayStart);
                    Ref.render.addRefEntity(rent);
                }
                
//                DefaultVehicleRaycaster ray =  v.getRayCaster();
//                Vector3f start = new Vector3f(0,0,5);
//                Vector3f end = new Vector3f(0,0,-1);
//                VehicleRaycasterResult res = new VehicleRaycasterResult();
//                ray.castRay(start, end, res);
//                end.set(res.hitPointInWorld);
//                end.scale(DefaultPhysics.INV_SCALE_FACTOR);
//                start.scale(DefaultPhysics.INV_SCALE_FACTOR);
//                
//                RenderEntity rent = Ref.render.createEntity(REType.BBOX);
//                rent.flags = RenderEntity.FLAG_NOLIGHT;
//                rent.origin.set(end);
//                
//                if(res.distFraction != 1.0f) rent.outcolor.set(0, 255, 0, 255);
//                else rent.outcolor.set(255, 0, 0, 255);
//
//                Ref.render.addRefEntity(rent);
//
//                rent = Ref.render.createEntity(REType.BBOX);
//                rent.flags = RenderEntity.FLAG_NOLIGHT;
//                rent.origin.set(start);
//                Ref.render.addRefEntity(rent);
                
                int lowStart = 1000;
                int lowEnd = 3000;
                int highStart = 2500;
                int highEnd = 7000;
                float rpm = v.getState().rpm;
                float rpmLow = ((rpm-lowStart) / (lowEnd-lowStart)) + 0.8f;
                
                float volumeLow = 1f;
                volumeLow = 1f - (0.9f * (Helper.Clamp(rpm - lowEnd, 0, 3000) / 3000f));
                
                float rpmHigh = ((rpm-highStart) / (highEnd-highStart)) + 0.8f;
                float volumeHigh = 0.1f;
                if(rpm < highStart) volumeHigh = 0.1f;
                volumeHigh = 0.1f + (0.9f * (Helper.Clamp(rpm - highStart, 0, 2000) / 2000f));
                
//                SoundHandle low = Ref.soundMan.AddWavSound("data/sounds/idle_engine.wav");
//                SoundHandle high = Ref.soundMan.AddWavSound("data/sounds/highrev_engine.wav");
//
//                Ref.soundMan.addLoopingSound(ent.s.number+2, ent.r.currentOrigin, ent.s.pos.delta, low, volumeLow, rpmLow);
//                Ref.soundMan.addLoopingSound(ent.s.number+1, ent.r.currentOrigin, ent.s.pos.delta, high, volumeHigh, rpmHigh);
            }
        }
    }

    private void DrawScoreboard() {
        // Request update if scoreboard info is too old
        if(game.cg.scoresRequestTime + 2000 < game.cg.time) {
            game.cg.scoresRequestTime =game.cg.time;
            Ref.client.clc.AddReliableCommand("score", false);
        }

        Vector2f res = Ref.glRef.GetResolution();

        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(0.1f*res.x, 0.1f*res.y), new Vector2f(0.8f*res.x, 0.8f*res.y), null, null, null);
        spr.SetColor(0, 0, 0, 127);
        float yOffset = 0.1f*res.y + 8;
        float lineHeight = Ref.textMan.GetCharHeight();

        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        float hearderSize = lineHeight;
        spr.Set(new Vector2f(0.1f*res.x, res.y - yOffset - hearderSize-1), new Vector2f(0.8f*res.x, hearderSize+8+1), null, null, null);
        spr.SetColor(0, 0, 0, 127);
        
        Ref.textMan.AddText(new Vector2f(0.1f * res.x, yOffset), "Players:", Align.LEFT, Type.HUD);
        Ref.textMan.AddText(new Vector2f(0.8f * res.x, yOffset), "Score:", Align.CENTER, Type.HUD);
        Ref.textMan.AddText(new Vector2f(0.9f * res.x, yOffset), "Ping:", Align.RIGHT, Type.HUD);
        yOffset += lineHeight+5;
        Score[] scores = game.cg.scores.getValidScores(true);
        for (int i= 0; i < scores.length; i++) {
            Score score = scores[i];
            DrawClientScore(yOffset + lineHeight * i, score);
        }
        
    }
    
    private int drawProfilerRecursive(Profiler.StackEntry entry, int indent, int startline, int maxlevels) {
        int line = startline;
        // Draw this
        Vector2f position = new Vector2f(indent * 20f,  (line+1) * Ref.textMan.GetCharHeight() * 0.75f);
        Ref.textMan.AddText(position, entry.toString(), Align.LEFT, Type.HUD, 0.75f);
        line++;
        
        // Draw children
        if(entry.subTags != null && indent < maxlevels) {
            for (Profiler.StackEntry e : entry.subTags) {
                line = drawProfilerRecursive(e, indent+1, line, maxlevels);
            }
        }
        return line;
    }
    
    private void drawStackProfiler() {
        ArrayList<Profiler.StackEntry> entries = Profiler.getStackFrames();
        int line = 8;
        int maxlevels = 10;
        Vector2f position = new Vector2f(3 * 20f,  (line) * Ref.textMan.GetCharHeight() * 0.75f);
        Ref.textMan.AddText(position, "Stack Profiler:", Align.LEFT, Type.HUD, 1.0f);
        for (Profiler.StackEntry entry : entries) {
            line = drawProfilerRecursive(entry, 3, line, maxlevels);
        }
    }

    private void drawprofiler() {
        Vector2f res = Ref.glRef.GetResolution();
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(10, 10), new Vector2f(res.x-20, res.y-20), null, null, null);
        spr.SetColor(0, 0, 0, 240);
        float[] times = Profiler.getTimes();
        float[] percentage = new float[times.length];

        float totalTime = Ref.common.framemsec; // this will be 100% usage
        for (int i= 0; i < times.length; i++) {
            percentage[i] = Helper.Clamp(times[i] / totalTime, 0, 1);
        }
        
        float spacing = 220;
        
        float x = 100;
        float y = 20;

        float[] frametimes = Profiler.getFrameTimes();
        int frametimesOffset  =Profiler.getFrametimeOffset();
        float width = (res.x-150) / frametimes.length;
        float height = 90;
        for (int i= 0; i < frametimes.length; i++) {
            float time = frametimes[(i+frametimesOffset)%frametimes.length];
            time /= 1000/60f;
            spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(x - 25 + (i)*width, 15), new Vector2f(width-2, height*time), null, null, null);
            spr.SetColor(255, 0, 0, 150);
        }

        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.Set(new Vector2f(x-25, 10), new Vector2f(res.x-150, 100), null, null, null);
        spr.SetColor(0, 0, 0, 200);

        

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

            spr = Ref.SpriteMan.GetSprite(Type.HUD);
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
        
        drawStackProfiler();
        
//        if(Ref.game != null && Ref.game.level.physics != null && Ref.game.level.physics.world != null) {
//            Ref.textMan.AddText(new Vector2f(900, 620), "gNumDeepPen: " + BulletStats.gNumDeepPenetrationChecks, Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(900, 620-Ref.textMan.GetCharHeight()), "gNumGjkChecks: " + BulletStats.gNumGjkChecks, Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(900, 620-Ref.textMan.GetCharHeight()*2f), "gNumSplitImpulseRec: " + BulletStats.gNumSplitImpulseRecoveries, Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(900, 620-Ref.textMan.GetCharHeight()*3f), "objects: " + Ref.game.level.physics.world.getNumCollisionObjects(), Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(900, 620-Ref.textMan.GetCharHeight()*4f), "pairs: " + Ref.game.level.physics.world.getBroadphase().getOverlappingPairCache().getNumOverlappingPairs(), Align.LEFT, Type.HUD);
//        }
        
        int offset = 0;
        for (String s : Ref.cgame.map.builder.getInfo()) {
            Ref.textMan.AddText(new Vector2f(900, 180+Ref.textMan.GetCharHeight()*(offset++)), s, Align.LEFT, Type.HUD);
        }
    }

    private void DrawClientScore(float yOffset, Score score) {
        if(score.client < 0 || score.client >= game.cgs.clientinfo.length) {
            Ref.cgame.Print("DrawClientScore: Invalid ci index: " + score.client);
            return;
        }
        Vector2f res = Ref.glRef.GetResolution();
        ClientInfo ci = game.cgs.clientinfo[score.client];
        if(score.client == game.cg.cur_ps.clientNum) {
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(0.1f* res.x, res.y-(yOffset-0.01f)-Ref.textMan.GetCharHeight()), new Vector2f(0.8f* res.x, Ref.textMan.GetCharHeight()), null, null, null);
            spr.SetColor(255,255,255,80);
        }

        String extra = "";
        if(score.ping == -2) // disable for now
            extra = " ^0(Connecting...)";

        String ping;
        if(score.ping == -1) ping = "Bot";
        else ping = String.format("%d", score.ping);

        if(score.isDead) {
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(0.1f* res.x+2, res.y-(yOffset-0.01f)-Ref.textMan.GetCharHeight()+2),
                    new Vector2f(28,28),
                    Ref.ResMan.LoadTexture("data/textures/dead.png"), null, null);
            spr.SetColor(255, 255,255, 210);
        }

        Ref.textMan.AddText(new Vector2f(0.1f * res.x + 32, yOffset), String.format("%s%s", ci.name, extra), Align.LEFT, Type.HUD);
        Ref.textMan.AddText(new Vector2f(0.8f * res.x, yOffset), String.format("%d", score.score), Align.LEFT, Type.HUD);
        Ref.textMan.AddText(new Vector2f(0.9f * res.x, yOffset), ping, Align.RIGHT, Type.HUD);
    }

    private void DrawChat() {
        int time = Ref.client.realtime;
        int lineIndex = 1;
        float lineHeight = Ref.textMan.GetCharHeight();
        CGame.ChatLog log = game.chatLogs[game.cg.cur_localClientNum];
        int start = log.log.size() - 8;
        start = Helper.Clamp(start, 0, log.log.size());
        for (int i= start; i < log.log.size(); i++) {
            CGame.ChatLine line = log.log.get(i);
            if(time - line.time > game.cg_chattime.iValue + game.cg_chatfadetime.iValue)
                continue;

            Color color = new Color(255,255,255,255);

            if(time - line.time >= game.cg_chattime.iValue) {
                color.setAlpha((int)(1-(time - line.time - game.cg_chattime.iValue)/game.cg_chatfadetime.fValue)*255);
            }

            Ref.textMan.AddText(new Vector2f(10, Ref.glRef.GetResolution().y - (200 + lineHeight * lineIndex)),
                    line.str, Align.LEFT, color, null, Type.HUD,1);

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

        int ping = Ref.cgame.cg.cur_ps.ping;
        int fps = Ref.client.clRender.currentFPS;

        int in = Ref.net.stats.clLastBytesIn;
        int out = Ref.net.stats.clLastBytesOut;
        int inAvgBytes = Ref.net.stats.clAvgBytesIn;
        int outAvgBytes = Ref.net.stats.clAvgBytesOut;
        int inRate = Ref.net.stats.clAvgPacketsIn;
        int outRate = Ref.net.stats.clAvgPacketsOut;

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
    


    



}
