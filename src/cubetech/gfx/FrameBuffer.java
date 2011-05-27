package cubetech.gfx;

import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.opengl.ARBTextureRectangle;
import cubetech.common.Common;
import org.lwjgl.opengl.EXTFramebufferObject;
import cubetech.misc.Ref;
import org.lwjgl.opengl.EXTFramebufferSRGB;
import org.lwjgl.opengl.EXTRescaleNormal;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;



/**
 *
 * @author mads
 */
public class FrameBuffer {
    private int fboId = 0;
    private int depthId = 0;
    private int fboColorId = 0;
    private boolean fboInited = false;

    private boolean useColor = true;
    private boolean useDepth = true;
    private int w, h;
    
    public FrameBuffer(boolean color, boolean depth, int w, int h) {
        useColor = color;
        useDepth = depth;
        this.w = w;
        this.h = h;
        InitFBO();
    }

    public int getTextureId() {
        return fboColorId;
    }

    public void Bind() {
        if(!fboInited) InitFBO();
        if(fboId == 0) return;

        bind(fboId);
    }

    private void bind(int id) {
        EXTFramebufferObject.glBindFramebufferEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, id );
        // re-enable color
        if(useDepth && !useColor) {
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        }
    }

    public void Unbind() {
        bind(0);
        // re-enable color
        if(useDepth && !useColor) {
            glDrawBuffer(GL_BACK);
            glReadBuffer(GL_BACK);
        }
    }
    
    private void InitFBO() {
        if(fboInited) return;
        fboInited = true;
        if(!Ref.glRef.caps.GL_EXT_framebuffer_object) return;

        

        // generate id
        fboId = EXTFramebufferObject.glGenFramebuffersEXT( );
        GLRef.checkError();
        if(fboId == 0) {
            Common.Log("Could not create FBO - got zero index back from OpenGL");
            return;
        }
        Bind(); GLRef.checkError();

        if(useColor) {
            // Create the color texture
            fboColorId = Ref.ResMan.CreateEmptyTexture(w,h,ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB,false);
            glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB, 0);
            GLRef.checkError();
            // Attach it
            EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
                    EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, // attachment
                    ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB, // textarget
                    fboColorId, // texture
                    0); // level
            GLRef.checkError();
        }

        if(useDepth && useColor) {
            depthId = EXTFramebufferObject.glGenRenderbuffersEXT(); GLRef.checkError();
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, depthId );
            GLRef.checkError();
            EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    GL14.GL_DEPTH_COMPONENT24,
                    w, h); GLRef.checkError();
            // attach depth buffer to fbo
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                    EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    depthId); GLRef.checkError();
        } else if(useDepth) {
            fboColorId = Ref.ResMan.CreateEmptyDepthTexture(512, 512, GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, 0);
            GLRef.checkError();
            // Attach it
            EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
                    EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, // attachment
                    GL_TEXTURE_2D, // textarget
                    fboColorId, // texture
                    0); // level

            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        }

        GLRef.checkError();

        validate();

        Unbind();
        
    }

    private void validate() {
        // Check for error
        int framebuffer = EXTFramebufferObject.glCheckFramebufferStatusEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT );
        switch ( framebuffer ) {
            case EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT:
                    break;
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
                    throw new RuntimeException( "FrameBuffer: " + fboId
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT exception" );
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
                    throw new RuntimeException( "FrameBuffer: " + fboId
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT exception" );
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                    throw new RuntimeException( "FrameBuffer: " + fboId
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT exception" );
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
                    throw new RuntimeException( "FrameBuffer: " + fboId
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT exception" );
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                    throw new RuntimeException( "FrameBuffer: " + fboId
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT exception" );
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
                    throw new RuntimeException( "FrameBuffer: " + fboId
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT exception" );
            default:
                    throw new RuntimeException( "Unexpected reply from glCheckFramebufferStatusEXT: " + framebuffer );
        }
    }

    public void BlitFBO() {
        if(!fboInited)
            InitFBO();

        if(fboId == 0) return;

        Ref.glRef.PushShader(Ref.glRef.getShader("RectBlit"));

        Unbind();
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glEnable(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB);
        glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB, fboColorId);
        glEnable(EXTFramebufferSRGB.GL_FRAMEBUFFER_SRGB_EXT);
        //Ref.ResMan.getWhiteTexture().Bind();

        Vector2f res = Ref.glRef.GetResolution();
        Vector2f TexOffset = new Vector2f(0, 0);
        Vector2f TexSize = new Vector2f(res);
        Vector2f Extent = new Vector2f(res);
        float depth = 0;
        Color color = (Color) Color.WHITE;

        glPushAttrib(GL_VIEWPORT_BIT);
        glViewport(0, 0, (int)res.x, (int)res.y);

         glBegin( GL_QUADS);
        {
            if(Ref.glRef.isShadersSupported() && Ref.glRef.shader != null) {
                // Fancy pants shaders
                 glVertexAttrib2f(2, TexOffset.x, TexOffset.y);
                 glVertexAttrib2f(3, 0,0);
                 glVertexAttrib4Nub(1, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertexAttrib3f(0, 0, 0, depth);

                 glVertexAttrib2f(2, TexOffset.x+TexSize.x, TexOffset.y);
                 glVertexAttrib2f(3, 1,0);
                 glVertexAttrib4Nub(1, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertexAttrib3f(0, Extent.x, 0, depth);

                 glVertexAttrib2f(2, TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
                 glVertexAttrib2f(3, 1,1);
                 glVertexAttrib4Nub(1, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertexAttrib3f(0, Extent.x, Extent.y, depth);

                 glVertexAttrib2f(2, TexOffset.x, TexOffset.y+TexSize.y);
                 glVertexAttrib2f(3, 0,1);
                 glVertexAttrib4Nub(1, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertexAttrib3f(0, 0, Extent.y, depth);
            } else {
                // Good ol' fixed function
                 glTexCoord2f(TexOffset.x, TexOffset.y);
                 glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertex3f( 0, 0, depth);

                 glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y);
                 glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertex3f( Extent.x, 0, depth);

                 glTexCoord2f(TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
                 glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertex3f( Extent.x, Extent.y, depth);

                 glTexCoord2f(TexOffset.x, TexOffset.y+TexSize.y);
                 glColor4ub(color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertex3f( 0, Extent.y, depth);
            }

        }
         glEnd();

        glPopAttrib();
        glDisable(EXTFramebufferSRGB.GL_FRAMEBUFFER_SRGB_EXT);
        glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB, 0);
        glDisable(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        Bind();
        Ref.glRef.PopShader();
    }
}
