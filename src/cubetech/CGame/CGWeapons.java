/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.CGame;

import cubetech.common.items.Weapon;
import cubetech.common.items.WeaponInfo;
import cubetech.common.items.WeaponItem;
import cubetech.entities.EntityState;
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
            Ref.soundMan.startSound(null, ent.ClientNum, buffer, SoundChannel.WEAPON, 1.0f);
        }

        

    }

    void missileHitWall(Weapon weapon, int clientNum, Vector3f origin, Vector3f dir) {
        String sound = Ref.common.items.getWeapon(weapon).getWeaponInfo().explodeSound;

        switch(weapon) {
            case ROCKETLAUNCHER:
                // explosion yay!
                float radius = 130;
                RingEmitter.spawn(radius, origin);
                FireEmitter.create(20, radius*0.5f, origin);
                SparkEmitter.spawn(radius, origin);
                TrailEmitter.spawn(origin, radius);

                if(dir == null) {
                    dir = new Vector3f(0,0.0f,1);
                }
                dir.normalise();

                Ref.cgame.marks.impactMark(origin, dir, radius*0.5f, false, Ref.ResMan.LoadTexture("data/explosionmark.png"));
                
                Ref.cgame.physics.explosionImpulse(origin, radius, radius, true);
                break;
        }
    }
}
