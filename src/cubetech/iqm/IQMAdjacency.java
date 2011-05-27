package cubetech.iqm;

import java.nio.ByteBuffer;

/**
 *
 * @author mads
 */
public class IQMAdjacency {

    static void loadAdjacency(IQMModel model, ByteBuffer buffer) {
        if(model.header.ofs_adjacency == 0 || model.header.num_triangles == 0) return;

        buffer.position(model.header.ofs_adjacency);
        model.adjacency = new IQMAdjacency[model.header.num_triangles];
        for (int i= 0; i < model.header.num_triangles; i++) {
            IQMAdjacency a = new IQMAdjacency();
            a.triangle[0] = buffer.getInt();
            a.triangle[1] = buffer.getInt();
            a.triangle[2] = buffer.getInt();
            model.adjacency[i] = a;
        }
    }
    int[] triangle = new int[3];
}
