package cubetech.iqm;



import cubetech.CGame.CEntity;
import cubetech.CGame.CGPhysics;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import nbullet.PhysicsSystem;
import nbullet.collision.shapes.BoxShape;
import nbullet.collision.shapes.CollisionShape;
import nbullet.collision.shapes.ConvexHullShape;

import nbullet.constraints.Generic6DofConstraint;
import nbullet.constraints.Generic6DofSpringConstraint;
import nbullet.objects.CollisionObject;
import nbullet.objects.RigidBody;
import nbullet.util.DirectMotionState;
import nbullet.util.Transform;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.*;

/**
 *
 * @author Mads
 */
public class RigidBoneMesh {
    private IQMModel model;
    private IQMMesh mesh;
    private IQMJoint joint;
    private Vector3f[] poseVertices; // base pose
    private Matrix4f modelToBone;
    public RigidBody rigidBody;
    public BoneMeshInfo info;
    private BoneMeshInfo originalInfo;
    private boolean boneToPhysics = true;
    
    private PhysicsSystem system;

    // For dynamic bonemeshes connected to something
    private boolean connectedToParent = false;
    private RigidBoneMesh parentBoneMesh = null;
    private Generic6DofSpringConstraint springCons;
    private Generic6DofConstraint cons;
    
    public RigidBoneMesh(PhysicsSystem system, IQMModel model, BoneMeshInfo info) {
        this.system = system;
        this.info = info;
        originalInfo = info.clone(null);
        this.mesh = info.mesh;
        this.joint = info.joint;
        this.model = model;
        generateLocalMesh();
    }
    
    public void restoreInfo(CEntity cent) {
        changeInfo(originalInfo.clone(null), cent);
    }
    
    public BoneMeshInfo getOriginalInfo() {
        return originalInfo;
    }
    
    
    
    public void changeInfo(BoneMeshInfo info, CEntity cent) {
        if(this.info.equals(info)) return;
        BoneMeshInfo oldinfo = this.info;
        this.info = info;
        
        RigidBoneMesh parentMesh = parentBoneMesh;
        if(oldinfo.type != BoneMeshInfo.Type.RIGID && connectedToParent) {
            // Need to remove the old constraint
            removeConnection();
        }
        
        if(info.type != BoneMeshInfo.Type.RIGID) {
            if(parentMesh != null) {
                // Connect to parent (again)
                connectToParent(parentMesh);
            } else if(cent != null) {
                connectToParent(cent);
            } else {
                Common.Log("RigidBoneMesh.changeInfo: Can't build constraint for %s because ");
            }
        }
    }
    
    private static RigidBoneMesh findParent(RigidBoneMesh src, CEntity cent) {
        int parentid = src.getJoint().getParent();
        
        
        IQMJoint[] joints = cent.pe.boneMeshModel.getJoints();
        
        // Check for parent joint override in the bonemesh info
        String parentOverride = src.info.parentBoneMeshJoint;
        if(parentOverride != null) {
            // Try to locate the joint
            IQMJoint j = src.model.controllerMap.get(parentOverride.toLowerCase());
            if(j != null) parentid = j.index;
        }
        
        if(parentid < 0) return null;
        
        // Traverse the bone hierachy (child->parent) until
        // a joint with a bonemesh is found or the end of the skeleton is reached
        RigidBoneMesh found = null;
        IQMJoint currentParent = null;
        do
        {
            currentParent = joints[parentid];

            // Check if there's a meshbone for this joint
            for (RigidBoneMesh mesh : cent.pe.boneMeshes) {
                if(mesh.getJoint() == currentParent) {
                    // Found it
                    found = mesh;
                    break;
                }
            }
            
            // Continue with next parent
            if(found == null) parentid = currentParent.getParent();
        } while(found == null && parentid >= 0);
        
        return found;
    }
    
