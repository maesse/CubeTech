package cubetech.collision;

import cubetech.common.CVar;
import cubetech.misc.Ref;
import nbullet.PhysicsSystem;
import nbullet.collision.shapes.ChunkShape;
import nbullet.collision.shapes.CollisionShape;
import nbullet.objects.CollisionObject;
import nbullet.objects.RigidBody;
import nbullet.objects.SoftBody;
import nbullet.util.BufferUtil;
import nbullet.util.DirectMotionState;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class DefaultPhysics {
    public static final float INV_SCALE_FACTOR = 32f;
    public static final float SCALE_FACTOR = 1f / INV_SCALE_FACTOR;

    protected PhysicsSystem world;
    
    protected int stepFrequency = 120;
    protected int maxSubSteps = 10;

    public DefaultPhysics() {
        // Initalized the bullet physics world
        world = new PhysicsSystem();
        
        // Set gravity
        float grav = -800 * SCALE_FACTOR;
        CVar gravity = Ref.cvars.Find("sv_gravity");
        if(gravity != null) {
            grav = -gravity.fValue * SCALE_FACTOR;
        }
        world.setGravity(new Vector3f(0, 0, grav));
    }

    public void stepPhysics(int time) {
        world.stepPhysics(time, maxSubSteps, Ref.common.com_timescale.fValue / stepFrequency);
    }

    /**
     * Grabs all bodies in the radius around origin and applies an outward force from origin to the bodies
     * center
     * @param origin
     * @param radius
     * @param force
     * @param forcefalloff true if force should decrease with distance
     */
    public void explosionImpulse(Vector3f origin, float radius, float force, boolean forcefalloff) {
//        GhostObject ghostObj = new GhostObject();
//        
//        // Set shape
//        ghostObj.setCollisionShape(new SphereShape(radius*SCALE_FACTOR));
//
//        // Set origin
//        Transform t = new Transform();
//        t.setIdentity();
//        t.origin.set(origin.x, origin.y, origin.z);
//        t.origin.scale(SCALE_FACTOR); // scale to physics size
//        ghostObj.setWorldTransform(t);
//        
//        // Don't push bodies away
//        ghostObj.setCollisionFlags(ghostObj.getCollisionFlags() | CollisionFlags.NO_CONTACT_RESPONSE);
//
//        // add to world and out ghostobject list
//        world.addCollisionObject(ghostObj);
//        explosionObjects.add(ghostObj);
    }


    public void deleteBody(RigidBody body) {
        world.removeRigidBody(body);
        body.destroyAll();
    }
    
//    public void removeBody(RigidBody body) {
//        world.removeRigidBody(body);
//    }
    
    public void deleteBody(SoftBody body) {
        world.removeSoftBody(body);
        body.destroy();
    }

    public RigidBody localCreateRigidBody(float mass, Vector3f startTransform, CollisionShape shape) {
        Matrix3f basis = new Matrix3f();
        basis.setIdentity();
        
        DirectMotionState myMotionState = new DirectMotionState(basis, startTransform, null);
        return localCreateRigidBody(mass, myMotionState, shape);
    }

    public RigidBody localCreateRigidBody(float mass, DirectMotionState mState, CollisionShape shape) {
        RigidBody body = new RigidBody(mass, mState, shape);
        world.addRigidBody(body);
        return body;
    }
    
    public void addRigidBody(RigidBody body) {
        world.addRigidBody(body);
    }
    
    public void addChunk(CubeChunk chunk) {
        ChunkShape shape = new ChunkShape(chunk.blockType);
        
        Vector3f origin = new Vector3f(chunk.absmin[0] * SCALE_FACTOR, chunk.absmin[1] * SCALE_FACTOR, chunk.absmin[2] * SCALE_FACTOR);
        CollisionObject chunkObject = new CollisionObject();
        chunkObject.setCollisionShape(shape);
        chunkObject.setCollisionFlags(CollisionObject.CF_STATIC_OBJECT);
        chunkObject.setActivationState(CollisionObject.ActivationStates.DISABLE_SIMULATION);
        chunkObject.setWorldTransform(BufferUtil.toNativeTransform(null, origin));
        
        short collisionFilterGroup =  CollisionObject.FILTER_STATIC;
	short collisionFilterMask = (short) (CollisionObject.FILTER_ALL ^ CollisionObject.FILTER_STATIC);
        world.addCollisionObject(chunkObject, collisionFilterGroup, collisionFilterMask);
    }
    
    public void clearOverlappingCache(CollisionObject object) {
        world.clearOverlappingCache(object);
    }

    public PhysicsSystem getWorld() {
        return world;
    }
}
