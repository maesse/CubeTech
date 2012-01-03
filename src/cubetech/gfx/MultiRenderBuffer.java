package cubetech.gfx;

import cubetech.misc.Ref;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.ARBTextureFloat.*;

/**
 *
 * @author Mads
 */
public class MultiRenderBuffer {
    int fbHandle;
    int width, height;
    int[] rbHandles = null;
    int[] texHandles = null;
    Format[] formats = null;
    IntBuffer intBuffer = null;
    CubeTexture[] asTexture = null;

    void dispose() {
        for (int i = 0; i < rbHandles.length; i++) {
            glDeleteRenderbuffersEXT(rbHandles[i]);
        }
        glDeleteFramebuffersEXT(fbHandle);
        for (int i = 0; i < texHandles.length; i++) {
            glDeleteTextures(texHandles[i]);
        }
        fbHandle = 0;
        rbHandles = null;
        texHandles = null;
        asTexture = null;
    }

    
    
    public enum Format {
        RGBA,
        RGBA32F,
        RGBA16F,
        DEPTH24
    }
    
    public MultiRenderBuffer(int width, int height, Format[] formats) {
        // Error checking
        if(formats == null || formats.length == 0) throw new IllegalArgumentException("Needs at least one format");
        int maxCount = glGetInteger(GL_MAX_RENDERBUFFER_SIZE_EXT);
        int count = formats.length;
        if(count > maxCount) throw new IllegalArgumentException("Max render buffers: " + maxCount);
        if(width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid width or height");
        
        this.width = width; this.height = height;
        this.asTexture = new CubeTexture[formats.length];
        
        // Generate buffers
        fbHandle = glGenFramebuffersEXT();        
        rbHandles = new int[count];
        for (int i = 0; i < count; i++) {
            rbHandles[i] = glGenRenderbuffersEXT();
        }
        
        // Bind the FBO so that the next operations will be bound to it
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fbHandle);
        
        // bind the render targets
        this.formats = formats;
        int nColor = 0;
        for (int i = 0; i < count; i++) {
            int format = formatToOpenGL(formats[i]);
            int rbHandle = rbHandles[i];
            int rbtarget = GL_DEPTH_ATTACHMENT_EXT;
            if(format != GL_DEPTH_COMPONENT24) {
                rbtarget = GL_COLOR_ATTACHMENT0_EXT + nColor;
                nColor++;
            }
            
            glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, rbHandle);
            glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, format, width, height);
            glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, rbtarget, GL_RENDERBUFFER_EXT, rbHandle);
        }
        
        intBuffer = BufferUtils.createIntBuffer(nColor);
        texHandles = new int[count];
        nColor = 0;
        for (int i = 0; i < count; i++) {
            if(formats[i] == Format.DEPTH24) {
                texHandles[i] = Ref.ResMan.CreateEmptyDepthTexture(width, height, 24, GL_TEXTURE_2D);
                glFramebufferTexture2DEXT( GL_FRAMEBUFFER_EXT, // target
                        GL_DEPTH_ATTACHMENT_EXT, // attachment
                        GL_TEXTURE_2D, // textarget
                        texHandles[i], // texture
                        0); // level
                continue;
            }
            texHandles[i] = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texHandles[i]);
            glTexImage2D(GL_TEXTURE_2D, 0, formatToOpenGL(formats[i]), width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT + nColor, GL_TEXTURE_2D, texHandles[i], 0);
            nColor++;
        }
        
        int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
        if(status != GL_FRAMEBUFFER_COMPLETE_EXT) {
            throw new IllegalStateException("Framebuffer is not complete");
        }
        
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    }
    
    public void start(boolean depthOnly) {
        // Bind our FBO and set the viewport to the proper size
	glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fbHandle);
	glPushAttrib(GL_VIEWPORT_BIT);
	glViewport(0,0,width, height);

	// Clear the render targets
        if(!depthOnly) {
            glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );
            glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
        }

	//glActiveTextureARB(GL_TEXTURE0_ARB);
	glEnable(GL_TEXTURE_2D);

	// Specify what to render an start acquiring
        if(depthOnly) {
            //glReadBuffer(GL_BACK);
        } else {
            int nColor = 0;
            intBuffer.clear();
            for (int i = 0; i < formats.length; i++) {
                if(formats[i] == Format.DEPTH24) continue;
                intBuffer.put(GL_COLOR_ATTACHMENT0_EXT+nColor);
                nColor++;
            }
            intBuffer.flip();
            glDrawBuffers(intBuffer);
        }
        GLRef.checkError();
    }
    
    public void stop() {
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
        glPopAttrib();
    }
    
    public CubeTexture asTexture(int i) {
        if(asTexture[i] != null) return asTexture[i];
        asTexture[i] = new CubeTexture(GL_TEXTURE_2D, texHandles[i], "MRT Texture");
        asTexture[i].loaded = true;
        return asTexture[i];
    }
    
    public void showTexture(int i, float fSizeX, float fSizeY, float x, float y) {
        Ref.glRef.pushShader("World");
	int texture = texHandles[i];

	//Projection setup
	glMatrixMode(GL_PROJECTION);
	glPushMatrix();
	glLoadIdentity();
	glOrtho(0,width,0,height,0.1f,2);	

	//Model setup
	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();

//	glActiveTextureARB(GL_TEXTURE0_ARB);
	glEnable(GL_TEXTURE_2D);
	glBindTexture(GL_TEXTURE_2D, texture);

	// Render the quad
	glLoadIdentity();
	glTranslatef(x,-y,-1.0f);

	glColor3f(1,1,1);
	glBegin(GL_QUADS);
	glTexCoord2f( 0, 1 );
	glVertex3f(    0.0f,  (float) height, 0.0f);
	glTexCoord2f( 0, 0 );
	glVertex3f(    0.0f,   height-fSizeY, 0.0f);
	glTexCoord2f( 1, 0 );
	glVertex3f(   fSizeX,  height-fSizeY, 0.0f);
	glTexCoord2f( 1, 1 );
	glVertex3f(   fSizeX, (float) height, 0.0f);
	glEnd();

	glBindTexture(GL_TEXTURE_2D, 0);

	//Reset to the matrices	
	glMatrixMode(GL_PROJECTION);
	glPopMatrix();
	glMatrixMode(GL_MODELVIEW);
	glPopMatrix();
        Ref.glRef.PopShader();
    }
    
    private static int formatToOpenGL(Format format) {
        switch(format) {
            case RGBA: return GL_RGBA;
            case RGBA32F: return GL_RGBA32F_ARB;
            case RGBA16F: return GL_RGBA16F_ARB;
            case DEPTH24: return GL_DEPTH_COMPONENT24;
            default: throw new IllegalArgumentException("Unknown format");
        }
    }
}
