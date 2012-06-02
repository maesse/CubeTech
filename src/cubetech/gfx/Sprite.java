package cubetech.gfx;

import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;
/**
 *
 * @author mads
 */
public class Sprite {
    private Vector2f Center;
    private Vector2f Extent;
    private Color color;
    private float Angle = 0f;
    private float depth = 0f;
    
    CubeTexture Texture;
    Vector2f TexOffset;
    Vector2f TexSize;

    public boolean invalid = false;

    // Data format: 32bytes stride for cache alignment
    // 12byte   Vector3f position
    // 8byte    Vector2f texcoords
    // 4byte    byte4u   color
    // 8byte    Vector2f padding
    public static final int STRIDE = (4)*4; // 32 bytes aligned
    float[] data = new float[20]; // pos, tex * 4
    boolean sort = false;
    boolean dirty = true;

    public Sprite() {
        Texture = null;
        color = new Color(1,1,1,1);
        TexOffset = new Vector2f();
        TexSize = new Vector2f(1,1);
        Extent = new Vector2f();
        Center = new Vector2f();
        sort = false;
        Angle = 0f;
    }

    public float getDepth() {
        return depth;
    }

    public void SetColor(int r, int g, int b, int a) {
        color.set(r, g, b, a);
        if(a < 255)
            sort = true;
        dirty = true;
    }

    public void SetDepth(float d) {
        depth = d;
        dirty = true;
    }

    public void SetColor(Color color) {
        SetColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        //SetColor(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
    }

    public void SetAngle(float angle) {
        this.Angle = angle;
        dirty = true;
    }

    // Derp. Queue OpenGL commands
    int special = 0;
    boolean value;
    int a, b, c, d;

    public void SetSpecial(int type, boolean value) {
        special = type;
        this.value = value;
        invalid = false;
    }

    public void SetSpecial(int type, int x, int y, int w, int h) {
        special = type;
        if(w < 0)
            w = -w;
        if(h < 0)
            h = -h;
        a=x;b=y;c=w;d=h;
        invalid = false;
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
        if(tex != null)
            sort = tex.needSort();
        if(texOffset != null) {
            TexOffset.x = texOffset.x;
            TexOffset.y = texOffset.y;
        } else {
            TexOffset.x = 0;
            TexOffset.y = 0;
        }
        if(texSize != null) {
            TexSize.x = texSize.x;
            TexSize.y = texSize.y;
        } else {
            TexSize.set(1,1);
        }
        color.set(255, 255, 255, 255);
        Angle = 0;
        special = 0;
        depth = 0;
        dirty = true;
        invalid = false;
    }

    // Position = Lower left
    // Size = Real size (eg. not half-size)
    public void Set(float x, float y, float w, float h, CubeTexture tex,
                    Vector2f texOffset, Vector2f texSize) {
        Center.x = x + (w/2f);
        Center.y = y + (h/2f);
        Extent.x = w / 2f;
        Extent.y = h / 2f;
        this.Texture = tex;
        if(tex != null)
            sort = tex.needSort();
        if(texOffset != null) {
            TexOffset.x = texOffset.x;
            TexOffset.y = texOffset.y;
        } else {
            TexOffset.x = 0;
            TexOffset.y = 0;
        }
        if(texSize != null) {
            TexSize.x = texSize.x;
            TexSize.y = texSize.y;
        } else {
            TexSize.set(1,1);
        }
        color.set(255, 255, 255, 255);
        Angle = 0;
        special = 0;
        depth = 0;
        dirty = true;
        invalid = false;
    }

    // Position = Lower left
    // Size = Real size (eg. not half-size)
    public void Set(float x, float y, float w, float h, CubeTexture tex) {
        Center.x = x + w/2f;
        Center.y = y + h/2f;
        Extent.x = w / 2f;
        Extent.y = h / 2f;
        this.Texture = tex;
        if(tex != null) sort = tex.needSort();
        TexOffset.x = 0;
        TexOffset.y = 0;
        TexSize.x = 1;
        TexSize.y = 1;
        color.set(255, 255, 255, 255);
        Angle = 0;
        special = 0;
        depth = 0;
        dirty = true;
        invalid = false;
    }

    public void Set(float x, float y, float radius) {
        Texture = null;
        Center.set(x, y);
        Extent.set(radius, radius);
        color.set(255, 255, 255, 255);
        TexOffset.set(0, 0);
        TexSize.set(1, 1);
        Angle = 0f;
        depth = 0;
        special = 0;
        dirty = true;
        invalid = false;
    }

