package cubetech.gfx;

import cubetech.common.Common;
import java.util.ArrayList;

/**
 *
 * @author mads
 */
public class VBOPool {
    public static VBOPool Global = new VBOPool();

    private ArrayList<VBO> busyList = new ArrayList<VBO>();
    private ArrayList<VBO> freeList = new ArrayList<VBO>();

    public int getTotalCount() {
        return busyList.size() + freeList.size();
    }

    public int getBusyCount() {
        return busyList.size();
    }

    public int getFreeCount() {
        return freeList.size();
    }

    public VBO allocateVBO(int size, VBO.BufferTarget target) {
        VBO vbo = poolFreeList(size, target);
        if(vbo == null) {
//            Common.Log("Pool empty. Creating new VBO");
            vbo = new VBO(size, target);
        }
        busyList.add(vbo);
        return vbo;
    }

    public void freeVBO(VBO vbo) {
        if(vbo != null) {
            boolean sucess = busyList.remove(vbo);
            if(sucess) {
                freeList.add(vbo);
            }
        }
    }

    private VBO poolFreeList(int size, VBO.BufferTarget target) {
        for (int i= 0; i < freeList.size(); i++) {
            VBO vbo = freeList.get(i);
            if(vbo.getSize() >= size && vbo.getTarget() == target) {
                freeList.remove(i);
//                Common.Log("Using pooled VBO");
                return vbo;
            }
        }

        return null;
    }
}
