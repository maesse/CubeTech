package cubetech.gfx;

import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import static org.lwjgl.opengl.GL11.*;
/**
 *
 * @author mads
 */
public class Sprite {
    
    public Vector2f Position;
    public Vector2f Size;
    CubeTexture Texture;
    Vector2f TexOffset;
    Vector2f TexSize;
    public float Angle = 0f;
    
    public Vector4f Color;

    public Sprite() {
        Texture = null;
        Color = new Vector4f(1,1,1,1f);
        TexOffset = new Vector2f();
        TexSize = new Vector2f(1,1);
        Size = new Vector2f();
    }

    public void Set(Vector2f Position, Vector2f Size, CubeTexture tex,
                    Vector2f texOffset, Vector2f texSize) {
        this.Position = Position;
        this.Size = Size;
        this.Texture = tex;
        this.TexOffset = new Vector2f(texOffset.x, texOffset.y);
        this.TexSize = new Vector2f(texSize.x, texSize.y);
        this.Color.x = this.Color.y = this.Color.z = this.Color.w = 1;
        Angle = 0;
    }

    public void Set(Vector2f Position, float radius) {
        Texture = null;
        this.Position = new Vector2f(Position.x, Position.y);
        this.Position.x -= radius;
        this.Position.y -= radius;
        this.Size = new Vector2f(radius*2f, radius*2f);
        this.Color.x = this.Color.y = this.Color.z = this.Color.w = 1;
        this.TexOffset.x = this.TexOffset.y = 0f;
        this.TexSize.x = this.TexSize.y = 1f;
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
        GL11.glTranslatef(Position.x+Size.x/2f, Position.y+Size.y/2f, 0);
        
        if(Angle != 0f)
            GL11.glRotatef(Angle * 180f/(float)Math.PI,0,0,1);

        // Texture coords are flipped on y axis
        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f(TexOffset.x, TexOffset.y+TexSize.y);
            GL11.glVertex2f(-Size.x/2f, -Size.y/2f);

            GL11.glTexCoord2f(TexOffset.x, TexOffset.y);
            GL11.glVertex2f(-Size.x/2f, Size.y/2f);

            GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y);
            GL11.glVertex2f(Size.x/2f, Size.y/2f);

            GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
            GL11.glVertex2f(Size.x/2f, -Size.y/2f);
        }
        GL11.glEnd();

        GL11.glPopMatrix();
    }
}