    public void createRigidBody(Matrix4f modelMatrix, IQMFrame frame) {
        // create collisionshape
        CollisionShape shape = getLocalShape();

        // Set up a motionstate so the mesh matches the posed position
        Matrix4f poseMatrix = getPosedModelToBone(frame, null);
        modelMatrix = Matrix4f.mul(modelMatrix, poseMatrix, null);
        Helper.transposeAxis(modelMatrix);
        modelMatrix.m30 *= CGPhysics.SCALE_FACTOR;
        modelMatrix.m31 *= CGPhysics.SCALE_FACTOR;
        modelMatrix.m32 *= CGPhysics.SCALE_FACTOR;
        DirectMotionState ms = new DirectMotionState(modelMatrix);
        
        if(boneToPhysics) {
            rigidBody = new RigidBody(0, ms, shape);
            rigidBody.setCollisionFlags(RigidBody.CF_KINEMATIC_OBJECT);
            rigidBody.setActivationState(CollisionObject.ActivationStates.DISABLE_DEACTIVATION);
            system.addRigidBody(rigidBody);
        } else {
            
            rigidBody = Ref.cgame.physics.localCreateRigidBody(info.mass, ms, shape);
//            rigidBody.setAngularVelocity(new Vector3f());
//            rigidBody.setLinearVelocity(new Vector3f());
        }
    }
    
    public void connectToParent(RigidBoneMesh other) {
        setBoneToPhysics(false);
        if(connectedToParent) {
            if(parentBoneMesh == other) return; // already connected
            removeConnection();
        }
        Vector3f boneOrigin = new Vector3f(joint.jointDelta);
        
        // Set up body a
        Vector3f meshOrigin = new Vector3f(other.modelToBone.m30, other.modelToBone.m31, other.modelToBone.m32);
        Vector3f aLocalTrans = Vector3f.sub(meshOrigin, boneOrigin, null);
        aLocalTrans.scale(-CGPhysics.SCALE_FACTOR);
        RigidBody bodya = other.rigidBody;
        Transform a = new Transform();
        a.origin.set(aLocalTrans);
        
        // Set up body b
        meshOrigin = new Vector3f(modelToBone.m30, modelToBone.m31, modelToBone.m32);
        Vector3f bLocalTrans = Vector3f.sub(meshOrigin, boneOrigin, null);
        bLocalTrans.scale(-CGPhysics.SCALE_FACTOR);
        RigidBody bodyb = rigidBody;
        Transform b = new Transform();
        b.origin.set(bLocalTrans);
        
        if(info.type == BoneMeshInfo.Type.SPRING) {
            springCons = new Generic6DofSpringConstraint(system, bodya, bodyb, a, b, true);
            springCons.setAngularLowerLimit(new Vector3f(PhysicsSystem.SIMD_PI * info.rminx,PhysicsSystem.SIMD_PI * info.rminy,PhysicsSystem.SIMD_PI * info.rminz));
            springCons.setAngularUpperLimit(new Vector3f(PhysicsSystem.SIMD_PI  * info.rmaxx,PhysicsSystem.SIMD_PI  * info.rmaxy,PhysicsSystem.SIMD_PI  * info.rmaxz));
            
            if(info.springx) {
                springCons.enableSpring(Generic6DofSpringConstraint.SpringIndex.RX, true);
                springCons.setStiffness(Generic6DofSpringConstraint.SpringIndex.RX, info.sforcex);
                springCons.setDamping(Generic6DofSpringConstraint.SpringIndex.RX, info.sdampingx);
                springCons.setEquilibriumPoint(Generic6DofSpringConstraint.SpringIndex.RX, 0f);
            }
            
            if(info.springy) {
                springCons.enableSpring(Generic6DofSpringConstraint.SpringIndex.RY, true);
                springCons.setStiffness(Generic6DofSpringConstraint.SpringIndex.RY, info.sforcey);
                springCons.setDamping(Generic6DofSpringConstraint.SpringIndex.RY, info.sdampingy);
                springCons.setEquilibriumPoint(Generic6DofSpringConstraint.SpringIndex.RY, 0f);
            }
            
            if(info.springz) {
                springCons.enableSpring(Generic6DofSpringConstraint.SpringIndex.RZ, true);
                springCons.setStiffness(Generic6DofSpringConstraint.SpringIndex.RZ, info.sforcez);
                springCons.setDamping(Generic6DofSpringConstraint.SpringIndex.RZ, info.sdampingz);
                springCons.setEquilibriumPoint(Generic6DofSpringConstraint.SpringIndex.RZ, 0f);
            }
            
            system.addConstraint(springCons, true);
        } else if(info.type == BoneMeshInfo.Type.FLEXIBLE) {
            cons = new Generic6DofConstraint(system, bodya, bodyb, a, b, true);
            cons.setAngularLowerLimit(new Vector3f(PhysicsSystem.SIMD_PI * info.rminx,PhysicsSystem.SIMD_PI * info.rminy,PhysicsSystem.SIMD_PI * info.rminz));
            cons.setAngularUpperLimit(new Vector3f(PhysicsSystem.SIMD_PI  * info.rmaxx,PhysicsSystem.SIMD_PI  * info.rmaxy,PhysicsSystem.SIMD_PI  * info.rmaxz));
            system.addConstraint(cons, true);
        }
        
        parentBoneMesh = other;
        connectedToParent = true;
        
        rigidBody.setDamping(0.1f, 0.5f);
    }
    
