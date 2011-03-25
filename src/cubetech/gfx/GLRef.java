package cubetech.gfx;

import cubetech.common.Common;
import java.util.ArrayList;
import java.util.AbstractMap.SimpleEntry;
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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.vector.Vector2f;
import cubetech.common.ICommand;
import cubetech.common.CVarFlags;
import java.util.EnumSet;
import cubetech.common.CVar;
import cubetech.common.Commands.ExecType;
import cubetech.common.Common.ErrorCode;
import cubetech.misc.Ref;
import java.awt.Canvas;
import java.io.IOException;
import java.net.MalformedURLException;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL20;
import static org.lwjgl.opengl.GL11.*;
/**
 * Controls OpenGL
 * @author mads
 */
public class GLRef {
    public DisplayMode currentMode;

    private DisplayMode[] availableModes;
    private DisplayMode desktopMode;
    private ContextCapabilities caps = null;
    private Canvas displayParent; // Canvas this glContext exists in.
    private static boolean initialized = false;

    public CVar r_vsync;
    public CVar r_mode;
    public CVar r_fullscreen;
    public CVar r_refreshrate; // A hint for selecting modes
    public CVar r_mindepth;
    public CVar r_maxdepth;
    public CVar r_toggleRes;
    private Vector2f resolution;
    
    boolean screenActive = false;

    public Shader shader = null;
    public HashMap<String, Shader> shaders = new HashMap<String, Shader>();


    // VBO Buffer handling
//    private static final int GLREF_MAX_BUFFERS = 100;
    private IntBuffer intBuf = BufferUtils.createIntBuffer(1);
    private IntBuffer intBuf16 = BufferUtils.createIntBuffer(16);
//    private FloatBuffer dataBuffers[] = new FloatBuffer[GLREF_MAX_BUFFERS];
//    private IntBuffer indiceBuffers[] = new IntBuffer[GLREF_MAX_BUFFERS];
    private int floatBufferIndex = 0;
    private int intBufferIndex = 0;

    private int maxVertices = 4096;
    private int maxIndices = 4096;
    private boolean isApplet = false;
    private Applet applet = null;

    

    public enum BufferTarget {
        Vertex,
        Index
    }

    public GLRef() {
        Init();
//        for (int i= 0; i < dataBuffers.length; i++) {
//            dataBuffers[i] = BufferUtils.createFloatBuffer(GL12.GL_MAX_ELEMENTS_VERTICES);
//            indiceBuffers[i] = BufferUtils.createIntBuffer(GL12.GL_MAX_ELEMENTS_INDICES);
//        }
    }

//    public FloatBuffer GetNextFloatBuffer() {
//        FloatBuffer buf = dataBuffers[floatBufferIndex++ % GLREF_MAX_BUFFERS];
//        buf.clear();
//        return buf;
//    }
//
//    public IntBuffer GetNextIntBuffer() {
//        return indiceBuffers[intBufferIndex++ % GLREF_MAX_BUFFERS];
//    }

    public Vector2f GetResolution() {
        return resolution;
    }

    // Set up cvars
    private void Init() {
        r_vsync = Ref.cvars.Get("r_vsync", "1", EnumSet.of(CVarFlags.NONE));
        r_mode = Ref.cvars.Get("r_mode", "1024x768", EnumSet.of(CVarFlags.NONE));
        r_fullscreen = Ref.cvars.Get("r_fullscreen", "0", EnumSet.of(CVarFlags.NONE));
        r_refreshrate = Ref.cvars.Get("r_refreshrate", "60", EnumSet.of(CVarFlags.NONE));
        r_mindepth = Ref.cvars.Get("r_mindepth", "0", EnumSet.of(CVarFlags.NONE));
        r_maxdepth = Ref.cvars.Get("r_maxdepth", "1000", EnumSet.of(CVarFlags.NONE));
        r_toggleRes = Ref.cvars.Get("r_toggleRes", "0", EnumSet.of(CVarFlags.NONE));
        r_toggleRes.modified = false;
        Ref.commands.AddCommand("listmodes", new Cmd_listmodes());
        
        // Don't set all these at the first frame, it is done during init
        r_vsync.modified = false;
        r_mode.modified = false;
        r_fullscreen.modified = false;
    }

    public boolean isInitalized() {
        return initialized;
    }

    private class Cmd_listmodes implements ICommand {
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
    }

