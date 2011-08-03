package cubetech.iqm;

import cubetech.common.Helper;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 *
 * @author mads
 */
// ofs_* fields are relative to the beginning of the header
// ofs_* fields must be set to 0 when the particular data is empt
public class IQMHeader {
    public static final String MAGIC = "INTERQUAKEMODEL\0";
    int version; // must be version 1
    long filesize;
    int flags;
    int num_text, ofs_text;
    int num_meshes, ofs_meshes;
    int num_vertexarrays, num_vertexes, ofs_vertexarrays;
    int num_triangles, ofs_triangles, ofs_adjacency;
    int num_joints, ofs_joints;
    int num_poses, ofs_poses;
    int num_anims, ofs_anims;
    int num_frames, num_framechannels, ofs_frames, ofs_bounds;
    int num_comment, ofs_comment;
    int num_extensions, ofs_extensions; // these are stored as a linked list, not as a contiguous array

    String pathName;
    String modelName;

    public static IQMHeader loadHeader(IQMModel model, ByteBuffer buffer, String file) {
        IQMHeader h = new IQMHeader();
        h.pathName = Helper.getPath(file);
        h.modelName = Helper.stripPath(file);
        byte[] dat = new byte[IQMHeader.MAGIC.length()];
        for (int i= 0; i < dat.length; i++) {
            dat[i] = buffer.get();
        }
        try {
            String magic = new String(dat, "US-ASCII");
            if(!magic.equals(IQMHeader.MAGIC)) throw new IllegalArgumentException("Invalid Magic");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        
        h.version = buffer.getInt();
        if(h.version != 2) throw new IllegalArgumentException("Invalid version " + h.version + ". Must be 2");
        h.filesize = buffer.getInt();
        h.flags = buffer.getInt();
        h.num_text = buffer.getInt();
        h.ofs_text = buffer.getInt();
        h.num_meshes = buffer.getInt();
        h.ofs_meshes = buffer.getInt();

        h.num_vertexarrays= buffer.getInt(); h.num_vertexes= buffer.getInt(); h.ofs_vertexarrays= buffer.getInt();
        h.num_triangles= buffer.getInt(); h.ofs_triangles= buffer.getInt(); h.ofs_adjacency= buffer.getInt();
        h.num_joints= buffer.getInt(); h.ofs_joints= buffer.getInt();
        h.num_poses= buffer.getInt(); h.ofs_poses= buffer.getInt();
        h.num_anims= buffer.getInt(); h.ofs_anims= buffer.getInt();
        h.num_frames= buffer.getInt(); h.num_framechannels= buffer.getInt();
        h.ofs_frames= buffer.getInt(); h.ofs_bounds= buffer.getInt();
        h.num_comment= buffer.getInt(); h.ofs_comment= buffer.getInt();
        h.num_extensions= buffer.getInt(); h.ofs_extensions= buffer.getInt(); // these are stored as a linked list, not as a contiguous array

        model.header = h;
        return h;
    }
}
