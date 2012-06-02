package cubetech.Game;

import cubetech.collision.DefaultPhysics;
import cubetech.common.Quaternion;
import cubetech.common.Trajectory;
import cubetech.iqm.IQMMesh;
import cubetech.iqm.IQMModel;
import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import nbullet.collision.shapes.BoxShape;
import nbullet.collision.shapes.CollisionShape;
import nbullet.collision.shapes.ConvexHullShape;
import nbullet.objects.RigidBody;
import nbullet.util.DirectMotionState;
import nbullet.util.Transform;
import org.lwjgl.util.mapped.CacheUtil;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Mads
 */
public class GPhysicsEntity extends Gentity {
    public RigidBody physicsBody;
    public Transform centerOfMass;
    
    @Override
    public void runThink() {
        updateFromPhysics();
        if(r.linked) Link(); // relink
        super.runThink();
    }
    
    @Override
    public void Clear() {
        super.Clear();
        if(physicsBody != null) clearPhysicsBody();
    }
    
    @Override
    public void Free() {
        super.Free();
        if(physicsBody != null) clearPhysicsBody();
    }
    
    private void initPhysicsBody() {
        // Use bbox size from model if possible
        IQMModel model = getModel();
        IQMMesh convexMesh = null;
        
        if(model != null) {
            r.mins.set(model.getMins());
            r.maxs.set(model.getMaxs());
            Link(); // ?? fix
            convexMesh = model.getMesh("@convexcollision");
            if(convexMesh == null) convexMesh = model.getMesh("@bbox");
        }
        
        CollisionShape shape = null;
        
        float mass = 0f;
        Vector3f halfSize = Vector3f.sub(r.maxs, r.mins, null);
        halfSize.scale(0.5f);
        
        Vector3f offset = Vector3f.add(r.mins, halfSize, null);
        offset.scale(DefaultPhysics.SCALE_FACTOR);

        // Origin transform
        Transform t = new Transform();
        t.origin.set(r.currentOrigin).scale(DefaultPhysics.SCALE_FACTOR);

        // Center of mass offset        
//        PhysicsSystem.toScaledVecmath((Vector3f)Vector3f.add(r.mins, halfSize, null).scale(-1f), centerOfMass.origin);

        centerOfMass = new Transform(offset);
        mass = (halfSize.x*halfSize.y*halfSize.z) / 500f;
        if(convexMesh != null) {
            Vector3f[] vertices = convexMesh.getLocalMesh(centerOfMass);
            for (int i = 0; i < vertices.length; i++) {
                vertices[i].scale(DefaultPhysics.SCALE_FACTOR);
            }
            centerOfMass.origin.scale(-DefaultPhysics.SCALE_FACTOR);
            ByteBuffer b = verticesAsByteBuffer(vertices);
            shape = new ConvexHullShape(b, vertices.length, 4*3);
        } else {
            Vector3f.add(t.origin, offset, t.origin);
            halfSize.scale(DefaultPhysics.SCALE_FACTOR);
            shape = new BoxShape(halfSize);
        }
        
        DirectMotionState motionState = new DirectMotionState(t.basis, t.origin, centerOfMass);
        physicsBody = Ref.game.level.physics.localCreateRigidBody(mass, motionState, shape);
        
    }
    
    // Packs the verts into 32byte aligned chunks
    private static ByteBuffer verticesAsByteBuffer(Vector3f[] verts) {
        ByteBuffer b = CacheUtil.createByteBuffer(verts.length * 3 * 4);
        b.order(ByteOrder.nativeOrder());
        for (int i = 0; i < verts.length; i++) {
            b.putFloat(verts[i].x);
            b.putFloat(verts[i].y);
            b.putFloat(verts[i].z);
        }
        b.flip();
        return b;
    }
    
    private void clearPhysicsBody() {
        if(physicsBody == null) return;
        
        CollisionShape shape = physicsBody.getCollisionShape();
        Ref.game.level.physics.deleteBody(physicsBody);
        shape.destroy();
        
        physicsBody = null;
    }
    
    // Read position from physics system
    public void updateFromPhysics() {
        if(physicsBody == null) initPhysicsBody();
        
        DirectMotionState ms = physicsBody.getMotionState();
        Matrix4f msTrans = new Matrix4f();
        ms.getCurrentState(msTrans);
        
        // Set rotation
        s.apos.type = Trajectory.QUATERNION;
        s.apos.time = Ref.game.level.time;
        s.apos.quater = Quaternion.setFromMatrix(msTrans, s.apos.quater);
        s.apos.quater.normalise();
        
        // Set origin
        ms.getOrigin(r.currentOrigin).scale(DefaultPhysics.INV_SCALE_FACTOR);
        s.pos.type = Trajectory.INTERPOLATE;
        s.pos.base.set(r.currentOrigin);
        s.pos.time = Ref.game.level.time;
    }
    
    public void pushToPhysics(Vector3f position) {
        if(physicsBody == null) return;
        physicsBody.getMotionState().setWorldOrigin(position);
    }
}
