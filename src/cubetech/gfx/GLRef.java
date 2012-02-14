package cubetech.gfx;

import org.lwjgl.opengl.GL11;
import org.lwjgl.Sys;
import cubetech.common.Helper;
import cubetech.CGame.Render;
import java.nio.FloatBuffer;
import java.util.Stack;
import org.lwjgl.opengl.ARBShaderObjects;
import cubetech.ui.UI.MENU;
import cubetech.common.Commands;
import cubetech.common.Common;
import java.util.ArrayList;
import java.net.URL;
import java.applet.Applet;
import java.util.HashMap;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.PixelFormat;
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
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
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
    public CVar r_fill;
    public CVar r_softparticles;
    private Vector2f resolution;
    CVar r_clearcolor;

    private ArrayList<Shader> shader_recompile_queue = new ArrayList<Shader>();

    public Shader shader = null; // current shader
    public HashMap<String, Shader> shaders = new HashMap<String, Shader>();

    private boolean shadersSupported = true;
    private boolean isMac = false;
    ContextCapabilities caps = null;

    private int maxVertices = 4096;
    private int maxIndices = 4096;
    int maxAniso = 0;
    
    private boolean isApplet = false;
    private Applet applet = null;
    private Canvas displayParent;

    private IntBuffer intBuf = BufferUtils.createIntBuffer(1);
    private IntBuffer intBuf16 = BufferUtils.createIntBuffer(16);
    private FloatBuffer floatBuff16 = BufferUtils.createFloatBuffer(16);
    public FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    

    public boolean fboSupported = false;

    public DeferredShading deferred = null;
    public ShadowManager shadowMan;
    

    public GLRef() {
        Init();
    }

    public void enqueueShaderRecompile(Shader shader) {
        shader_recompile_queue.add(shader);
    }

    public ContextCapabilities getGLCaps() {
        return caps;
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
        Display.create(new PixelFormat(0, 0, 0, 0), new ContextAttribs(3, 2).withProfileCompatibility(true));//, new ContextAttribs(3, 0));
        checkError();

        // Set vsync
        try {
            Display.setVSyncEnabled(r_vsync.iValue == 1);
        } catch (Exception ex) {}

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
        Common.Log("LWJGL version: " + Sys.getVersion());
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
            if(subTokens[0].length() > 1) continue;
            if(Character.isDigit(subTokens[0].charAt(0)) && subTokens[0].charAt(0) <= '1') {
                shadersSupported = false;
                Common.Log("WARNING: Your graphics card does not support shaders");
                break;
            }
        }

        if(isMac) shadersSupported = false;

        if(!CheckCaps()) {
            //Ref.common.Error(ErrorCode.FATAL, "Your grahics card is not supported");
            shadersSupported = false;
        }
        doVaoWorkaround();

        if(shadersSupported) loadShaders();
            

        // Set default states
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_TEXTURE_CUBE_MAP);
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        intBuf16.position(0);
        intBuf16.put(255).put(255).put(255).put(255).flip();
        glFog(GL_FOG_COLOR, intBuf16);
        
        glDepthMask(true);
        glLineWidth(2f);
        glDepthFunc(GL_LEQUAL);
        GLState.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glViewport(0, 0, currentMode.getWidth(), currentMode.getHeight());
        checkError();

