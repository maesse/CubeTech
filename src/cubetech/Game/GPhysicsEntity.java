package cubetech.Game;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;
import cubetech.common.Common;
import cubetech.common.Quaternion;
import cubetech.common.Trajectory;
import cubetech.common.items.IItem;
import cubetech.common.items.ItemType;
import cubetech.common.items.WeaponInfo;
import cubetech.common.items.WeaponItem;
import cubetech.entities.EntityType;
import cubetech.iqm.IQMMesh;
import cubetech.iqm.IQMModel;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Mads
 */
public class GPhysicsEntity extends Gentity {
    private static final javax.vecmath.Vector3f tmpVec = new javax.vecmath.Vector3f();
    public RigidBody physicsBody;
    public DefaultMotionState motionState;
    
    @Override
    public void runItem() {
        updateFromPhysics();
        Link();
        runThink();
    }
    
    @Override
    public void Clear() {
        super.Clear();
        physicsBody = null;
    }
    
    @Override
    public void Free() {
        super.Free();
        if(physicsBody != null) clearPhysicsBody();
    }
    
    private ObjectArrayList<javax.vecmath.Vector3f> getDataFromMesh(IQMMesh mesh) {
        ObjectArrayList<javax.vecmath.Vector3f> triangles = new ObjectArrayList<javax.vecmath.Vector3f>();
        Vector3f[] unScaledTriangles = mesh.getVertices();
        for (int i = 0; i < unScaledTriangles.length; i++) {
            triangles.add(PhysicsSystem.toScaledVecmath(unScaledTriangles[i], null));
        }
        return triangles;
    }
    
    private void initPhysicsBody() {
        if(!physicsObject) return;
        // Use bbox size from model if possible
        IQMModel model = getModel();
        boolean hasConvexMesh = false;
        
        if(model != null) {
            r.mins.set(model.getMins());
            r.maxs.set(model.getMaxs());
            Link();
            hasConvexMesh = model.getMesh("@convexcollision") != null;
            
        }
        
        CollisionShape shape = null;
        
        float mass = 0f;
        Vector3f halfSize = Vector3f.sub(r.maxs, r.mins, null);
        halfSize.scale(0.5f);

        // Origin transform
        Transform t = new Transform();
        t.setIdentity();
        PhysicsSystem.toScaledVecmath(r.currentOrigin, t.origin);

        // Center of mass offset
        Transform centerOfMass = new Transform();
        centerOfMass.setIdentity();
        
//        PhysicsSystem.toScaledVecmath((Vector3f)Vector3f.add(r.mins, halfSize, null).scale(-1f), centerOfMass.origin);

        motionState = new DefaultMotionState(t, centerOfMass);
        mass = (halfSize.x*halfSize.y*halfSize.z) / 200f;
        if(hasConvexMesh) {
            IQMMesh mesh = model.getMesh("@convexcollision");
            ObjectArrayList<javax.vecmath.Vector3f> hulldata = getDataFromMesh(mesh);
            shape = new ConvexHullShape(hulldata);
        } else {
            shape = new BoxShape(PhysicsSystem.toScaledVecmath(halfSize, null));          
        }
        
        physicsBody = Ref.game.level.physics.localCreateRigidBody(mass, motionState, shape);
    }
    
    private void clearPhysicsBody() {
        if(physicsBody == null) return;
        Ref.game.level.physics.deleteBody(physicsBody);
        physicsBody = null;
    }
    
    // Read position from physics system
    public void updateFromPhysics() {
        if(physicsBody == null) initPhysicsBody();
        
        Transform t = motionState.graphicsWorldTrans;
        PhysicsSystem.toUnscaledVec(t.origin, r.currentOrigin);
        s.pos.type = Trajectory.INTERPOLATE;
        s.pos.base.set(r.currentOrigin);
        s.pos.time = Ref.game.level.time;
        s.apos.type = Trajectory.QUATERNION;
        s.apos.time = Ref.game.level.time;
        Quaternion q = new Quaternion();
        
        q.setFromMatrix(t.basis);
        //s.apos.base.set(Helper.AxisToAngles(t.basis, null));
        s.apos.quater.set(q);
        s.apos.quater.normalise();
    }
    
    public void pushToPhysics(Vector3f position) {
        if(physicsBody == null) return;
        Transform t = new Transform();
        t.setIdentity();
        PhysicsSystem.toScaledVecmath(position, t.origin);
        physicsBody.getMotionState().setWorldTransform(t);
    }
}
