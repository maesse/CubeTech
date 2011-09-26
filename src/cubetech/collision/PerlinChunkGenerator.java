/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.collision;

import cubetech.common.ImprovedNoise;
import cubetech.gfx.CubeType;
import cubetech.misc.NativeNoise;
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

    private void getNoise(float[] dest, int x, int y, int z, int axissize, float freq, float ampl) {
        NativeNoise.noise(dest,
                (x * axissize)*freq, (y * axissize)*freq, (z * axissize)*freq,
                freq, freq, freq, axissize, ampl);
    }

    private float getNoise(int chunkx, int chunky, int chunkz, int x, int y, int z,
            float freq, float ampl) {
        return (float) ((SimplexNoise.noise(
                (chunkx * CubeChunk.SIZE + x) * freq,
                (chunky * CubeChunk.SIZE + y) * freq,
                (chunkz * CubeChunk.SIZE + z) * freq)) * ampl);
    }


    private float getHeightFrac(float min, float max, float current) {
        current -= max;
        if(current > 0) current = 0;
        float total = max - min;
        if(current < -total) current = -total;
        float frac = current / -total;
        return frac;
    }

    private double getCachedNoise(int x, int y, int z, double[] loopup) {
        // lookup range: 0 -> noiseSize
        // input range: 0 -> 31

        // linear interp for x
        int a = (int) Math.floor(noiseSize * (x / 32f));
        int b = (int) Math.floor(noiseSize * (y / 32f));
        int c = (int) Math.floor(noiseSize * (z / 32f));
        return loopup[a + b * noiseSize + c * noiseSize * noiseSize];
    }

    // Trilinear interpolation lookup in the cache
    private float getCachedNoiseSmoothed(int x, int y, int z, float[] loopup) {
        // lookup range: 0 -> noiseSize
        // input range: 0 -> 31

        // linear interp for x
        float xfrac = (x / 32f) * (noiseSize-1);
        float yfrac = (y / 32f) * (noiseSize-1);
        float zfrac = (z / 32f) * (noiseSize-1);
        int xfloor = (int) Math.floor(xfrac);
        int yfloor = (int) Math.floor(yfrac);
        int zfloor = (int) Math.floor(zfrac);

        // upper z
        float t001 = loopup[(zfloor+1)*noiseSize*noiseSize + yfloor*noiseSize + xfloor];
        float t101 = loopup[(zfloor+1)*noiseSize*noiseSize + yfloor*noiseSize + xfloor+1];
        float t011 = loopup[(zfloor+1)*noiseSize*noiseSize + (yfloor+1)*noiseSize + xfloor];
        float t111 = loopup[(zfloor+1)*noiseSize*noiseSize + (yfloor+1)*noiseSize + xfloor+1];
        // lower z
        float t000 = loopup[zfloor*noiseSize*noiseSize + yfloor*noiseSize + xfloor];
        float t100 = loopup[zfloor*noiseSize*noiseSize + yfloor*noiseSize + xfloor+1];
        float t010 = loopup[zfloor*noiseSize*noiseSize + (yfloor+1)*noiseSize + xfloor];
        float t110 = loopup[zfloor*noiseSize*noiseSize + (yfloor+1)*noiseSize + xfloor+1];

        xfrac = 1f-(xfrac-xfloor);
        yfrac = 1f-(yfrac-yfloor);
        zfrac = 1f-(zfrac-zfloor);

        float x00 = t000 * xfrac + t100 * (1f-xfrac);
        float x10 = t010 * xfrac + t110 * (1f-xfrac);

        float x01 = t001 * xfrac + t101 * (1f-xfrac);
        float x11 = t011 * xfrac + t111 * (1f-xfrac);

        float y0 = x00 * yfrac + x10 * (1f-yfrac);
        float y1 = x01 * yfrac + x11 * (1f-yfrac);

        float z0 = y0 * zfrac + y1 * (1f-zfrac);
        return z0;
    }

    int cacheLevel = 8;
    int noiseSize = (CubeChunk.SIZE / cacheLevel)+1;
    float[] noiseLookup = new float[(noiseSize)*(noiseSize)*(noiseSize)];
    float[] noiseLookup2 = new float[(noiseSize)*(noiseSize)*(noiseSize)];
    public CubeChunk generateChunk(CubeMap map, int x, int y, int z) {
        CubeChunk chunk = new CubeChunk(map.chunks, x, y, z);

        float maxHeight = 512;
        float groundlevel = -1024;
        float minHeight = CubeChunk.SIZE * CubeChunk.BLOCK_SIZE * CubeMap.MIN_Z;

        getNoise(noiseLookup2, x+1024, y+1024, z+1024, noiseSize, 0.1f, 1f);
        
//        for (int i= 0; i < noiseSize; i++) {
//            for (int j= 0; j < noiseSize; j++) {
//                for (int k= 0; k < noiseSize; k++) {
////                    noiseLookup[i*noiseSize*noiseSize + j*noiseSize + k] =
////                            getNoise(x,y,z,k*cacheLevel,j*cacheLevel,i*cacheLevel, 0.001f, 1f);
//                    noiseLookup2[i*noiseSize*noiseSize + j*noiseSize + k] =
//                            getNoise(x,y,z,k*cacheLevel,j*cacheLevel,i*cacheLevel, 0.005f, 0.5f);
//                }
//            }
//        }
        
        for (int i= 0; i < CubeChunk.SIZE; i++) {
            for (int j= 0; j < CubeChunk.SIZE; j++) {
                for (int k= 0; k < CubeChunk.SIZE; k++) {
//                    double rnd = getCachedNoise(k,j,i,noiseLookup);//getNoise(x,y,z,k,j,i, 0.001f, 1f);
//                    double rnd = 0.5f;
//                    // get height gradient
                    float currentHeight = (z * CubeChunk.SIZE + i) * CubeChunk.BLOCK_SIZE;
//                    float frac = getHeightFrac(minHeight, maxHeight, currentHeight);
//
//                    // Multiply noise by gradient
//                    rnd *= frac*frac;
//                    rnd += frac*0.02;

                    


                    double rnd = getCachedNoiseSmoothed(k,j,i,noiseLookup2);
                    //double rnd = getNoise(x,y,z,k,j,i, 0.005f, 0.5f);
                    if(currentHeight > 1024) {
                        rnd = -1;
                    }
//                    else if(currentHeight > 0) {
//                        rnd -= currentHeight / 1024f;
//                    }
//                    frac = getHeightFrac(-1024, 1024, currentHeight);
//                    rnd2 *= frac * rnd2;
//                    rnd += rnd2;
////                    if(rnd2 > 0.04f) rnd = 1f;
////                    else if(frac < 1f && frac > 0.5f) rnd = 0f;
//
//                    // saturate
//                    if(rnd > 0.02f) rnd = 1f;

                    boolean empty = rnd < 0.5f;
                    chunk.setCubeType(k,j,i, empty?CubeType.EMPTY:CubeType.GRASS, false);

                    
                }
            }
        
        }
//        chunk.notifyChange();
        return chunk;
    }

}
