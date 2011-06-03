/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.common.Common;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author mads
 */
public class TerrainTextureCache {
    private static final float imgSize = 256;
    private static final int count = 16;
    private static final float textureEpsilon = 0.00015f;
    
    private static Vector4f[] offsets = new Vector4f[count*count];
    private static boolean initialized = false;
    private static MultiTexture[] multitexs = new MultiTexture[-Byte.MIN_VALUE];

    public enum Side {
        TOP,
        BOTTOM,
        SIDE
    }

    private static class MultiTexture {
        int top, bottom, side;
        public MultiTexture(int top, int bottom, int side) {
            this.top = top; this.bottom = bottom; this.side = side;
        }
        public int get(Side side) {
            if(side == Side.TOP) return top;
            if(side == Side.BOTTOM) return bottom;
            return this.side;
        }
    }

    public static Vector4f getTexOffset(int index) {
        if(!initialized)
            generateOffsets();

        if(index < 0 || index >= offsets.length) {
            Common.Log("TerrainTextureCache.getTexOffset(): Invalid index " + index);
            return new Vector4f();
        }
        
        return offsets[index];
    }

    public static Vector4f getSide(byte index, Side side) {
        if(!initialized)
            generateOffsets();
        
        int type = -index-1;
        
        if(index >= 0 || multitexs[type] == null) {
            Common.Log("TerrainTextureCache.getSide(): Invalid index " + index);
            return new Vector4f();
        }

        MultiTexture tex = multitexs[type];
        return getTexOffset(tex.get(side));
    }

    private static void generateOffsets() {
        Vector2f min = new Vector2f();
        Vector2f max = new Vector2f();
        for (int i= 0; i < count*count; i++) {
            getTexOffset(true, true, i, min);
            getTexOffset(false, false, i, max);

            offsets[i] = new Vector4f(min.x, min.y, max.x, max.y);
        }

        // Set up multitextures
        multitexs[-CubeType.GRASS-1] = new MultiTexture(16*15,16*15+2,16*15+3);

        initialized = true;
    }

    private static Vector2f getTexOffset(boolean xmin, boolean ymin, int imgIndex, Vector2f dest) {
        if(dest == null)
            dest = new Vector2f();

        int y = imgIndex / count;
        imgIndex = imgIndex % count;
        float sizeDelta = count/imgSize;
        dest.set(imgIndex*sizeDelta, y*sizeDelta);
        if(!xmin) {
            dest.x += sizeDelta - textureEpsilon;
        }else {
            dest.x += textureEpsilon;
        }
        if(!ymin) {
            dest.y += sizeDelta - textureEpsilon;
        } else {
            dest.y += textureEpsilon;
        }


        return dest;
    }

}
