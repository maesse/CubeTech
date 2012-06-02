package cubetech.iqm;

import cubetech.CGame.ViewParams;
import cubetech.gfx.VBO;
import java.util.EnumMap;
import java.util.HashMap;
import cubetech.common.Animations;
import cubetech.gfx.CubeTexture;
import org.lwjgl.util.vector.Matrix3f;
import cubetech.common.Helper;
import cubetech.misc.Ref;
import java.util.ArrayList;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
/**
 *
 * @author mads
 */
public class IQMModel {
    IQMHeader header;
    String comment;
    IQMMesh[] meshes;
    IQMTriangle[] triangles;
    IQMJoint[] joints;
    IQMBounds[] bounds;

    // Temporary data
    IQMVertexArray[] vertexarrays;
    IQMAdjacency[] adjacency;
    int[] framedata;
    byte[] text;
    IQMPose[] poses;
    public IQMAnim[] anims;
    IQMExtension[] extensions;

    Matrix4f[] frames;
//    Matrix4f[] outframe;
    Matrix4f[] baseframe;
    Matrix4f[] invbaseframe;

    // Vertex data
    Vector3f[] in_position;
    Vector3f[] in_normal;
    Vector4f[] in_tangent;
    Vector2f[] in_texcoord;
    byte[] in_blendindex;
    byte[] in_blendweight;
    
    EnumMap<Animations, IQMAnim> animationMap = new EnumMap<Animations, IQMAnim>(Animations.class);
    Vector3f min = new Vector3f(-32,-32,-32);
    Vector3f max = new Vector3f(32,32,32);
    
    IQMJoint[] thighJoints = null;
    
    
    ShapeKeyCollection shapeKeys = null;
    

    // Joints that have attachment
    HashMap<String, IQMJoint> attachments = new HashMap<String, IQMJoint>();
    Vector3f[] jointPose; // joint positions when in pose
    
    // Known strings-to-bone lookups (eg. "Hips")
    public HashMap<String, IQMJoint> controllerMap = new HashMap<String, IQMJoint>();
    
    public ArrayList<BoneMeshInfo> boneMeshInfo = new ArrayList<BoneMeshInfo>();
    //HashMap<IQMMesh, IQMJoint> boneMeshes = new HashMap<IQMMesh, IQMJoint>();
    IQMFrame staticFrame = null; // if static model, cache the frame
    private Matrix4f tempMatrix = new Matrix4f();
    
    VBO cachedVertexBuffer = null;
    VBO cachedIndiceBuffer = null;
    boolean needsMultiPass = false; // can this model be rendered in one pass?

    IQMModel() {
        
    }
    
    public IQMAnim getAnimation(Animations animations) {
        return animationMap.get(animations);
    }

    public void destroy() {
//        envMap = null;
    }

    public boolean isStatic() {
        return anims == null || joints == null;
    }
    
//    public HashMap<IQMMesh, IQMJoint> getBoneMeshes() {
//        return boneMeshes;
//    } 
    
    public IQMJoint[] getJoints() {
        return joints;
    }
    