    private void removeConnection() {
        connectedToParent = false;
        parentBoneMesh = null;
        
        // Clean up constraint
        if(springCons != null) {
            system.removeConstraint(springCons);
            springCons.destroy(true);
            springCons = null;
        }
        
        if(cons != null) {
            system.removeConstraint(cons);
            cons.destroy(true);
            cons = null;
        }
    }
    
    public void setBoneToPhysics(boolean istrue) {
        if(boneToPhysics == istrue) {
            return;
        } // no change
        if(rigidBody != null) {
            system.removeRigidBody(rigidBody);
            if(!istrue) {
                // Go from kinematic to rigid
                rigidBody.setCollisionFlags(0);
                rigidBody.setActivationState(CollisionObject.ActivationStates.ACTIVE_TAG);
                float mass = 20f;
                Vector3f inertia = new Vector3f();
                rigidBody.getCollisionShape().calcLocalInertial(mass, inertia);
                rigidBody.setMassProps(mass, inertia);
                rigidBody.updateInertiaTensor();
                rigidBody.setAngularVelocity(new Vector3f());
                rigidBody.setLinearVelocity(new Vector3f());
                
            } else {
                // remove previous constraint
                if(connectedToParent) {
                    removeConnection();
                }
                // go from rigid to kinematic
                rigidBody.setCollisionFlags(RigidBody.CF_KINEMATIC_OBJECT);
                rigidBody.setActivationState(CollisionObject.ActivationStates.DISABLE_DEACTIVATION);
                rigidBody.setMassProps(0, new Vector3f());
                rigidBody.updateInertiaTensor();
                rigidBody.setAngularVelocity(new Vector3f());
                rigidBody.setLinearVelocity(new Vector3f());
                rigidBody.setDamping(0f, 0f);
                
            }
            system.addRigidBody(rigidBody);
        }
        boneToPhysics = istrue;
    }
    
    public boolean isBoneToPhysics() {
        return boneToPhysics;
    }
    
    public ConvexHullShape getLocalShape() {
        ByteBuffer b = BufferUtils.createByteBuffer(4*3*poseVertices.length);
        b.order(ByteOrder.nativeOrder());
        for (int i = 0; i < poseVertices.length; i++) {
            b.putFloat(poseVertices[i].x * CGPhysics.SCALE_FACTOR);
            b.putFloat(poseVertices[i].y * CGPhysics.SCALE_FACTOR);
            b.putFloat(poseVertices[i].z * CGPhysics.SCALE_FACTOR);
        }
        b.flip();
        ConvexHullShape shape = new ConvexHullShape(b, poseVertices.length, 4*3);
        return shape;
    }

    // Returns the vertices making up the mesh, aligned to the current
    // animation, in modelspace.
    public Vector3f[] poseMesh(IQMFrame frame) {
        Matrix4f trans = getPosedModelToBone(frame, null);

        Vector3f[] verts = new Vector3f[poseVertices.length];
        for (int i = 0; i < verts.length; i++) {
            verts[i] = Helper.transform(trans, poseVertices[i], verts[i]);
        }

        return verts;
    }

