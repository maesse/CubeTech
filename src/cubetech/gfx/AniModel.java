package cubetech.gfx;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class AniModel {
    Bone root_bone = new Bone();

    public AniModel() {
        root_bone.AttachBone(new Bone());
    }

    public void Render(Vector2f position) {
        root_bone.Render();
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
