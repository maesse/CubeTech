package cubetech.iqm;

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class BoneAttachment {

        public Vector3f lastposition = new Vector3f();
        public Vector3f[] axis = new Vector3f[] {new Vector3f(1,0,0), new Vector3f(0,1,0), new Vector3f(0,0,1)}; // ?
        public String name;
        public int boneIndex;
    
}
