package cubetech.CGame;

import cubetech.iqm.BoneController;
import cubetech.iqm.IQMModel;
import cubetech.iqm.RigidBoneMesh;
import java.util.ArrayList;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class PlayerEntity {
    public LerpFrame torso = new LerpFrame();
    public LerpFrame legs = new LerpFrame();
    public LerpFrame fpsweapon = new LerpFrame();
    public int lastRenderFrame;
    
    // Physics simulated bones
    public ArrayList<RigidBoneMesh> boneMeshes;
    public IQMModel boneMeshModel;
    public int lastcontents;
    public boolean isRagdoll = false;
    public Vector3f[] lastCameraAngles = null;
    public Vector4f lastCameraOrigin = null;
    
}
