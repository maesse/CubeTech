package cubetech.iqm;

import cubetech.gfx.Matrix3x4;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class IQMAnim {
    public static final int FLAG_ANIM_LOOP = 1;
    String name;
    
    public int first_frame, num_frames;
    float framerate;
    int flags;

    // Custom
    public int initialLerp = 50; // msec to get to first frame
    public int frameLerp = 0; // msec between frames
    public int loopFrames = 0;

    public boolean isLoopFlag() {
        return (flags & 1) == 1;
    }

    static void loadAnims(IQMModel model, ByteBuffer buffer) {
        if(model.header.num_anims == 0 || model.header.ofs_anims == 0) return;

        buffer.position(model.header.ofs_anims);
        model.anims = new IQMAnim[model.header.num_anims];
        for (int i= 0; i < model.header.num_anims; i++) {
            IQMAnim a = new IQMAnim();
            int name = buffer.getInt();
            if(name >= 0) {
                int end = name;
                while(end < model.text.length && model.text[end] != '\0') end++;
                try {
                    a.name = new String(model.text, name, end-name, "US-ASCII");
                } catch (UnsupportedEncodingException ex) {
                }
            }
            a.first_frame = buffer.getInt();
            a.num_frames = buffer.getInt();
            a.framerate = buffer.getFloat();
            a.flags = buffer.getInt();

            if(a.framerate == 0f) a.framerate = 25f;
            a.frameLerp = (int) (1000/a.framerate);
            a.loopFrames = a.num_frames;
            model.anims[i] = a;
        }
    }

    static void prepareAnim(IQMModel model) {
        int framedata = 0;
        model.frames = new Matrix4f[model.header.num_frames * model.header.num_poses];
        for (int i= 0; i < model.header.num_frames; i++) {
            for (int j= 0; j < model.header.num_poses; j++) {
                IQMPose p = model.poses[j];
                float tx = p.channeloffset[0]; if((p.channelmask&0x01)!=0) tx += model.framedata[framedata++] * p.channelscale[0];
                float ty = p.channeloffset[1]; if((p.channelmask&0x02)!=0) ty += model.framedata[framedata++] * p.channelscale[1];
                float tz = p.channeloffset[2]; if((p.channelmask&0x04)!=0) tz += model.framedata[framedata++] * p.channelscale[2];
                float rx = p.channeloffset[3]; if((p.channelmask&0x08)!=0) rx += model.framedata[framedata++] * p.channelscale[3];
                float ry = p.channeloffset[4]; if((p.channelmask&0x10)!=0) ry += model.framedata[framedata++] * p.channelscale[4];
                float rz = p.channeloffset[5]; if((p.channelmask&0x20)!=0) rz += model.framedata[framedata++] * p.channelscale[5];
                float sx = p.channeloffset[6]; if((p.channelmask&0x40)!=0) sx += model.framedata[framedata++] * p.channelscale[6];
                float sy = p.channeloffset[7]; if((p.channelmask&0x80)!=0) sy += model.framedata[framedata++] * p.channelscale[7];
                float sz = p.channeloffset[8]; if((p.channelmask&0x100)!=0) sz += model.framedata[framedata++] * p.channelscale[8];

                // Concatenate each pose with the inverse base pose to avoid doing this at animation time.
                // If the joint has a parent, then it needs to be pre-concatenated with its parent's base pose.
                // Thus it all negates at animation time like so:
                //   (parentPose * parentInverseBasePose) * (parentBasePose * childPose * childInverseBasePose) =>
                //   parentPose * (parentInverseBasePose * parentBasePose) * childPose * childInverseBasePose =>
                //   parentPose * childPose * childInverseBasePose
                Quaternion q = new Quaternion(rx, ry, rz, (float)-Math.sqrt(Math.max(1- (rx*rx + ry*ry + rz*rz),0)));
                Matrix3x4 mm = new Matrix3x4(q, new Vector3f(sx, sy, sz), new Vector3f(tx, ty, tz));
                Matrix4f m = new Matrix4f();
                mm.toMatrix4f(m);
                
                if(p._parent >= 0) {
                    Matrix4f.mul(model.baseframe[p._parent], m, m);
                    Matrix4f.mul(m, model.invbaseframe[j], m);
                    model.frames[i*model.header.num_poses + j] = m;
                } else {
                    Matrix4f.mul(m, model.invbaseframe[j], m);
                    model.frames[i*model.header.num_poses + j] = m;
                }
            }
        }
    }
}
