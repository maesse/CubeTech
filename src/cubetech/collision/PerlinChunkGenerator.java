/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.collision;

import cubetech.common.ImprovedNoise;
import cubetech.gfx.CubeType;
import cubetech.misc.SimplexNoise;

/**
 *
 * @author mads
 */
public class PerlinChunkGenerator implements IChunkGenerator {
    float seed = 0.015f;
    public void setSeed(long seed) {
        //this.seed = seed;
    }

    public CubeChunk generateChunk(CubeMap map, int x, int y, int z) {
        CubeChunk chunk = new CubeChunk(map, x, y, z);
        
        for (int i= 0; i < CubeChunk.SIZE; i++) {
            for (int j= 0; j < CubeChunk.SIZE; j++) {
                for (int k= 0; k < CubeChunk.SIZE; k++) {

                    float freq = 0.8484f;
                    float ampl = 0.1f;
                    double rnd = (SimplexNoise.noise(
                            (x * CubeChunk.SIZE + k)*freq,
                            (y * CubeChunk.SIZE + j)*freq,
                            (z * CubeChunk.SIZE + i)*freq)) * ampl;

                    freq = 0.4f;
                    ampl = 0.2f;
                    rnd += (SimplexNoise.noise(
                            (x * CubeChunk.SIZE + k)*freq,
                            (y * CubeChunk.SIZE + j)*freq,
                            (z * CubeChunk.SIZE + i)*freq)) * ampl;

                    freq = 0.2f;
                    ampl = 0.4f;
                    rnd += (SimplexNoise.noise(
                            (x * CubeChunk.SIZE + k)*freq,
                            (y * CubeChunk.SIZE + j)*freq,
                            (z * CubeChunk.SIZE + i)*freq)) * ampl;

                    freq = 0.1f;
                    ampl = 8f;
                    rnd += (SimplexNoise.noise(
                            (x * CubeChunk.SIZE + k)*freq,
                            (y * CubeChunk.SIZE + j)*freq,
                            (z * CubeChunk.SIZE + i)*freq)) * ampl;

//                    float freq = 0.8484f;
//                    float ampl = 1f;
//                    float xyScale = 0.2323f;
//                    rnd += (ImprovedNoise.noise(
//                            (x * CubeChunk.SIZE + k)*seed*freq*xyScale,
//                            (y * CubeChunk.SIZE + j)*seed*freq*xyScale,
//                            (z * CubeChunk.SIZE + i)*seed*freq)) * ampl;
//
//                    freq = 0.798465f;
//                    ampl = 1.2f;
//                    xyScale = 0.8f;
//                    rnd += (ImprovedNoise.noise(
//                            (x * CubeChunk.SIZE + k)*seed*freq*xyScale,
//                            (y * CubeChunk.SIZE + j)*seed*freq*xyScale,
//                            (z * CubeChunk.SIZE + i)*seed*freq)) * ampl;
//
//
//                    freq = 0.65421f;
//                    ampl = 1.5f;
//                    rnd += (ImprovedNoise.noise(
//                            (x * CubeChunk.SIZE + k)*seed*freq*xyScale,
//                            (y * CubeChunk.SIZE + j)*seed*freq*xyScale,
//                            (z * CubeChunk.SIZE + i)*seed*freq)) * ampl;
//
//                    freq = 0.3216512f;
//                    ampl = 3.1233123f;
//                    rnd += (ImprovedNoise.noise(
//                            (x * CubeChunk.SIZE + k)*seed*freq*xyScale,
//                            (y * CubeChunk.SIZE + j)*seed*freq*xyScale,
//                            (z * CubeChunk.SIZE + i)*seed*freq)) * ampl;




                    if(z > 0) {
                        rnd -= 100f;
                    }
                    boolean empty = rnd <= 0.0f;

                    chunk.setCubeType(k,j,i, empty?CubeType.EMPTY:CubeType.GRASS);
                }
            }
        
        }
        return chunk;
    }

}
