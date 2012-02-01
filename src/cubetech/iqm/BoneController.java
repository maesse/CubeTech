package cubetech.iqm;

import cubetech.common.Helper;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class BoneController {
    public String boneName;
    private Vector3f boneAngles;
    private Matrix4f boneMatrix;
    public Type type;
    
    public enum Type {
        ABSOLUTE, // overrides the animated bone completely (ragdolls, etc.)
        ADDITIVE // adds to the blended animation (turning torso)
    }
    
    public BoneController(Type type, String bonename, Vector3f angles) {
        this.type = type;
        this.boneName = bonename;
        this.boneAngles = angles;
    }
    
    public BoneController(Type type, String bonename, Matrix4f boneMatrix) {
        this.type = type;
        this.boneName = bonename;
        this.boneMatrix = boneMatrix;
    }
    
    public Matrix4f getMatrix() {
        if(boneMatrix != null) return boneMatrix;
        
        Vector3f[] faxis = Helper.AnglesToAxis(boneAngles);
        Matrix4f rot = Helper.axisToMatrix(faxis, null);
        return rot;
    }

    
}
