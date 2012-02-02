package cubetech.gfx;

import cubetech.CGame.ViewParams;
import java.util.ArrayList;

/**
 *
 * @author Mads
 */
public abstract class AbstractBatchRender {
    public VBO vbo;
    public ArrayList<IBatchCall> calls;
    public abstract void setState(ViewParams view);
    public abstract void unsetState();
}
