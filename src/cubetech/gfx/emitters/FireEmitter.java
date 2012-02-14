/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx.emitters;

import cubetech.CGame.LocalEntity;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.common.Trajectory;
import cubetech.gfx.CubeMaterial;
import cubetech.gfx.IEmitter;
import cubetech.misc.Ref;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class FireEmitter implements IEmitter {
    private int lifeTime = 800;
    private int localMsec = 0;

    private Vector3f origin = new Vector3f();
    private float radius;
    private int count;

    private CubeMaterial mat;

    public FireEmitter(Vector3f origin, float radius, int count) {
        this.origin.set(origin);
        this.radius = radius;
        this.count = count;
        try {
            mat = CubeMaterial.Load("data/particles/fire.mat", true);
        } catch (Exception ex) {
            Common.Log(Common.getExceptionString(ex));
            mat = Ref.ResMan.LoadTexture("data/particles/fire.png").asMaterial();
        }
    }

    public void update(int msec) {
        boolean spawn = localMsec == 0;
        localMsec += msec;
        lifeTime = 2000;
        count = 20;
        if(localMsec > lifeTime) {
            localMsec = msec;
            spawn = true;
        }

        if(spawn) {
            create(count, radius, lifeTime, origin, mat);
        }

    }

    public static void create(int count, float radius, Vector3f origin) {
        try {
            create(count, radius, 800, origin, CubeMaterial.Load("data/particles/fire.mat", true));
        } catch (Exception ex) {
            create(count, radius, 800, origin, Ref.ResMan.LoadTexture("data/particles/fire.tga").asMaterial());
        }
    }

    private static void create(int count, float radius, int lifeTime, Vector3f origin, CubeMaterial mat) {
        int time = Ref.cgame.cg.time;
        for (int i= 0; i < count; i++) {
            Vector3f dir = getRandomVector(true);
            if(dir.z < 0) dir.z *= -1f;
            float rnd = Ref.rnd.nextFloat();
            float rndVelocity =  radius*(0.3f + 0.8f * rnd);



            
            int lifetimeRnd = (int)((rnd) * 600f) + lifeTime;
            int timeRnd = time - (int)(rnd * 100) - 100;

            
            Vector3f rndOrigin = Helper.VectorMA(origin, rnd * 10, dir, null);
            
            dir.scale(rndVelocity);
            float dirLen = dir.length();
            dir.x *= 0.4f;
            dir.y *= 0.4f;
            Helper.VectorMA(rndOrigin, 1f, dir, rndOrigin);
            
            
            
            
            float startRnd = rnd * 0.2f;
            float r = 0.7f - startRnd;
            float g = 0.1f + startRnd;
            float b = 0f + startRnd*0.1f;
            

            float endRnd = rnd * 0.15f;
            float r1 = endRnd;
            float g1 = endRnd;
            float b1 = endRnd;
            
            float lenFrac = Helper.Clamp(dirLen / 200f, 0, 1f);
            float invLenFrac = 1f - lenFrac;
            r = r * invLenFrac + r1 * lenFrac;
            g = g * invLenFrac + g1 * lenFrac;
            b = b * invLenFrac + b1 * lenFrac;
            dirLen /= rndVelocity;
            if(dirLen > 1) dirLen = 1;
            float fadeScale = (1-dirLen)*0.3f;
            LocalEntity ent = LocalEntity.colorFadedSmokePuff(rndOrigin, dir, 80f + 40 * rnd, r, g, b, 1, lifetimeRnd, timeRnd, 0, mat, r1, g1, b1,1,0.3f+fadeScale);
            ent.rEntity.frame = i % mat.getFrameCount();
            ent.pos.type = Trajectory.GRAVITY;
            ent.pos.duration = (int)( -100 * (0.5f + 0.5f * rnd));
        }
    }

    private static Vector3f getRandomVector(boolean normalize) {
        Vector3f vec = new Vector3f((float)Ref.rnd.nextGaussian(),(float)Ref.rnd.nextGaussian(),(float)Ref.rnd.nextGaussian());
        if(normalize) Helper.Normalize(vec);
        return vec;
    }
}
