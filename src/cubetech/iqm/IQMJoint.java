package cubetech.iqm;

import cubetech.gfx.Matrix3x4;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class IQMJoint {
    String name;
    int index;
    int parent; // parent < 0 means this is a root bone
    float[] translate = new float[3], rotate = new float[4], scale = new float[3];
    Matrix4f baseframe;
    Matrix4f invbaseframe;
    Matrix3x4 mm;
    Vector4f jointDelta;
    // translate is translation <Tx, Ty, Tz>, and rotate is quaternion rotation <Qx, Qy, Qz, Qw> where Qw = -sqrt(max(1 - Qx*Qx - Qy*qy - Qz*qz, 0))
    // rotation is in relative/parent local space
    // scale is pre-scaling <Sx, Sy, Sz>
    // output = (input*scale)*rotation + translation
    
    public String getName() {
        return name;
    }
    
    public Vector3f getJointOrigin(Vector3f dest) {
        if(dest == null) dest = new Vector3f();
        dest.set(jointDelta.x, jointDelta.y, jointDelta.z);
        return dest;
    }
    
    public int getIndex() {
        return index;
    }
    
    public int getParent() {
        return parent;
    }

    static void loadJoints(IQMModel model, ByteBuffer buffer) {
        if(model.header.num_joints == 0 || model.header.ofs_joints == 0) return;

        buffer.position(model.header.ofs_joints);
        model.joints = new IQMJoint[model.header.num_joints];
        model.baseframe = new Matrix4f[model.header.num_joints];
        model.invbaseframe = new Matrix4f[model.header.num_joints];
        for (int i= 0; i < model.header.num_joints; i++) {
            IQMJoint j = new IQMJoint();
            j.index = i;
            int name = buffer.getInt();
            if(name >= 0) {
                int end = name;
                while(end < model.text.length && model.text[end] != '\0') end++;
                try {
                    j.name = new String(model.text, name, end-name, "US-ASCII");
                } catch (UnsupportedEncodingException ex) {
                }
            }
            j.parent = buffer.getInt();
            j.translate[0] = buffer.getFloat();
            j.translate[1] = buffer.getFloat();
            j.translate[2] = buffer.getFloat();
            j.rotate[0] = buffer.getFloat();
            j.rotate[1] = buffer.getFloat();
            j.rotate[2] = buffer.getFloat();
            j.rotate[3] = buffer.getFloat();
            j.scale[0] = buffer.getFloat();
            j.scale[1] = buffer.getFloat();
            j.scale[2] = buffer.getFloat();

            model.joints[i] = j;
            
            Quaternion q = new Quaternion(j.rotate[0], j.rotate[1], j.rotate[2], j.rotate[3]);
            q.normalise();
            Matrix3x4 mm = new Matrix3x4(q, new Vector3f(j.scale[0], j.scale[1], j.scale[2]), new Vector3f(j.translate[0], j.translate[1], j.translate[2]));
            j.mm = mm;
            Matrix4f m = new Matrix4f();
            
            mm.toMatrix4f(m);
            
            model.baseframe[i] = m;
            j.baseframe = m;

            Matrix4f n = new Matrix4f();
            Matrix4f.invert(m, n);

            model.invbaseframe[i] = n;
            j.invbaseframe = n;

            if(j.parent >= 0) {
                Matrix4f.mul(model.baseframe[j.parent], model.baseframe[i], model.baseframe[i]);
                Matrix4f.mul(model.invbaseframe[i], model.invbaseframe[j.parent], model.invbaseframe[i]);
            }
            
            j.jointDelta = new Vector4f(0, 0, 0, 1);
            Matrix4f.transform(m, j.jointDelta, j.jointDelta);
        }
    }
}
