package cubetech.gfx;

import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.util.Color;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL13;
import cubetech.ui.UI.MENU;
import cubetech.common.Commands;
import cubetech.common.Common;
import java.util.ArrayList;
import java.net.URL;
import java.applet.Applet;
import java.util.HashMap;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.PixelFormat;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.opengl.GL12;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.vector.Vector2f;
import cubetech.common.ICommand;
import cubetech.common.CVarFlags;
import java.util.EnumSet;
import cubetech.common.CVar;
import cubetech.common.Common.ErrorCode;
import cubetech.misc.Ref;
import java.awt.Canvas;
import java.net.MalformedURLException;
import org.lwjgl.opengl.ARBTextureRectangle;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
/**
 * Controls OpenGL
 * @author mads
 */
public class GLRef {
    private static boolean initialized = false;
    private static Vector2f MinimumResolution = new Vector2f(800,600);

    private DisplayMode currentMode;
    private boolean screenHasFocus = false;
    private DisplayMode[] availableModes;
    private DisplayMode desktopMode;

    private CVar r_vsync;
    private CVar r_mode;
    private CVar r_fullscreen;
    private CVar r_refreshrate; // A hint for selecting modes
    private CVar r_toggleRes;
    private Vector2f resolution;

    public Shader shader = null; // current shader
    public HashMap<String, Shader> shaders = new HashMap<String, Shader>();

    private boolean shadersSupported = true;
    private boolean isMac = false;
    private ContextCapabilities caps = null;

    private int maxVertices = 4096;
    private int maxIndices = 4096;
    
    private boolean isApplet = false;
    private Applet applet = null;
    private Canvas displayParent;

    private IntBuffer intBuf = BufferUtils.createIntBuffer(1);
    private IntBuffer intBuf16 = BufferUtils.createIntBuffer(16);
    private HashMap<Integer, ByteBuffer> cachedBuffers
            = new HashMap<Integer, ByteBuffer>();

    public boolean fboSupported = false;
    

    public enum BufferTarget {
        Vertex,
        Index
    }

    public GLRef() {
        Init();
    }

    public void InitWindow(Canvas parent, Applet applet, boolean lowGraphics) throws Exception {

        this.applet = applet;

        availableModes = Display.getAvailableDisplayModes();
        shadersSupported = !lowGraphics;
        desktopMode = Display.getDesktopDisplayMode();

        

        Common.Log("Desktop displaymode: " + desktopMode);
        displayParent = parent; // Save off canvas if there is one
        SetResolution(r_mode.sValue);

        if(parent == null) {
            // If we are creating a new window, center it
            Display.setLocation(-1,-1);
        } else {
            Display.setParent(displayParent); // Applets use this

            if(applet != null)
                isApplet = true;
        }

        // Create the display
        //Display.create(new PixelFormat());
        Display.create(new PixelFormat(8, 8, 0, 4));
        checkError();

        // Set vsync
        try {
            Display.setVSyncEnabled(r_vsync.iValue == 1);
        } catch (Exception ex) {};

        initialized = true;
        // Get max vertices
        intBuf16.position(0);
        glGetInteger(GL12.GL_MAX_ELEMENTS_VERTICES, intBuf16);
        checkError();
        maxVertices = intBuf16.get(0);

        // Get max indices
        intBuf16.position(0);
        glGetInteger(GL12.GL_MAX_ELEMENTS_INDICES, intBuf16);
        checkError();
        maxIndices = intBuf16.get(0);
        String osName = System.getProperty("os.name");
        isMac = osName.startsWith("Mac OS X");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        String jvmVersion = System.getProperty("java.runtime.version");
        Common.Log("Operating System: " + osName + " - " + osVersion + " (" + osArch + ")");
        Common.Log("Java version: " + jvmVersion + " :: " + System.getProperty("java.vm.name") + " v: " + System.getProperty("java.vm.version"));
        Common.Log("LWJGL version: " + Display.getVersion());
        Common.Log("OpenGL version: " + glGetString(GL_VERSION));
        Common.Log("VBO support detected (V: " + maxVertices + ") (I: " + maxIndices + ")");
        caps = GLContext.getCapabilities();


        OnPostDisplayCreate();
    }

