package cubetech.CGame;

import com.bulletphysics.BulletGlobals;
import com.bulletphysics.collision.broadphase.*;
import com.bulletphysics.collision.dispatch.*;
import com.bulletphysics.collision.shapes.*;
import com.bulletphysics.dynamics.*;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.*;
import cubetech.collision.CubeChunk;
import cubetech.common.CVar;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.gfx.GLRef.BufferTarget;
import cubetech.gfx.VBO;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import javax.vecmath.Quat4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CGPhysics {
    public static final float INV_SCALE_FACTOR = 32f;
    public static final float SCALE_FACTOR = 1f / INV_SCALE_FACTOR;

    public ArrayList<RigidBody> bodies = new ArrayList<RigidBody>();
    private BoxShape boxShape;
    private int lastPhysicsTime;
    public DiscreteDynamicsWorld world;

    public ByteBuffer sharedIndiceBuffer;

    // Shared vertex buffer for the physics-cubes
    public VBO vertexBuffer;

    CGPhysics() {
        initIndiceBuffer();
        initBoxVBO();

        // Initalized the bullet physics world
        CollisionConfiguration collConfig = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatch = new CollisionDispatcher(collConfig);
        BroadphaseInterface broadphase = new DbvtBroadphase();
        broadphase.getOverlappingPairCache().setInternalGhostPairCallback(new GhostPairCallback());
        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

        world = new DiscreteDynamicsWorld(dispatch, broadphase, solver, collConfig);
        CVar gravity = Ref.cvars.Find("sv_gravity");
        float grav = 800;
        if(gravity != null) {
            grav = -gravity.fValue * SCALE_FACTOR;
        }
        world.setGravity(new javax.vecmath.Vector3f(0, 0, grav));

        float boxHalfSize = (CubeChunk.BLOCK_SIZE/2f) * SCALE_FACTOR;
        boxShape = new BoxShape(new javax.vecmath.Vector3f(boxHalfSize, boxHalfSize, boxHalfSize));


        
        BulletGlobals.setDeactivationDisabled(false);
        BulletGlobals.setDeactivationTime(1.0f);
    }

    private void initBoxVBO() {
        // We're going to be rendering the same box a lot, so set up a vbo for it
        int nVerts = 4 * 6; // 4 points * 6 sides
        int stride = 32; // 32bytes for each point

        // Create a VBO and grab a mapped bytebuffer
        vertexBuffer = new VBO(nVerts * stride, BufferTarget.Vertex);
        ByteBuffer data = vertexBuffer.map();

        // Fill the buffer
        float boxHalfSize = (CubeChunk.BLOCK_SIZE/2f) * SCALE_FACTOR;
        // X+


        vertexBuffer.unmap();
    }

    private void initIndiceBuffer() {
        // Create a big indice buffer that can be used for all chunks
        int nSides = 32 * 32 * 32;
        int sideSize = 6 * 4; // 6 integer indices
        sharedIndiceBuffer = ByteBuffer.allocateDirect(nSides * sideSize).order(ByteOrder.nativeOrder());
        
        // For each quad...
        for (int i= 0; i < nSides; i++) {
            // ... build the indices for it
            sharedIndiceBuffer.putInt(4 * i);
            sharedIndiceBuffer.putInt(4 * i + 1);
            sharedIndiceBuffer.putInt(4 * i + 2);
            sharedIndiceBuffer.putInt(4 * i + 2);
            sharedIndiceBuffer.putInt(4 * i + 1);
            sharedIndiceBuffer.putInt(4 * i + 3);
        }
        sharedIndiceBuffer.flip();
    }

    public void stepPhysics() {
        int time = Ref.cgame.cg.time;
        if(lastPhysicsTime == 0) {
            lastPhysicsTime = time - 16;
        }

        if(time > lastPhysicsTime) {
            float delta = (time - lastPhysicsTime) / 1000f;
            world.stepSimulation(delta);
            lastPhysicsTime = time;
            handleGhostObjects();
        }
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
        GhostObject ghostObj = new GhostObject();
        
        // Set shape
        ghostObj.setCollisionShape(new SphereShape(radius*SCALE_FACTOR));

        // Set origin
        Transform t = new Transform();
        t.setIdentity();
        t.origin.set(origin.x, origin.y, origin.z);
        t.origin.scale(SCALE_FACTOR); // scale to physics size
        ghostObj.setWorldTransform(t);
        
        // Don't push bodies away
        ghostObj.setCollisionFlags(ghostObj.getCollisionFlags() | CollisionFlags.NO_CONTACT_RESPONSE);

        // add to world and out ghostobject list
        world.addCollisionObject(ghostObj);
        explosionObjects.add(ghostObj);
    }

    public ArrayList<GhostObject> explosionObjects = new ArrayList<GhostObject>();
    private void handleGhostObjects() {
        javax.vecmath.Vector3f impulse = new javax.vecmath.Vector3f(0, 0, 200);
        Transform t1 = new Transform();
        Transform t2 = new Transform();
        for (GhostObject ghostObject : explosionObjects) {
            ghostObject.getWorldTransform(t1); // get ghost center
            
            for (CollisionObject collisionObject : ghostObject.getOverlappingPairs()) {
                RigidBody b = RigidBody.upcast(collisionObject);
                if(b != null && !b.isStaticObject()) {
                    b.getWorldTransform(t2);
                    impulse.x = t2.origin.x - t1.origin.x;
                    impulse.y = t2.origin.y - t1.origin.y;
                    impulse.z = t2.origin.z - t1.origin.z;
                    float len = impulse.length();
                    impulse.normalize();
                    float scale = 200 - len;
                    impulse.scale(scale);
                    b.activate(true);
                    b.applyCentralImpulse(impulse);
                }
            }
            world.removeCollisionObject(ghostObject);
        }
        explosionObjects.clear();
    }

    public void deleteBody(RigidBody body) {
        world.removeRigidBody(body);
        bodies.remove(body);
    }

    public RigidBody localCreateRigidBody(float mass, Transform startTransform, CollisionShape shape) {
        DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
        return localCreateRigidBody(mass, myMotionState, shape);
    }

    public RigidBody localCreateRigidBody(float mass, MotionState mState, CollisionShape shape) {
        // rigidbody is dynamic if and only if mass is non zero, otherwise static
        boolean isDynamic = (mass != 0f);

        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0f, 0f, 0f);
        if (isDynamic) {
                shape.calculateLocalInertia(mass, localInertia);
        }

        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
        RigidBodyConstructionInfo cInfo = new RigidBodyConstructionInfo(mass, mState, shape, localInertia);
        RigidBody body = new RigidBody(cInfo);

        world.addRigidBody(body);
        return body;
    }

    

    public ICommand firebox = new ICommand() {
        public void RunCommand(String[] args) {
            Vector3f origin = Ref.cgame.cg.predictedPlayerEntity.lerpOrigin;
            shootBox(origin, Ref.cgame.cg.refdef.ViewAxis[0], 20f); // shootbox scales it down
        }
    };

    public void renderBodies() {
//        Transform t = new Transform();
//        for (RigidBody rigidBody : bodies) {
//            //rigidBody.getWorldTransform(t);
//            MotionState mState = rigidBody.getMotionState();
//            mState.getWorldTransform(t);
//            RenderEntity ent = Ref.render.createEntity(REType.SPRITE);
//            ent.origin.set(t.origin.x, t.origin.y, t.origin.z);
////            Vector3f dest = Helper.VectorMA(Ref.cgame.cg.refdef.Origin, 20f, Ref.cgame.cg.refdef.ViewAxis[0], null);
////            ent.origin.set(dest);
//
//            ent.radius = 10;
//            int activationState = rigidBody.getActivationState();
//            ent.color.set(1,1,1,1);
//            switch(activationState) {
//                case 1: // active
//                    ent.color.set(1,0,0,1);
//                    break;
//                case 2:
//                    ent.color.set(1,1,0,1);
//                    break;
//                default:
//                    ent.color.set(0,0,1,1);
//            }
//
//            ent.outcolor.set(ent.color);
//            ent.outcolor.scale(255);
//            ent.mat = Ref.ResMan.getWhiteTexture().asMaterial();
//
//            Ref.render.addRefEntity(ent);
//        }
    }

    public void shootBox(Vector3f origin, Vector3f dir, float force) {
        if (world != null) {

            LocalEntity lent = LocalEntity.physicsBox(origin, Ref.cgame.cg.time, 20000, Ref.ResMan.getWhiteTexture().asMaterial(), boxShape);
            javax.vecmath.Vector3f linVel = new javax.vecmath.Vector3f(dir.x, dir.y, dir.z);
            linVel.normalize();
            linVel.scale(force);

            lent.phys_body.setLinearVelocity(linVel);
            lent.phys_body.setAngularVelocity(new javax.vecmath.Vector3f(0f, 0f, 0f));
            bodies.add(lent.phys_body);
        }
    }
}
