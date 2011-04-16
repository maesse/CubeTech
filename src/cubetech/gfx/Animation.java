package cubetech.gfx;

import cubetech.common.Common;
import java.util.Iterator;

/**
 *
 * @author mads
 */
public class Animation {
    private String name;
    private KeyFrame headFrame; // Linked list
    private int currentTime;
    private KeyFrame currentKeyFrame;
    public boolean dirty = true;

    public void setFrame(int frame) {
        currentTime = frame;
        currentKeyFrame = LocateKeyframe(frame);
    }

    public Animation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Applies the animation to a bone-structure.
     * @param time
     * @param root_bone
     */
    public void ApplyAnimationToBones(int time, Bone root_bone) {
        setFrame(time);

        applyCurrentToBones(root_bone, true);
    }

    private void applyCurrentToBones(Bone root, boolean lerp) {
        if(currentKeyFrame == null || currentKeyFrame.root == null)
            return;
        if(root == null)
            return;

        if(currentKeyFrame.time > currentTime) {
            Common.Log("Animation.applyCurrentToBones: keyframe.time > currenttime");
        }
        
        if(lerp && currentKeyFrame.next != null) {
            // Do linear interpolation
            int nextTime = currentKeyFrame.next.time;
            int thisTime = currentKeyFrame.time;
            int deltaTime = currentTime - thisTime;
            int deltaTotal = nextTime - thisTime;
            float frac = (float)deltaTime/deltaTotal;
            applyToBones_Recursive_linear(root, currentKeyFrame, currentKeyFrame.next, frac);
            return;
        }
        
        applyToBones_Recursive(root, currentKeyFrame);
    }

    private void applyToBones_Recursive(Bone current, KeyFrame keyframe) {
        // Find BoneID in datalist
        int id = current.getId();
        KeyFrameBone targetBone = keyframe.FindBoneId(id);
        if(targetBone != null) {
            // Apply state to bone
            current.setAngle(targetBone.angle);
            current.setLenght(targetBone.lenght);
        }

        Iterator<Bone> it = current.GetChildBonesIterator();
        while(it.hasNext()) {
            Bone child = it.next();
            applyToBones_Recursive(child, keyframe);
        }
    }

    private void applyToBones_Recursive_linear(Bone current, KeyFrame keyframe, KeyFrame keyframe2, float frac) {
        // Find BoneID in datalist
        int id = current.getId();
        KeyFrameBone targetBone = keyframe.FindBoneId(id);
        KeyFrameBone targetBone2 = keyframe2.FindBoneId(id);
        if(targetBone != null && targetBone2 != null) {
            // Apply state to bone
            current.setAngle(targetBone.angle + (targetBone2.angle - targetBone.angle)*frac);
            current.setLenght(targetBone.lenght + (targetBone2.lenght - targetBone.lenght)*frac);
        } else if(targetBone != null) {
            current.setAngle(targetBone.angle);
            current.setLenght(targetBone.lenght);
        }

        Iterator<Bone> it = current.GetChildBonesIterator();
        while(it.hasNext()) {
            Bone child = it.next();
            applyToBones_Recursive_linear(child, keyframe, keyframe2, frac);
        }
    }

    /**
     * Inserts an keyframe into the animation
     * @param frame - the frame to insert. the time should be set before inserting.
     */
    public void InsertFrame(KeyFrame frame) {
        // Nearest frame that happens at frame.time or before
        KeyFrame linkFrame = LocateKeyframe(frame.time);
        if(linkFrame != null) {
            if(linkFrame.time == frame.time) 
            { // We're replacing this frame
                frame.next = linkFrame.next; // Copy the child

                // Relink parent
                linkFrame = LocateKeyframe(frame.time-1);
                if(linkFrame != null)
                    linkFrame.next = frame;
                return;
            }
            // We got a frame with time < frame.time - so just link it in
            frame.next = linkFrame.next;
            linkFrame.next = frame;
        } else {
            // This is going to be the first frame
            if(headFrame != null)
                headFrame.next = frame; // link headFrame in as a child
            headFrame = frame;
        }
        dirty = true;
    }

    public void InsertFrame(KeyFrameBone frame) {
        KeyFrame keyframe = new KeyFrame();
        keyframe.root = frame;
        keyframe.time = currentTime;
        InsertFrame(keyframe);
    }

    /**
     * Seeks through the animation up till time and returns
     * the last keyframe iterated
     * @param time
     * @return null for no pre-existing keyframe
     */
    public KeyFrame LocateKeyframe(int time) {
        if(headFrame == null) 
            return null; // empty list
        
        KeyFrame frame = headFrame;
        KeyFrame lastFrame = null;
        do {
            if(frame.time == time)
                return frame; // exact fit

            if(frame.time > time)
                return lastFrame; // reached middle

            lastFrame = frame;
        } while ((frame = frame.next) != null);
        return lastFrame; // reached end
    }

    public KeyFrame GetRootFrame() {
        return headFrame;
    }

    public int getTime() {
        return currentTime;
    }
}
