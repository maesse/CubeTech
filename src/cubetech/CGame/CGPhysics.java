package cubetech.CGame;



import cubetech.collision.CubeChunk;
import cubetech.common.CVar;
import cubetech.common.Helper;
import cubetech.common.ICommand;
import cubetech.gfx.PolyVert;
import cubetech.gfx.VBO;
import cubetech.misc.Ref;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import nbullet.PhysicsSystem;
import nbullet.collision.ClosestRayResult;
import nbullet.collision.shapes.BoxShape;
import nbullet.collision.shapes.ChunkShape;
import nbullet.collision.shapes.CollisionShape;
import nbullet.constraints.Generic6DofConstraint;
import nbullet.constraints.Point2PointConstraint;
import nbullet.objects.CollisionObject;
import nbullet.objects.RigidBody;
import nbullet.objects.SoftBody;
import nbullet.util.BufferUtil;
import nbullet.util.DirectMotionState;
import nbullet.util.Transform;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.mapped.CacheUtil;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class CGPhysics {
    public static final float INV_SCALE_FACTOR = 32f;
    public static final float SCALE_FACTOR = 1f / INV_SCALE_FACTOR;

    PhysicsSystem world;
    BoxShape boxShape;
    int lastPhysicsTime;
    
    // Shared vertex buffer for the physics-cubes
    public VBO vertexBuffer;
    
    SoftBody soft;
    int[] softIndices;
    FloatBuffer softBuffer;
    

    CGPhysics() {
        // Initalized the bullet physics world
        world = new PhysicsSystem();
        
        CVar gravity = Ref.cvars.Find("sv_gravity");
        float grav = 800 * SCALE_FACTOR;
        if(gravity != null) {
            grav = -gravity.fValue * SCALE_FACTOR;
        }
        world.setGravity(new Vector3f(0, 0, grav));
//        world.testNativePhysics(); // creates test ground plane

        float boxHalfSize = (CubeChunk.BLOCK_SIZE/2f) * SCALE_FACTOR;
        boxShape = new BoxShape(new Vector3f(boxHalfSize, boxHalfSize, boxHalfSize));
        
        Ref.commands.AddCommand("+pick", pick_cmd);
        Ref.commands.AddCommand("-pick", pick_cmd);
        Ref.commands.AddCommand("testsoft", pick_testsoft);

//        BulletGlobals.setDeactivationDisabled(false);
//        BulletGlobals.setDeactivationTime(1.0f);
//        
//        createCharacterProxy();
    }


    public void stepPhysics() {
        int time = Ref.cgame.cg.time;
        
        
        handlePick();
        
        world.stepPhysics(time, 10, Ref.common.com_timescale.fValue / 120f);
        
        if(soft != null ) {
            // Update softbody vertices
            int vcount = soft.getVertexCount();
            int stride = (4*4*2);
            if(softBuffer == null || softBuffer.capacity() < vcount * stride) {
                softBuffer = CacheUtil.createFloatBuffer(vcount * stride / 4);
            }
            softBuffer.clear();
            soft.getVertexData(softBuffer, true);
            Vector3f[] verts = new Vector3f[vcount];
            Vector3f[] normals = new Vector3f[vcount];
            for (int i = 0; i < vcount; i++) {
                verts[i] = new Vector3f(softBuffer.get(),softBuffer.get(),softBuffer.get());
                verts[i].scale(INV_SCALE_FACTOR);
                softBuffer.get();
                normals[i] = new Vector3f(softBuffer.get(),softBuffer.get(),softBuffer.get());
                softBuffer.get();
            }
            
            int icount = soft.getIndiceCount();
            if(softIndices == null || softIndices.length < icount) {
                
                softIndices = new int[icount];
                IntBuffer buf = CacheUtil.createIntBuffer(icount);
                soft.getIndiceData(buf);
                buf.get(softIndices);
            }
            
            RenderEntity ent = Ref.render.createEntity(REType.POLY);

            //ent.flags = RenderEntity.FLAG_NOLIGHT;
            ent.verts = new PolyVert[icount];
            ent.frame = icount;
            ent.oldframe = GL11.GL_TRIANGLES;
            for (int i = 0; i < icount; i++) {
                ent.verts[i] = new PolyVert();
                int index = softIndices[i];
                ent.verts[i].xyz = verts[index];
                if(normals != null) ent.verts[i].normal = normals[index];
            }

            Ref.render.addRefEntity(ent);
        }
    }
    
    private void handlePick() {
        if (pickConstraint != null && pickConstraint.isValid()) {
           // move the constraint pivot
           Point2PointConstraint p2p = (Point2PointConstraint) pickConstraint;
           if (p2p != null && pickedBody.isValid()) {
               // keep it at the same picking distance
               Vector3f origin = Ref.cgame.cg.refdef.Origin;
               Vector3f ray = Ref.cgame.cg.refdef.ViewAxis[0];
               Vector3f cameraPosition = new Vector3f(origin);
               cameraPosition.scale(SCALE_FACTOR);
               Vector3f rayTo = new Vector3f(ray);
               rayTo.scale(oldPickDist);
               Vector3f.add(cameraPosition, rayTo, rayTo);
               p2p.setPivotB(rayTo);
               
               RenderEntity ent = Ref.render.createEntity(REType.POLY);
               Transform t = pickedBody.getCenterOfMassTransform(null);
               
               
               t.toAxis(ent.axis);
               
               t.getOrigin(ent.origin);
               ent.origin.scale(INV_SCALE_FACTOR);
               ent.flags = RenderEntity.FLAG_NOLIGHT;
               ent.verts = new PolyVert[6];
               for (int i = 0; i < 6; i++) {
                   ent.verts[i] = new PolyVert();
               }
               float axisLen = 32f;
               ent.frame = 6;
               ent.oldframe = GL11.GL_LINES;
               ent.verts[0].xyz = ent.origin;
               ent.verts[1].xyz = Helper.VectorMA(ent.origin, axisLen, ent.axis[0], null);
               ent.verts[2].xyz = ent.origin;
               ent.verts[3].xyz = Helper.VectorMA(ent.origin, axisLen, ent.axis[1], null);
               ent.verts[4].xyz = ent.origin;
               ent.verts[5].xyz = Helper.VectorMA(ent.origin, axisLen, ent.axis[2], null);
               Ref.render.addRefEntity(ent);
           }
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
    
    public void removeBody(RigidBody body) {
        world.removeRigidBody(body);
    }
    
    public void deleteBody(SoftBody body) {
        world.removeSoftBody(body);
        body.destroy();
    }

    public RigidBody localCreateRigidBody(float mass, Vector3f startTransform, CollisionShape shape) {
        Matrix3f basis = new Matrix3f();
        basis.setIdentity();
        
        DirectMotionState myMotionState = new DirectMotionState(basis, startTransform);
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
    
    
    private ICommand pick_cmd = new ICommand() {
        public void RunCommand(String[] args) {
            boolean activate = !args[0].startsWith("-");
            handlePick(activate, Ref.cgame.cg.refdef.Origin, Ref.cgame.cg.refdef.ViewAxis[0]);
        }
    };
    
    private ICommand pick_testsoft = new ICommand() {
        public void RunCommand(String[] args) {
            if(soft != null) {
                deleteBody(soft);
                softIndices = null;
                softBuffer = null;
                soft = null;
            }
            Vector3f org = new Vector3f(Ref.cgame.cg.refdef.Origin);
            org.scale(SCALE_FACTOR);
            soft = SoftBody.createBall(world, org, 1, 512);
            
            short collisionFilterGroup =  CollisionObject.FILTER_DEFAULT;
            short collisionFilterMask = (short) (CollisionObject.FILTER_ALL);
            world.addSoftBody(soft, collisionFilterGroup, collisionFilterMask);
        }

        
    };


    RigidBody pickedBody = null;
    Point2PointConstraint pickConstraint = null;
    Vector3f oldPickOrigin = new Vector3f();
    float oldPickDist = 0f;
    
    private void handlePick(boolean activate, Vector3f origin, Vector3f ray) {
        if (activate) {
            Vector3f cameraPosition = new Vector3f(origin.x, origin.y, origin.z);
            cameraPosition.scale(SCALE_FACTOR);
            Vector3f rayTo = new Vector3f(ray.x, ray.y, ray.z);
            rayTo.scale(100f*SCALE_FACTOR);
            Vector3f.add(rayTo, cameraPosition, rayTo);
            
            // add a point to point constraint for picking
            if (world != null) {
                ClosestRayResult rayCallback = world.rayTestClosest(cameraPosition, rayTo);
                if (rayCallback.hasHit()) {
                    CollisionObject obj = rayCallback.getCollisionObject();
                    RigidBody body = null;
                    if(obj instanceof RigidBody)  body = (RigidBody)obj;
                    if (body != null) {
                        // other exclusions?
                        if (!(body.isStaticObject() || body.isKinematicObject())) {
                            pickedBody = body;
                            pickedBody.setActivationState(CollisionObject.ActivationStates.DISABLE_DEACTIVATION);
//                            pickedBody.setDamping(0.1f, 0.99f);
                            Vector3f delta = Vector3f.sub(rayTo, cameraPosition, null);
                            delta.scale(rayCallback.getHitFraction());
                            
                            Vector3f pickPos = Vector3f.add(cameraPosition, delta, null);
                            
                            Transform tmpTrans = body.getCenterOfMassTransform(null);
                            
                            tmpTrans.inverse();
                            Vector3f tmpPick = new Vector3f(pickPos);
                            tmpTrans.transform(tmpPick);
                            
                            Point2PointConstraint p2p = new Point2PointConstraint(world, body, tmpPick);
                            p2p.setImpulseClamp(10f);
                            p2p.setTau(0.1f);

                            world.addConstraint(p2p, false);
                            pickConstraint = p2p;
                            // save mouse position for dragging
                            oldPickOrigin.set(rayTo);
                            
                            Vector3f eyePos = new Vector3f(cameraPosition);
                            Vector3f tmp = new Vector3f();
                            Vector3f.sub(pickPos, eyePos, tmp);
                            oldPickDist = tmp.length();
                            // very weak constraint for picking
                        }
                    }
                }
            }

        } else {
            if (pickConstraint != null && world != null ) {
                if(pickConstraint.isValid()) {
                    world.removeConstraint(pickConstraint);
                }
                // delete m_pickConstraint;
                //printf("removed constraint %i",gPickingConstraintId);
                pickConstraint = null;
//                pickedBody.setDamping(0.1f, 0.2f);
                if(pickedBody != null && pickedBody.isValid()) {
                    pickedBody.forceActivationState(CollisionObject.ActivationStates.ACTIVE_TAG);
                    pickedBody.setDeactivationTime(1f);
                }
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
//        if(boxPoly == null) {
//            Vector3f[] boxverts = Helper.createBoxVerts(10, null);
//            boxPoly = new PolyVert[boxverts.length];
//            for (int i = 0; i < boxPoly.length; i++) {
//                boxPoly[i] = new PolyVert();
//                boxPoly[i].xyz = boxverts[i];
//            }
//        }
//        
//        
//                
//        Transform t = new Transform();
//        for (RigidBody rigidBody : bodies) {
//            //rigidBody.getWorldTransform(t);
//            MotionState mState = rigidBody.getMotionState();
//            mState.getWorldTransform(t);
//            
//            RenderEntity ent = Ref.render.createEntity(REType.POLY);
//            ent.flags |= RenderEntity.FLAG_SPRITE_AXIS;
//            ent.origin.set(t.origin.x, t.origin.y, t.origin.z);
//            Helper.matrixToAxis(t.basis, ent.axis);
////            ent.axis[0].set(1,0,0);
////            ent.axis[1].set(0,1,0);
////            ent.axis[2].set(0,0,1);
////            Vector3f dest = Helper.VectorMA(Ref.cgame.cg.refdef.Origin, 20f, Ref.cgame.cg.refdef.ViewAxis[0], null);
////            ent.origin.set(dest);
//
//            
//            
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
//            ent.verts = boxPoly;
//            ent.frame = boxPoly.length;
//
//            Ref.render.addRefEntity(ent);
//        }
    }
    
    

    public void shootBox(Vector3f origin, Vector3f dir, float force) {
        if (world != null) {

            LocalEntity lent = LocalEntity.physicsBox(origin, Ref.cgame.cg.time, 20000, Ref.ResMan.LoadTexture("data/textures/tile.png").asMaterial(), boxShape);
            Vector3f linVel = new Vector3f(dir.x, dir.y, dir.z);
            linVel.normalise();
            linVel.scale(force);

            lent.phys_body.setLinearVelocity(linVel);
            lent.phys_body.setAngularVelocity(new Vector3f(0f, 0f, 0f));
            lent.phys_body.setDamping(0.1f, 0.2f);
            
//            Vector3f org2 = Helper.VectorMA(origin, 100f, dir, null);
//            LocalEntity lent2 = LocalEntity.physicsBox(org2, Ref.cgame.cg.time, 21000, Ref.ResMan.LoadTexture("data/textures/tile.png").asMaterial(), boxShape);
////            Vector3f linVel2 = new Vector3f(dir.x, dir.y, dir.z);
////            linVel2.normalise();
////            linVel2.scale(force);
//
////            lent2.phys_body.setLinearVelocity(linVel2);
////            lent2.phys_body.setAngularVelocity(new Vector3f(0f, 0f, 0f));
////            lent2.phys_body.setDamping(0.1f, 0.2f);
//            
//            Transform a = new Transform();
//            a.origin.set(0.0f, 0.0f, 0.5f);
//            Transform b = new Transform();
//            b.origin.set(0.0f, 0.0f, -0.5f);
//            lent.phys_body.setCollisionFlags(RigidBody.CF_NO_CONTACT_RESPONSE);
//            Generic6DofConstraint cons = new Generic6DofConstraint(world, lent.phys_body, lent2.phys_body, a, b, true);
//            cons.setAngularLowerLimit(new Vector3f(-PhysicsSystem.SIMD_PI * 0.3f,-PhysicsSystem.SIMD_PI * 0.3f,-PhysicsSystem.SIMD_EPSILON));
//            cons.setAngularUpperLimit(new Vector3f(PhysicsSystem.SIMD_PI * 0.3f,PhysicsSystem.SIMD_PI * 0.3f,PhysicsSystem.SIMD_EPSILON));
//            world.addConstraint(cons, true);
//            bodies.add(lent.phys_body);
        }
    }

    void addChunk(CubeChunk chunk) {
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
        

////        chunk.physicsShape = localCreateRigidBody(0, tr, shape);
////        chunk.physicsShape.forceActivationState(RigidBody.DISABLE_SIMULATION);
//        chunk.physicsSystem = this; // a bit derp
    }


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
        
//    private void createCharacterProxy() {
//        Transform t = new Transform();
//        t.setIdentity();
//        
//        MotionState ms = new KinematicMotionState(t);
//        player = localCreateRigidKinematicBody(ms, characterShape);
//    }

    

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
//    private class KinematicMotionState extends MotionState {
//        Transform t;
//        private KinematicMotionState(Transform t) {
//            this.t = t;
//        }
//        @Override
//        public Transform getWorldTransform(Transform t) {
//            t.set(this.t);
//            return t;
//        }
//
//        @Override
//        public void setWorldTransform(Transform t) {
//            
//        }
//        
//    }
    
//    private void updatePlayerProxy(RigidBody body, PlayerState ps) {
//        Vector3f org = ps.origin;
//        
//        
//        KinematicMotionState ms =  (KinematicMotionState) body.getMotionState();
//        ms.t.origin.set(org.x, org.y, org.z);
//        ms.t.origin.scale(SCALE_FACTOR);
////        ms.t.origin.add(charHalfSize);
//        
//        javax.vecmath.Vector3f vel = new javax.vecmath.Vector3f(ps.velocity.x, ps.velocity.y, ps.velocity.z);
//        vel.scale(SCALE_FACTOR);
//        body.setLinearVelocity(vel);
//    }

    public PhysicsSystem getWorld() {
        return world;
    }
}
