package cubetech.CGame;

import com.bulletphysics.BulletGlobals;
import com.bulletphysics.collision.broadphase.*;
import com.bulletphysics.collision.dispatch.*;
import com.bulletphysics.collision.shapes.*;
import com.bulletphysics.dynamics.*;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.*;
import cubetech.collision.CubeChunk;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.misc.Ref;
import java.util.ArrayList;
import javax.vecmath.Quat4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class CGPhysics {
    public ArrayList<RigidBody> bodies = new ArrayList<RigidBody>();
    private BoxShape boxShape;
    private int lastPhysicsTime;
    public DiscreteDynamicsWorld world;

    private float scaleFactor = 1 / 32f;

    CGPhysics() {
        // Initalized the bullet physics world
        CollisionConfiguration collConfig = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatch = new CollisionDispatcher(collConfig);
        BroadphaseInterface broadphase = new DbvtBroadphase();
        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

        world = new DiscreteDynamicsWorld(dispatch, broadphase, solver, collConfig);
        float grav = -Ref.cvars.Find("sv_gravity").iValue;
        world.setGravity(new javax.vecmath.Vector3f(0, 0, grav));

        float boxHalfSize = CubeChunk.BLOCK_SIZE/2;
        boxShape = new BoxShape(new javax.vecmath.Vector3f(boxHalfSize, boxHalfSize, boxHalfSize));
        
        BulletGlobals.setDeactivationDisabled(false);
    }

    public void stepPhysics() {
        BulletGlobals.setDeactivationDisabled(false);
        float deactiavtion = BulletGlobals.getDeactivationTime();
        BulletGlobals.setDeactivationTime(1.0f);
        int time = Ref.cgame.cg.time;
        if(lastPhysicsTime == 0) {
            lastPhysicsTime = time - 16;
        }

        if(time > lastPhysicsTime) {
            float delta = (time - lastPhysicsTime) / 1000f;
            world.stepSimulation(delta);
            lastPhysicsTime = time;
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

        //#define USE_MOTIONSTATE 1
        //#ifdef USE_MOTIONSTATE
        

        RigidBodyConstructionInfo cInfo = new RigidBodyConstructionInfo(mass, mState, shape, localInertia);
        cInfo.linearSleepingThreshold = 4f;
        cInfo.angularSleepingThreshold = 1f;
        cInfo.linearDamping = 0.01f;
        cInfo.angularDamping = 0.01f;
        //cInfo.angularSleepingThreshold *= 4f;
        RigidBody body = new RigidBody(cInfo);
        //#else
        //btRigidBody* body = new btRigidBody(mass,0,shape,localInertia);
        //body->setWorldTransform(startTransform);
        //#endif//

        world.addRigidBody(body);

        return body;
    }

    

    public ICommand firebox = new ICommand() {

        public void RunCommand(String[] args) {

            Vector3f dest = Helper.VectorMA(Ref.cgame.cg.refdef.Origin, 20f, Ref.cgame.cg.refdef.ViewAxis[0], null);
            shootBox(dest);
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

    public void shootBox(Vector3f destination) {
        if (world != null) {


//            Transform startTransform = new Transform();
//            startTransform.setIdentity();
            Vector3f cam = Ref.cgame.cg.refdef.Origin;
//
//            javax.vecmath.Vector3f camPos = new javax.vecmath.Vector3f(cam.x, cam.y, cam.z);
//            startTransform.origin.set(camPos);

            LocalEntity lent = LocalEntity.physicsBox(cam, Ref.cgame.cg.time, 20000, Ref.ResMan.getWhiteTexture().asMaterial(), boxShape);
            javax.vecmath.Vector3f linVel = new javax.vecmath.Vector3f(destination.x - cam.x, destination.y - cam.y, destination.z - cam.z);
            linVel.normalize();
            linVel.scale(200f);



//            Transform worldTrans = body.getWorldTransform(new Transform());
//            worldTrans.origin.set(cam.x, cam.y, cam.z);
//            worldTrans.setRotation(new Quat4f(0f, 0f, 0f, 1f));
//            body.setWorldTransform(worldTrans);

            lent.phys_body.setLinearVelocity(linVel);
            lent.phys_body.setAngularVelocity(new javax.vecmath.Vector3f(0f, 0f, 0f));
            bodies.add(lent.phys_body);
            
        }
    }
}
