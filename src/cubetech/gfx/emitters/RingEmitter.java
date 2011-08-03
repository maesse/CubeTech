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
public class RingEmitter implements IEmitter {
    private int lifeTime = 800;
    private int localMsec = 0;

    private Vector3f origin = new Vector3f();
    private Vector3f normal = new Vector3f();
    private float radius;

    private CubeMaterial mat;

    public RingEmitter(Vector3f origin, float radius, Vector3f normal) {
        this.origin.set(origin);
        this.normal.set(normal);
        this.radius = radius;

        mat = Ref.ResMan.LoadTexture("data/particles/ringexplosion.png").asMaterial();
    }

    private static Vector3f up = new Vector3f(0, 0, 1);
    public static void spawn(float radius, Vector3f origin) {
        int time = Ref.cgame.cg.time;
        Vector3f normal = up;
        CubeMaterial mat = Ref.ResMan.LoadTexture("data/particles/ringexplosion.png").asMaterial();
        LocalEntity.ringExplosion(origin, normal, radius, 800, time, mat, 1, 0.3f, 0, 1);
    }

    public void update(int msec) {
        boolean spawn = localMsec == 0;
        localMsec += msec;
        lifeTime = 500;
        if(localMsec > lifeTime) {
            localMsec = msec;
            spawn = true;
        }

        if(spawn) {
            int time = Ref.cgame.cg.time;
            LocalEntity.ringExplosion(origin, normal, radius, lifeTime, time, mat, 1, 0.3f, 0, 1);
        }

    }
}
