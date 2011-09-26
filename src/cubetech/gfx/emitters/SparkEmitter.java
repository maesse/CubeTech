/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx.emitters;

import cubetech.CGame.LocalEntity;
import cubetech.common.Helper;
import cubetech.common.Trajectory;
import cubetech.gfx.CubeMaterial;
import cubetech.gfx.IEmitter;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class SparkEmitter implements IEmitter {
    private int lifeTime = 800;
    private int localMsec = 0;

    private Vector3f origin = new Vector3f();
    private float radius;
    private int count;

    private CubeMaterial mat;

    public SparkEmitter(Vector3f origin, float radius, int count) {
        this.origin.set(origin);
        this.radius = radius;
        this.count = count;

        mat = Ref.ResMan.LoadTexture("data/particles/spark2.png").asMaterial();
    }

    public void update(int msec) {
        boolean spawn = localMsec == 0;
        localMsec += msec;

        if(localMsec > lifeTime) {
            localMsec = msec;
            spawn = true;
        }

        if(spawn) {
            create(count, radius, lifeTime, origin, mat);
        }

    }
    
    public static void spawn(float radius, Vector3f origin) {
        create(20, radius, 800, origin, Ref.ResMan.LoadTexture("data/particles/spark2.png").asMaterial());
    }

    private static void create(int count, float radius, int lifeTime, Vector3f origin, CubeMaterial mat) {
        int time = Ref.cgame.cg.time;
        for (int i= 0; i < count; i++) {
            Vector3f dir = getRandomVector(false);

            float rndVelocity =  radius;
            dir.scale(rndVelocity);
            Vector3f baseDir = new Vector3f(dir);
            baseDir.scale(0.5f);


            int lifetimeRnd = (int)((Ref.rnd.nextFloat()) * -300f) + lifeTime;

            LocalEntity ent = LocalEntity.sparkTrail(origin, dir, baseDir, 10f, lifetimeRnd, time, mat,1,0.55f,0,1);
            ent.Flags = LocalEntity.FLAG_DONT_SCALE;
            ent.pos.type = Trajectory.GRAVITY;
            ent.pos.duration = 200;
            ent.pos.time -= 200;
            ent.angles.delta.set(ent.pos.delta);

            ent.angles.type = Trajectory.GRAVITY;
            ent.angles.duration = 200;
        }
    }

    private static Vector3f getRandomVector(boolean normalize) {
        Vector3f vec = new Vector3f((float)Ref.rnd.nextGaussian(),(float)Ref.rnd.nextGaussian(),(float)Ref.rnd.nextGaussian());
        if(normalize) Helper.Normalize(vec);
        return vec;
    }

}
