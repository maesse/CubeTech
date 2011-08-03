package cubetech.iqm;

import java.nio.ByteBuffer;

/**
 *
 * @author mads
 */
public class IQMPose {
    int _parent; // parent < 0 means this is a root bone
    IQMPose parent;
    int channelmask; // mask of which 9 channels are present for this joint pose
    float[] channeloffset = new float[10], channelscale = new float[10];
    // channels 0..2 are translation <Tx, Ty, Tz> and channels 3..5 are quaternion rotation <Qx, Qy, Qz, Qw> where Qw = -sqrt(max(1 - Qx*Qx - Qy*qy - Qz*qz, 0))
    // rotation is in relative/parent local space
    // channels 6..8 are scale <Sx, Sy, Sz>
    // output = (input*scale)*rotation + translation

    static void loadPoses(IQMModel model, ByteBuffer buffer) {
        if(model.header.num_poses == 0 || model.header.ofs_poses == 0) return;

        buffer.position(model.header.ofs_poses);
        model.poses = new IQMPose[model.header.num_poses];
        for (int i= 0; i < model.header.num_poses; i++) {
            IQMPose pose = new IQMPose();
            pose._parent = buffer.getInt();
            pose.channelmask = buffer.getInt();
            for (int j= 0; j < pose.channeloffset.length; j++) {
                pose.channeloffset[j] = buffer.getFloat();
            }
            for (int j= 0; j < pose.channelscale.length; j++) {
                pose.channelscale[j] = buffer.getFloat();
            }

            model.poses[i] = pose;
        }

        for (int i= 0; i < model.poses.length; i++) {
            if(model.poses[i]._parent >= 0) {
                model.poses[i].parent = model.poses[model.poses[i]._parent];
            }
        }
    }
}