    private void blendAnimation(IQMFrame iqmFrame) {
        if(isStatic()) return; // nothing to animate
        int frame1 = iqmFrame.frame;
        int frame2 = iqmFrame.oldframe;
        float frameOffset = iqmFrame.backlerp;

        frame1 %= header.num_frames;
        frame2 %= header.num_frames;

        int mat1 = frame1 * header.num_joints;
        int mat2 = frame2 * header.num_joints;
        
        IQMJoint[] controllerJoints = null;
        if(iqmFrame.controllers != null) {
            controllerJoints = new IQMJoint[iqmFrame.controllers.size()];
            for (int i = 0; i < iqmFrame.controllers.size(); i++) {
                BoneController bCtrl = iqmFrame.controllers.get(i);
                controllerJoints[i] = controllerMap.get(bCtrl.boneName.toLowerCase());
            }
        }
        
        iqmFrame.outframe = new Matrix4f[header.num_joints];
        // Interpolate matrixes between the two closest frames and concatenate with parent matrix if necessary.
        // Concatenate the result with the inverse of the base pose.
        // You would normally do animation blending and inter-frame blending here in a 3D engine.
        Matrix4f temp = new Matrix4f();
        for (int i= 0; i < header.num_joints; i++) {
            Matrix4f m1 = frames[mat1+i];
            Matrix4f m2 = frames[mat2+i];

            if(iqmFrame.outframe[i] == null) {
                iqmFrame.outframe[i] = new Matrix4f();
            }
            Matrix4f dest = iqmFrame.outframe[i];
            
            // Find bone controller for this joint, if any
            BoneController ctrl = null;
            if(controllerJoints != null) {
                for (int j = 0; j < controllerJoints.length; j++) {
                    if(controllerJoints[j] == null) continue;
                    if(joints[i] != controllerJoints[j]) continue;
                    if(ctrl == null || ctrl.type == BoneController.Type.ADDITIVE) {
                        // There may be more than one controller for one bone.
                        // could be better.
                        ctrl = iqmFrame.controllers.get(j);
                    }
                }
            }
            
            if(ctrl != null && ctrl.type == BoneController.Type.ABSOLUTE) {
                // got an absolute controller, load it directly
                dest.load(ctrl.getMatrix());
            } else {
                // Do normal blended animation
                Helper.scale((1-frameOffset), m1, dest);
                Helper.scale((frameOffset), m2, temp);
                Matrix4f.add(dest, temp, dest);
                dest.m33 = 1;
                
                // check for additive bonecontroller
                if(ctrl != null && ctrl.type == BoneController.Type.ADDITIVE) {
                    Matrix4f m = Matrix4f.mul(ctrl.getMatrix(), joints[i].invbaseframe, null);
                    Matrix4f.mul(joints[i].baseframe, m, m);
                    Matrix4f.mul(m, dest, dest);
                }
                if(joints[i].parent >= 0) {
                    Matrix4f.mul(iqmFrame.outframe[joints[i].parent], dest, dest);
                }
            }
        }

        
    }
    
    
    
    public IQMFrame buildFrame(int frame, int oldframe, float backlerp, ArrayList<BoneController> controllers) {
        // Use cached frame if it's a static model
        if(isStatic() && staticFrame != null) {
            return staticFrame;
        }
        
        IQMFrame iqmFrame = new IQMFrame(this, frame, oldframe, backlerp);
        iqmFrame.controllers = controllers;
        iqmFrame.needMultiPass = needsMultiPass;
        
        blendAnimation(iqmFrame);
        if(isStatic()) staticFrame = iqmFrame;
        return iqmFrame;
    }

//    public void animate(int frame, int oldframe, float backlerp) {
////        if(isStatic()) {
////            if(staticModelVB == null) {
////                createStaticBuffer();
////            }
////            return;
////        }
////
////        if(gpuskinning && shapeKeys == null) {
////            createDynamicBuffer();
////            return;
////        }
////
////        if(shapeKeys != null) {
////            for (int i= 0; i < in_position.length; i++) {
////                if(out_position[i] == null) out_position[i] = new Vector3f();
////                out_position[i].set(in_position[i]);
////            }
////            shapeKeys.setShapeKey("Key 1", (float) Math.abs(Math.sin(Ref.client.realtime/576f)));
////            shapeKeys.setShapeKey("Key 2", (float) Math.abs(Math.sin(Ref.client.realtime/1000f)));
////            shapeKeys.applyShapes(out_position);
////        }
//    }

    

    

    public Vector3f getMins() {
        return min;
    }

    public Vector3f getMaxs() {
        return max;
    }
    
    public IQMMesh getMesh(String name) {
        for (IQMMesh iQMMesh : meshes) {
            if(iQMMesh.name.equalsIgnoreCase(name)) return iQMMesh;
        }
        return null;
    }
   
}

