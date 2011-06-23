package cubetech.misc;

import cubetech.GameLoop;
import cubetech.gfx.Graphics;
import org.lwjgl.LWJGLException;

/**
 *
 * @author mads
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Graphics.init(null);
            Ref.InitRef();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        
        
        GameLoop loop = new GameLoop();
        try {
            while(true) {
                loop.RunFrame();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
