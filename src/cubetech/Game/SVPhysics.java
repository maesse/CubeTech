
package cubetech.Game;


import cubetech.collision.CubeChunk;
import cubetech.collision.DefaultPhysics;
import cubetech.common.CVar;
import cubetech.common.Common;
import cubetech.common.PlayerState;
import cubetech.misc.Ref;
import java.util.ArrayList;
import nbullet.objects.RigidBody;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class SVPhysics extends DefaultPhysics {
//    
    private boolean enablePlayerKinetic = false;
    private RigidBody[] players = new RigidBody[32];
//
//    public ArrayList<RigidBody> bodies = new ArrayList<RigidBody>();
//    private BoxShape boxShape;
//    private BoxShape characterShape;
//    private javax.vecmath.Vector3f charHalfSize;
//    
//    private int lastPhysicsTime;
//    public DiscreteDynamicsWorld world;
//    
    

//    PhysicsSystem() {
//        // Initalized the bullet physics world
//        CollisionConfiguration collConfig = new DefaultCollisionConfiguration();
//        CollisionDispatcher dispatch = new CollisionDispatcher(collConfig);
//        BroadphaseInterface broadphase = new DbvtBroadphase();
//        broadphase.getOverlappingPairCache().setInternalGhostPairCallback(new GhostPairCallback());
//        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();
//
//        world = new DiscreteDynamicsWorld(dispatch, broadphase, solver, collConfig);
//        CVar gravity = Ref.cvars.Find("sv_gravity");
//        float grav = 800;
//        if(gravity != null) {
//            grav = -gravity.fValue * SCALE_FACTOR;
//        }
//        world.setGravity(new javax.vecmath.Vector3f(0, 0, grav));
//
//        float boxHalfSize = (CubeChunk.BLOCK_SIZE/2f) * SCALE_FACTOR;
//        boxShape = new BoxShape(new javax.vecmath.Vector3f(boxHalfSize, boxHalfSize, boxHalfSize));
//        Vector3f charHalfSize2 = Vector3f.sub(Game.PlayerMaxs, Game.PlayerMins, null);
//        charHalfSize2.scale(0.5f* SCALE_FACTOR);
//        charHalfSize = new javax.vecmath.Vector3f(charHalfSize2.x, charHalfSize2.y, charHalfSize2.z);
//        characterShape = new BoxShape(charHalfSize);
//        
//        BulletGlobals.setDeactivationDisabled(false);
//        BulletGlobals.setDeactivationTime(1.0f);
//    }
//    
//    private RigidBody createCharacterProxy() {
//        Transform t = new Transform();
//        t.setIdentity();
//        
//        MotionState ms = new KinematicMotionState(t);
//        RigidBody body = localCreateRigidKinematicBody(ms, characterShape);
//        return body;
//    }
//    
//
//    public void stepPhysics(int time) {        
//        if(lastPhysicsTime == 0) {
//            lastPhysicsTime = time - 16;
//        }
//        BulletStats.setProfileEnabled(true);
//        float delta = (time - lastPhysicsTime) / 1000f;
//        world.stepSimulation(delta, 5, (1f*Ref.common.com_timescale.fValue)/120f);
//        CProfileIterator it =  CProfileManager.getIterator();
//        lastPhysicsTime = time;
//        handleGhostObjects();
//    }
//
//
//    /**
//     * Grabs all bodies in the radius around origin and applies an outward force from origin to the bodies
//     * center
//     * @param origin
//     * @param radius
//     * @param force
//     * @param forcefalloff true if force should decrease with distance
//     */
//    public void explosionImpulse(Vector3f origin, float radius, float force, boolean forcefalloff) {
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
//    }
//
//    public ArrayList<GhostObject> explosionObjects = new ArrayList<GhostObject>();
//    private void handleGhostObjects() {
//        javax.vecmath.Vector3f impulse = new javax.vecmath.Vector3f(0, 0, 200);
//        Transform t1 = new Transform();
//        Transform t2 = new Transform();
//        for (GhostObject ghostObject : explosionObjects) {
//            ghostObject.getWorldTransform(t1); // get ghost center
//            
//            for (CollisionObject collisionObject : ghostObject.getOverlappingPairs()) {
//                RigidBody b = RigidBody.upcast(collisionObject);
//                if(b != null && !b.isStaticObject()) {
//                    b.getWorldTransform(t2);
//                    impulse.x = t2.origin.x - t1.origin.x;
//                    impulse.y = t2.origin.y - t1.origin.y;
//                    impulse.z = t2.origin.z - t1.origin.z;
//                    float len = impulse.length();
//                    impulse.normalize();
//                    float scale = 200 - len;
//                    impulse.scale(scale);
//                    b.activate(true);
//                    b.applyCentralImpulse(impulse);
//                }
//            }
//            world.removeCollisionObject(ghostObject);
//        }
//        explosionObjects.clear();
//    }
//
//    public void deleteBody(RigidBody body) {
//        world.removeRigidBody(body);
//        bodies.remove(body);
//    }
//
//    public RigidBody localCreateRigidBody(float mass, Transform startTransform, CollisionShape shape) {
//        DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
//        return localCreateRigidBody(mass, myMotionState, shape);
//    }
//
//    public RigidBody localCreateRigidBody(float mass, MotionState mState, CollisionShape shape) {
//        // rigidbody is dynamic if and only if mass is non zero, otherwise static
//        boolean isDynamic = (mass != 0f);
//
//        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0f, 0f, 0f);
//        if (isDynamic) {
//                shape.calculateLocalInertia(mass, localInertia);
//        }
//
//        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
//        RigidBodyConstructionInfo cInfo = new RigidBodyConstructionInfo(mass, mState, shape, localInertia);
//        RigidBody body = new RigidBody(cInfo);
//        
//        
//        
//        world.addRigidBody(body);
//        
//        return body;
//    }
//    
//    public RigidBody localCreateRigidKinematicBody(MotionState mState, CollisionShape shape) {
//        // rigidbody is dynamic if and only if mass is non zero, otherwise static
//        
//
//        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0f, 0f, 0f);
//
//        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
//        RigidBodyConstructionInfo cInfo = new RigidBodyConstructionInfo(0, mState, shape, localInertia);
//        RigidBody body = new RigidBody(cInfo);
//        body.setCollisionFlags(CollisionFlags.KINEMATIC_OBJECT);
//        body.setActivationState(RigidBody.DISABLE_DEACTIVATION);
//        
//        
//        world.addRigidBody(body);
//        
//        return body;
//    }
//    
//
//    public void addChunk(CubeChunk chunk) {
//        ChunkShape shape = new ChunkShape(chunk.blockType);
//        Transform tr = new Transform();
//        tr.setIdentity();
//        tr.origin.set(chunk.absmin[0] * SCALE_FACTOR, chunk.absmin[1] * SCALE_FACTOR, chunk.absmin[2] * SCALE_FACTOR);
//        CollisionObject chunkObject = new CollisionObject();
//        chunkObject.setCollisionFlags(CollisionFlags.STATIC_OBJECT);
//        chunkObject.setCollisionShape(shape);
//        chunkObject.forceActivationState(CollisionObject.DISABLE_SIMULATION);
//        short collisionFilterGroup =  (short) CollisionFilterGroups.STATIC_FILTER;
//	short collisionFilterMask = (short) (CollisionFilterGroups.ALL_FILTER ^ CollisionFilterGroups.STATIC_FILTER);
//        chunkObject.setWorldTransform(tr);
//        
//        world.addCollisionObject(chunkObject, collisionFilterGroup, collisionFilterMask);
////        chunk.physicsShape = localCreateRigidBody(0, tr, shape);
////        chunk.physicsShape.forceActivationState(RigidBody.DISABLE_SIMULATION);
//        chunk.physicsSystem = this; // a bit derp
//    }
//    
    public void PlayerSpawn(PlayerState ps) {
        if(!enablePlayerKinetic) return;
//        int clientId = ps.clientNum;
//        if(players[clientId] == null) {
//            players[clientId] = createCharacterProxy();
//        }
//        RigidBody body = players[clientId];
//        
//        // Always react to this body
//        body.setActivationState(RigidBody.DISABLE_DEACTIVATION);
//        // Set the right position
//        PlayerUpdatePosition(ps);
    }
    
    public void PlayerDie(PlayerState ps) {
        if(!enablePlayerKinetic) return;
//        int clientId = ps.clientNum;
//        if(players[clientId] == null) {
//            Common.Log("Physics:  player isn't created in physics world");
//            return;
//        }
//        RigidBody body = players[clientId];
//        // Don't react on this body temporarily
//        body.setActivationState(RigidBody.DISABLE_SIMULATION);
    }
    
    public void PlayerLeave(PlayerState ps) {
        if(!enablePlayerKinetic) return;
        int clientId = ps.clientNum;
        if(players[clientId] == null) {
            Common.Log("Physics:  player isn't created in physics world");
            return;
        }
        RigidBody body = players[clientId];
        // Remove the body
        deleteBody(body);
        players[clientId] = null;
    }
    
    // Updates the kinematic body for the given player
    public void PlayerUpdatePosition(PlayerState ps) {
        if(!enablePlayerKinetic) return;
//        int clientId = ps.clientNum;
//        if(players[clientId] == null) {
//            Common.Log("Physics:  player isn't created in physics world");
//            return;
//        }
//        
//        RigidBody body = players[clientId];
//        
//        KinematicMotionState ms = (KinematicMotionState) body.getMotionState();
//        toScaledVecmath(ps.origin, ms.t.origin);
//        body.setLinearVelocity(toScaledVecmath(ps.velocity, null));
    }
//    
//    public static javax.vecmath.Vector3f toVecmath(Vector3f v, javax.vecmath.Vector3f dest) {
//        if(dest == null) dest = new javax.vecmath.Vector3f();
//        dest.set(v.x, v.y, v.z);
//        return dest;
//    }
//    
//    public static javax.vecmath.Vector3f toScaledVecmath(Vector3f v, javax.vecmath.Vector3f dest) {
//        if(dest == null) dest = new javax.vecmath.Vector3f();
//        dest.set(v.x*SCALE_FACTOR, v.y*SCALE_FACTOR, v.z*SCALE_FACTOR);
//        return dest;
//    }
//    
//    public static Vector3f toUnscaledVec(javax.vecmath.Vector3f v, Vector3f dest) {
//        if(dest == null) dest = new Vector3f();
//        dest.set(v.x*INV_SCALE_FACTOR, v.y*INV_SCALE_FACTOR, v.z*INV_SCALE_FACTOR);
//        return dest;
//    }
//    
//    private class KinematicMotionState extends MotionState {
//        Transform t;
//        private KinematicMotionState(Transform t) {
//            this.t = t;
//        }
//        @Override
//        public Transform getWorldTransform(Transform t) {
//            // todo: can i use this.t?
//            t.set(this.t);
//            return t;
//        }
//        @Override
//        public void setWorldTransform(Transform t) {}
//    }

    
}

