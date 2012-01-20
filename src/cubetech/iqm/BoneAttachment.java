package cubetech.iqm;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class BoneAttachment {
    public String name;
    public int boneIndex;
    
    public Vector4f lastposition = new Vector4f(0,0,0,1);
    public Vector3f[] axis = new Vector3f[] {
        new Vector3f(1,0,0), 
        new Vector3f(0,1,0), 
        new Vector3f(0,0,1)};

    public Vector3f getAttachment(String attachPoint, Vector3f dest) {
        if(dest == null) dest = new Vector3f();
        dest.set(lastposition);
        return dest;
    }
    
}
