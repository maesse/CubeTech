package cubetech;

import org.lwjgl.opengl.GL11;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector2f;
/**
 *
 * @author mads
 */
public class Camera {
    Vector2f Position;
    public Vector2f VisibleSize;
    public Vector2f DefaultSize;
    
    public Camera(Vector2f position, float width) {
        this.Position = position;

        // Variable height depending on the aspect ratio
        float aspect = (float)Ref.glRef.currentMode.getHeight()/(float)Ref.glRef.currentMode.getWidth();
        this.VisibleSize = new Vector2f(width, width * aspect);
        DefaultSize = new Vector2f(VisibleSize.x, VisibleSize.y);
    }

    public void UpdatePosition(Vector2f position) {
        this.Position = position;
        if(this.Position.y < 0f)
            this.Position.y = 0;
    }

    public void PositionCamera() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, VisibleSize.x, 0, VisibleSize.y, 1,-1);
        GL11.glTranslatef(-Position.x, -Position.y, 0);
    }
}
