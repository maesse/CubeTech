package cubetech.gfx;

import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class Sprite {
    private Vector2f Center = new Vector2f();
    private Vector2f Extent = new Vector2f();
    private Vector4f Color = new Vector4f(1,1,1,1);
    private float Angle = 0f;
    private CubeTexture Texture = null;
    private Vector2f TexOffset = new Vector2f();
    private Vector2f TexSize = new Vector2f(1,1);

    Sprite() {
    }

    // Individual setters
    public void setCenter(Vector2f position) {
        Center.set(position);
    }

    public void setExtent(Vector2f extent) {
        Extent.set(extent);
    }

    public void setColor(Vector4f color) {
        Color.set(color);
    }

    public void setColor(Color color) {
        setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * Values should be in the 0-255 range. If you want to input a 0-1 range,
     * use setColor(Vector4f) instead.
     * @param r
     * @param g
     * @param b
     * @param a
     */
    public void setColor(float r, float g, float b, float a) {
        Color.set(r/255f,g/255f,b/255f,a/255f);
    }

    public void setAngle(float angle) {
        this.Angle = angle;
    }

    /**
     * Set sprite position and size, starting at position growing in size
     * @param position
     * @param size
     * @param tex
     */
    public void setFromCorner(Vector2f position, Vector2f size, CubeTexture tex) {
        Extent.set(size);
        Extent.scale(0.5f);
        Vector2f.add(position, Extent, Center);
        this.Texture = tex;
    }

    /**
     * Set sprite position and size, starting at position growing in size
     * @param position
     * @param size
     * @param tex
     */
    public void setFromCorner(Vector2f position, Vector2f size, CubeTexture tex,
                              Vector2f textureOffset, Vector2f textureSize) {
        setFromCorner(position, size, tex);
        TexOffset.set(textureOffset);
        TexSize.set(textureSize);
    }

    /**
     * Sets sprite position, using the position as a center.
     * @param position
     * @param radius
     * @param tex
     */
    public void setFromCenter(Vector2f position, float radius, CubeTexture tex) {
        setFromCenter(position, new Vector2f(radius*2, radius*2), tex);
    }

    public void setFromCenter(Vector2f position, Vector2f size, CubeTexture tex) {
        Texture = tex;
        Center.set(position);
        Extent.set(size);
        Extent.scale(0.5f);
    }

    public void setFromCenter(Vector2f position, Vector2f size, CubeTexture tex,
                              Vector2f textureOffset, Vector2f textureSize) {
        setFromCenter(position, size, tex);
        TexOffset.set(textureOffset);
        TexSize.set(textureSize);
    }


    void Draw() {
        if(Texture != null) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            Texture.Bind();
        } else
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            
        GL11.glColor4f(Color.x, Color.y, Color.z, Color.w);

        // Move camera so sprite is centered
        GL11.glPushMatrix();
        GL11.glTranslatef(Center.x, Center.y, 0);
        
        if(Angle != 0f)
            GL11.glRotatef(Angle * 180f/(float)Math.PI,0,0,1);

        // Draw the four corners
        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f(TexOffset.x, TexOffset.y);
            GL11.glVertex2f(-Extent.x, -Extent.y);

            GL11.glTexCoord2f(TexOffset.x, TexOffset.y+TexSize.y);
            GL11.glVertex2f(-Extent.x, Extent.y);

            GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
            GL11.glVertex2f(Extent.x, Extent.y);

            GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y);
            GL11.glVertex2f(Extent.x, -Extent.y);
        }
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    void reset() {
        // Clears sprite
        Center = new Vector2f();
        Extent = new Vector2f();
        Color = new Vector4f(1,1,1,1);
        Angle = 0f;
        Texture = null;
        TexOffset = new Vector2f();
        TexSize = new Vector2f(1,1);
    }
}
