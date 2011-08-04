package cubetech.CGame;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import cubetech.common.Helper;
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
    public static final int TYPE_SCALE_FADE_COLOR = 1;
    public static final int TYPE_SCALE_FADE_MOVE = 2;
    public static final int TYPE_FADE = 3;
    public static final int TYPE_SCALE_DOUBLE_MOVE = 4;
    public static final int TYPE_PHYSICSOBJECT = 5;
    // flags
    public static final int FLAG_DONT_SCALE = 1;



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
    public Vector4f color2 = new Vector4f();

    public float radius;

    public RenderEntity rEntity = null;

    boolean freeMe = false;

    public MotionState phys_motionState;
    public RigidBody phys_body;

    public void free() {
        freeMe = true;
        LocalEntities.free(this);
    }

    void clear() {
        if(Type == TYPE_PHYSICSOBJECT && phys_body != null) {
            // remove physics object from world
            Ref.cgame.physics.deleteBody(phys_body);
        }
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
        phys_motionState = null;
        phys_body = null;
    }

    public static LocalEntity physicsBox(Vector3f origin, int starttime, int duration, CubeMaterial mat, CollisionShape boxShape) {
        LocalEntity le = LocalEntities.allocate();
        le.Type = LocalEntity.TYPE_PHYSICSOBJECT;
        le.startTime = starttime;
        le.endTime = starttime + duration;
        le.rEntity = new RenderEntity(REType.SPRITE);
        le.rEntity.radius = 10;
        le.rEntity.mat = mat;
        le.rEntity.outcolor.set(255,255,255,255);

        le.pos.base.set(origin);

        Transform t = new Transform();
        t.setIdentity();
        t.origin.set(origin.x, origin.y, origin.z);
        le.phys_motionState = new DefaultMotionState(t);

        le.phys_body = Ref.cgame.physics.localCreateRigidBody(10f, le.phys_motionState, boxShape);
        le.phys_body.setCcdMotionThreshold(2f);
        le.phys_body.setCcdSweptSphereRadius(0.2f);
        return le;
    }

    // stretches out along dir
    public static LocalEntity ringExplosion(Vector3f origin, Vector3f dir, float radius, int duration, int startTime, CubeMaterial mat,
            float r, float g, float b, float a) {
        LocalEntity le = LocalEntities.allocate();
        le.Type = LocalEntity.TYPE_SCALE_FADE;
        le.startTime = startTime;
        le.endTime = le.startTime + duration;
        le.lifeRate = 1.0f / (le.endTime - le.startTime);

        le.pos.base.set(origin);
        le.pos.type = Trajectory.LINEAR;
        le.pos.time = startTime;
        le.pos.delta.set(dir);

        le.color.set(r,g,b,a);

        le.rEntity = new RenderEntity(REType.SPRITE);
        RenderEntity re = le.rEntity;
        le.radius = re.radius = radius;
        re.origin.set(origin);
        re.color.set(le.color);
        re.outcolor.x = le.color.x * 0xff;
        re.outcolor.y = le.color.y * 0xff;
        re.outcolor.z = le.color.z * 0xff;
        re.outcolor.w = 0xff;
        re.mat = mat;
        
        re.flags = RenderEntity.FLAG_SPRITE_AXIS;
        Helper.perpendicularVector(dir, re.axis[0]);
        Vector3f.cross(re.axis[0], dir, re.axis[1]);


        return le;
    }

    // stretches out along dir
    public static LocalEntity sparkTrail(Vector3f origin, Vector3f dir, float radius, int duration, int startTime, CubeMaterial mat,
            float r, float g, float b, float a) {
        LocalEntity le = LocalEntities.allocate();
        le.Type = LocalEntity.TYPE_FADE;
        le.startTime = startTime;
        le.endTime = le.startTime + duration;
        le.lifeRate = 1.0f / (le.endTime - le.startTime);

        le.pos.base.set(origin);
        le.pos.type = Trajectory.LINEAR;
        le.pos.time = startTime;
        le.pos.delta.set(dir);

        le.color.set(r,g,b,a);

        le.rEntity = new RenderEntity(REType.BEAM);
        RenderEntity re = le.rEntity;
        le.radius = re.radius = radius;
        re.origin.set(origin);
        re.oldOrigin.set(origin);
        re.color.set(le.color);
        re.outcolor.x = le.color.x * 0xff;
        re.outcolor.y = le.color.y * 0xff;
        re.outcolor.z = le.color.z * 0xff;
        re.outcolor.w = 0xff;
        re.mat = mat;

        return le;
    }

    // Moves and stretches along dir
    public static LocalEntity sparkTrail(Vector3f origin, Vector3f dir, Vector3f originDir, float radius, int duration, int startTime, CubeMaterial mat,
            float r, float g, float b, float a) {
        LocalEntity le = LocalEntities.allocate();
        le.Type = LocalEntity.TYPE_SCALE_DOUBLE_MOVE;
        le.startTime = startTime;
        le.endTime = le.startTime + duration;
        le.lifeRate = 1.0f / (le.endTime - le.startTime);

        le.pos.base.set(origin);
        le.pos.type = Trajectory.LINEAR;
        le.pos.time = startTime;
        le.pos.delta.set(dir);

        le.angles.base.set(origin);
        le.angles.type = Trajectory.LINEAR;
        le.angles.time = startTime;
        le.angles.delta.set(originDir);

        le.color.set(r,g,b,a);

        le.rEntity = new RenderEntity(REType.BEAM);
        RenderEntity re = le.rEntity;
        le.radius = re.radius = radius;
        re.origin.set(origin);
        re.oldOrigin.set(origin);
        re.color.set(le.color);
        re.outcolor.x = le.color.x * 0xff;
        re.outcolor.y = le.color.y * 0xff;
        re.outcolor.z = le.color.z * 0xff;
        re.outcolor.w = 0xff;
        re.mat = mat;

        return le;
    }

    
    public static LocalEntity smokePuff(Vector3f p, Vector3f vel, float radius, float r, float g, float b, float a,
                                        float duration, int startTime, int fadeInTime, int leFlags, CubeMaterial mat) {
        LocalEntity le = LocalEntities.allocate();
        le.Flags = leFlags;
        le.radius = radius;
        le.rEntity = new RenderEntity(REType.SPRITE);
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

        re.radius = le.radius;

        return le;
    }

    // r2, g2, etc is colors of the final color
    // colortime is how long the first color will take to fade out
    public static LocalEntity colorFadedSmokePuff(Vector3f p, Vector3f dir, float radius, float r, float g, float b, float a,
                                        float duration, int startTime, int leFlags, CubeMaterial mat,
                                        float r2, float g2, float b2, float a2, float colortime) {
        LocalEntity le = LocalEntities.allocate();
        le.Flags = leFlags;
        le.radius = radius;
        le.rEntity = new RenderEntity(REType.SPRITE);
        

        le.Type = LocalEntity.TYPE_SCALE_FADE_COLOR;
        le.startTime = startTime;
        le.endTime = startTime + (int)duration;
        le.lifeRate = 1.0f / (le.endTime - le.startTime);

        le.color.set(r,g,b,a);
        le.color2.set(r2,g2,b2,a2);

        le.bouncefactor =   colortime;

        le.pos.base.set(p);
        le.pos.type = Trajectory.LINEAR;
        le.pos.time = startTime;
        if(dir != null) {
            le.pos.delta.set(dir);
        }

        le.pos.base.set(p);

        RenderEntity re = le.rEntity;
        re.radius = radius;

        re.origin.set(p);
        re.mat = mat;

        re.color.set(le.color);
        re.outcolor.x = le.color.x * 0xff;
        re.outcolor.y = le.color.y * 0xff;
        re.outcolor.z = le.color.z * 0xff;
        re.outcolor.w = 0xff;

        return le;
    }
    
}
