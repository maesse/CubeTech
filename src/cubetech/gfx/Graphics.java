package cubetech.gfx;


import java.awt.Canvas;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.vector.Vector2f;
import static org.lwjgl.opengl.GL11.*;
/**
 *
 * @author mads
 */
public class Graphics {
    private static Vector2f resolution = new Vector2f(1024,768);
    private static boolean initialized = false;
    private static DisplayMode[] displayModes = null;
    private static DisplayMode desktopMode;
    private static DisplayMode currentMode = null;
    private static Canvas appletCanvas = null;

    public static void clearScreen() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public static void init(Canvas parent) throws LWJGLException {
        if(initialized)
        {
            return;
        }

        // Set initial resolution
        desktopMode = Display.getDesktopDisplayMode();
        displayModes = Display.getAvailableDisplayModes();
        setResolution((int)resolution.x, (int)resolution.y);

        // Center or attach to applet
        if(parent != null) {
            Display.setParent(parent);
        } else {
            Display.setLocation(-1, -1);
        }

        // Create and show
        Display.create();

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glDepthFunc(GL_LEQUAL);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glViewport(0, 0, currentMode.getWidth(), currentMode.getHeight());

        initialized = true;
    }

    public static void setResolution(int width, int height) {
        // Check for integers on both side on the delimiter
        try {
            // Look through availableModes
            DisplayMode newmode = null;
            for (int i= 0; i < displayModes.length; i++) {
                DisplayMode validmode = displayModes[i];

                if(validmode.getWidth() == width
                        && validmode.getHeight() == height) {
                    // Found one
                    newmode = validmode;
                    if(validmode.getBitsPerPixel() != desktopMode.getBitsPerPixel())
                        continue; // We wan't the same pixeldepth as the desktop mode

                    if(validmode.getFrequency() != desktopMode.getFrequency()
                            && validmode.getFrequency() != 60 && validmode.getFrequency() != 0)
                        continue; // We also want the same frequency

                    break;
                }
            }

            if(Display.isFullscreen() && !newmode.isFullscreenCapable()) {

                System.out.println("Displaymode not valid in fullscreen");
                return;
            }

            System.out.println("Setting displaymode: " + newmode);
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
        } catch(NumberFormatException e) { // Can be NumberFormatException and IndexOutOfBounds
            System.out.println("Invalid displaymode: " + width + ", " + height);
        } catch(IndexOutOfBoundsException e){
            System.out.println("Invalid displaymode: " + width + ", " + height);
        } catch (LWJGLException ex){
            System.out.println("Invalid displaymode: " + width + ", " + height);
            System.out.println("LWJGL error: " + ex);
        }
    }

    public static float getWidth() {
        return resolution.x;
    }

    public static float getHeight() {
        return resolution.y;
    }

    public static Vector2f getResolution() {
        return new Vector2f(resolution);
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void checkError() {
        if(!initialized)
            return;
        int error = glGetError();
        if(error != GL_NO_ERROR)
            throw new RuntimeException("OpenGL error: " + error);
    }

    public static void setHUDProjection() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, getWidth(), 0, getHeight(), 1,-1);
    }
}
