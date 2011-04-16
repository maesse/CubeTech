package cubetech.gfx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class AniModel {
    int bone_id_counter = 0;
    Bone root_bone = new Bone(bone_id_counter++);
    HashMap<String, Animation> animations = new HashMap<String, Animation>();
    Animation currentAnim = null;

    public AniModel() {
        root_bone.AttachBone(new Bone(bone_id_counter++));
        currentAnim = new Animation("idle");
        animations.put(currentAnim.getName(), currentAnim);
    }

    public Set<String> GetAnimationNames() {
        return animations.keySet();
    }

    public void AnimateToTime(int time) {
        if(currentAnim != null)
            currentAnim.ApplyAnimationToBones(time, root_bone);
    }

    public Animation GetCurrentAnimation() {
        return currentAnim;
    }

    public Animation GetAnimation(String str) {
        return animations.get(str);
    }

    public void SetCurrentAnimation(Animation anim) {
        currentAnim = anim;
    }

    public void SetCurrentAnimation(String anim) {
        SetCurrentAnimation(GetAnimation(anim));
    }

    public Animation CreateAnimation(String name) {
        Animation anim = new Animation(name);
        animations.put(name, anim);
        return anim;
    }

    public Bone CreateBone() {
        Bone bone = new Bone(bone_id_counter++);
        return bone;
    }

    public void Render(Vector2f position) {
        root_bone.Render();
    }

    /**
     * Creates and populates a KeyFrameBone structure with the current bone state.
     */
    public KeyFrameBone TakeSnapshot() {
        KeyFrameBone root = new KeyFrameBone();
        BuildBoneList(root, root_bone);
        return root;
    }

    private KeyFrameBone BuildBoneList(KeyFrameBone dst, Bone bone) {
        if(dst.boneId > 0) {
            dst.next = new KeyFrameBone();
            dst = dst.next;
        }
        dst.angle = bone.getAngle();
        dst.lenght = bone.getLenght();
        dst.boneId = bone.getId();


        Iterator<Bone> it = bone.GetChildBonesIterator();
        while(it.hasNext()) {
            Bone b = it.next();
            dst = BuildBoneList(dst, b); // Recurse down children
        }

        return dst;
    }
    
    /**
     * Go though the heirachy and find a colliding bone
     * @param position to test against
     * @return null if no bone hit
     */
    public Bone TraceForBone(Vector2f position, float lineThickness) {
        return root_bone.TraceForBone(position, new Vector2f(), 0f, lineThickness);
    }

}