    // Starts up stuff that was waiting for a display to be created
    public void OnPostDisplayCreate() throws Exception {

        currentMode = Display.getDisplayMode();
        checkError();
        resolution = new Vector2f(currentMode.getWidth(), currentMode.getHeight());

        

        String v = glGetString(GL_VERSION);
        String[] tokens = Commands.TokenizeString(v, true);
        for (int i= 0; i < tokens.length; i++) {
            String token = tokens[i];
            if(!Character.isDigit(token.charAt(0)))
                continue;
            String subTokens[] = token.split("\\.");
            if(subTokens.length < 2)
                continue;
            if(Character.isDigit(subTokens[0].charAt(0)) && subTokens[0].charAt(0) <= '1') {
                shadersSupported = false;
                Common.Log("WARNING: Your graphics card does not support shaders");
                break;
            }
        }

        if(isMac)
            shadersSupported = false;

        if(!CheckCaps()) {
            //Ref.common.Error(ErrorCode.FATAL, "Your grahics card is not supported");
            shadersSupported = false;
        }
        doVaoWorkaround();

        if(shadersSupported)
            loadShaders();

        // Set default states
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
        glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);
        glDepthMask(true);
        glDepthFunc(GL_LEQUAL);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glViewport(0, 0, currentMode.getWidth(), currentMode.getHeight());
        checkError();

        glDisable(GL_CULL_FACE);

        InitFBO();


        // Init systems waiting for opengl
        if(Ref.textMan != null)
            Ref.textMan.Init();

        if(isApplet())
            setAppletSize();

        // There may be an error sitting in OpenGL.
        // If it isn't cleared, it may trigger an exception later on.
        checkError();
    }

    private int fboId = 0;
    private int fboColorId = 0;

    private void InitFBO() {
        if(!fboSupported)
            return;

        IntBuffer buffer = ByteBuffer.allocateDirect(1*4).order(ByteOrder.nativeOrder()).asIntBuffer(); // allocate a 1 int byte buffer
        EXTFramebufferObject.glGenFramebuffersEXT( buffer ); // generate
        fboId = buffer.get();

        checkError();
        if(fboId == 0) {
            fboSupported = false;
            Common.Log("Could not create FBO - got zero index back from OpenGL");
            return;
        }

        // Create the texture
        fboColorId = Ref.ResMan.CreateEmptyTexture((int)GetResolution().x, (int)GetResolution().y);
        Ref.ResMan.CreateEmptyTexture((int)GetResolution().x, (int)GetResolution().y);
        EXTFramebufferObject.glBindFramebufferEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId );
        EXTFramebufferObject.glFramebufferTexture2DEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
                ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB, fboColorId, 0);
        glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB, 0);
        
