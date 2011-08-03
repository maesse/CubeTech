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

    private double getNoise(int chunkx, int chunky, int chunkz, int x, int y, int z,
            float freq, float ampl) {
        return (SimplexNoise.noise(
                            (chunkx * CubeChunk.SIZE + x)*freq,
                            (chunky * CubeChunk.SIZE + y)*freq,
                            (chunkz * CubeChunk.SIZE + z)*freq)+1) * 0.5f * ampl;
    }


    private float getHeightFrac(float min, float max, float current) {
        current -= max;
        if(current > 0) current = 0;
        float total = max - min;
        if(current < -total) current = -total;
        float frac = current / -total;
        return frac;
    }
    public CubeChunk generateChunk(CubeMap map, int x, int y, int z) {
        CubeChunk chunk = new CubeChunk(map.chunks, x, y, z);

        float maxHeight = 512;
        float groundlevel = -1024;
        float minHeight = CubeChunk.SIZE * CubeChunk.BLOCK_SIZE * CubeMap.MIN_Z;

        int cacheLevel = 4;
        int noiseSize = (CubeChunk.SIZE / cacheLevel) + 1;
        double[] noiseLookup = new double[noiseSize*noiseSize*noiseSize];
        for (int i= 0; i < noiseSize; i++) {
            for (int j= 0; j < noiseSize; j++) {
                for (int k= 0; k < noiseSize; k++) {
                    noiseLookup[i*noiseSize*noiseSize + j*noiseSize + k] =
                            getNoise(x,y,z,k*cacheLevel,j*cacheLevel,i*cacheLevel, 0.001f, 1f);
                }
            }
        }
        
        for (int i= 0; i < CubeChunk.SIZE; i++) {
            for (int j= 0; j < CubeChunk.SIZE; j++) {
                for (int k= 0; k < CubeChunk.SIZE; k++) {
                    double rnd = getNoise(x,y,z,k,j,i, 0.001f, 1f);

                    // get height gradient
                    float currentHeight = (z * CubeChunk.SIZE + i) * CubeChunk.BLOCK_SIZE;
                    float frac = getHeightFrac(minHeight, maxHeight, currentHeight);

                    // Multiply noise by gradient
                    rnd *= frac*frac;
                    rnd += frac*0.02;

                    


                    double rnd2 = getNoise(x,z,y,k,i,j, 0.005f, 0.5f);
                    frac = getHeightFrac(-1024, 1024, currentHeight);
                    rnd2 *= frac * rnd2;
                    rnd += rnd2;
//                    if(rnd2 > 0.04f) rnd = 1f;
//                    else if(frac < 1f && frac > 0.5f) rnd = 0f;

                    // saturate
                    if(rnd > 0.02f) rnd = 1f;


//                    if(z * CubeChunk.SIZE * CubeChunk.BLOCK_SIZE > 0) {
//                        rnd = -100f;
//                    }
                    //rnd = ((z * CubeChunk.SIZE + i) + CubeChunk.SIZE * CubeChunk.BLOCK_SIZE * -CubeMap.MIN_Z) / height;

//                    freq = 0.4f;
//                    ampl = 0.2f;
//                    rnd += (SimplexNoise.noise(
//                            (x * CubeChunk.SIZE + k)*freq,
//                            (y * CubeChunk.SIZE + j)*freq,
//                            (z * CubeChunk.SIZE + i)*freq)) * ampl;
//
//                    freq = 0.2f;
//                    ampl = 0.4f;
//                    rnd += (SimplexNoise.noise(
//                            (x * CubeChunk.SIZE + k)*freq,
//                            (y * CubeChunk.SIZE + j)*freq,
//                            (z * CubeChunk.SIZE + i)*freq)) * ampl;
//
//                    freq = 0.1f;
//                    ampl = 8f;
//                    rnd += (SimplexNoise.noise(
//                            (x * CubeChunk.SIZE + k)*freq,
//                            (y * CubeChunk.SIZE + j)*freq,
//                            (z * CubeChunk.SIZE + i)*freq)) * ampl;

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


                    

                    
                    boolean empty = rnd <= 0.5f;

                    chunk.setCubeType(k,j,i, empty?CubeType.EMPTY:CubeType.GRASS, false);
                }
            }
        
        }
//        chunk.notifyChange();
        return chunk;
    }

}