    // Mainly for checking if cvars have changed
    public void Update() {
        if(r_toggleRes.modified) {
            if(r_toggleRes.iValue == 1) {
                toggelResolution(true);
            } else if(r_toggleRes.iValue == -1 && GetResolution().x > 800 && GetResolution().y > 600) {
                toggelResolution(false);
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
                Logger.getLogger(GLRef.class.getName()).log(Level.SEVERE, null, ex);
                //Ref.common.Error(ErrorCode.FATAL, "Cannot change vsync; cannot get current thread state");
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
        if(screenActive != Display.isActive()) {
            screenActive = !screenActive;
            if(!screenActive && Ref.Input != null) {
                // Clear keys when loosing focus..
                Ref.Input.ClearKeys();
            }
        }
    }

    public boolean isScreenFocused() {
        return screenActive;
    }

    public boolean isApplet() {
        return isApplet;
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
            Common.Log(ex.getMessage());
        }
    }

    public DisplayMode[] getNiceModes() {
        ArrayList<DisplayMode> modes = new ArrayList<DisplayMode>();
        // Look through availableModes
        for (int i= 0; i < availableModes.length; i++) {
            DisplayMode validmode = availableModes[i];

            if(validmode.getBitsPerPixel() != desktopMode.getBitsPerPixel())
                continue; // We wan't the same pixeldepth as the desktop mode

            if(validmode.getFrequency() != desktopMode.getFrequency()
                    && validmode.getFrequency() != r_refreshrate.iValue && validmode.getFrequency() != 0)
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

        Ref.commands.ExecuteText(ExecType.NOW, "listmodes");

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

    public void Destroy() {
        if(Display.isCreated())
            Display.destroy();
        initialized = false;
    }

    private void toggelResolution(boolean increase) {
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

    public void InitWindow(Canvas parent, Applet applet) throws Exception {

        this.applet = applet;

        availableModes = Display.getAvailableDisplayModes();

        desktopMode = Display.getDesktopDisplayMode();
        
        Common.Log("Desktop displaymode: " + desktopMode);
        displayParent = parent; // Save off canvas if there is one
        SetResolution(r_mode.sValue);
        
        if(parent == null) {
            // If we are creating a new window, center it
            Display.setLocation((int)(desktopMode.getWidth()/2f - currentMode.getWidth()/2f),
                    (int)(desktopMode.getHeight()/2f - currentMode.getHeight()/2f));
            
            
        } else {
            Display.setParent(displayParent); // Applets use this
            
            if(applet != null)
                isApplet = true;
        }

        // Create the display
        Display.create(new PixelFormat(8, 8, 0, 0));
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
        Common.Log("OpenGL version: " + glGetString(GL_VERSION));

        Common.Log("VBO support detected (V: " + maxVertices + ") (I: " + maxIndices + ")");
        caps = GLContext.getCapabilities();
        if(!CheckCaps())
            Ref.common.Error(ErrorCode.FATAL, "Your grahics card is not supported");
        doVaoWorkaround();
        
        OnPostDisplayCreate();
    }


    // Some drivers mess up VBO's when there's no VAO's bound (even if not used)
    private void doVaoWorkaround() {
        if(!caps.GL_ARB_vertex_array_object)
            return;

        int index = ARBVertexArrayObject.glGenVertexArrays();
        checkError();
        ARBVertexArrayObject.glBindVertexArray(index);
        checkError();
    }

    // Starts up stuff that was waiting for a display to be created
    public void OnPostDisplayCreate() throws Exception {
        
        currentMode = Display.getDisplayMode();
        checkError();
        resolution = new Vector2f(currentMode.getWidth(), currentMode.getHeight());
        
        
        
        loadShaders();
        
        // Set default states
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glDepthMask(true);
        glDepthFunc(GL_LEQUAL);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glViewport(0, 0, currentMode.getWidth(), currentMode.getHeight());
        checkError();
        

        

        // Init systems waiting for opengl
        if(Ref.textMan != null)
            Ref.textMan.Init();

        if(isApplet())
            setAppletSize();

        // There may be an error sitting in OpenGL.
        // If it isn't cleared, it may trigger an exception later on.
        checkError();
    }

    public static void checkError() {
        if(!initialized)
            return;
        int error = glGetError();
        if(error != GL_NO_ERROR)
            throw new RuntimeException("OpenGL error: " + error);
    }

    public void setShader(String str) {
        shaders.get(str).Bind();
    }


    private void loadShaders() {
        try {
            
            shader = new Shader("gfx/sprite");
            shaders.put("sprite", shader);
            shader.Bind();
//            shaders.put("litobject", new Shader("gfx/litobject"));
//
//            Light.test();
        } catch (Exception ex) {
            Logger.getLogger(GLRef.class.getName()).log(Level.SEVERE, null, ex);
            Ref.common.Error(ErrorCode.FATAL, "Failed to load graphics shaders\n" + ex);
        }

    }

    

    private boolean CheckCaps() {
        boolean okay = true;
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
//        caps.GL_ARB_draw_elements_base_vertex

        return okay;
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

    HashMap<Integer, ByteBuffer> cachedBuffers
            = new HashMap<Integer, ByteBuffer>();

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

//    public void fillVBO(int bufferid, FloatBuffer data) {
//        if(caps.GL_ARB_vertex_buffer_object) {
//            data.limit();
//            data.position(0);
//            //data.flip();
//            ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, bufferid);
//            ARBVertexBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, data, ARBVertexBufferObject.GL_STATIC_DRAW_ARB);
//        }
//    }
//
//    public void fillVBOIndices(int bufferid, IntBuffer data) {
//        if(caps.GL_ARB_vertex_buffer_object) {
//            ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, bufferid);
//            ARBVertexBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, data, ARBVertexBufferObject.GL_STATIC_DRAW_ARB);
//        }
//    }



}