//        checkError();

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

    public void BindFBO() {
        if(!fboSupported)
            return;
        
        
        EXTFramebufferObject.glBindFramebufferEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);

    }

    public void UnbindFBO() {
        if(!fboSupported)
            return;

        
        EXTFramebufferObject.glBindFramebufferEXT( EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
        
    }

    public Shader getShader(String str) {
        return shaders.get(str);
    }

    public void BlitFBO() {
        if(!fboSupported)
            return;

        if(Ref.cgame == null || Ref.cgame.cgr.sunPositionOnScreen == null)
            return;

        setShader("scatter");

        int SunPosition_uniform = ARBShaderObjects.glGetUniformLocationARB(getShader("scatter").getShaderId(), "lightPositionOnScreen");
        glUniform2f(SunPosition_uniform, Ref.cgame.cgr.sunPositionOnScreen.x, Ref.cgame.cgr.sunPositionOnScreen.y);
        
        
        glPushAttrib(GL_VIEWPORT_BIT);
        glViewport(0, 0, (int)GetResolution().x, (int)GetResolution().y);
        
        CubeTexture.Unbind();
        
        glDisable(GL_TEXTURE_2D);
        glEnable(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB);
        glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB, fboColorId);
//        glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB, fboColorId);
        
        Vector2f TexOffset = new Vector2f(0, 0);
        Vector2f TexSize = new Vector2f(GetResolution());
        Vector2f Extent = new Vector2f(GetResolution().x, GetResolution().y);
//        Extent.scale(0.5f);
        float depth = 0;
        Color color = (Color) Color.WHITE;
//        color.setAlpha((byte)127);

         glBegin( GL_QUADS);
        {
            if(Ref.glRef.isShadersSupported()) {
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
        glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB, 0);
        glDisable(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB);
        glEnable(GL_TEXTURE_2D);
        setShader("sprite");
    }

    // Set up cvars
    private void Init() {
        r_vsync = Ref.cvars.Get("r_vsync", "1", EnumSet.of(CVarFlags.ARCHIVE));
        r_mode = Ref.cvars.Get("r_mode", "1024x768", EnumSet.of(CVarFlags.ARCHIVE));
        r_fullscreen = Ref.cvars.Get("r_fullscreen", "0", EnumSet.of(CVarFlags.ARCHIVE));
        r_refreshrate = Ref.cvars.Get("r_refreshrate", "60", EnumSet.of(CVarFlags.ARCHIVE));
        r_toggleRes = Ref.cvars.Get("r_toggleRes", "0", EnumSet.of(CVarFlags.NONE));
        
        Ref.commands.AddCommand("listmodes", Cmd_listmodes);
        
        // Don't set all these at the first frame, it is done during init
        r_toggleRes.modified = false;
        r_vsync.modified = false;
        r_mode.modified = false;
        r_fullscreen.modified = false;
        r_refreshrate.modified = false;
    }

    // Mainly for checking if cvars have changed
    public void Update() {
        if(r_toggleRes.modified) {
            if(r_toggleRes.iValue == 1) {
                toggleResolution(true);
            } else if(r_toggleRes.iValue == -1 
                    && GetResolution().x > MinimumResolution.x
                    && GetResolution().y > MinimumResolution.y) {
                toggleResolution(false);
            }
            
            Ref.cvars.Set2("r_toggleRes", "0", true);
            r_toggleRes.modified = false;
        }

        // Change VSync
        if(r_vsync.modified) {
            try {
                if (!Display.isCurrent()) {
                    Ref.common.Error(ErrorCode.FATAL, "Cannot change vsync; current thread is not the OpenGL thread");
                }
                Display.setVSyncEnabled(r_vsync.iValue == 1);
                checkError();
            } catch (Exception ex) {
                Common.LogDebug(Common.getExceptionString(ex));
            }

            r_vsync.modified = false;
        }

        // Change resolution
        if(r_mode.modified) {
            String newMode = r_mode.sValue;
            SetResolution(newMode);
            r_mode.modified = false;
        }

        // Toggle fullscreen
        if(r_fullscreen.modified) {
            SetFullscreen(r_fullscreen.iValue == 1);
            r_fullscreen.modified = false;
        }

        // Check if screen has lost focus
        if(screenHasFocus != Display.isActive()) {
            screenHasFocus = !screenHasFocus;
            Ref.cvars.Set2("com_unfocused", screenHasFocus?"0":"1", true);
            if(Ref.common.developer.iValue == 0) {
                if(!screenHasFocus)
                    Ref.ui.SetActiveMenu(MENU.MAINMENU);
            }
            if(!screenHasFocus && Ref.Input != null) {
                // Clear keys when loosing focus..
                Ref.Input.ClearKeys();
            }
        }
    }

    private void SetFullscreen(boolean fullscreen) {
        try {
            if(fullscreen && !currentMode.isFullscreenCapable()) {
                Common.Log("Current resolution is not fullscreen capable");
                return;
            }

            if(Ref.Input != null)
                Ref.Input.ClearKeys();

            if(fullscreen) {
                Display.setDisplayModeAndFullscreen(currentMode);
                checkError();
            }
            else {
                Display.setFullscreen(false);
                checkError();
                Display.setDisplayMode(currentMode);
                checkError();
                // Center window if not running in custom window
                if(displayParent == null) {
                    Display.setLocation((int)(desktopMode.getWidth()/2f - currentMode.getWidth()/2f), (int)(desktopMode.getHeight()/2f - currentMode.getHeight()/2f));
                    checkError();
                }
            }

        } catch (Exception ex) {
            Common.Log(Common.getExceptionString(ex));
        }
    }

    public DisplayMode[] getNiceModes() {
        ArrayList<DisplayMode> modes = new ArrayList<DisplayMode>();
        // Look through available modes
        for (int i= 0; i < availableModes.length; i++) {
            DisplayMode validmode = availableModes[i];

            if(validmode.getBitsPerPixel() != desktopMode.getBitsPerPixel())
                continue; // We want the same pixeldepth as the desktop mode

            if(validmode.getFrequency() != desktopMode.getFrequency()
                    && validmode.getFrequency() != r_refreshrate.iValue
                    && validmode.getFrequency() != 0)
                continue; // We also want the same frequency

            modes.add(validmode);
        }

        DisplayMode[] mode_array = new DisplayMode[modes.size()];
        modes.toArray(mode_array);
        return mode_array;
    }

    // Expecting "800x600" or "800:600"
    public void SetResolution(String mode) {
        int xIndex = mode.indexOf("x");
        if(xIndex == -1 && (xIndex = mode.indexOf(":")) == -1) {
            Common.Log("Invalid displaymode: " + mode);
            r_mode.sValue = currentMode.getWidth()+"x"+currentMode.getHeight();
            return; // not valid
        }

        if(Ref.Input != null)
            Ref.Input.ClearKeys();

        // Check for integers on both side on the delimiter
        try {
            int width, height;
            width = Integer.parseInt(mode.substring(0, xIndex));
            height = Integer.parseInt(mode.substring(xIndex+1, mode.length()));

            // Look through availableModes
            DisplayMode newmode = null;
            for (int i= 0; i < availableModes.length; i++) {
                DisplayMode validmode = availableModes[i];
                
                if(validmode.getWidth() == width
                        && validmode.getHeight() == height) {
                    // Found one
                    newmode = validmode;
                    if(validmode.getBitsPerPixel() != desktopMode.getBitsPerPixel())
                        continue; // We wan't the same pixeldepth as the desktop mode

                    if(validmode.getFrequency() != desktopMode.getFrequency()
                            && validmode.getFrequency() != r_refreshrate.iValue && validmode.getFrequency() != 0)
                        continue; // We also want the same frequency

                    break;
                }
            }
            
            if(Display.isFullscreen() && !newmode.isFullscreenCapable()) {
                Common.Log("Displaymode not valid in fullscreen");
                r_mode.sValue = currentMode.getWidth()+"x"+currentMode.getHeight();
                return;
            }

            Common.Log("Setting displaymode: " + newmode);
            if(Display.isFullscreen()) {
                Display.setDisplayModeAndFullscreen(newmode);
                checkError();
                Display.setDisplayMode(newmode); // Derp. This call is also needed.
                checkError();
            } else {
                Display.setDisplayMode(newmode);
                checkError();
            }
            
            // Adjust viewport
            if(Display.isCreated()) {
                glViewport(0, 0, newmode.getWidth(), newmode.getHeight());
                checkError();
            }
            currentMode = newmode;
            resolution = new Vector2f(currentMode.getWidth(), currentMode.getHeight());
            r_mode.sValue = currentMode.getWidth()+"x"+currentMode.getHeight();
        } catch(NumberFormatException e) { // Can be NumberFormatException and IndexOutOfBounds
            Common.Log("Invalid displaymode: " + mode);
            r_mode.sValue = currentMode.getWidth()+"x"+currentMode.getHeight();
        } catch(IndexOutOfBoundsException e){
            Common.Log("Invalid displaymode: " + mode);
            r_mode.sValue = currentMode.getWidth()+"x"+currentMode.getHeight();
        } catch (LWJGLException ex){
            Common.Log("Invalid displaymode: " + mode);
            Common.Log("LWJGL error: " + ex);
            r_mode.sValue = currentMode.getWidth()+"x"+currentMode.getHeight();
        }

        if(isApplet())
            setAppletSize();
    }

    public void setAppletSize() {
        try {
            applet.setSize((int)GetResolution().x, (int)GetResolution().y);
            applet.getAppletContext().showDocument(new URL("javascript:doResize(" + (int)GetResolution().x + "," + (int)GetResolution().y + ")"));
        } catch (MalformedURLException ex) {
        }
    }

    // Closes the context and window
    public void Destroy() {
        if(Display.isCreated())
            Display.destroy();
        initialized = false;
    }

    // Increases or decreases the resolution by one step
    private void toggleResolution(boolean increase) {
        Vector2f current = GetResolution();
        float area = (int)current.x * (int)current.y;
        float bestArea = area;
        DisplayMode bestFit = null;
        for (DisplayMode displayMode : availableModes) {
            if(current.x == displayMode.getWidth()
                    && current.y == displayMode.getHeight())
                continue;

            float modeArea = displayMode.getWidth() * displayMode.getHeight();

            if(increase && modeArea < area)
                continue;
            if(!increase && modeArea > area)
                continue;

            if(displayMode.getBitsPerPixel() != desktopMode.getBitsPerPixel())
                continue; // We wan't the same pixeldepth as the desktop mode

            if(displayMode.getFrequency() != desktopMode.getFrequency()
                    && displayMode.getFrequency() != r_refreshrate.iValue)
                continue; // We also want the same frequency

            // first fit
            if(bestFit == null)
            {
                bestFit = displayMode;
                bestArea = modeArea;
                continue;
            }

            if(increase && modeArea < bestArea) {
                bestFit = displayMode;
                bestArea = modeArea;
            }
            if(!increase && modeArea > bestArea) {
                bestFit = displayMode;
                bestArea = modeArea;
            }
        }

        if(bestFit != null)
            SetResolution(bestFit.getWidth()+"x"+bestFit.getHeight());
    }

    public static void checkError() {
        if(!initialized)
            return;
        int error = glGetError();
        if(error != GL_NO_ERROR)
            throw new RuntimeException("OpenGL error: " + error);
    }

    public void setShader(String str) {
        Shader s = shaders.get(str);
        shader = s;
        s.Bind();
    }

    private void loadShaders() {
        try {
            shaders.put("sprite", new Shader("gfx/sprite"));
            shaders.put("imgspace", new Shader("gfx/imgspace"));
            shaders.put("scatter", new Shader("gfx/scatter"));
            shaders.put("blackshader", new Shader("gfx/blackshader"));

            setShader("sprite");
        } catch (Exception ex) {
            Logger.getLogger(GLRef.class.getName()).log(Level.SEVERE, null, ex);
            Ref.common.Error(ErrorCode.FATAL, "Failed to load graphics shaders\n" + Common.getExceptionString(ex));
        }

    }

    private boolean CheckCaps() {
        boolean okay = true;
        if(shadersSupported) {
            if(!caps.GL_ARB_vertex_buffer_object) {
                okay = false;
                Common.Log("ARB_Vertex_Buffer_Object not supported by your graphics card");
            }
            if(!caps.GL_ARB_vertex_shader) {
                okay = false;
                Common.Log("ARB_vertex_shader is not supported by your graphics card.");
            }
    //        if(!caps.GL_ARB_geometry_shader4) {
    //            okay = false;
    //            System.out.println("ARB_Geometry_Shader4 not supported by your graphics card");
    //        }
            if(!caps.GL_EXT_draw_range_elements) {
                okay = false;
                Common.Log("EXT_draw_range_elements not supported by your graphics card");
            }

            if(!caps.GL_EXT_framebuffer_object) {
                fboSupported = false;
                Common.Log("EXT_framebuffer_object not supported by your graphics card");
            } else if(!caps.GL_EXT_texture_rectangle) {
                fboSupported = false;
                Common.Log("GL_EXT_texture_rectangle not supported by your graphics card");
            } else if(okay)
                fboSupported = true;

        }

        return okay;
    }

    public boolean isShadersSupported() {
        return shadersSupported;
    }

    public boolean isInitalized() {
        return initialized;
    }

    public Vector2f GetResolution() {
        return resolution;
    }

    public boolean isApplet() {
        return isApplet;
    }

    public Applet getApplet() {
        if(isApplet())
            return applet;
        return null;
    }

//    public boolean isScreenFocused() {
//        return screenHasFocus;
//    }

    private ICommand Cmd_listmodes = new ICommand() {
      public void RunCommand(String[] args) {
            boolean skipUninteresting = true; // default to non-spammy mode
            if(args.length > 1)
                skipUninteresting = false;
            for (int i= 0; i < availableModes.length; i++) {
                if(skipUninteresting) {
                    // Skip if BPP doesn't match
                    if(availableModes[i].getBitsPerPixel() != desktopMode.getBitsPerPixel())
                        continue;
                    // Skip if refreshrate doesn't match
                    if(availableModes[i].getFrequency() != desktopMode.getFrequency()
                            && availableModes[i].getFrequency() != r_refreshrate.iValue)
                        continue;
                }

                System.out.println(availableModes[i]);
            }
        }
    };

    // Some drivers mess up VBO's when there's no VAO's bound (even if not used)
    private void doVaoWorkaround() {
        if(!shadersSupported)
            return;
        
        if(!caps.GL_ARB_vertex_array_object)
            return;

        int index = ARBVertexArrayObject.glGenVertexArrays();
        checkError();
        ARBVertexArrayObject.glBindVertexArray(index);
        checkError();
    }

    public int createVBOid() {
        ARBVertexBufferObject.glGenBuffersARB(intBuf);
        GLRef.checkError();
        return intBuf.get(0);
    }

    private int BufferTargetToOpenGL(BufferTarget value) {
        int t = ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB;
        if(value == BufferTarget.Index)
            t = ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB;
        return t;
    }

    

    // Size is in bytes
    public ByteBuffer mapVBO(BufferTarget target, int bufferId, int size) {
        int t = BufferTargetToOpenGL(target);
        ARBVertexBufferObject.glBindBufferARB(t, bufferId);
        GLRef.checkError();
        Integer bufferID = (target == BufferTarget.Index?0:1) + (bufferId+1) << 1;
        ByteBuffer cachedBuf = cachedBuffers.get(bufferID);
        boolean newBuffer = cachedBuf == null;
        ByteBuffer buf =  ARBVertexBufferObject.glMapBufferARB(t, ARBVertexBufferObject.GL_WRITE_ONLY_ARB, size, cachedBuf);
        GLRef.checkError();
        buf.order(ByteOrder.nativeOrder());

        if(newBuffer)
            cachedBuffers.put(bufferID, buf);
        
        return buf;
    }



    public void unmapVBO(BufferTarget target, boolean unbind) {
        int t = BufferTargetToOpenGL(target);
        ARBVertexBufferObject.glUnmapBufferARB(t);
        GLRef.checkError();
        if(unbind) {
            ARBVertexBufferObject.glBindBufferARB(t, 0);
            GLRef.checkError();
        }
    }

    void unbindVBO(BufferTarget target) {
        int t = BufferTargetToOpenGL(target);
        ARBVertexBufferObject.glBindBufferARB(t, 0);
        GLRef.checkError();
    }

    void bindVBO(BufferTarget target, int bufferId) {
        int t = BufferTargetToOpenGL(target);
        ARBVertexBufferObject.glBindBufferARB(t, bufferId);
        GLRef.checkError();
    }

    // Size is in elements, so 1 int = 1 size
    public void sizeVBO(BufferTarget target, int bufferid, int size) {
        int t = BufferTargetToOpenGL(target);
        ARBVertexBufferObject.glBindBufferARB(t, bufferid);
        GLRef.checkError();
        ARBVertexBufferObject.glBufferDataARB(t, size, ARBVertexBufferObject.GL_DYNAMIC_DRAW_ARB);
        GLRef.checkError();
    }

}
