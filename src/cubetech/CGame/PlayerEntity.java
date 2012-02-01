package cubetech.CGame;

import cubetech.iqm.IQMModel;
import cubetech.iqm.RigidBoneMesh;
import java.util.ArrayList;

/**
 *
 * @author mads
 */
public class PlayerEntity {
    public LerpFrame torso = new LerpFrame();
    public LerpFrame legs = new LerpFrame();
    
    // Physics simulated bones
    public ArrayList<RigidBoneMesh> boneMeshes;
    public IQMModel boneMeshModel;
    public int lastcontents;
}
