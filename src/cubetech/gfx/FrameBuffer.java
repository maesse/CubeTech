package cubetech.gfx;

import cubetech.common.Helper;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.opengl.ARBTextureRectangle;
import org.lwjgl.opengl.EXTTextureRectangle;
import cubetech.common.Common;
import org.lwjgl.opengl.EXTFramebufferObject;
import cubetech.misc.Ref;
import org.lwjgl.opengl.EXTFramebufferSRGB;
import org.lwjgl.opengl.EXTRescaleNormal;
import org.lwjgl.opengl.EXTTextureArray;
import org.lwjgl.opengl.NVTextureRectangle;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
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

    private int Target = EXTTextureRectangle.GL_TEXTURE_RECTANGLE_EXT;

    Shader shader = null;
    private boolean npot = false; // non-power-of-two
    private int depthBitResolution = 24;
    private int arrayLevels = 1;

    private CubeTexture asTexture = null;

    public FrameBuffer(int depthSize, int nLevels, int nBits) {
        useColor = false;
        useDepth = true;
        Target = EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT;
        this.w = depthSize;
        this.h = depthSize;
        arrayLevels = nLevels;
        depthBitResolution = nBits;
        InitFBO();
    }

    public int getResolution() {
        return w;
    }
    
    public FrameBuffer(boolean color, boolean depth, int w, int h, int target, int nLevels, int nBits) {
        Target = target; 
        useColor = color;
        useDepth = depth;
        arrayLevels = nLevels;
        depthBitResolution = nBits;
        this.w = w;
        this.h = h;
        InitFBO();
    }
    
    public FrameBuffer(boolean color, boolean depth, int w, int h, int target) {
        Target = target; 
        useColor = color;
        useDepth = depth;
        this.w = w;
        this.h = h;
        InitFBO();
    }
    
    public FrameBuffer(boolean color, boolean depth, int w, int h) {
        
        if(w != h || Helper.get2Fold(w) != w)
        {
            npot = true;
        }
        

        if(npot) {
            shader = Ref.glRef.getShader("RectBlit");

            if( Ref.glRef.caps.GL_ARB_texture_rectangle) { // try arb
                Target = ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB;
            } else if(Ref.glRef.caps.GL_EXT_texture_rectangle) { 
                Target = EXTTextureRectangle.GL_TEXTURE_RECTANGLE_EXT;
            }else {
                Target = GL_TEXTURE_2D;
                // give up
                // fix: do gamma correction in software
                Ref.common.Error(Common.ErrorCode.FATAL, "Your graphics card doesn't support NPOT textures :/");
            }
        } else {
            Target = GL_TEXTURE_2D; 
        }

        useColor = color;
        useDepth = depth;
        this.w = w;
        this.h = h;
        InitFBO();
        
    }
    
    public CubeTexture getAsTexture() {
        if(asTexture == null) {
            asTexture = new CubeTexture(getTarget(), getDepthTextureId(), null);
        }
        return asTexture;
    }

    public int getTextureTarget() {
        return Target;
    }

    public void resize(int x, int y) {
        if(x == w && y == h) return; // no change
        
        this.w = x; this.h = y;
        Bind();

        if(useColor && !useDepth) {
            GL11.glDeleteTextures(fboColorId);
            fboColorId = Ref.ResMan.CreateEmptyTexture(w,h,Target,false, null);
            glBindTexture(Target, 0);
            GLRef.checkError();
            // Attach it
            EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
                    EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, // attachment
                    Target, // textarget
                    fboColorId, // texture
                    0); // level
        }
        else if(useColor && useDepth) {
            // Create the color texture
            GL11.glDeleteTextures(fboColorId);
            fboColorId = Ref.ResMan.CreateEmptyTexture(w,h,Target,false, null);
            glBindTexture(Target, 0);
            GLRef.checkError();
            // Attach it
            EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
                    EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, // attachment
                    Target, // textarget
                    fboColorId, // texture
                    0); // level

            GL11.glDeleteTextures(depthId);
            depthId = Ref.ResMan.CreateEmptyDepthTexture(w, h, depthBitResolution, Target);
            glBindTexture(Target, 0);
            GLRef.checkError();
            // Attach it
            EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
                    EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, // attachment
                    Target, // textarget
                    depthId, // texture
                    0); // level
        } else if(useDepth) {
            GL11.glDeleteTextures(fboColorId);
            if(Target == EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT) {
                fboColorId = Ref.ResMan.CreateEmptyDepthTexture(w, h, depthBitResolution, Target, arrayLevels);
            } else {
                fboColorId = Ref.ResMan.CreateEmptyDepthTexture(w, h, depthBitResolution, Target);
            }
            glTexParameteri(Target, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(Target, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glBindTexture(Target, 0);
            GLRef.checkError();
            // Attach it
            EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
                    EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, // attachment
                    Target, // textarget
                    fboColorId, // texture
                    0); // level
        }

        validate();
        GLRef.checkError();
    }

    public void destroy() {
        if(!fboInited) return;

        if(useColor && useDepth) {
            //EXTFramebufferObject.glDeleteRenderbuffersEXT(depthId);
            GL11.glDeleteTextures(depthId);
            GL11.glDeleteTextures(fboColorId);
            fboColorId = 0;
            depthId = 0;
        } else if(useDepth) {
            GL11.glDeleteTextures(fboColorId);
            fboColorId = 0;
        }

        EXTFramebufferObject.glDeleteFramebuffersEXT(fboId);
        fboId = 0;
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
        if(useDepth && !useColor && id != 0) {
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        }
        else if(useColor && id != 0) {
            glDrawBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
            glReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
        } else if(!useColor && id == 0) {
            glDrawBuffer(GL_BACK);
            glReadBuffer(GL_BACK);
        }
        GLRef.checkError();
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
            fboColorId = Ref.ResMan.CreateEmptyTexture(w,h,Target,false, null);
            glBindTexture(Target, 0);
            GLRef.checkError();
            // Attach it
            int bindTarget = Target;
            if(Target == GL_TEXTURE_CUBE_MAP) bindTarget = GL_TEXTURE_CUBE_MAP_POSITIVE_X;
            EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
                    EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, // attachment
                    bindTarget, // textarget
                    fboColorId, // texture
                    0); // level
