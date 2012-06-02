package cubetech.CGame;



import cubetech.collision.CubeChunk;
import cubetech.collision.DefaultPhysics;
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
public class CGPhysics extends DefaultPhysics {
    // Shared vertex buffer for the physics-cubes
    BoxShape boxShape;
    public VBO vertexBuffer;
    
    SoftBody soft;
    int[] softIndices;
    FloatBuffer softBuffer;
    
    RigidBody pickedBody = null;
    Point2PointConstraint pickConstraint = null;
    Vector3f oldPickOrigin = new Vector3f();
    float oldPickDist = 0f;
    

    public CGPhysics() {
        super();

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
        
        world.stepPhysics(time, maxSubSteps, Ref.common.com_timescale.fValue / stepFrequency);
        
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
                            p2p.setImpulseClamp(100f);
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
            Vector3f origin = Helper.VectorMA(Ref.cgame.cg.cur_lc.predictedPlayerEntity.lerpOrigin, Ref.cgame.cg.cur_lc.predictedPlayerState.viewheight, new Vector3f(0, 0, 1), null);
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
        }
    }

}
