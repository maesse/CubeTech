package cubetech.iqm;

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class BoneAttachment {

        Vector3f lastposition = new Vector3f();
        Vector3f[] axis = new Vector3f[] {new Vector3f(1,0,0), new Vector3f(0,1,0), new Vector3f(0,0,1)}; // ?
        String name;
        int boneIndex;
    
}
