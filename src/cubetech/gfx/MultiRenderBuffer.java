package cubetech.gfx;


import java.util.ArrayList;
import cubetech.CGame.ViewParams;
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
    // FBO handle
    int fbHandle;

    private MRBBuilder info = null;
    private CubeTexture[] asTexture = null;
    private IntBuffer intBuffer = null;    

    public enum Format {
        RGBA,
        RGBA32F,
        RGBA16F,
        DEPTH24
    }
    
    public MultiRenderBuffer(MRBBuilder builder) {
        // Error checking
        if(builder.ehs.isEmpty()) throw new IllegalArgumentException("Needs at least one format");
        if(builder.width <= 0 || builder.height <= 0) throw new IllegalArgumentException("Invalid width or height");
        this.info = builder;
        
        // Generate buffers
        fbHandle = glGenFramebuffersEXT();
        // Bind the FBO so that the next operations will be bound to it
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fbHandle);
        int targetCounter = 0;
        
        for (int i = 0; i < builder.ehs.size(); i++) {
            MRBBuilder.Eh eh = builder.ehs.get(i);
            int format = formatToOpenGL(eh.format);
            int target = GL_DEPTH_ATTACHMENT_EXT;
            if(format != GL_DEPTH_COMPONENT24) {
                target = GL_COLOR_ATTACHMENT0_EXT + targetCounter;
                targetCounter++;
            }
            
            if(eh.rtt) {
                // create texture handle
                int texHandle = eh.handle == -1 ? glGenTextures() : eh.handle;
                // bind the handle
                glBindTexture(GL_TEXTURE_2D, texHandle);
                
                if(eh.handle == -1) {
                    // allocate texture storage
                    glTexImage2D(GL_TEXTURE_2D, 0, format, builder.width, builder.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
                    // set default parameters
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                }
                
                // attach to fbo
                glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, target, GL_TEXTURE_2D, texHandle, 0);
                eh.handle = texHandle;
            } else {
                int rbHandle = eh.handle == -1 ? glGenRenderbuffersEXT() : eh.handle;
                
                // bind
                glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, rbHandle);
                if(eh.handle == -1) {
                    // create internal storage
                    glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, format, builder.width, builder.height);
                }
                // attach to fbo
                glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, target, GL_RENDERBUFFER_EXT, rbHandle);
                eh.handle = rbHandle;
            }
        }
        
