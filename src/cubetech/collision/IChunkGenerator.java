/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.collision;

/**
 *
 * @author mads
 */
public interface IChunkGenerator {
    public void setSeed(long seed);
    public CubeChunk generateChunk(CubeMap map, int x, int y, int z);
}
