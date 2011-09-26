/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech;

/**
 *
 * @author mads
 */
public class TmpTest {
    private NoiseGeneratorOctaves field_912_k;
    private NoiseGeneratorOctaves field_911_l;
    private NoiseGeneratorOctaves field_910_m;
    private NoiseGeneratorOctaves field_909_n;
    private NoiseGeneratorOctaves field_908_o;
    public NoiseGeneratorOctaves field_922_a;
    public NoiseGeneratorOctaves field_921_b;
    public NoiseGeneratorOctaves mobSpawnerNoise;
    private World worldObj;
    private class World {

    }
    private class NoiseGeneratorOctaves {
        double[] generateNoiseOctaves(double[] dst, int x, int y, int z, int s_x, int s_y, int s_z, double sx, double sy, double sz) {
            return null;
        }

        double[] func_4109_a(double[] dst, int x, int y, int z, int s_x, double sx, double sy, double sz) {
            return null;
        }
    }
    private double[] func_4061_a(double dest[], int xoffset, int zoffset, int yoffset, int xsize, int zsize, int ysize)
    {
        if(dest == null)
        {
            dest = new double[xsize * zsize * ysize];
        }
        double d = 684.41200000000003D;
        double d1 = 684.41200000000003D;
        double temperature[] = null;//worldObj.getWorldChunkManager().temperature;
        double humidity[] = null;//worldObj.getWorldChunkManager().humidity;
        // 2d noise ?
        double[] noiseA = field_922_a.func_4109_a(null, xoffset, yoffset, xsize, ysize, 1.121D, 1.121D, 0.5D);
        double[] noiseB = field_921_b.func_4109_a(null, xoffset, yoffset, xsize, ysize, 200D, 200D, 0.5D);
        // 3d noise
        double[] noiseC = field_910_m.generateNoiseOctaves(null, xoffset, zoffset, yoffset, xsize, zsize, ysize, d / 80D, d1 / 160D, d / 80D);
        double[] noiseD = field_912_k.generateNoiseOctaves(null, xoffset, zoffset, yoffset, xsize, zsize, ysize, d, d1, d);
        double[] noiseE = field_911_l.generateNoiseOctaves(null, xoffset, zoffset, yoffset, xsize, zsize, ysize, d, d1, d);
        int xyzNoiseIndex = 0; // index noiseC/D/E (3d noise)
        int xyNoiseIndex = 0; // indexes noiseA and noiseB (2d noise)
        int xyscale = 16 / xsize; // 4x scale to bring from sampling size to real size, used for temp & humidity
        for(int x = 0; x < xsize; x++)
        {
            int xscaled = x * xyscale + xyscale / 2;
            for(int y = 0; y < ysize; y++)
            {
                int yscaled = y * xyscale + xyscale / 2;

                // humidity formula: 1f-((1f-(humidity*temperature))^3)
                double humTemp = humidity[xscaled * 16 + yscaled] * temperature[xscaled * 16 + yscaled];
                double modHumTemp = 1.0D - humTemp;
                modHumTemp = 1.0 - (modHumTemp * modHumTemp * modHumTemp);
                
                double noiseAFrac = (noiseA[xyNoiseIndex] + 256D) / 512D; // 0-1 noise
                noiseAFrac *= modHumTemp; // modify noise with humidity and temperature
                // clamp noiseA
                if(noiseAFrac > 1.0D)
                {
                    noiseAFrac = 1.0D;
                }
                if(noiseAFrac < 0.0D)
                {
                    noiseAFrac = 0.0D;
                }

                double noiseBFrac = noiseB[xyNoiseIndex] / 8000D;
                // if < 0, scale 0.3f & abs
                if(noiseBFrac < 0.0D)
                {
                    noiseBFrac = -noiseBFrac * 0.29999999999999999D;
                }
                // high pass filter?
                noiseBFrac = noiseBFrac * 3D - 2D;

                if(noiseBFrac < 0.0D)
                {
                    noiseBFrac /= 2D;
                    if(noiseBFrac < -1D)
                    {
                        noiseBFrac = -1D;
                    }
                    noiseBFrac /= 1.3999999999999999D;
                    noiseBFrac /= 2D;
                    noiseAFrac = 0.0D;
                } else
                {
                    if(noiseBFrac > 1.0D)
                    {
                        noiseBFrac = 1.0D;
                    }
                    noiseBFrac /= 8D;
                }
                
                noiseAFrac += 0.5D;
                noiseBFrac = (noiseBFrac * (double)zsize) / 16D;
                double scaledBNoise = (double)zsize / 2D + noiseBFrac * 4D;
                xyNoiseIndex++;
                for(int z = 0; z < zsize; z++) // 16
                {
                    // scale b noise by height.. low levels, more neg
                    double abNoiseFrac = (((double)z - scaledBNoise) * 12D) / noiseAFrac;
                    if(abNoiseFrac < 0.0D)
                    {
                        abNoiseFrac *= 4D;
                    }
                    // noise D&E is almost the same.. just different seed?
                    double noiseDFrac = noiseD[xyzNoiseIndex] / 512D;
                    double noiseEFrac = noiseE[xyzNoiseIndex] / 512D;
                    // noise c, scale /80 on xy /160 on z
                    double noiseCFrac = (noiseC[xyzNoiseIndex] / 10D + 1.0D) / 2D;

                    double voxel = 0.0D;
                    if(noiseCFrac < 0.0D)
                    {
                        voxel = noiseDFrac;
                    } else
                    if(noiseCFrac > 1.0D)
                    {
                        voxel = noiseEFrac;
                    } else
                    {
                        voxel = noiseDFrac + (noiseEFrac - noiseDFrac) * noiseCFrac;
                    }
                    voxel -= abNoiseFrac;
                    if(z > zsize - 4) // if top chunk
                    {
                        // just how close are we?
                        double heightFrac = (float)(z - (zsize - 4)) / 3F;
                        // bring voxel towards negative the closer we get
                        voxel = voxel * (1.0D - heightFrac) + -10D * heightFrac;
                    }
                    dest[xyzNoiseIndex] = voxel;
                    xyzNoiseIndex++;
                }

            }

        }

        return dest;
    }
}
