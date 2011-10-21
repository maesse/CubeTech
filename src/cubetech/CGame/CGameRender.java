package cubetech.CGame;

import cubetech.common.Score;
import cubetech.Game.Game;
import cubetech.Game.Gentity;
import cubetech.Game.GentityFilter;
import cubetech.collision.CubeChunk;
import cubetech.collision.SingleCube;
import cubetech.common.*;
import cubetech.common.items.*;
import cubetech.entities.EntityFlags;
import cubetech.entities.EntityState;
import cubetech.entities.EntityType;
import cubetech.entities.Vehicle;
import cubetech.entities.Vehicle.VehicleControls;
import cubetech.entities.Vehicle.VehicleState;
import cubetech.gfx.CubeMaterial;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager.Align;
import cubetech.gfx.VBO;
import cubetech.gfx.VBOPool;
import cubetech.iqm.BoneAttachment;
import cubetech.iqm.BoneController;
import cubetech.iqm.IQMAnim;
import cubetech.iqm.IQMModel;
import cubetech.misc.Profiler;
import cubetech.misc.Ref;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Locale;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Matrix4f;
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
        if(es.modelindex == 0) return;

        RenderEntity ref = Ref.render.createEntity(REType.MODEL);
        String modelname = Ref.client.cl.GameState.get(CS.CS_MODELS+es.modelindex-1);
        IQMModel model = Ref.ResMan.loadModel(modelname);
        ref.model = model;
        es.pos.Evaluate(game.cg.time, ref.origin);
        ref.axis = Helper.AnglesToAxis(es.Angles);
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
            Helper.rotateAroundDirection(ent.axis, game.cg.time/5f);

            ent.model = wi.missileModel;
            ent.origin.set(cent.lerpOrigin);
            Ref.render.addRefEntity(ent);

            if(wi.missileSound != null && !wi.missileSound.isEmpty()) {
                Vector3f vel = cent.currentState.pos.delta;
                
                Ref.soundMan.addLoopingSound(cent.currentState.ClientNum, cent.lerpOrigin, vel, Ref.soundMan.AddWavSound(wi.missileSound));
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
                ent.model = wModel;
                ent.origin.set(cent.lerpOrigin);
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
        if(item.getType() == ItemType.HEALTH) spr.SetAngle(game.cg.autoAngle);
    }


    public void renderViewModel(PlayerState ps) {
        // no gun if in third person view or a camera is active
        if (Ref.cgame.cg_tps.isTrue() || (Ref.cgame.cg_freecam.isTrue() && Ref.cgame.cg.playingdemo)) return;

        // don't draw if testing a gun model
        if(Ref.cgame.cg.testGun) return;

        RenderEntity ent = Ref.render.createEntity(REType.MODEL);
        ent.flags |= RenderEntity.FLAG_NOSHADOW;
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
        if(ps.weaponState == WeaponState.DROPPING) {
            float dropFrac = 1f - ((float)ps.weaponTime / wi.getDropTime());
            if(dropFrac < 0) dropFrac = 0;
            if(dropFrac > 1) dropFrac = 1f;
            angles.x += dropFrac * 90;
        } else if(ps.weaponState == WeaponState.RAISING) {
            float dropFrac = ((float)ps.weaponTime / wi.getRaiseTime());
            if(dropFrac < 0) dropFrac = 0;
            if(dropFrac > 1) dropFrac = 1f;
            angles.x += dropFrac * 90;
        }
        ent.axis = Helper.AnglesToAxis(angles, ent.axis);

        Helper.VectorMA(ent.origin, game.cg.cg_gun_x.fValue, ent.axis[0], ent.origin);
        Helper.VectorMA(ent.origin, game.cg.cg_gun_y.fValue, ent.axis[1], ent.origin);
        Helper.VectorMA(ent.origin, game.cg.cg_gun_z.fValue, ent.axis[2], ent.origin);
        boolean isFiring = ps.weaponState == WeaponState.FIRING &&
                    ps.stats.getAmmo(ps.weapon)>0;
        if(ent.model.anims != null && ent.model.anims.length > 0) {
            IQMAnim anim = ent.model.anims[0];

            if(ps.weaponState == WeaponState.READY && ent.model.getAnimation(Animations.READY) != null) {
                anim = ent.model.getAnimation(Animations.READY);
            } else if(isFiring && ent.model.getAnimation(Animations.FIRING) != null ) {
                anim = ent.model.getAnimation(Animations.FIRING);
            }
            int num = anim.num_frames;
            int first = anim.first_frame;
            if(ps.weaponTime > 0 && isFiring) {
                float frac = 1f-(ps.weaponTime / (float)wi.getFireTime());
                ent.frame = (int) Math.ceil(frac * num);
                
                float lerp = frac * num;
                ent.backlerp = -(lerp-ent.frame);
//                ent.backlerp = 0f;
                ent.oldframe = ent.frame - 1;
                if(ent.oldframe < 0) ent.oldframe = num-1;
                
                ent.frame += first;
                ent.oldframe += first;
            } else {
                float left = (Ref.cgame.cg.time/50f) - (Ref.cgame.cg.time/50);
                ent.backlerp = 1f-left;
                ent.frame = (Ref.cgame.cg.time/50) % num;
                ent.oldframe = ent.frame - 1;
                if(ent.oldframe < 0) ent.oldframe = num-1;
                ent.frame += first;
                ent.oldframe += first;
            }
        }

        Ref.render.addRefEntity(ent);
        ent.model.animate(ent.frame, ent.oldframe, ent.backlerp);
        BoneAttachment muzzleBone = ent.model.getAttachment("muzzle");
        if(muzzleBone == null || winfo.flashTexture == null) return;
//
//        // Add muzzle flash
        CEntity cent = Ref.cgame.cg.predictedPlayerEntity;
        if(Ref.cgame.cg.time - cent.muzzleFlashTime > 20) return;
//
        RenderEntity flash = Ref.render.createEntity(REType.SPRITE);
        flash.flags |= RenderEntity.FLAG_SPRITE_AXIS;
        flash.mat = Ref.ResMan.LoadTexture(winfo.flashTexture).asMaterial();
        flash.mat.blendmode = CubeMaterial.BlendMode.ONE;
        flash.outcolor.set(255,255,255,255);
        flash.radius = 5f;
        Matrix4f modelMatrix = createModelMatrix(ent.axis, ent.origin, null);
        Vector4f vec = new Vector4f(muzzleBone.lastposition.x, muzzleBone.lastposition.y, muzzleBone.lastposition.z, 1);
        Matrix4f.transform(modelMatrix, vec, vec);
        flash.origin.set(vec.x, vec.y, vec.z);
        // Get attachment point
        Vector3f[] rotatedMuzzleAxis = new Vector3f[3];
        rotatedMuzzleAxis[0] = muzzleBone.axis[1];
        rotatedMuzzleAxis[1] = muzzleBone.axis[2];
        rotatedMuzzleAxis[2] = muzzleBone.axis[0];
        Helper.mul(rotatedMuzzleAxis, ent.axis, flash.axis);
        Ref.render.addRefEntity(flash);
    }

    
    private Matrix4f createModelMatrix(Vector3f[] axis, Vector3f position, Matrix4f dest) {
        if(dest == null) dest = new Matrix4f();
        // Set rotation matrix
        FloatBuffer viewbuffer = Ref.glRef.matrixBuffer;
        viewbuffer.position(0);
        viewbuffer.put(axis[0].x);viewbuffer.put(axis[1].x);viewbuffer.put(axis[2].x);viewbuffer.put(0);
        viewbuffer.put(axis[0].y);viewbuffer.put(axis[1].y);viewbuffer.put(axis[2].y); viewbuffer.put(0);
        viewbuffer.put(axis[0].z); viewbuffer.put(axis[1].z); viewbuffer.put(axis[2].z);viewbuffer.put(0);
        viewbuffer.put(0); viewbuffer.put(0); viewbuffer.put(0); viewbuffer.put(1);
        viewbuffer.flip();
        Matrix4f mMatrix = (Matrix4f) dest.load(viewbuffer);
        mMatrix.invert();
        mMatrix.m30 = position.x;
        mMatrix.m31 = position.y;
        mMatrix.m32 = position.z;

        viewbuffer.clear();
        return mMatrix;
    }

    private void CalculateWeaponPosition(Vector3f position, Vector3f angles) {
        position.set(game.cg.refdef.Origin);
        angles.set(game.cg.refdef.Angles);

        // on odd legs, invert some angles
        float scale;
        if((game.cg.bobcycle & 1) != 0) {
            scale = -game.cg.xyspeed;
        } else {
            scale = game.cg.xyspeed;
        }

        // gun angles from bobbing
        angles.z += scale * game.cg.bobfracsin * 0.005;
        angles.y += scale * game.cg.bobfracsin * 0.01;
        angles.x += game.cg.xyspeed * game.cg.bobfracsin * 0.005;

        CEntity cent = game.cg.predictedPlayerEntity;
        swingAngles(angles.x, 0, 20, 0.2f, cent.pe.torso, true);
        angles.x = cent.pe.torso.yawAngle;
        
        swingAngles(angles.y, 0, 20, 0.3f, cent.pe.torso, false);
        angles.y = cent.pe.torso.pitchAngle;
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

        Vector3f angles = new Vector3f();

        angles.set(cent.lerpAngles);
        if(angles.x > 1) angles.x = 1;
        if(angles.x < -25) angles.x = -25;
        ent.axis = Helper.AnglesToAxis(angles, ent.axis);

        playerAngles(cent, ent);

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

        ent.color = new Vector4f(255, 255, 255, 255f);

        Ref.render.addRefEntity(ent);
        addWeaponToPlayer(cent, ent);
        
    }

    private void addWeaponToPlayer(CEntity cent, RenderEntity ent) {
        // Add player weapon
        if(cent.currentState.weapon == Weapon.NONE) return;

        WeaponItem w = Ref.common.items.getWeapon(cent.currentState.weapon);
        IQMModel weaponModel = w.getWeaponInfo().worldModel;
        if(weaponModel == null) return;

        ent.model.animate(ent.frame, ent.oldframe, ent.backlerp);
        BoneAttachment bone = ent.model.getAttachment("weapon");
        if(bone == null) return;

        Vector3f boneOrigin = new Vector3f(bone.lastposition.x
                    , bone.lastposition.y
                    , bone.lastposition.z);



        RenderEntity went = Ref.render.createEntity(REType.MODEL);
        went.color.set(255,255,255,255);
        went.model = weaponModel;
        weaponModel.animate(0, 0, 0);

        Helper.transform(ent.axis, ent.origin, boneOrigin);
        went.origin.set(boneOrigin);

        Vector3f[] test = new Vector3f[3];
        test[1] = new Vector3f(0,3,0);
        test[2] = new Vector3f(0,0,3);
        test[0] = new Vector3f(3,0,0);

        Helper.mul(test, bone.axis, test);
        Helper.mul(test, ent.axis, test);

        went.axis = test;

        Ref.render.addRefEntity(went);
    }

    private void playerAnimation(CEntity cent, RenderEntity ent) {
        float speedScale = 1.0f;
        
        ClientInfo ci = Ref.cgame.cgs.clientinfo[cent.currentState.ClientNum];
        // Change animation based on what weapon the client has
        // Figure out what animation was requested
        Animations anim = cent.currentState.frameAsAnimation();
        if(anim == null) {
            Common.LogDebug("Invalid animation %d", cent.currentState.frame);
            return;
        }
        if(cent.currentState.weapon == Weapon.AK47) {
            if(anim == Animations.IDLE) anim = Animations.IDLE_GUN1;
            if(anim == Animations.WALK) anim = Animations.WALK_GUN1;
        }
        runLerpFrame(ci, cent.pe.torso, anim.ordinal(), speedScale);

        ent.oldframe = cent.pe.torso.oldFrame;
        ent.frame = cent.pe.torso.frame;
        ent.backlerp = cent.pe.torso.backlerp;
    }

    //
    // UI
    //
    void Draw2D() {
        DrawChat();
        game.lag.Draw();

        if(Ref.common.isDeveloper()) {
            Ref.textMan.AddText(new Vector2f(0, 0),
                    "Position: " + game.cg.refdef.Origin, Align.LEFT, Type.HUD);
            Ref.textMan.AddText(new Vector2f(0, Ref.textMan.GetCharHeight()),
                    "Velocity: " + game.cg.predictedPlayerState.velocity, Align.LEFT, Type.HUD);
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
                    "Chunks: " + game.map.nChunks + " visible. " + game.map.nSides + " quads. " + game.map.nVBOthisFrame + " vbo's filled this frame", Align.LEFT, Type.HUD);
        }

        // Draw crosshair
        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
        Vector2f res = Ref.glRef.GetResolution();
        float width = 14;
        spr.setLine(new Vector2f(res.x/2f - width / 2f, res.y/2f), new Vector2f(res.x/2f + width / 2f, res.y/2f), 2f);
        spr.SetColor(255, 0, 0, 255);
        spr = Ref.SpriteMan.GetSprite(Type.HUD);
        spr.setLine(new Vector2f(res.x/2f , res.y/2f - width / 2f), new Vector2f(res.x/2f , res.y/2f + width / 2f), 2f);
        spr.SetColor(255, 0, 0, 255);

        if(Ref.net.net_graph.iValue > 0) {
            DrawNetGraph();
        }

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
        
        // Handle changes from server
        if(game.cg.showScores)
            DrawScoreboard();

        if(Ref.common.cl_paused.iValue == 1) {
            Vector2f pos = new Vector2f(Ref.glRef.GetResolution());
            pos.scale(0.5f);
            Ref.textMan.AddText(pos, "^3PAUSED", Align.CENTER, Type.HUD, 2,0);
        }

        if(Ref.client.servername.equals("localhost")) {
            Gentity ent = Ref.game.Find(null, GentityFilter.CLASSNAME, "vehicle");
            if(ent != null) {
                Vehicle v = (Vehicle)ent;
                VehicleState state = v.getState();
                float x = Ref.glRef.GetResolution().x * 0.8f;
                Ref.textMan.AddText(new Vector2f(x, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()*1),
                        "RPM: " + state.rpm, Align.LEFT, Type.HUD);
                Ref.textMan.AddText(new Vector2f(x, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()*2),
                        "Torque: " + state.torque, Align.LEFT, Type.HUD);
                VehicleControls ctrl = v.getControls();
                Ref.textMan.AddText(new Vector2f(x, Ref.glRef.GetResolution().y - Ref.textMan.GetCharHeight()*3),
                        (ctrl.brake>0?" ^2BRK":"") + (ctrl.throttle>0?" ^3THR":""), Align.LEFT, Type.HUD);
            }
        }
    }

    private void DrawScoreboard() {
        // Request update if scoreboard info is too old
        if(game.cg.scoresRequestTime + 2000 < game.cg.time) {
            game.cg.scoresRequestTime =game.cg.time;
            Ref.client.AddReliableCommand("score", false);
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
        if(Ref.common.developer.isTrue()) drawprofiler();
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

        float[] frametimes = Profiler.getFrameTimes();
        int frametimesOffset  =Profiler.getFrametimeOffset();
        float width = (res.x-150) / frametimes.length;
        float height = 90;
        for (int i= 0; i < frametimes.length; i++) {
            float time = frametimes[(i+frametimesOffset)%frametimes.length];
            time /= 1000/60f;
            Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
            spr.Set(new Vector2f(x - 25 + (i)*width, 15), new Vector2f(width-2, height*time), null, null, null);
            spr.SetColor(255, 0, 0, 150);
        }

        Sprite spr = Ref.SpriteMan.GetSprite(Type.HUD);
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
    }

    private void DrawClientScore(float yOffset, Score score) {
        if(score.client < 0 || score.client >= game.cgs.clientinfo.length) {
            Ref.cgame.Print("DrawClientScore: Invalid ci index: " + score.client);
            return;
        }
        Vector2f res = Ref.glRef.GetResolution();
        ClientInfo ci = game.cgs.clientinfo[score.client];
        if(score.client == game.cg.snap.ps.clientNum) {
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
        for (int i= 0; i < game.chatLines.length; i++) {
            CGame.ChatLine line = game.chatLines[(game.chatIndex+7-i) % 8];
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
        Vector3f legsAngle = new Vector3f();
//        cent.pe.legs.yawing = true;
        cent.pe.legs.pitching = false;

        Vector3f torsoAngle = new Vector3f();
//        cent.pe.torso.yawing = true;
        cent.pe.torso.pitching = false;
        // yaw
        torsoAngle.y = headAngle.y + 0.35f * moveOffsets[dir];
        legsAngle.y = headAngle.y + moveOffsets[dir];
        swingAngles(torsoAngle.y, 25, 90, game.cg_swingspeed.fValue, cent.pe.torso, true); // yaw
        Animations anim = cent.currentState.frameAsAnimation();
        if(anim != null && anim == Animations.IDLE) {
            cent.pe.legs.yawing = false;
            swingAngles(legsAngle.y, 20, 110, game.cg_swingspeed.fValue*0.5f, cent.pe.legs, true); // yaw
        } else if(cent.pe.legs.yawing) {
            swingAngles(legsAngle.y, 0, 110, game.cg_swingspeed.fValue, cent.pe.legs, true); // yaw
        } else {
            swingAngles(legsAngle.y, 40, 110, game.cg_swingspeed.fValue, cent.pe.legs, true); // yaw
        }
        torsoAngle.y = cent.pe.torso.yawAngle;
        legsAngle.y = cent.pe.legs.yawAngle;

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
//        torsoAngle.x = cent.pe.torso.pitchAngle;
        BoneController legController1 = new BoneController("Bone.007", new Vector3f(-torsoAngle.x, legsAngle.y-torsoAngle.y, 0));
        BoneController legController2 = new BoneController("Hips", new Vector3f(-torsoAngle.x, legsAngle.y-torsoAngle.y, 0));
        BoneController legController3 = new BoneController("Head", new Vector3f(-torsoAngle.x, headAngle.y-torsoAngle.y, 0));
        rent.controllers = new BoneController[] {legController1,legController2,legController3};
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
        scale /= clampTolerance;
//        scale /= swingTolerance;
        if(scale < 0.0) scale = 0.0f;
        if(scale > 1.0) scale = 1.0f;
        float minscale = 0.1f;
        scale *= 1f/(1f+minscale);
        scale += minscale;
        scale = Helper.SimpleSpline(scale);
        

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
