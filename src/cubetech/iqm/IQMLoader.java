package cubetech.iqm;

import cubetech.common.Animations;
import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.common.Helper;
import cubetech.gfx.ResourceManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * http://lee.fov120.com/iqm
 * // IQM: Inter-Quake Model format
   // version 1: April 20, 2010
 */
public class IQMLoader {
    public static IQMModel LoadModel(String file) throws IOException {
        ByteBuffer buffer = ResourceManager.OpenFileAsByteBuffer(file, true).getKey();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        IQMModel model = new IQMModel();
        IQMHeader.loadHeader(model, buffer, file);
        
        loadText(model, buffer);
        IQMMesh.loadMesh(model, buffer);
        IQMVertexArray.loadArrays(model, buffer);
        IQMTriangle.loadTriangles(model, buffer);
        IQMAdjacency.loadAdjacency(model, buffer);
        IQMJoint.loadJoints(model, buffer);
        IQMPose.loadPoses(model, buffer);
        loadFrames(model, buffer);
        IQMAnim.loadAnims(model, buffer);
        IQMBounds.loadBounds(model, buffer);
        loadComments(model, buffer);
        //IQMExtension.loadExtensions(model, buffer);

        IQMAnim.prepareAnim(model);

        // Custom stuff
        // Check for a mesh named "@bbox"
        Vector3f min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
        for (IQMMesh iQMMesh : model.meshes) {
            if(!iQMMesh.name.equals("@bbox")) continue;
            min.set(100000,100000,100000);
            max.set(-100000,-100000,-100000);
            // We've got a bbox
            // Add all vertices to bounds
            for (int j= 0; j < iQMMesh.num_triangles; j++) {
                IQMTriangle tri = model.triangles[iQMMesh.first_triangle+j];
                for (int k= 0; k < tri.vertex.length; k++) {
                    int indice = tri.vertex[k];
                    Helper.AddPointToBounds(model.in_position[indice], min, max);
                }
            }

            // Save off bbox
            model.min.set(min);
            model.max.set(max);

            break;
        }

        // Map known animations
        if(model.anims != null) {
            for (IQMAnim iQMAnim : model.anims) {
                try {
                    Animations anim = Animations.valueOf(iQMAnim.name.toUpperCase());
                    // map it
                    model.animationMap.put(anim, iQMAnim);
                } catch(IllegalArgumentException ex) {
                }
            }
        }

        // Check for bone-attachments
        if(model.joints != null) {
            ArrayList<IQMJoint> thighBones = new ArrayList<IQMJoint>();
            for (int i= 0; i < model.joints.length; i++) {
                IQMJoint iQMJoint = model.joints[i];
                model.controllerMap.put(iQMJoint.name.toLowerCase(), iQMJoint);
                String jointName = iQMJoint.name.toUpperCase();
                if(jointName.startsWith("thigh_")) {
                    thighBones.add(iQMJoint);
                }
                if(jointName.startsWith("@ATTACH ")) {
                    String bonename = iQMJoint.name.substring(8).trim();
                    
                    model.attachments.put(bonename, iQMJoint);
                }
            }
            if(!thighBones.isEmpty()) {
                model.thighJoints = (IQMJoint[]) thighBones.toArray();
            }
        }
        
        
        parseIQMScript(model);
        
        if(model.header.num_joints > 0 && model.joints.length > 0) {
            // Grab bone origins when in pose
            model.jointPose = new Vector3f[model.joints.length];
            for (int i= 0; i < model.joints.length; i++) {
                 IQMJoint j = model.joints[i];

                 Vector4f v = new Vector4f(0,0,0,1);
                 Matrix4f.transform(j.baseframe, v, v);
                 model.jointPose[i] = new Vector3f(v);
            }
        }

        // shapekeys are experimental
        String keyShapeFile = file.replace(".iqm", ".shapekeys");
        if(ResourceManager.FileExists(keyShapeFile)) {
            try {
                ShapeKeyLoader.loadSkapeKey(keyShapeFile, true).attachToModel(model);
            } catch (Exception ex) {
                Logger.getLogger(IQMLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return model;
    }
    
    private static void parseIQMScript(IQMModel model) {
        // Parse commands
        ArrayList<Command> commands = tokenizeComments(model);
        for (int i = 0; i < commands.size(); i++) {
            Command cmd = commands.get(i);
            
            if(cmd.name.equalsIgnoreCase("rigid") ||
                    cmd.name.equalsIgnoreCase("spring") ||
                    cmd.name.equalsIgnoreCase("flexible")) {
                String bone = cmd.params.get("bone");
                String mesh = cmd.params.get("mesh");
                if(bone == null || bone.isEmpty() || mesh == null || mesh.isEmpty()) continue;
                
                IQMJoint found = null;
                for (IQMJoint joint : model.joints) {
                    if(!joint.name.equalsIgnoreCase(bone)) continue;
                    found = joint;
                    break;
                }
                if(found == null) {
                    Common.Log("Coulnd't find bone %s for physicsbones", bone);
                    continue;
                }
                IQMMesh foundmesh = null;
                for (IQMMesh meshs : model.meshes) {
                    if(meshs.name.equalsIgnoreCase(mesh)) {
                        foundmesh = meshs;
                        break;
                    }
                }
                if(foundmesh == null) {
                    Common.Log("Coulnd't find mesh %s for physicsbones", mesh);
                    continue;
                }
                
                BoneMeshInfo info = new BoneMeshInfo(cmd.params);
                info.mesh = foundmesh;
                info.joint = found;
                BoneMeshInfo.Type type = BoneMeshInfo.Type.RIGID;
                if(cmd.name.equalsIgnoreCase("spring")) type = BoneMeshInfo.Type.SPRING;
                else if(cmd.name.equalsIgnoreCase("flexible")) type = BoneMeshInfo.Type.FLEXIBLE;
                info.type = type;
                model.boneMeshInfo.add(info);
            } else if(cmd.name.equalsIgnoreCase("controller")) {
                String bone = cmd.params.get("bone");
                String name = cmd.params.get("name");
                for (int j = 0; j < model.joints.length; j++) {
                    if(model.joints[j].name.equalsIgnoreCase(bone)) {
                        model.controllerMap.put(name.toLowerCase(), model.joints[j]);
                        break;
                    }
                }
            } else if(cmd.name.equalsIgnoreCase("attachment")) {
                String bone = cmd.params.get("bone");
                String name = cmd.params.get("name");
                for (int j = 0; j < model.joints.length; j++) {
                    if(model.joints[j].name.equalsIgnoreCase(bone)) {
                        model.attachments.put(name, model.joints[j]);
                        break;
                    }
                }
            }
        }
    }
    

    
    private static class Command {
        String name;
        HashMap<String, String> params;
        Command(String name, HashMap<String, String> params) {
            this.name = name;
            this.params = params;
        }
    }
    
    private static ArrayList<Command> tokenizeComments(IQMModel model) {
        // format:
        // <command> {
        // <parameter> <value>
        // ...
        // }
        if(model.comment == null || model.comment.isEmpty()) return new ArrayList<Command>();
        String[] tokens = Commands.TokenizeString(model.comment, false);
        tokens = Commands.trimTokens(tokens);
        String command = null;
        ArrayList<Command> commands = new ArrayList<Command>();
        HashMap<String, String> params = null;
        
        for (int i = 0; i < tokens.length; i++) {
            if(command == null) {
                // Entering command
                if((i+1) >= tokens.length) break;
                if(!"{".equals(tokens[i+1])) break;
                command = tokens[i];
                params = new HashMap<String, String>();
                i++;
                continue;
            } else {
                // Inside command
                if("}".equals(tokens[i])) {
                    // exiting command
                    commands.add(new Command(command, params));
                    params = null;
                    command = null;
                    continue;
                }
                
                // Read parameter + value
                if((i+1) >= tokens.length) break;
                String param = tokens[i];
                String value = tokens[i+1];
                params.put(param.toLowerCase(), value);
                i++;
                continue;
            }
        }
        return commands;
    }

    static void loadText(IQMModel model, ByteBuffer buffer) {
        if(model.header.num_text == 0 || model.header.ofs_text == 0)
            return;

        buffer.position(model.header.ofs_text);
        model.text = new byte[model.header.num_text];
        for (int i= 0; i < model.header.num_text; i++) {
            model.text[i] = buffer.get();
        }
    }

    static void loadFrames(IQMModel model, ByteBuffer buffer) {
        if(model.header.num_frames == 0 || model.header.ofs_frames == 0) return;

        buffer.position(model.header.ofs_frames);
        model.framedata = new int[model.header.num_frames * model.header.num_framechannels];
        for (int i= 0; i < model.framedata.length; i++) {
            model.framedata[i] = buffer.getShort() & 0xffff;
        }
    }

    static void loadComments(IQMModel model, ByteBuffer buffer) {
        if(model.header.num_comment == 0 || model.header.ofs_comment == 0) return;
        buffer.position(model.header.ofs_comment + model.header.ofs_text);

        byte[] data = new byte[model.header.num_comment];
        for (int i= 0; i < model.header.num_comment; i++) {
            data[i] = buffer.get();
        }
        try {
            model.comment = new String(data, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
        }
    }
    
}
