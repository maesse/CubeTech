/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.iqm;

import java.nio.ByteBuffer;

/**
 *
 * @author mads
 */
public class IQMTriangle {
    int[] vertex = new int[3];

    static void loadTriangles(IQMModel model, ByteBuffer buffer) {
        if(model.header.num_triangles == 0 || model.header.ofs_triangles == 0) return;

        buffer.position(model.header.ofs_triangles);
        model.triangles = new IQMTriangle[model.header.num_triangles];
        for (int i= 0; i < model.header.num_triangles; i++) {
            IQMTriangle a = new IQMTriangle();
            a.vertex[0] = buffer.getInt() & 0xffffffff;
            a.vertex[1] = buffer.getInt() & 0xffffffff;
            a.vertex[2] = buffer.getInt() & 0xffffffff;
            model.triangles[i] = a;
        }
    }
}
