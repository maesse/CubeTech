package cubetech.gfx;

import cubetech.collision.Collision;
import cubetech.common.Common.ErrorCode;
import cubetech.common.Helper;
import cubetech.misc.Ref;
import java.util.ArrayList;
import java.util.Iterator;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class Bone {
    private ArrayList<Bone> childBones = new ArrayList<Bone>();

    private float angle; // in radian
    private float lenght = 10;

    private Bone parent = null; // root bones don't have any parent

    public boolean selected = false;

    private int boneId = -1;

    Bone(int boneid, float angle, float lenght) {
        this.angle = angle;
        this.lenght = lenght;
        boneId = boneid;
    }

    Bone(int boneid) {
        boneId = boneid;
    }

    public Iterator<Bone> GetChildBonesIterator() {
        return childBones.iterator();
    }

    public void setAngle(float ang) {
        angle = ang;
    }

    public void setLenght(float len) {
        lenght = len;
    }

    /**
     * Attaches a bone to the end of this bone.
     * The attached-bone's parent will be this bone.
     * @param bone
     */
    public void AttachBone(Bone bone) {
        childBones.add(bone);
        bone.parent = this;
    }

    /**
     * Moves the bone end-point around
     * @param position - the endpoint the bone should have
     * @param correctChildren - if false, child bones will get the angle delta added
     */
    public void SetBoneEndPoint(Vector2f position, boolean correctChildren) {

        Vector2f boneStart = new Vector2f();
        CalculateStartPoint(boneStart);

        // Now substract this from the position
        Vector2f vector = Vector2f.sub(position, boneStart, null);
        lenght = Helper.Normalize(vector);
        float angleDelta = angle;
        angle = (float)Math.atan2(vector.y, vector.x) - CalculateGlobalAngle();
        if(!correctChildren) {
            angleDelta = angle - angleDelta;
            for (Bone bone : childBones) {
                bone.angle -= angleDelta;
            }
        }
    }

    // Add up angles from all parents.
    private float CalculateGlobalAngle() {
        Bone bParent = this;
        float angl = 0;
        while((bParent = bParent.parent) != null) {
            angl += bParent.angle;
        }
        return angl;
    }

    /**
     * Tests a point against the point and returns true
     * if the point was close to the end-point of this bone.
     * @param position position in global coordinates
     * @param sensitivity defines the radius from the endpoint
     *        that should result in a collision.
     * @return true on collision, false on no collision
     */
    public boolean TraceAgainstEnd(Vector2f position, float sensitivity) {
        Vector2f end = new Vector2f();
        float accum_angle = CalculateStartPoint(end);
        Vector2f vector = Helper.CreateVector(angle + accum_angle, lenght, null);
        Vector2f.add(end, vector, end);

        return Collision.TestPointPointRadius(end, position, sensitivity);
    }

    private float CalculateStartPoint(Vector2f dest) {
        Vector2f boneStart = dest;
        if(parent != null)
            return parent.CalculateStartPoint_Recursive(boneStart);
        else
            return 0f;
    }

    // Tail recurse though the bones
    private float CalculateStartPoint_Recursive(Vector2f dest) {
        float angl = angle;
        if(parent != null)
            angl += parent.CalculateStartPoint_Recursive(dest);

        // Add in angle * direction to dest
        dest.x += (float)Math.cos(angl) * lenght;
        dest.y += (float)Math.sin(angl) * lenght;
        
        return angl;
    }

    // Creates a vector for this bones movement in global space.
    public Vector2f GenerateDeltaVector(float parent_angle, Vector2f dest) {
        return Helper.CreateVector(angle + parent_angle, lenght, dest);
    }

    /**
     * Removes this bone from the parent.
     * All childbones for this bone will also be lost.
     */
    public void RemoveBone() {
        if(parent != null)
            if(!parent.childBones.remove(this))
                Ref.common.Error(ErrorCode.FATAL, "Bone.RemoveBone: Tried to remove from parent, but was not a child.");
    }

    /**
     * Traces a point against the bones as if were they line segments.
     * @param test is the point to test against
     * @param origin is the endpoint of the last bone
     * @param sensitivity determines how close the test point should be
     * @return
     */
    public Bone TraceForBone(Vector2f test, Vector2f origin, float curr_angle, float sensitivity) {
        // Plane separation:
        curr_angle += angle; // Append angle from childBones

        // Generate direction vector from given angle
        Vector2f direction = new Vector2f();
        Helper.CreateVector(curr_angle, lenght, direction);
        Vector2f endPos = Vector2f.add(direction, origin, null);

        // Normalize for axis test
        Helper.Normalize(direction);
        float start = Vector2f.dot(direction, origin);
        float end = Vector2f.dot(direction, endPos);
        float testPoint = Vector2f.dot(direction, test);

        if(testPoint >= start && testPoint <= end) {
            // Bone hit in first axis -- now for the perpendicular axis
            Vector2f dir = new Vector2f((float)Math.cos(curr_angle+Math.PI/2), (float) Math.sin(curr_angle+Math.PI/2));
            start = Vector2f.dot(dir, origin);
            testPoint = Vector2f.dot(dir, test);

            float delta = (float)Math.abs(start - testPoint);
            if(delta < sensitivity)
                return this;
        }


        for (Bone bone : childBones) {
            // endpos is the new origin
            Bone success = bone.TraceForBone(test, endPos, curr_angle, sensitivity);
            if(success != null)
                return success;
        }

        return null; // nothing hit
    }

    


    public void Render() {
        GL11.glPushMatrix();
//        

        Ref.ResMan.getWhiteTexture().Bind();

        Vector2f TexSize = new Vector2f(1,1);
        Vector2f TexOffset = new Vector2f();

        byte w = (byte) 255;
        byte hw = selected?w:(byte)170;
        byte b = (byte) 0;

        float depth = -9;

        float lineSize = 5* Ref.cgame.cg.refdef.FovY / Ref.glRef.GetResolution().y;
        if(angle != 0f)
            GL11.glRotatef(angle * 180f/(float)Math.PI,0,0,1);
         // Texture coords are flipped on y axis
        GL11.glBegin(GL11.GL_TRIANGLES);
        {
            if(Ref.glRef.isShadersSupported()) {
                // Fancy pants shaders
                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x, TexOffset.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, b,hw,b,w);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, 0, -lineSize, depth);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x+TexSize.x, TexOffset.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, hw,b,b,w);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, lenght, 0, depth);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x, TexOffset.y+TexSize.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, b,hw,b,w);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, 0, lineSize, depth);
            } else {
                // Good ol' fixed function
                GL11.glTexCoord2f(TexOffset.x, TexOffset.y);
                GL11.glColor4ub(b,w,b,w);
                GL11.glVertex3f( 0, -lineSize, depth);

                GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y);
                GL11.glColor4ub(w,b,b,w);
                GL11.glVertex3f( lenght, 0, depth);

                GL11.glTexCoord2f(TexOffset.x, TexOffset.y+TexSize.y);
                GL11.glColor4ub(b,w,b,w);
                GL11.glVertex3f( 0, lineSize, depth);
            }

        }
        GL11.glEnd();

        if(selected) {
            RenderCorners(TexOffset, TexSize, depth);
            selected = false;
        }

        GL11.glTranslatef(lenght, 0, 0);

        for (Bone bone : childBones) {
            bone.Render();
        }

        GL11.glPopMatrix();
        GLRef.checkError();
    }

    private void RenderCorners(Vector2f TexOffset, Vector2f TexSize, float depth) {
        Ref.ResMan.LoadTexture("data/corner.png").Bind();
        byte w = (byte) 255;
        float cornerSize = 0.8f * Ref.cgame.cg.refdef.FovX * 1f/220;
        depth += 2;

        GL11.glBegin(GL11.GL_QUADS);
        {
            if(Ref.glRef.isShadersSupported()) {
                // Fancy pants shaders
                // First corner
//                GL20.glVertexAttrib2f(2, TexOffset.x, TexOffset.y);
//                GL20.glVertexAttrib4Nub(1, w,w,w,w);
//                GL20.glVertexAttrib3f(0, -cornerSize, -cornerSize, depth);
//
//                GL20.glVertexAttrib2f(2, TexOffset.x+TexSize.x, TexOffset.y);
//                GL20.glVertexAttrib4Nub(1, w,w,w,w);
//                GL20.glVertexAttrib3f(0, cornerSize, -cornerSize, depth);
//
//                GL20.glVertexAttrib2f(2, TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
//                GL20.glVertexAttrib4Nub(1, w,w,w,w);
//                GL20.glVertexAttrib3f(0, cornerSize, cornerSize, depth);
//
//                GL20.glVertexAttrib2f(2, TexOffset.x, TexOffset.y+TexSize.y);
//                GL20.glVertexAttrib4Nub(1, w,w,w,w);
//                GL20.glVertexAttrib3f(0, -cornerSize, cornerSize, depth);

                // Second corner
                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x, TexOffset.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, w,w,w,w);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, -cornerSize + lenght, -cornerSize, depth);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x+TexSize.x, TexOffset.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, w,w,w,w);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, cornerSize + lenght, -cornerSize, depth);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, w,w,w,w);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, cornerSize + lenght, cornerSize, depth);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x, TexOffset.y+TexSize.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, w,w,w,w);
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, -cornerSize + lenght, cornerSize, depth);
            } else {
                // Good ol' fixed function
                GL11.glTexCoord2f(TexOffset.x, TexOffset.y);
                GL11.glColor4ub(w,w,w,w);
                GL11.glVertex3f( -cornerSize, -cornerSize, depth);

                GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y);
                GL11.glColor4ub(w,w,w,w);
                GL11.glVertex3f( cornerSize, -cornerSize, depth);

                GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
                GL11.glColor4ub(w,w,w,w);
                GL11.glVertex3f( cornerSize, cornerSize, depth);

                GL11.glTexCoord2f(TexOffset.x, TexOffset.y+TexSize.y);
                GL11.glColor4ub(w,w,w,w);
                GL11.glVertex3f( -cornerSize, cornerSize, depth);
            }

        }
        GL11.glEnd();
    }

    public float getAngle() {
        return angle;
    }

    public float getLenght() {
        return lenght;
    }

    public int getId() {
        return boneId;
    }
}