    public void Set(Vector2f Position, float radius, CubeTexture tex) {
        Set(Position.x, Position.y, radius);
        Texture = tex;
        if(tex != null)
            sort = tex.needSort();
    }

    public void Set(float x, float y, float radius, CubeTexture tex) {
        Set(x, y, radius);
        Texture = tex;
        if(tex != null)
            sort = tex.needSort();
    }

    public void setLine(Vector2f start, Vector2f end) {
        float width = Ref.cgame.cg.refdef.FovX;
        setLine(start, end, width / Ref.glRef.GetResolution().x);
    }

    public void setLine(Vector2f start, Vector2f end, float thickness) {
        setLine(start,end,thickness, null);
    }
    public void setLine(Vector2f start, Vector2f end, float thickness, CubeTexture tex) {
        Vector2f delta = new Vector2f(end.x-start.x, end.y-start.y);
        float len = delta.length();
        Vector2f center = new Vector2f(start.x + delta.x * 0.5f, start.y + delta.y * 0.5f);
        float angle = (float)Math.atan2(delta.y, delta.x);

        Texture = tex;
        Center.set(center.x, center.y);
        Extent.set(len/2f, thickness * 0.5f);
        color.set(255, 255, 255, 255);
        TexOffset.set(0, 0);
        TexSize.set(1, 1);
        Angle = angle;
        depth = 0;
        special = 0;
        dirty = true;
        invalid = false;
    }

    private static float packCoords(float s, float t) {
        if(s > 1) s = 1; if(t > 1) t = 1;
        if(s < 0) s = 1-s;
        if(t < 0) t = 1-t;
        
        int packed = (int)(s * 4095) | (int)(t * 4095) << 12;
        return (float)packed / (float)(1 << 24);
    }
    
    public void FillBuffer(ByteBuffer buf) {
        if(buf == null) return;

        float sin=0,cos=1,sin2=0,cos2=1;
        if(Angle != 0) {
            sin2 = (float) Math.sin(Angle* -1f);
            cos2 = (float) Math.cos(Angle* -1f);
            sin = (float) Math.sin(Angle );
            cos = (float) Math.cos(Angle );
        }

        buf.putFloat(Center.x - (Extent.x * cos2 - Extent.y * sin2));
        buf.putFloat(Center.y + (Extent.y * cos2 + Extent.x * sin2));
        color.writeRGBA(buf);
        buf.putFloat(packCoords(TexOffset.x, TexOffset.y + TexSize.y));

        buf.putFloat(Center.x - (Extent.x * cos - Extent.y * sin));
        buf.putFloat(Center.y - (Extent.y * cos + Extent.x * sin));
        color.writeRGBA(buf);
        buf.putFloat(packCoords(TexOffset.x, TexOffset.y));

        buf.putFloat(Center.x + (Extent.x * cos2 - Extent.y*sin2));
        buf.putFloat(Center.y - (Extent.y * cos2 + Extent.x*sin2));
        color.writeRGBA(buf);
        buf.putFloat(packCoords(TexOffset.x + TexSize.x, TexOffset.y));

        buf.putFloat(Center.x + Extent.x*cos - Extent.y*sin);
        buf.putFloat(Center.y + Extent.y*cos + Extent.x*sin);
        color.writeRGBA(buf);
        buf.putFloat(packCoords(TexOffset.x  + TexSize.x, TexOffset.y+TexSize.y));
    }

    private void updateData() {
        int index = 0;
        float sin=0,cos=1,sin2=0,cos2=1;
        if(Angle != 0) {
            sin2 = (float) Math.sin(Angle* -1f);
            cos2 = (float) Math.cos(Angle* -1f);
            sin = (float) Math.sin(Angle );
            cos = (float) Math.cos(Angle );
        }
        
        data[index++] = Center.x - (Extent.x * cos2 - Extent.y * sin2);
        data[index++] = Center.y + (Extent.y * cos2 + Extent.x * sin2);
//        data[index++] = depth;
        data[index++] = TexOffset.x;
        data[index++] = TexOffset.y + TexSize.y;
        // color isn't float, so can't put it here
        index++;
        data[index++] = Center.x - (Extent.x * cos - Extent.y * sin);
        data[index++] = Center.y - (Extent.y * cos + Extent.x * sin);
//        data[index++] = depth;
        data[index++ ] = TexOffset.x;
        data[index++ ] = TexOffset.y;
index++;
        data[index++] = Center.x + (Extent.x * cos2 - Extent.y*sin2);
        data[index++] = Center.y - (Extent.y * cos2 + Extent.x*sin2);
//        data[index++] = depth;
        data[index++] = TexOffset.x + TexSize.x;
        data[index++] = TexOffset.y;
index++;
        data[index++] = Center.x + Extent.x*cos - Extent.y*sin;
        data[index++] = Center.y + Extent.y*cos + Extent.x*sin;
//        data[index++] = depth;
        data[index++] = TexOffset.x + TexSize.x;
        data[index++] = TexOffset.y+TexSize.y;
        index++;
        dirty = false;
    }