//            Bind();
            GLRef.checkError();
        }

        if(useDepth && useColor) {
                depthId = Ref.ResMan.CreateEmptyDepthTexture(w, h, depthBitResolution, Target);
                glBindTexture(Target, 0);
                GLRef.checkError();
                // Attach it
                EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
                        EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, // attachment
                        Target, // textarget
                        depthId, // texture
                        0); // level
        } else if(useDepth) {
            if(Target == EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT) {
                fboColorId = Ref.ResMan.CreateEmptyDepthTexture(w, h, depthBitResolution, Target, arrayLevels);
            } else {
                fboColorId = Ref.ResMan.CreateEmptyDepthTexture(w, h, depthBitResolution, Target);
            }
            
            glTexParameteri(Target, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(Target, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glBindTexture(Target, 0);
            GLRef.checkError();
            // Attach it
            
            if(Target == EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT) {
                EXTTextureArray.glFramebufferTextureLayerEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
                        EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, // attachment
                        fboColorId, // texture
                        0,  // mip level
                        0); // array index
                
            } 
//            else if(Target == GL_TEXTURE_CUBE_MAP) {
//                EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
//                        EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, // attachment
//                        GL_TEXTURE_CUBE_MAP_POSITIVE_X, // textarget
//                        fboColorId, // texture
//                        0); // level
//            } 
            else {
                EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, // target
                        EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, // attachment
                        Target, // textarget
                        fboColorId, // texture
                        0); // level
            }

            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        }
        

        GLRef.checkError();

        try {
            validate();
        } catch(RuntimeException ex) {
            // clean up
            destroy();

            throw ex;
        }
        GLRef.checkError();
        Unbind();
        GLRef.checkError();
    }

    private void validate() {
        // Check for error
        int framebuffer = EXTFramebufferObject.glCheckFramebufferStatusEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT );
        switch ( framebuffer ) {

            case EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT:
                    break;
            case EXTFramebufferObject.GL_FRAMEBUFFER_UNSUPPORTED_EXT:
                throw new RuntimeException( "FrameBuffer: " + fboId
                                    + ", has caused a GL_FRAMEBUFFER_UNSUPPORTED_EXT exception" );
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
        
        Ref.glRef.PushShader(shader);

        Unbind();
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glEnable(Target);
        glBindTexture(Target, fboColorId);
        glDisable(EXTFramebufferSRGB.GL_FRAMEBUFFER_SRGB_EXT);
//        
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
                 glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x, TexOffset.y);
                 glVertexAttrib2f(Shader.INDICE_COORDS2, 0,0);
                 glVertexAttrib4Nub(Shader.INDICE_COLOR, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertexAttrib3f(Shader.INDICE_POSITION, 0, 0, depth);

                 glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x+TexSize.x, TexOffset.y);
                 glVertexAttrib2f(Shader.INDICE_COORDS2, 1,0);
                 glVertexAttrib4Nub(Shader.INDICE_COLOR, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertexAttrib3f(Shader.INDICE_POSITION, Extent.x, 0, depth);

                 glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x+TexSize.x, TexOffset.y+TexSize.y);
                 glVertexAttrib2f(Shader.INDICE_COORDS2, 1,1);
                 glVertexAttrib4Nub(Shader.INDICE_COLOR, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertexAttrib3f(Shader.INDICE_POSITION, Extent.x, Extent.y, depth);

                 glVertexAttrib2f(Shader.INDICE_COORDS, TexOffset.x, TexOffset.y+TexSize.y);
                 glVertexAttrib2f(Shader.INDICE_COORDS2, 0,1);
                 glVertexAttrib4Nub(Shader.INDICE_COLOR, color.getRedByte(), color.getGreenByte(), color.getBlueByte(), color.getAlphaByte());
                 glVertexAttrib3f(Shader.INDICE_POSITION, 0, Extent.y, depth);
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
//
        glEnable(EXTFramebufferSRGB.GL_FRAMEBUFFER_SRGB_EXT);
        glBindTexture(Target, 0);
        glDisable(Target);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        Bind();
        Ref.glRef.PopShader();
    }

    public int getDepthTextureId() {
        if(!useColor && useDepth) return fboColorId;
        return depthId;
    }

    public int getTarget() {
        return Target;
    }
}
