package cubetech.iqm;

import cubetech.gfx.ResourceManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