    public void doSpecial() {
        if(special == 0) return;
        if(special == GL11.GL_SCISSOR_TEST) {
            if(value)
                GL11.glEnable(special);
            else
                GL11.glDisable(special);
        } else if(special == GL11.GL_SCISSOR_BOX) {
            GL11.glScissor(a, b, c, d);
        }
    }

    public void DrawFromBuffer() {
        if(special != 0) {
            doSpecial();
            GLRef.checkError();
            return;
        }

        if(dirty) updateData();

        GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());

        // Texture coords are flipped on y axis
        GL11.glBegin(GL11.GL_QUADS);
        {
            // Good ol' fixed function
            GL20.glVertexAttrib2f(Shader.INDICE_COORDS,data[3], data[4]);
//            GL11.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
            GL11.glVertex3f(data[0], data[1], data[2]);

            GL20.glVertexAttrib2f(Shader.INDICE_COORDS,data[8], data[9]);
//            GL11.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
            GL11.glVertex3f(data[5], data[6], data[7]);

            GL20.glVertexAttrib2f(Shader.INDICE_COORDS,data[13], data[14]);
//            GL11.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
            GL11.glVertex3f(data[10], data[11], data[12]);

            GL20.glVertexAttrib2f(Shader.INDICE_COORDS,data[18], data[19]);
//            GL11.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
            GL11.glVertex3f(data[15], data[16], data[17]);

        }
        GL11.glEnd();

//        GLRef.checkError();
    }

    // Old immediate mode rendering
    public void Draw() {
        if(special != 0) {
            if(special == GL11.GL_SCISSOR_TEST) {
                if(value)
                    GL11.glEnable(special);
                else
                    GL11.glDisable(special);
            } else if(special == GL11.GL_SCISSOR_BOX) {
                GL11.glScissor(a, b, c, d);
            }
            GLRef.checkError();
            return;
        }
            
        GL11.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
        
        // draw a quad textured to match the sprite
        GL11.glPushMatrix();
        GL11.glTranslatef(Center.x, Center.y, 0);
        
        if(Angle != 0f)
            GL11.glRotatef(Angle * 180f/(float)Math.PI,0,0,1);

        // Texture coords are flipped on y axis
        GL11.glBegin(GL11.GL_QUADS);
        {
            if(Ref.glRef.isShadersSupported()) {
                // Fancy pants shaders
                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x, TexOffset.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, -Extent.x, -Extent.y, depth);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x+TexSize.x, TexOffset.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, Extent.x, -Extent.y, depth);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, Extent.x, Extent.y, depth);

                GL20.glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x, TexOffset.y+TexSize.y);
                GL20.glVertexAttrib4Nub(Shader.INDICE_COLOR, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                GL20.glVertexAttrib3f(Shader.INDICE_POSITION, -Extent.x, Extent.y, depth);
            } else {
                // Good ol' fixed function
                GL11.glTexCoord2f(TexOffset.x, TexOffset.y);
                GL11.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                GL11.glVertex3f( -Extent.x, -Extent.y, depth);

                GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y);
                GL11.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                GL11.glVertex3f( Extent.x, -Extent.y, depth);

                GL11.glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
                GL11.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                GL11.glVertex3f( Extent.x, Extent.y, depth);

                GL11.glTexCoord2f(TexOffset.x, TexOffset.y+TexSize.y);
                GL11.glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                GL11.glVertex3f( -Extent.x, Extent.y, depth);
            }
            
        }
        GL11.glEnd();
        
        GL11.glPopMatrix();
        GLRef.checkError();
    }
}
