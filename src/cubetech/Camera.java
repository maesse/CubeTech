package cubetech;

import cubetech.gfx.Graphics;
import org.lwjgl.util.vector.Vector2f;
import static org.lwjgl.opengl.GL11.*;

/**
 * An instance of a camera.. Basically wrapping the static positionCameraCorner method
 * @author mads
 */
public class Camera {
    private Vector2f Position;
    private Vector2f VisibleSize;
    
    public Camera(Vector2f position, float width) {
        this.Position = position;

        // Variable height depending on the aspect ratio
        float aspect = (float)Graphics.getHeight()/(float)Graphics.getWidth();
        this.VisibleSize = new Vector2f(width, width * aspect);
    }

    public Camera(Vector2f position, float width, float height) {
        this.Position = position;
        this.VisibleSize = new Vector2f(width, height);
    }

    /**
     * Inspect and change the current viewsize when rendering with this camera
     * @return
     */
    public Vector2f getViewSize() {
        return VisibleSize;
    }

    /**
     * Define where the camera should be positioned
     * @param position
     */
    public void setPosition(Vector2f position) {
        this.Position = position;
    }

    /**
     * Applies the currently set position in OpenGL
     */
    public void applyCameraPosition() {
        positionCameraCorner(VisibleSize.x, VisibleSize.y, Position);
    }

    // Method for setting up the GAME camera position and viewsize
    public static void positionCameraCentered(float width, float height, Vector2f center) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        width /= 2f;
        height /= 2f;
        glOrtho(-width, width, -height, height, 1,-1);
        glTranslatef(-center.x, -center.y, 0);
    }

    public static void positionCameraCorner(float width, float height, Vector2f position) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, 0, height, 1,-1);
        glTranslatef(-position.x, -position.y, 0);
    }
}
