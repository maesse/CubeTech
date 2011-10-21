package cubetech.iqm;

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class BoneController {
    public String boneName;
    public Vector3f boneAngles;
    
    public BoneController(String bonename, Vector3f angles) {
        this.boneName = bonename;
        this.boneAngles = angles;
    }

    
}
