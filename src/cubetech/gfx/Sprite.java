package cubetech.gfx;

import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
/**
 *
 * @author mads
 */
public class Sprite {

    private Vector2f Center;
    private Vector2f Extent;
    private Vector4f Color;
    private float Angle = 0f;
    
    CubeTexture Texture;
    Vector2f TexOffset;
    Vector2f TexSize;

    public Sprite() {
        Texture = null;
        Color = new Vector4f(1,1,1,1f);
        TexOffset = new Vector2f();
        TexSize = new Vector2f(1,1);
        Extent = new Vector2f();
        Center = new Vector2f();
        Angle = 0f;
    }

    public void SetCenter(Vector2f position) {
        Center.x = position.x;
        Center.y = position.y;
    }

    public void SetColor(Vector4f color) {
        Color.x = color.x;
        Color.y = color.y;
        Color.z = color.z;
        Color.w = color.w;
    }

    public void SetAngle(float angle) {
        this.Angle = angle;
    }


    // Position = Lower left
    // Size = Real size (eg. not half-size)
    public void Set(Vector2f Position, Vector2f Size, CubeTexture tex,
                    Vector2f texOffset, Vector2f texSize) {
        Center.x = Position.x + Size.x/2f;
        Center.y = Position.y + Size.y/2f;
        Extent.x = Size.x / 2f;
        Extent.y = Size.y / 2f;
        this.Texture = tex;
        TexOffset.x = texOffset.x;
        TexOffset.y = texOffset.y;
        //this.TexOffset = new Vector2f(texOffset.x, texOffset.y);
        TexSize.x = texSize.x;
        TexSize.y = texSize.y;
        //this.TexSize = new Vector2f(texSize.x, texSize.y);
        this.Color.x = this.Color.y = this.Color.z = this.Color.w = 1;
        Angle = 0;
    }

    public void Set(Vector2f Position, float radius) {
        Texture = null;
        Center.x = Position.x;
        Center.y = Position.y;
        Extent.x = Extent.y = radius;
        this.Color.x = this.Color.y = this.Color.z = this.Color.w = 1;
        this.TexOffset.x = this.TexOffset.y = 0f;
        this.TexSize.x = this.TexSize.y = 1f;
        Angle = 0f;
    }

    public void Set(Vector2f Position, float radius, CubeTexture tex) {
        Set(Position, radius);
        Texture = tex;
    }

    public void Draw() {
        if(Texture != null) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            Texture.Bind();
        } else
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            
        GL11.glColor4f(Color.x, Color.y, Color.z, Color.w);
        // draw a quad textured to match the sprite
        GL11.glPushMatrix();
        GL11.glTranslatef(Center.x, Center.y, 0);
        
        if(Angle != 0f)
            GL11.glRotatef(Angle * 180f/(float)Math.PI,0,0,1);

        // Texture coords are flipped on y axis
        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f(TexOffset.x, TexOffset.y+TexSize.y);
            GL11.glVertex2f(-Extent.x, -Extent.y);

            GL11.glTexCoord2f(TexOffset.x, TexOffset.y);
            GL11.glVertex2f(-Extent.x, Extent.y);

            GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y);
            GL11.glVertex2f(Extent.x, Extent.y);

            GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
            GL11.glVertex2f(Extent.x, -Extent.y);
        }
        GL11.glEnd();

        GL11.glPopMatrix();
    }
}
