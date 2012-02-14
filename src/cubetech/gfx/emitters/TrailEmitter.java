package cubetech.gfx.emitters;

import cubetech.CGame.LocalEntity;
import cubetech.common.Helper;
import cubetech.gfx.CubeMaterial;
import cubetech.gfx.IEmitter;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class TrailEmitter implements IEmitter {
    private int lifeTime = 800;
    private int localMsec = 0;

    private Vector3f origin = new Vector3f();
    private float radius;
    private int count;

    private CubeMaterial mat;

    public TrailEmitter(Vector3f origin, float radius, int count) {
        this.origin.set(origin);
        this.radius = radius;
        this.count = count;

        mat = Ref.ResMan.LoadTexture("data/particles/spark.tga").asMaterial();
    }

    public static void spawn(Vector3f origin, float radius) {
        create(10, radius, origin, 400, Ref.ResMan.LoadTexture("data/particles/spark.tga").asMaterial());
    }

    public void update(int msec) {
        boolean spawn = localMsec == 0;
        localMsec += msec;
        lifeTime = 700;
        if(localMsec > lifeTime) {
            localMsec = msec;
            spawn = true;
        }

        if(spawn) {
            create(count, radius, origin, lifeTime, mat);
        }

    }

    private static void create(int count, float radius, Vector3f origin, int lifeTime, CubeMaterial mat) {
        int time = Ref.cgame.cg.time;
        Vector3f hitNormal = new Vector3f(0, 0, 1);
            for (int i= 0; i < count; i++) {
                Vector3f dir = getRandomVector(false);
                if(Vector3f.dot(dir, hitNormal) < 0) {
                    dir.scale(-1f);
                }
                float rnd = Ref.rnd.nextFloat();
                float rndVelocity =  radius * (1.3f + 0.7f * rnd);
                
                int lifetimeRnd = (int)((rnd) * -50f) + lifeTime;
                int timeRnd = time - (int)(rnd * 100);
                dir.scale(rndVelocity);
                LocalEntity ent = LocalEntity.sparkTrail(origin, dir, 8f, lifetimeRnd, timeRnd, mat,0.9f,0.56f * rnd,0,1);
                ent.Type = LocalEntity.TYPE_SCALE_FADE_MOVE;
                ent.Flags = LocalEntity.FLAG_DONT_SCALE;
            }
    }

    private static Vector3f getRandomVector(boolean normalize) {
        Vector3f vec = new Vector3f((float)Ref.rnd.nextGaussian(),(float)Ref.rnd.nextGaussian(),(float)Ref.rnd.nextGaussian());
        if(normalize) Helper.Normalize(vec);
        return vec;
    }
}
