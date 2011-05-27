package cubetech.iqm;

import java.nio.ByteBuffer;

/**
 *
 * @author mads
 */
public class IQMBounds {
    float[] bbmins = new float[3], bbmaxs = new float[3]; // the minimum and maximum coordinates of the bounding box for this animation frame
    float xyradius, radius; // the circular radius in the X-Y plane, as well as the spherical radius

    static void loadBounds(IQMModel model, ByteBuffer buffer) {
        if(model.header.ofs_bounds == 0) return;

        buffer.position(model.header.ofs_bounds);
        model.bounds = new IQMBounds[model.header.num_frames];
        for (int i= 0; i < model.header.num_frames; i++) {
            IQMBounds b = new IQMBounds();
            b.bbmins[0] = buffer.getFloat();
            b.bbmins[1] = buffer.getFloat();
            b.bbmins[2] = buffer.getFloat();
            b.bbmaxs[0] = buffer.getFloat();
            b.bbmaxs[1] = buffer.getFloat();
            b.bbmaxs[2] = buffer.getFloat();
            b.xyradius = buffer.getFloat();
            b.radius = buffer.getFloat();

            model.bounds[i] = b;
        }
    }
}