    private void generateLocalMesh() {
        poseVertices = getStaticVertices(mesh);
        poseVertices = removeDuplicates(poseVertices, 0.001f);
        Vector3f offset = makeLocalBoneMesh(poseVertices, poseVertices);

        modelToBone = Helper.getModelMatrix(new Vector3f[] {
            new Vector3f(1,0,0), 
            new Vector3f(0,1,0), 
            new Vector3f(0,0,1)}, 
                offset, null);
    }

    // Finds the vertex center and makes it the new origin of the mesh
    // eg. [2,4,6] -> [-2,0,2] + 4
    private static Vector3f makeLocalBoneMesh(Vector3f[] vertices, Vector3f[] dest) {
        if(dest == null) dest = new Vector3f[vertices.length];
        Vector3f center = new Vector3f();
        float count = vertices.length;
        for (int i = 0; i < vertices.length; i++) {
            Helper.VectorMA(center, 1f/count, vertices[i], center);
        }

        // now theres a center of mass which wil be the new model origin
        for (int i = 0; i < vertices.length; i++) {
            if(dest[i] == null) dest[i] = new Vector3f();
            Vector3f.sub(center, vertices[i], dest[i]);
        }

        return center;
    }

    // modelspace static mesh
    private Vector3f[] getStaticVertices(IQMMesh mesh) {
        int first = mesh.first_vertex;
        int count = mesh.num_vertexes;
        Vector3f[] vertices = new Vector3f[count];
        for (int i = 0; i < count; i++) {
            vertices[i] = new Vector3f(model.in_position[first+i]);
        }
        return vertices;
    }
    
    private Vector3f[] removeDuplicates(Vector3f[] input, float epsilon) {
        double sqEpsilon = epsilon*epsilon;
        boolean[] ok = new boolean[input.length];
        int okCount = 0;
        for (int i = 0; i < input.length; i++) {
            // vertex to cull
            Vector3f v = input[i];
            
            boolean vOk = true;
            // if some later vertex has this vertice
            // don't set this one to ok
            for (int j = i+1; j < input.length; j++) {
                Vector3f v2 = input[j];
                float dx,dy,dz;
                dx = v.x - v2.x;
                dy = v.y - v2.y;
                dz = v.z - v2.z;
                float sqLen = dx * dx + dy * dy + dz * dz;
                if(sqLen <= sqEpsilon) {
                    // Cull
                    vOk = false;
                    break;
                }
            }
            ok[i] = vOk;
            if(vOk) okCount++;
        }
        
        // Create cleaned array
        Vector3f[] culled = new Vector3f[okCount];
        int cullIndex = 0;
        for (int i = 0; i < input.length; i++) {
            if(!ok[i]) continue;
            culled[cullIndex++] = input[i];
        }
        
        return culled;
    }
    
    public IQMModel getModel() {
        return model;
    }
    
    public IQMJoint getJoint() {
        return joint;
    }
    
    public IQMMesh getMesh() {
        return mesh;
    }
    
    public Vector3f[] getLocalMesh() {
        return poseVertices;
    }
    
    public Matrix4f getModelToBoneMatrix() {
        return modelToBone;
    }
    
    public Matrix4f getBoneFromPosedModelMatrix(Matrix4f src, Matrix4f dest) {
        Matrix4f invModelToBone = (Matrix4f) new Matrix4f(modelToBone).invert();
        dest = Matrix4f.mul(src, invModelToBone, dest);
        return dest;
    }
    
    public Matrix4f getPosedModelToBone(IQMFrame frame, Matrix4f dest) {
        Matrix4f boneMatrix = new Matrix4f(frame.outframe[joint.index]);
        dest = Matrix4f.mul(boneMatrix, modelToBone, dest);
        return dest;
    }
    
    public boolean hasBoneMeshParent(CEntity cent) {
        RigidBoneMesh found = findParent(this, cent);
        return found != null;
    }

    public void connectToParent(CEntity cent) {
        RigidBoneMesh found = findParent(this, cent);
        if(found != null) {
            connectToParent(found);
        } else if(getJoint().getIndex() != 0) {
            Common.Log("Model missing base bone mesh");
        }
    }
}
