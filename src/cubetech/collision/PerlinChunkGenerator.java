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

    private void getNoise(float[] dest, int x, int y, int z, int axissize, float xyfreq, float zfreq, float ampl) {
        if(false) {
            for (int i = 0; i < axissize; i++) {
                for (int j= 0; j < axissize; j++) {
                    for (int k= 0; k < axissize; k++) {
                        dest[i*axissize*axissize + j*axissize + k] =
                                (float)(SimplexNoise.noise(
                                (x * axissize + k)*xyfreq,
                                (y * axissize + j)*xyfreq,
                                (z * axissize + i)*zfreq) * ampl);
                    }
                }
            }
            return;
        }
        
        NativeNoise.noise(dest,
                (x * axissize)*xyfreq, (y * axissize)*xyfreq, (z * axissize)*zfreq,
                xyfreq, xyfreq, zfreq, axissize, ampl);

        
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
        if(z > 0) return chunk;
        float maxHeight = 1024;
        float minHeight = 0;

        // Get groundlevel noise
        double[] groundNoise = new double[32*32];
        for (int i= 0; i < 32*32; i++) {
            groundNoise[i] = SimplexNoise.noise((x*32+(i % 32))*0.00125f, (y*32+(i / 32))*0.00125f);
        }

        getNoise(noiseLookup2, x+1024, y+1024, z+1024, noiseSize, 0.05f, 0.1f, 1f);
        

        for (int i= 0; i < CubeChunk.SIZE; i++) {
            for (int j= 0; j < CubeChunk.SIZE; j++) {
                for (int k= 0; k < CubeChunk.SIZE; k++) {
                    double height = groundNoise[j*32+k]*maxHeight;
                    float currentHeight = (z * CubeChunk.SIZE + i) * CubeChunk.BLOCK_SIZE;

                    double rnd = height > currentHeight?1:0;
                    if(rnd == 0) {
                        float hFrac = currentHeight/maxHeight;
                        float rnd2 = getCachedNoiseSmoothed(k,j,i,noiseLookup2);
                        rnd += rnd2 * (1f-hFrac);
                    }
                    

                    boolean empty = rnd < 0.5f;
                    chunk.setCubeType(k,j,i, empty?CubeType.EMPTY:CubeType.GRASS, false);
                }
            }
        
        }
        return chunk;
    }

}
