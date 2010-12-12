/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class AreaBlocks {
    public Vector2f mins;
    public Vector2f maxs;
    public int[] data;
    public int dataOffset;

    public AreaBlocks(int maxBlocks, Vector2f min, Vector2f max) {
        this.mins = min;
        this.maxs = max;
        data = new int[maxBlocks];
    }
}