//        // unbind any dangling buffers
        glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, 0);
        glBindTexture(GL_TEXTURE_2D, 0);

        // Just caching some structures
        asTexture = new CubeTexture[targetCounter];
        intBuffer = BufferUtils.createIntBuffer(targetCounter);
        
        int nColor = 0;
        intBuffer.clear();
        for (int i = 0; i < info.ehs.size(); i++) {
            MRBBuilder.Eh eh = info.ehs.get(i);
            if(eh.format == Format.DEPTH24) continue;
            intBuffer.put(GL_COLOR_ATTACHMENT0_EXT+nColor);
            nColor++;
        }
        intBuffer.flip();
        glDrawBuffers(intBuffer);
        
        // Check framebuffer validity
        int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
        if(status != GL_FRAMEBUFFER_COMPLETE_EXT) {
            throw new IllegalStateException("Framebuffer is not complete");
        }
        glDrawBuffers(GL_NONE);
        
        // unbind
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    }
    
    public MRBBuilder getInfo() {
        return info;
    }
    
    void dispose() {
        if(info != null) {
            for (int i = 0; i < info.ehs.size(); i++) {
                MRBBuilder.Eh eh = info.ehs.get(i);
                if(eh.handle != -1) {
                    if(eh.rtt) glDeleteTextures(eh.handle);
                    else glDeleteRenderbuffersEXT(eh.handle);
                    eh.handle = -1;
                }
            }
            info = null;
        }
        glDeleteFramebuffersEXT(fbHandle);
        fbHandle = 0;
        asTexture = null;
    }
    
    public void start(boolean depthOnly, ViewParams view) {
        // Bind our FBO and set the viewport to the proper size
	glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fbHandle);
        // save viewport so it can be restored
	glPushAttrib(GL_VIEWPORT_BIT);
        glViewport(view.ViewportX,view.ViewportY,view.ViewportWidth, view.ViewportHeight);

	// Clear the render targets
        if(!depthOnly && view.ViewportX == 0) {
            glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );
            glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
        }

	glEnable(GL_TEXTURE_2D);

	// Specify what to render an start acquiring
        if(depthOnly) {
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        } else {
            int nColor = 0;
            intBuffer.clear();
            for (int i = 0; i < info.ehs.size(); i++) {
                MRBBuilder.Eh eh = info.ehs.get(i);
                if(eh.format == Format.DEPTH24) continue;
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
        asTexture[i] = new CubeTexture(GL_TEXTURE_2D, info.ehs.get(i).handle, "MRT Texture");
        asTexture[i].loaded = true;
        return asTexture[i];
    }
    
//    public void showTexture(int i, float fSizeX, float fSizeY, float x, float y) {
//        Ref.glRef.pushShader("World");
//	int texture = texHandles[i];
//
//	//Projection setup
//	glMatrixMode(GL_PROJECTION);
//	glPushMatrix();
//	glLoadIdentity();
//	glOrtho(0,width,0,height,0.1f,2);	
//
//	//Model setup
//	glMatrixMode(GL_MODELVIEW);
//	glPushMatrix();
//
////	glActiveTextureARB(GL_TEXTURE0_ARB);
//	glEnable(GL_TEXTURE_2D);
//	glBindTexture(GL_TEXTURE_2D, texture);
//
//	// Render the quad
//	glLoadIdentity();
//	glTranslatef(x,-y,-1.0f);
//
//	glColor3f(1,1,1);
//	glBegin(GL_QUADS);
//	glTexCoord2f( 0, 1 );
//	glVertex3f(    0.0f,  (float) height, 0.0f);
//	glTexCoord2f( 0, 0 );
//	glVertex3f(    0.0f,   height-fSizeY, 0.0f);
//	glTexCoord2f( 1, 0 );
//	glVertex3f(   fSizeX,  height-fSizeY, 0.0f);
//	glTexCoord2f( 1, 1 );
//	glVertex3f(   fSizeX, (float) height, 0.0f);
//	glEnd();
//
//	glBindTexture(GL_TEXTURE_2D, 0);
//
//	//Reset to the matrices	
//	glMatrixMode(GL_PROJECTION);
//	glPopMatrix();
//	glMatrixMode(GL_MODELVIEW);
//	glPopMatrix();
//        Ref.glRef.PopShader();
//    }
    
    private static int formatToOpenGL(Format format) {
        switch(format) {
            case RGBA: return GL_RGBA8;
            case RGBA32F: return GL_RGBA32F_ARB;
            case RGBA16F: return GL_RGBA16F_ARB;
            case DEPTH24: return GL_DEPTH_COMPONENT24;
            default: throw new IllegalArgumentException("Unknown format");
        }
    }
    
    public int getHandle(Format f) {
        for (int i = 0; i < info.ehs.size(); i++) {
            MRBBuilder.Eh eh = info.ehs.get(i);
            if(eh.format == f) return eh.handle;
        }
        return -1;
    }
    
    public static class MRBBuilder {
        private int width, height;
        private ArrayList<Eh> ehs = new ArrayList<Eh>();
        private class Eh {
            Format format;
            boolean rtt;
            int handle;
            private Eh(Format f, boolean rtt, int h) {
                format = f; this.rtt = rtt; handle = h;
            }
        }
        
        public MRBBuilder(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        public MRBBuilder cloneFormats(int width, int height) {
            MRBBuilder b = new MRBBuilder(width, height);
            for (Eh eh : ehs) {
                b.addFormat(eh.format, eh.rtt);
            }
            return b;
        }
        
        public void addFormat(Format format, boolean renderToTexture) {
            addExistingBuffer(format, renderToTexture, -1);
        }
        
        public void addExistingBuffer(Format format, boolean renderToTexture, int handle) {
            ehs.add(new Eh(format, renderToTexture, handle));
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
    }
}