//        try {
//            srgbBuffer = new FrameBuffer(true, true, (int)GetResolution().x, (int)GetResolution().y);
//        } catch(Exception ex) {
//            Common.Log("SRGB backbuffer disabled.");
//            // don't set SRGB flag on textures
//            Ref.ResMan.srgbSupported  = false;
//        }
        
        Ref.ResMan.generateTextures();
        
        Ref.render = new Render();

        // Init systems waiting for opengl
        if(Ref.textMan != null) Ref.textMan.Init();
        if(isApplet()) setAppletSize();
            
        deferred = new DeferredShading();
        shadowMan = new ShadowManager();

        // There may be an error sitting in OpenGL.
        // If it isn't cleared, it may trigger an exception later on.
        checkError();
    }


    public Shader getShader(String str) {
        Shader shad = shaders.get(str);
        if(shad == null) {
            Ref.common.Error(ErrorCode.FATAL, "Unregistered shader " + str);
        }
        return shad;
    }

    // Set up cvars
    private void Init() {
        r_vsync = Ref.cvars.Get("r_vsync", "1", EnumSet.of(CVarFlags.ARCHIVE));
        r_mode = Ref.cvars.Get("r_mode", "1280x800", EnumSet.of(CVarFlags.ARCHIVE));
        r_fullscreen = Ref.cvars.Get("r_fullscreen", "0", EnumSet.of(CVarFlags.ARCHIVE));
        r_refreshrate = Ref.cvars.Get("r_refreshrate", "60", EnumSet.of(CVarFlags.ARCHIVE));
        r_toggleRes = Ref.cvars.Get("r_toggleRes", "0", EnumSet.of(CVarFlags.NONE));
        r_fill = Ref.cvars.Get("r_fill", "1", EnumSet.of(CVarFlags.ARCHIVE));
        r_softparticles = Ref.cvars.Get("r_softparticles", "0", EnumSet.of(CVarFlags.ARCHIVE));
        r_clearcolor = Ref.cvars.Get("r_clearcolor", "95,87,67", EnumSet.of(CVarFlags.ARCHIVE));
        
        
        Ref.commands.AddCommand("listmodes", Cmd_listmodes);
        Ref.commands.AddCommand("shader", cmd_shader);
        
        // Don't set all these at the first frame, it is done during init
        r_toggleRes.modified = false;
        r_vsync.modified = false;
        r_mode.modified = false;
        r_fullscreen.modified = false;
        r_refreshrate.modified = false;
    }
    
    public void glLoadMatrix(Matrix4f m) {
        matrixBuffer.clear();
        m.store(matrixBuffer);
        matrixBuffer.flip();
        GL11.glLoadMatrix(matrixBuffer);
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

        if(r_fill.modified) {
            r_fill.modified = false;
        }

        // Check if screen has lost focus
        if(screenHasFocus != Display.isActive()) {
            screenHasFocus = !screenHasFocus;
            Ref.cvars.Set2("com_unfocused", screenHasFocus?"0":"1", true);
            
            if(!screenHasFocus && Ref.Input != null) {
                if(Ref.common.developer.iValue == 0) {
                    Ref.ui.SetActiveMenu(MENU.MAINMENU);
                }
                // Clear keys when loosing focus..
                Ref.Input.ClearKeys();

                if(r_fullscreen.isTrue()) {
                    System.out.println("Leaving fullscreen while unfocussed");
//                    try {
//                        //
//
//                    } catch (LWJGLException ex) {
//                        Logger.getLogger(GLRef.class.getName()).log(Level.SEVERE, null, ex);
//                    }
                }
            } else if(r_fullscreen.isTrue()) {
                System.out.println("Bringing back dat fullscreen");
                try {
                    Display.setFullscreen(false);
                    Display.setFullscreen(true);

                } catch (LWJGLException ex) {
                    Logger.getLogger(GLRef.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        if(r_clearcolor.modified) {
            String[] subStr = r_clearcolor.sValue.split(",");
            int[] color = {95,87,67};
            if(subStr.length == 3) {
                try {
                    for (int i= 0; i < 3; i++) {
                        color[i] = Integer.parseInt(subStr[i]);
                        if(color[i] < 0) color[i] = 0;
                        if(color[i] > 255) color[i] = 255;
                    }
                } catch(Exception ex) { Common.Log("Could not parse r_clearcolor");}
            }
            glClearColor(color[0]/255f,color[1]/255f,color[2]/255f,1);
            r_clearcolor.modified = false;
        }

        for (Shader s : shader_recompile_queue) {
            s.recompile();
        }
        shader_recompile_queue.clear();

        checkError();
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
            
            if(newmode == null) {
                // Just bruteforce one
                newmode = new DisplayMode(width, height);
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
            if(deferred != null) deferred.onResolutionChange();
            
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
        if(!initialized) return;
        initialized = false;
        
        if(Display.isCreated())
            Display.destroy();
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

    Stack<Shader> shaderStack = new Stack<Shader>();

    public Matrix4f getGLProjection(Matrix4f dest) {
        if(dest == null) dest = new Matrix4f();
        matrixBuffer.clear();
        glGetFloat(GL_PROJECTION_MATRIX, matrixBuffer);
        dest.load(matrixBuffer);
        matrixBuffer.clear();
        return dest;
    }
    
    public Matrix4f getGLView(Matrix4f dest) {
        if(dest == null) dest = new Matrix4f();
        matrixBuffer.clear();
        glGetFloat(GL_MODELVIEW_MATRIX, matrixBuffer);
        dest.load(matrixBuffer);
        matrixBuffer.clear();
        return dest;
    }
    
    public void PushShader(Shader shad) {
        shaderStack.push(shader);
        setShader(shad);
    }

    public void PopShader() {
        Shader shad = shaderStack.pop();
        setShader(shad);
    }

    public void setShader(String str) {
        Shader s = shaders.get(str);
        setShader(s);
    }

    public void setShader(Shader shad) {
        shader = shad;
        if(shad == null) {
            // Return to fixed function
            ARBShaderObjects.glUseProgramObjectARB(0);
            return;
        }
        shad.Bind();
    }

    private void loadShaders() {
        try {

            // sprite shader (w/ packed coords)
            ShaderBuilder builder = new ShaderBuilder("sprite");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.setAttribute("v_color", Shader.INDICE_COLOR);
            builder.mapTextureUniform("tex", 0);
            builder.createShader();

            // World shader
            builder = new ShaderBuilder("World");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.setAttribute("v_color", Shader.INDICE_COLOR);
            builder.mapTextureUniform("tex", 0);
            builder.createShader();
            
            // World shader
            builder = new ShaderBuilder("SkyBoxDeferred");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex", 0);
            builder.mapTextureUniform("tex2", 2);
            builder.createShader();
            
            // World shader
            builder = new ShaderBuilder("WorldDeferred");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.setAttribute("v_color", Shader.INDICE_COLOR);
            builder.setAttribute("v_normal", Shader.INDICE_NORMAL);
            builder.mapTextureUniform("tex", 0);
            builder.createShader();

            // iqm object shader for cpu skinned
            builder = new ShaderBuilder("litobjectpixel");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.setAttribute("v_color", Shader.INDICE_COLOR);
            builder.mapTextureUniform("tex", 0);
            builder.mapTextureUniform("shadows", 1);
            builder.mapTextureUniform("envmap", 2);
            builder.createShader();

            // iqm object shader for static models
            builder = new ShaderBuilder("litobjectpixel_1");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.setAttribute("v_color", Shader.INDICE_COLOR);
            builder.mapTextureUniform("tex", 0);
            builder.mapTextureUniform("shadows", 1);
            builder.mapTextureUniform("envmap", 2);
            builder.createShader();

            // iqm gpu skinning for dynamic objects
            builder = new ShaderBuilder("gpuskin");
            builder.setAttribute("vposition", Shader.INDICE_POSITION);
            builder.setAttribute("vcoords", Shader.INDICE_COORDS);
            builder.setAttribute("vweights", Shader.INDICE_WEIGHT);
            builder.setAttribute("vbones", Shader.INDICE_BONEINDEX);
            builder.setAttribute("vtangent", Shader.INDICE_TANGENT);
            //builder.mapTextureUniform("tex", 0);
            builder.createShader();

            // cube world recieving shadow
            builder = new ShaderBuilder("gpuskinShadowed");
            builder.setAttribute("vposition", Shader.INDICE_POSITION);
            builder.setAttribute("vnormal", Shader.INDICE_NORMAL);
            builder.setAttribute("vcoords", Shader.INDICE_COORDS);
            builder.setAttribute("vweights", Shader.INDICE_WEIGHT);
            builder.setAttribute("vbones", Shader.INDICE_BONEINDEX);
            builder.setAttribute("vtangent", Shader.INDICE_TANGENT);
            builder.mapTextureUniform("tex", 0);
            builder.mapTextureUniform("normalmap", 3);
            builder.mapTextureUniform("specularmap", 4);
            builder.mapTextureUniform("shadows", 1);
            builder.mapTextureUniform("envmap", 2);
            builder.createShader();
            

            // cube world recieving shadow
            builder = new ShaderBuilder("gpuskinLit");
            builder.setAttribute("vposition", Shader.INDICE_POSITION);
            builder.setAttribute("vnormal", Shader.INDICE_NORMAL);
            builder.setAttribute("vcoords", Shader.INDICE_COORDS);
            builder.setAttribute("vweights", Shader.INDICE_WEIGHT);
            builder.setAttribute("vbones", Shader.INDICE_BONEINDEX);
            builder.setAttribute("vtangent", Shader.INDICE_TANGENT);
            builder.mapTextureUniform("tex", 0);
            builder.mapTextureUniform("envmap", 2);
            builder.createShader();
            
            // cube world recieving shadow
            builder = new ShaderBuilder("gpuskinLitDeferred");
            builder.setAttribute("vposition", Shader.INDICE_POSITION);
            builder.setAttribute("vnormal", Shader.INDICE_NORMAL);
            builder.setAttribute("vcoords", Shader.INDICE_COORDS);
            builder.setAttribute("vweights", Shader.INDICE_WEIGHT);
            builder.setAttribute("vbones", Shader.INDICE_BONEINDEX);
            builder.setAttribute("vtangent", Shader.INDICE_TANGENT);
            builder.mapTextureUniform("tex", 0);
            builder.mapTextureUniform("normalmap", 3);
            builder.mapTextureUniform("specularmap", 4);
            builder.createShader();
            
            // cube world recieving shadow
            builder = new ShaderBuilder("modelDeferred");
            builder.setAttribute("vposition", Shader.INDICE_POSITION);
            builder.setAttribute("vnormal", Shader.INDICE_NORMAL);
            builder.setAttribute("vcoords", Shader.INDICE_COORDS);
            builder.setAttribute("vtangent", Shader.INDICE_TANGENT);
            builder.mapTextureUniform("tex", 0);
            builder.mapTextureUniform("normalmap", 3);
            builder.mapTextureUniform("specularmap", 4);
            builder.createShader();
            
            // cube world recieving shadow
            builder = new ShaderBuilder("PolyDeferred");
            builder.setAttribute("vposition", Shader.INDICE_POSITION);
            builder.setAttribute("vnormal", Shader.INDICE_NORMAL);
            builder.setAttribute("vcoords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex", 0);
            builder.mapTextureUniform("normalmap", 3);
            builder.mapTextureUniform("specularmap", 4);
            builder.createShader();
            
            // cube world recieving shadow
            builder = new ShaderBuilder("Poly");
            builder.setAttribute("vposition", Shader.INDICE_POSITION);
            builder.setAttribute("vcoords", Shader.INDICE_COORDS);
            builder.setAttribute("vcolor", Shader.INDICE_COLOR);
            builder.mapTextureUniform("tex", 0);
            builder.createShader();

            // iqm model with no pixel color
            builder = new ShaderBuilder("unlitObject");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.createShader();

            // cube world with fog
            builder = new ShaderBuilder("WorldFog");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.setAttribute("v_color", Shader.INDICE_COLOR);
            builder.mapTextureUniform("tex", 0);
            builder.createShader();

            // cube world recieving shadow
            builder = new ShaderBuilder("WorldFogShadow");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.setAttribute("v_color", Shader.INDICE_COLOR);
            builder.mapTextureUniform("tex", 0);
            builder.mapTextureUniform("shadows", 1);
            builder.createShader();

            // rectangular blit
            builder = new ShaderBuilder("RectBlit");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex", 0);
            builder.createShader();
            
            builder = new ShaderBuilder("Blit");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex", 0);
            builder.createShader();
            
            builder = new ShaderBuilder("DeferredShading");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_view", Shader.INDICE_NORMAL);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex0", 0);
            builder.mapTextureUniform("tex1", 1);
            builder.mapTextureUniform("tex2", 2);
            builder.mapTextureUniform("ssao", 3);
            builder.createShader();

            
            builder = new ShaderBuilder("DeferredDirectionalLight");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_view", Shader.INDICE_NORMAL);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex0", 0);
            builder.mapTextureUniform("tex1", 1);
            builder.mapTextureUniform("tex2", 2);
            builder.createShader();
            
            builder = new ShaderBuilder("DeferredPointLight");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_view", Shader.INDICE_NORMAL);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex0", 0);
            builder.mapTextureUniform("tex1", 1);
            builder.mapTextureUniform("tex2", 2);
            builder.createShader();
            
            builder = new ShaderBuilder("DeferredPointLightShadowed");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_view", Shader.INDICE_NORMAL);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex0", 0);
            builder.mapTextureUniform("tex1", 1);
            builder.mapTextureUniform("tex2", 2);
            builder.mapTextureUniform("shadows", 3);
            builder.createShader();
            
            builder = new ShaderBuilder("DeferredDirectionalLightShadowed");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_view", Shader.INDICE_NORMAL);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex0", 0);
            builder.mapTextureUniform("tex1", 1);
            builder.mapTextureUniform("tex2", 2);
            builder.mapTextureUniform("shadows", 3);
            builder.mapTextureUniform("ssao", 4);
            builder.mapTextureUniform("randomRot", 5);
            builder.createShader();
            
            builder = new ShaderBuilder("DeferredAmbientCube");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex0", 0);
            builder.mapTextureUniform("tex2", 2);
            builder.mapTextureUniform("envmap", 3);
            builder.mapTextureUniform("ssao", 4);
            builder.createShader();
            
            builder = new ShaderBuilder("DeferredFog");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_view", Shader.INDICE_NORMAL);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("tex2", 2);
            builder.createShader();
            
            builder = new ShaderBuilder("DeferredAO");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_view", Shader.INDICE_NORMAL);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("noise", 3);
            builder.mapTextureUniform("tex2", 2);
            builder.createShader();
            
            builder = new ShaderBuilder("SSAOBlur");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.mapTextureUniform("ssao", 4);
            builder.createShader();

            // soft sprites shader
            builder = new ShaderBuilder("softsprite");
            builder.setAttribute("v_position", Shader.INDICE_POSITION);
            builder.setAttribute("v_coords", Shader.INDICE_COORDS);
            builder.setAttribute("v_color", Shader.INDICE_COLOR);
            builder.mapTextureUniform("tex", 0);
            builder.mapTextureUniform("depth", 1);
            builder.createShader();

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

            if(caps.GL_EXT_texture_filter_anisotropic) {
                // Derp.
                maxAniso = (int) glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            }

            
            fboSupported = true;
            if(!caps.GL_EXT_framebuffer_object) {
                fboSupported = false;
                Common.Log("EXT_framebuffer_object not supported by your graphics card");
            } else if(!caps.GL_EXT_texture_rectangle) {
                boolean alternative = caps.GL_ARB_texture_rectangle;
                if(alternative) {
                    Common.Log("Using ARB_texture_rectangle instead of EXT_");
                } else {
                    Common.Log("GL_EXT_texture_rectangle not supported by your graphics card");
                }
            }

            if(!caps.GL_EXT_framebuffer_sRGB) {
                Common.Log("No EXT_FRAMEBUFFER_SRGB support.");
            }

            if(!caps.GL_ARB_depth_texture) {
                Common.Log("No Depth textures :/");
            }
                

            if(!caps.GL_EXT_texture_sRGB) {
                Common.Log("GL_EXT_texture_sRGB not supported by your graphics card");
            }

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

    private ICommand cmd_shader = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length <= 1) {
                Common.Log("usage: shader <command> [list, recompile]");
                return;
            }

            String arg = args[1].toLowerCase();
            if(arg.equals("list")) {
                Common.Log("Shader list:");
                for (String string : shaders.keySet()) {
                    Common.Log("    " + string);
                }
            } else if(arg.equals("recompile")) {
                if(args.length < 3 || shaders.get(args[2]) == null) {
                    Common.Log("usage: shader recompile <shadername>");
                    return;
                }
                Shader s = shaders.get(args[2]);
                s.recompile();
            } else {
                Common.Log("Unknown argument " + args[1]);
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

    public void pushShader(String string) {
        PushShader(getShader(string));
    }

    

}
