package cubetech.CGame;

import cubetech.common.Trajectory;
import cubetech.gfx.CubeMaterial;
import cubetech.misc.Ref;
import java.util.LinkedList;
import java.util.Queue;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class LocalEntity {
    // types
    public static final int TYPE_SCALE_FADE = 0;
    // flags
    public static final int FLAG_PUFF_DONT_SCALE = 1;

    public int Type;
    public int Flags;

    public int startTime;
    public int endTime;
    public int fadeInTime;

    public float lifeRate; // 1.0 / (endTime - startTime)

    public Trajectory pos = new Trajectory();
    public Trajectory angles = new Trajectory();

    public float bouncefactor; // 0.0 = no bounce, 1.0 = perfect

    public Vector4f color = new Vector4f();

    public float radius;

    public RenderEntity rEntity = Ref.render.createEntity();

    boolean freeMe = false;

    public void free() {
        freeMe = true;
        LocalEntities.free(this);
    }

    void clear() {
        Type = -1;
        Flags = 0;
        startTime = 0;
        endTime = 0;
        fadeInTime = 0;
        lifeRate = 0;
        pos = new Trajectory();
        angles = new Trajectory();
        bouncefactor = 0;
        color = new Vector4f();
        radius = 0;
        freeMe = false;
        rEntity = null;
    }

    
    public static LocalEntity smokePuff(Vector3f p, Vector3f vel, float radius, float r, float g, float b, float a,
                                        float duration, int startTime, int fadeInTime, int leFlags, CubeMaterial mat) {
        LocalEntity le = LocalEntities.allocate();
        le.Flags = leFlags;
        le.radius = radius;
        le.rEntity = new RenderEntity();
        RenderEntity re = le.rEntity;
        re.radius = radius;
        re.shaderTime = startTime / 1000f;

        le.Type = LocalEntity.TYPE_SCALE_FADE;
        le.startTime = startTime;
        le.fadeInTime = fadeInTime;
        le.endTime = startTime + (int)duration;
        if(fadeInTime > startTime) {
            le.lifeRate = 1.0f / (le.endTime - le.fadeInTime);
        } else {
            le.lifeRate = 1.0f / (le.endTime - le.startTime);
        }

        le.color.set(r,g,b,a);

        le.pos.type = Trajectory.LINEAR;
        le.pos.time = startTime;
        if(vel != null) le.pos.delta.set(vel);
        else le.pos.delta.set(0,0,0);

        le.pos.base.set(p);

        re.origin.set(p);
        re.mat = mat;

        re.color.set(le.color);
        re.outcolor.x = le.color.x * 0xff;
        re.outcolor.y = le.color.y * 0xff;
        re.outcolor.z = le.color.z * 0xff;
        re.outcolor.w = 0xff;

        re.Type = REType.SPRITE;
        re.radius = le.radius;

        return le;
    }
    
}
