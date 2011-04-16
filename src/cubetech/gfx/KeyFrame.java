package cubetech.gfx;

/**
 *
 * @author mads
 */
public class KeyFrame {
    public int time; // at this time
    public KeyFrame next = null; // Pointer to next frame
    public KeyFrameBone root = null; // Bone values are linked in here.

    public KeyFrameBone FindBoneId(int id) {
        KeyFrameBone curr = root;
        while(curr != null && curr.boneId != id)
            curr = curr.next;

        return curr;
    }
}
