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
public class WorldSector {
    public WorldSector[] children = new WorldSector[2];
    public float Dist;
    public int Axis;
    
    public int[] blockIndices;
    public int nIndices;

    public WorldSector() {
        blockIndices = new int[1000];
    }
}
