
package cubetech.gfx.emitters;

import cubetech.CGame.LocalEntities;
import cubetech.CGame.LocalEntity;
import cubetech.CGame.REType;
import cubetech.CGame.RenderEntity;
import cubetech.common.Helper;
import cubetech.gfx.CubeMaterial;
import cubetech.gfx.IEmitter;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class SmokeEmitter implements IEmitter {
    private int lifeTime = 1000;
    private int localMsec = 0;

    private Vector3f origin = new Vector3f();
    private float radius;
    private Vector3f normal  = new Vector3f();
    private float density;

    // names are bogous
    private Vector3f up;
    private Vector3f right;

    private CubeMaterial mat;

    public SmokeEmitter(Vector3f origin, float radius, Vector3f movePlane, float density) {
        // Emitter starts at origin and spits out smoke particles along the movePlane
        this.origin.set(origin);
        this.radius = radius;
        this.normal.set(movePlane);
        this.density = density;

        right = Helper.perpendicularVector(movePlane, null);
        up = Vector3f.cross(right, movePlane, null);
        
        mat = Ref.ResMan.LoadTexture("data/particles/smokesprites0067.png").asMaterial();
    }

    public void update(int msec) {
        boolean spawn = localMsec == 0;
        localMsec += msec;


        if(localMsec > lifeTime) {
            localMsec = msec;
            spawn = true;
        }

        if(spawn) {
            float area = (float) (Math.PI * radius * radius) * 0.05f;
            int spawnCount = (int)(area * density);

            int time = Ref.cgame.cg.time;
            for (int i= 0; i < spawnCount; i++) {
                // get two random numbers between -radius and radius
                float rightRnd = (Ref.rnd.nextFloat()*2f - 1);
                float upRnd = (Ref.rnd.nextFloat()*2f - 1);

                float len = (float) Math.sqrt(rightRnd * rightRnd + upRnd * upRnd);
                if(len > 1) {
                    rightRnd /= len;
                    upRnd /= len;
                }

                rightRnd *= radius;
                upRnd *= radius;

                // move along the ground plane
                // out = origin + right * rightRnd + up * upRnd
                Vector3f particleOrigin = new Vector3f(origin.x + rightRnd * right.x + upRnd * up.x,
                                                       origin.y + rightRnd * right.y + upRnd * up.y,
                                                       origin.z + rightRnd * right.z + upRnd * up.z);

                Vector3f delta = new Vector3f(
                        rightRnd * right.x + upRnd * up.x,
                        rightRnd * right.y + upRnd * up.y,
                        rightRnd * right.z + upRnd * up.z);

                LocalEntity ent = LocalEntity.smokePuff(particleOrigin, delta, 64, 1, 1, 1, 0.5f, lifeTime, time, 200, 0, mat);
                ent.Type = LocalEntity.TYPE_SCALE_FADE_MOVE;

            }
        }
    }

}
