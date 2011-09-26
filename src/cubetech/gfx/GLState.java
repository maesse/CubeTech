package cubetech.gfx;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
/**
 *
 * @author mads
 */
public class GLState {
    private static int _blendFunc_src = GL11.GL_ONE;
    private static int _blendFunc_dest = GL11.GL_ZERO;
    public static void glBlendFunc(int sourcefactor, int destfactor) {
        if(_blendFunc_src == sourcefactor && _blendFunc_dest == destfactor) return;
        _blendFunc_src = sourcefactor;
        _blendFunc_dest = destfactor;
        GL11.glBlendFunc(sourcefactor, destfactor);
    }
}
