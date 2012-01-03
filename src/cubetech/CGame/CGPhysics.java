package cubetech.CGame;

import com.bulletphysics.BulletGlobals;
import com.bulletphysics.BulletStats;
import com.bulletphysics.collision.broadphase.*;
import com.bulletphysics.collision.dispatch.*;
import com.bulletphysics.collision.shapes.*;
import com.bulletphysics.dynamics.*;
import com.bulletphysics.dynamics.constraintsolver.Point2PointConstraint;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.*;
import cubetech.Game.Game;
import cubetech.collision.CubeChunk;
import cubetech.common.CVar;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.common.PlayerState;
import cubetech.gfx.PolyVert;
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
    PolyVert[] boxPoly;
    private BoxShape characterShape;
    private int lastPhysicsTime;
    public DiscreteDynamicsWorld world;
    private javax.vecmath.Vector3f charHalfSize;


    
    // Shared vertex buffer for the physics-cubes
    public VBO vertexBuffer;
    
    private RigidBody player;

    CGPhysics() {
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
        Vector3f charHalfSize2 = Vector3f.sub(Game.PlayerMaxs, Game.PlayerMins, null);
        charHalfSize2.scale(0.5f* SCALE_FACTOR);
        charHalfSize = new javax.vecmath.Vector3f(charHalfSize2.x, charHalfSize2.y, charHalfSize2.z);
        characterShape = new BoxShape(charHalfSize);
        
        Ref.commands.AddCommand("+pick", pick_cmd);
        Ref.commands.AddCommand("-pick", pick_cmd);
        
        BulletGlobals.setDeactivationDisabled(false);
        BulletGlobals.setDeactivationTime(1.0f);
        
        createCharacterProxy();
    }
    
    private void createCharacterProxy() {
        Transform t = new Transform();
        t.setIdentity();
        
        MotionState ms = new KinematicMotionState(t);
        player = localCreateRigidKinematicBody(ms, characterShape);
    }

    

   
    
    private class KinematicMotionState extends MotionState {
        Transform t;
        private KinematicMotionState(Transform t) {
            this.t = t;
        }
        @Override
        public Transform getWorldTransform(Transform t) {
            t.set(this.t);
            return t;
        }

        @Override
        public void setWorldTransform(Transform t) {
            
        }
        
    }
    
    private void updatePlayerProxy(RigidBody body, PlayerState ps) {
        Vector3f org = ps.origin;
        
        
        KinematicMotionState ms =  (KinematicMotionState) body.getMotionState();
        ms.t.origin.set(org.x, org.y, org.z);
        ms.t.origin.scale(SCALE_FACTOR);
//        ms.t.origin.add(charHalfSize);
        
        javax.vecmath.Vector3f vel = new javax.vecmath.Vector3f(ps.velocity.x, ps.velocity.y, ps.velocity.z);
        vel.scale(SCALE_FACTOR);
        body.setLinearVelocity(vel);
    }

    public void stepPhysics() {
        int time = Ref.cgame.cg.time;
        
        if(lastPhysicsTime == 0) {
            lastPhysicsTime = time - 16;
        }

        if(time > lastPhysicsTime) {
            if(player != null) {
                updatePlayerProxy(player, Ref.cgame.cg.predictedPlayerState);
                
                
            }
            if (pickConstraint != null) {
                // move the constraint pivot
                Point2PointConstraint p2p = (Point2PointConstraint) pickConstraint;
                if (p2p != null) {
                    // keep it at the same picking distance
                    Vector3f origin = Ref.cgame.cg.refdef.Origin;
                    Vector3f ray = Ref.cgame.cg.refdef.ViewAxis[0];
                    javax.vecmath.Vector3f cameraPosition = new javax.vecmath.Vector3f(origin.x, origin.y, origin.z);
                    cameraPosition.scale(SCALE_FACTOR);
                    javax.vecmath.Vector3f rayTo = new javax.vecmath.Vector3f(ray.x, ray.y, ray.z);
                    rayTo.scale(BulletStats.gOldPickingDist);
                    rayTo.add(cameraPosition);
                    p2p.setPivotB(rayTo);
                }
            }
            BulletStats.setProfileEnabled(true);
            float delta = (time - lastPhysicsTime) / 1000f;
            world.stepSimulation(delta, 5, 1f/120f);
            
            CProfileIterator it =  CProfileManager.getIterator();
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
    
    public RigidBody localCreateRigidKinematicBody(MotionState mState, CollisionShape shape) {
        // rigidbody is dynamic if and only if mass is non zero, otherwise static
        

        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0f, 0f, 0f);

        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
        RigidBodyConstructionInfo cInfo = new RigidBodyConstructionInfo(0, mState, shape, localInertia);
        RigidBody body = new RigidBody(cInfo);
        body.setCollisionFlags(CollisionFlags.KINEMATIC_OBJECT);
        body.setActivationState(RigidBody.DISABLE_DEACTIVATION);
        
        
        world.addRigidBody(body);
        
        return body;
    }
    
    private ICommand pick_cmd = new ICommand() {
        public void RunCommand(String[] args) {
            boolean activate = !args[0].startsWith("-");
            handlePick(activate, Ref.cgame.cg.refdef.Origin, Ref.cgame.cg.refdef.ViewAxis[0]);
        }
    };


    RigidBody pickedBody = null;
    Point2PointConstraint pickConstraint = null;
    private void handlePick(boolean activate, Vector3f origin, Vector3f ray) {
        if (activate) {
            javax.vecmath.Vector3f cameraPosition = new javax.vecmath.Vector3f(origin.x, origin.y, origin.z);
            cameraPosition.scale(SCALE_FACTOR);
            javax.vecmath.Vector3f rayTo = new javax.vecmath.Vector3f(ray.x, ray.y, ray.z);
            rayTo.scale(100f*SCALE_FACTOR);
            rayTo.add(cameraPosition);
            
            // add a point to point constraint for picking
            if (world != null) {
                
                CollisionWorld.ClosestRayResultCallback rayCallback = new CollisionWorld.ClosestRayResultCallback(cameraPosition, rayTo);
                world.rayTest(cameraPosition, rayTo, rayCallback);
                if (rayCallback.hasHit()) {
                    RigidBody body = RigidBody.upcast(rayCallback.collisionObject);
                    if (body != null) {

                        // other exclusions?
                        if (!(body.isStaticObject() || body.isKinematicObject())) {
                            pickedBody = body;
                            pickedBody.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
                            pickedBody.setDamping(0.1f, 0.99f);
                            javax.vecmath.Vector3f pickPos = new javax.vecmath.Vector3f(rayCallback.hitPointWorld);

                            Transform tmpTrans = body.getCenterOfMassTransform(new Transform());
                            tmpTrans.inverse();
                            javax.vecmath.Vector3f localPivot = new javax.vecmath.Vector3f(pickPos);
                            tmpTrans.transform(localPivot);

                            Point2PointConstraint p2p = new Point2PointConstraint(body, localPivot);
                            p2p.setting.impulseClamp = 3f;

                            world.addConstraint(p2p);
                            pickConstraint = p2p;
                            // save mouse position for dragging
                            BulletStats.gOldPickingPos.set(rayTo);
                            javax.vecmath.Vector3f eyePos = new javax.vecmath.Vector3f(cameraPosition);
                            javax.vecmath.Vector3f tmp = new javax.vecmath.Vector3f();
                            tmp.sub(pickPos, eyePos);
                            BulletStats.gOldPickingDist = tmp.length();
                            // very weak constraint for picking
                            p2p.setting.tau = 0.1f;
                        }
                    }
                }
            }

        } else {
            if (pickConstraint != null && world != null) {
                world.removeConstraint(pickConstraint);
                // delete m_pickConstraint;
                //printf("removed constraint %i",gPickingConstraintId);
                pickConstraint = null;
                pickedBody.setDamping(0.1f, 0.2f);
                pickedBody.forceActivationState(CollisionObject.ACTIVE_TAG);
                pickedBody.setDeactivationTime(0f);
                pickedBody = null;
            }
        }
    }

    public ICommand firebox = new ICommand() {
        public void RunCommand(String[] args) {
            //Vector3f origin = Ref.cgame.cg.predictedPlayerEntity.lerpOrigin;
            Vector3f viewXY = new Vector3f(Ref.cgame.cg.refdef.ViewAxis[0]);
            viewXY.z = 0f;
            viewXY.normalise();
            Vector3f origin = Helper.VectorMA(Ref.cgame.cg.predictedPlayerEntity.lerpOrigin, Ref.cgame.cg.predictedPlayerState.viewheight, new Vector3f(0, 0, 1), null);
            origin = Helper.VectorMA(origin, 60, viewXY, origin);
            shootBox(origin, Ref.cgame.cg.refdef.ViewAxis[0], 20f); // shootbox scales it down
        }
    };

    public void renderBodies() {
        if(true) return;
        if(boxPoly == null) {
            Vector3f[] boxverts = Helper.createBoxVerts(10, null);
            boxPoly = new PolyVert[boxverts.length];
            for (int i = 0; i < boxPoly.length; i++) {
                boxPoly[i] = new PolyVert();
                boxPoly[i].xyz = boxverts[i];
            }
        }
        
        
                
        Transform t = new Transform();
        for (RigidBody rigidBody : bodies) {
            //rigidBody.getWorldTransform(t);
            MotionState mState = rigidBody.getMotionState();
            mState.getWorldTransform(t);
            
            RenderEntity ent = Ref.render.createEntity(REType.POLY);
            ent.flags |= RenderEntity.FLAG_SPRITE_AXIS;
            ent.origin.set(t.origin.x, t.origin.y, t.origin.z);
            Helper.matrixToAxis(t.basis, ent.axis);
//            ent.axis[0].set(1,0,0);
//            ent.axis[1].set(0,1,0);
//            ent.axis[2].set(0,0,1);
//            Vector3f dest = Helper.VectorMA(Ref.cgame.cg.refdef.Origin, 20f, Ref.cgame.cg.refdef.ViewAxis[0], null);
//            ent.origin.set(dest);

            
            
            int activationState = rigidBody.getActivationState();
            ent.color.set(1,1,1,1);
            switch(activationState) {
                case 1: // active
                    ent.color.set(1,0,0,1);
                    break;
                case 2:
                    ent.color.set(1,1,0,1);
                    break;
                default:
                    ent.color.set(0,0,1,1);
            }

            ent.outcolor.set(ent.color);
            ent.outcolor.scale(255);
            ent.mat = Ref.ResMan.getWhiteTexture().asMaterial();
            
            ent.verts = boxPoly;
            ent.frame = boxPoly.length;

            Ref.render.addRefEntity(ent);
        }
    }
    
    

    public void shootBox(Vector3f origin, Vector3f dir, float force) {
        if (world != null) {

            LocalEntity lent = LocalEntity.physicsBox(origin, Ref.cgame.cg.time, 20000, Ref.ResMan.LoadTexture("data/textures/tile.png").asMaterial(), boxShape);
            javax.vecmath.Vector3f linVel = new javax.vecmath.Vector3f(dir.x, dir.y, dir.z);
            linVel.normalize();
            linVel.scale(force);

            lent.phys_body.setLinearVelocity(linVel);
            lent.phys_body.setAngularVelocity(new javax.vecmath.Vector3f(0f, 0f, 0f));
            lent.phys_body.setDamping(0.1f, 0.2f);
            bodies.add(lent.phys_body);
        }
    }

    void addChunk(CubeChunk chunk) {
        ChunkShape shape = new ChunkShape(chunk.blockType);
        Transform tr = new Transform();
        tr.setIdentity();
        tr.origin.set(chunk.absmin[0] * SCALE_FACTOR, chunk.absmin[1] * SCALE_FACTOR, chunk.absmin[2] * SCALE_FACTOR);
        chunk.physicsShape = localCreateRigidBody(0, tr, shape);
        chunk.physicsShape.forceActivationState(RigidBody.DISABLE_SIMULATION);
        
    }
}
