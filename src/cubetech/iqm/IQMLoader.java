package cubetech.iqm;

import cubetech.common.Animations;
import cubetech.common.Helper;
import cubetech.gfx.ResourceManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector3f;

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

            // We've got a bbox
            // Add all vertices to bounds
            for (int j= 0; j < iQMMesh.num_triangles; j++) {
                IQMTriangle tri = model.triangles[iQMMesh.first_triangle+j];
                for (int k= 0; k < tri.vertex.length; k++) {
                    int indice = tri.vertex[k]-1;
                    if(indice < 0)  {
                        min.set(-32,-32,-32);
                        max.set(32,32,32);
                        continue;
                    }

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

        if(model.joints != null) {
            ArrayList<IQMJoint> thighBones = new ArrayList<IQMJoint>();
            for (IQMJoint iQMJoint : model.joints) {
                String jointName = iQMJoint.name.toUpperCase();
                if(jointName.startsWith("thigh_")) {
                    thighBones.add(iQMJoint);
                }
            }
            if(!thighBones.isEmpty()) {
                model.thighJoints = (IQMJoint[]) thighBones.toArray();
            }
        }
        
        return model;
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
        buffer.position(model.header.ofs_comment);

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
