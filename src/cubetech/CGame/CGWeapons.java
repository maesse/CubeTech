/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.common.Helper;
import cubetech.common.PlayerState;
import cubetech.common.items.Weapon;
import cubetech.common.items.WeaponInfo;
import cubetech.common.items.WeaponItem;
import cubetech.entities.EntityState;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.PolyVert;
import cubetech.gfx.emitters.FireEmitter;
import cubetech.gfx.emitters.RingEmitter;
import cubetech.gfx.emitters.SparkEmitter;
import cubetech.gfx.emitters.TrailEmitter;
import cubetech.misc.Ref;
import cubetech.snd.SoundChannel;
import cubetech.snd.SoundHandle;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CGWeapons {
    public void fireWeaponEvent(CEntity cent) {
        EntityState ent =  cent.currentState;
        if(ent.weapon == Weapon.NONE) {
            return;
        }

        WeaponItem w = Ref.common.items.getWeapon(ent.weapon);
        WeaponInfo wi = w.getWeaponInfo();
        // play a sound
        if(wi.fireSound != null && !wi.fireSound.isEmpty()) {
            SoundHandle buffer = Ref.soundMan.AddWavSound(wi.fireSound);
            Ref.soundMan.startSound(null, ent.number, buffer, SoundChannel.WEAPON, 0.35f);
        }

        cent.muzzleFlashTime = Ref.cgame.cg.time;
        
        


    }

    void missileHitWall(Weapon weapon, int clientNum, Vector3f origin, Vector3f dir) {
        String sound = Ref.common.items.getWeapon(weapon).getWeaponInfo().explodeSound;
        if(sound != null) {
            Ref.soundMan.startSound(origin, clientNum, Ref.soundMan.AddWavSound(sound), SoundChannel.AUTO, 1f);
        }
        
        CubeTexture mark = null;
        float markRadius = 10;
        switch(weapon) {
            case ROCKETLAUNCHER:
                // explosion yay!
                float radius = 130;
                RingEmitter.spawn(radius, origin);
                SparkEmitter.spawn(radius, origin);
                TrailEmitter.spawn(origin, radius);
                FireEmitter.create(20, radius*0.5f, origin);

                if(dir == null) {
                    dir = new Vector3f(0,0.0f,1);
                }
                dir.normalise();

                mark = Ref.ResMan.LoadTexture("data/textures/explosionmark.png");
                markRadius = radius*0.5f;
                Ref.cgame.physics.explosionImpulse(origin, radius, radius, true);
                break;
            case AK47:
                mark = Ref.ResMan.LoadTexture("data/textures/explosionmark.png");
                markRadius = 5f;
                break;
        }

        if(mark != null) {
            Ref.cgame.marks.impactMark(origin, dir, markRadius, false, mark);
        }
    }

    void bullet(Vector3f end, int sourceEntityNum, Vector3f normal, boolean flesh, int fleshEntityNum) {
        if(sourceEntityNum >= 0) {
            Vector3f start = calcMuzzlePoint(sourceEntityNum, null);
            if(start != null) tracer(start, end);
            
        }
        if(flesh) {
            bleed(end, fleshEntityNum);
            String sound = "data/sounds/bloodsplat" + (Ref.rnd.nextInt(3)+1) +  ".wav";
            if(sound != null) {
                Ref.soundMan.startSound(end, fleshEntityNum, Ref.soundMan.AddWavSound(sound), SoundChannel.AUTO, 1f);
            }
        } else {
            missileHitWall(Weapon.AK47, 0, end, normal);
        }
    }

    private void bleed(Vector3f origin, int entitynum) {
        // don't show player's own blood in view
        if(entitynum == Ref.cgame.cg.cur_ps.clientNum) return;
        Vector3f dir = Ref.cgame.cg_entities[entitynum].lerpOrigin;
        Vector3f.sub(origin, dir, dir);
        dir.normalise();
        dir.z += 0.5f;
        dir.scale(100f);
        LocalEntity.bloodExplosion(Ref.cgame.cg.time, origin, dir, Ref.cgame.cgs.media.t_blood);
    }

    private void tracer(Vector3f start, Vector3f end) {
        // todo/fix: this method uses the refdef from the last frame
        Vector3f.sub(end, start, forward);
        float len = Helper.Normalize(forward);

        // start at least a little ways from the muzzle
        if(len < 100) return;
        float beginOffset = 50 + Ref.rnd.nextFloat() * (len - 60);
        float endOffset = beginOffset + 200;
        if(endOffset > len) endOffset = len;

        Vector3f p1 = Helper.VectorMA(start, beginOffset, forward, null);
        Vector3f p2 = Helper.VectorMA(start, endOffset, forward, null);
        

        Vector3f line = new Vector3f();
        line.x = Vector3f.dot(forward, Ref.cgame.cg.refdef.ViewAxis[1]);
        line.y = Vector3f.dot(forward, Ref.cgame.cg.refdef.ViewAxis[2]);

        Vector3f right = new Vector3f(Ref.cgame.cg.refdef.ViewAxis[1]);
        right.scale(line.y);
        Helper.VectorMA(right, -line.x, Ref.cgame.cg.refdef.ViewAxis[2], right);
        right.normalise();

        float tracerWidth = 5;

        PolyVert[] verts = new PolyVert[4];
        PolyVert vert = new PolyVert();
        vert.s = 0;
        vert.t = 1;
        vert.xyz = Helper.VectorMA(p2, tracerWidth, right, null);
        verts[0] = vert;

        vert = new PolyVert();
        vert.s = 0;
        vert.t = 0;
        vert.xyz = Helper.VectorMA(p2, -tracerWidth, right, null);
        verts[3] = vert;

        vert = new PolyVert();
        vert.s = 1;
        vert.t = 0;
        vert.xyz = Helper.VectorMA(p1, -tracerWidth, right, null);
        verts[2] = vert;

        vert = new PolyVert();
        vert.s = 1;
        vert.t = 1;
        vert.xyz = Helper.VectorMA(p1, tracerWidth, right, null);
        verts[1] = vert;

        RenderEntity ent = RenderEntity.addPolyToScene(verts, Ref.ResMan.LoadTexture("data/particles/spark2.png"));
        ent.color.set(255,65,0,255);
    }

    private static Vector3f forward = new Vector3f();
    static Vector3f calcMuzzlePoint(int entitynum, Vector3f dest) {
        if(dest == null) dest = new Vector3f();
        for (int i = 0; i < Ref.cgame.cg.localClients.length; i++) {
            PlayerState lcPs = Ref.cgame.cg.localClients[i].predictedPlayerState;
            if(lcPs != null && Ref.cgame.cg.localClients[i].validPPS &&
                    lcPs.clientNum == entitynum) {
                dest.set(Ref.cgame.cg.cur_ps.origin);
                dest.z += Ref.cgame.cg.cur_ps.viewheight - 8;
                Helper.AngleVectors(Ref.cgame.cg.cur_ps.viewangles, forward, null, null);
                Helper.VectorMA(dest, 16, forward, dest);
                return dest;
            }
        }

        CEntity cent = Ref.cgame.cg_entities[entitynum];
        if(!cent.currentValid) return null;

        dest.set(cent.currentState.pos.base);
        Helper.AngleVectors(cent.currentState.apos.base, forward, null, null);
        Helper.VectorMA(dest, 16, forward, dest);
        return dest;
    }
}
