# 1 "SMAA.h"
# 1 "<indbygget>"
# 1 "<command-line>"
# 1 "SMAA.h"
/**

 * Copyright (C) 2011 Jorge Jimenez (jorge@iryoku.com)

 * Copyright (C) 2011 Belen Masia (bmasia@unizar.es) 

 * Copyright (C) 2011 Jose I. Echevarria (joseignacioechevarria@gmail.com) 

 * Copyright (C) 2011 Fernando Navarro (fernandn@microsoft.com) 

 * Copyright (C) 2011 Diego Gutierrez (diegog@unizar.es)

 * All rights reserved.

 * 

 * Redistribution and use in source and binary forms, with or without

 * modification, are permitted provided that the following conditions are met:

 * 

 *    1. Redistributions of source code must retain the above copyright notice,

 *       this list of conditions and the following disclaimer.

 * 

 *    2. Redistributions in binary form must reproduce the following disclaimer

 *       in the documentation and/or other materials provided with the 

 *       distribution:

 * 

 *      "Uses SMAA. Copyright (C) 2011 by Jorge Jimenez, Jose I. Echevarria,

 *       Belen Masia, Fernando Navarro and Diego Gutierrez."

 * 

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS 

 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 

 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 

 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDERS OR CONTRIBUTORS 

 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 

 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 

 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 

 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 

 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 

 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 

 * POSSIBILITY OF SUCH DAMAGE.

 * 

 * The views and conclusions contained in the software and documentation are 

 * those of the authors and should not be interpreted as representing official

 * policies, either expressed or implied, of the copyright holders.

 */
# 40 "SMAA.h"
/**

 *                  _______  ___  ___       ___           ___

 *                 /       ||   \/   |     /   \         /    *                |   (---- |  \  /  |    /  ^  \       /  ^   *                 \   \    |  |\/|  |   /  /_\  \     /  /_\   *              ----)   |   |  |  |  |  /  _____  \   /  _____   *             |_______/    |__|  |__| /__/     \__\ /__/     \__ * 
     \__\
 * 
 *                               E N H A N C E D

 *       S U B P I X E L   M O R P H O L O G I C A L   A N T I A L I A S I N G

 *

 *                         http://www.iryoku.com/smaa/

 *

 * Hi, welcome aboard!

 * 

 * Here you'll find instructions to get the shader up and running as fast as

 * possible.

 *

 * IMPORTANTE NOTICE: when updating, remember to update both this file and the

 * precomputed textures! They may change from version to version.

 *

 * The shader has three passes, chained together as follows:

 *

 *                           |input|------------------�

 *                              v                     |

 *                    [ SMAA*EdgeDetection ]          |

 *                              v                     |

 *                          |edgesTex|                |

 *                              v                     |

 *              [ SMAABlendingWeightCalculation ]     |

 *                              v                     |

 *                          |blendTex|                |

 *                              v                     |

 *                [ SMAANeighborhoodBlending ] <------�

 *                              v

 *                           |output|

 *

 * Note that each [pass] has its own vertex and pixel shader.

 *

 * You've three edge detection methods to choose from: luma, color or depth.

 * They represent different quality/performance and anti-aliasing/sharpness

 * tradeoffs, so our recommendation is for you to choose the one that best

 * suits your particular scenario:

 *

 * - Depth edge detection is usually the fastest but it may miss some edges.

 *

 * - Luma edge detection is usually more expensive than depth edge detection,

 *   but catches visible edges that depth edge detection can miss.

 *

 * - Color edge detection is usually the most expensive one but catches

 *   chroma-only edges.

 *

 * For quickstarters: just use luma edge detection.

 *

 * The general advice is to not rush the integration process and ensure each

 * step is done correctly (don't try to integrate SMAA T2x with predicated edge

 * detection from the start!). Ok then, let's go!

 *

 *  1. The first step is to create two RGBA temporal framebuffers for holding

 *     |edgesTex| and |blendTex|.

 *

 *     In DX10, you can use a RG framebuffer for the edges texture, but in our

 *     experience it yields worse performance.

 *

 *     On the Xbox 360, you can use the same framebuffer for resolving both

 *     |edgesTex| and |blendTex|, as they aren't needed simultaneously.

 *

 *  2. Both temporal framebuffers |edgesTex| and |blendTex| must be cleared

 *     each frame. Do not forget to clear the alpha channel!

 *

 *  3. The next step is loading the two supporting precalculated textures,

 *     'areaTex' and 'searchTex'. You'll find them in the 'Textures' folder as

 *     C++ headers, and also as regular DDS files. They'll be needed for the

 *     'SMAABlendingWeightCalculation' pass.

 *

 *     If you use the C++ headers, be sure to load them in the format specified

 *     inside of them.

 *

 *  4. In DX9, all samplers must be set to linear filtering and clamp, with the

 *     exception of 'searchTex', which must be set to point filtering.

 *

 *  5. All texture reads and buffer writes must be non-sRGB, with the exception

 *     of the input read and the output write of input in 

 *     'SMAANeighborhoodBlending' (and only in this pass!). If sRGB reads in

 *     this last pass are not possible, the technique will work anyway, but

 *     will perform antialiasing in gamma space. 

 *

 *     IMPORTANT: for best results the input read for the color/luma edge 

 *     detection should *NOT* be sRGB.

 *

 *  6. Before including SMAA.h you'll have to setup the framebuffer pixel size,

 *     the target and any optional configuration defines. Optionally you can

 *     use a preset.

 *

 *     You have three targets available: 

 *         SMAA_HLSL_3

 *         SMAA_HLSL_4

 *         SMAA_HLSL_4_1

 *         SMAA_GLSL_3 *

 *         SMAA_GLSL_4 *

 *

 *         * (See SMAA_ONLY_COMPILE_VS below).

 *

 *     And four presets:

 *         SMAA_PRESET_LOW          (%60 of the quality)

 *         SMAA_PRESET_MEDIUM       (%80 of the quality)

 *         SMAA_PRESET_HIGH         (%95 of the quality)

 *         SMAA_PRESET_ULTRA        (%99 of the quality)

 *

 *     For example:

 *         #define SMAA_PIXEL_SIZE float2(1.0 / 1280.0, 1.0 / 720.0)

 *         #define SMAA_HLSL_4 1 

 *         #define SMAA_PRESET_HIGH 1

 *         #include "SMAA.h"

 *

 *  7. Then, you'll have to setup the passes as indicated in the scheme above.

 *     You can take a look into SMAA.fx, to see how we did it for our demo.

 *     Checkout the function wrappers, you may want to copy-paste them!

 *

 *  8. It's recommended to validate the produced |edgesTex| and |blendTex|.

 *     It's advised to not continue with the implementation until both buffers

 *     are verified to produce identical results to our reference demo.

 *

 *  9. After you get the last pass to work, it's time to optimize. You'll have

 *     to initialize a stencil buffer in the first pass (discard is already in

 *     the code), then mask execution by using it the second pass. The last

 *     pass should be executed in all pixels.

 *

 *

 * After this point you can choose to enable predicated thresholding,

 * temporal supersampling and motion blur integration:

 *

 * a) If you want to use predicated thresholding, take a look into

 *    SMAA_PREDICATION; you'll need to pass an extra texture in the edge

 *    detection pass.

 *

 * b) If you want to enable temporal supersampling (SMAA T2x):

 *

 * 1. The first step is to render using subpixel jitters. I won't go into

 *    detail, but it's as simple as moving each vertex position in the

 *    vertex shader, you can check how we do it in our DX10 demo.

 *

 * 2. Then, you must setup the temporal resolve. You may want to take a look

 *    into SMAAResolve for resolving 2x modes. After you get it working, you'll

 *    probably see ghosting everywhere. But fear not, you can enable the

 *    CryENGINE temporal reprojection by setting the SMAA_REPROJECTION macro.

 *

 * 3. The next step is to apply SMAA to each subpixel jittered frame, just as

 *    done for 1x.

 *

 * 4. At this point you should already have something usable, but for best

 *    results the proper area textures must be set depending on current jitter.

 *    For this, the parameter 'subsampleIndices' of

 *    'SMAABlendingWeightCalculationPS' must be set as follows, for our T2x

 *    mode:

 *

 *    @SUBSAMPLE_INDICES

 *

 *    | S# |  Camera Jitter   |  subsampleIndices  |

 *    +----+------------------+--------------------+

 *    |  0 |  ( 0.25, -0.25)  |  int4(1, 1, 1, 0)  |

 *    |  1 |  (-0.25,  0.25)  |  int4(2, 2, 2, 0)  |

 *

 *    These jitter positions assume a bottom-to-top y axis. S# stands for the

 *    sample number.

 *

 * More information about temporal supersampling here:

 *    http://iryoku.com/aacourse/downloads/13-Anti-Aliasing-Methods-in-CryENGINE-3.pdf

 *

 * c) If you want to enable spatial multisampling (SMAA S2x):

 *

 * 1. The scene must be rendered using MSAA 2x. The MSAA 2x buffer must be

 *    created with:

 *      - DX10:     see below (*)

 *      - DX10.1:   D3D10_STANDARD_MULTISAMPLE_PATTERN or

 *      - DX11:     D3D11_STANDARD_MULTISAMPLE_PATTERN

 *

 *    This allows to ensure that the subsample order matches the table in

 *    @SUBSAMPLE_INDICES.

 *

 *    (*) In the case of DX10, we refer the reader to:

 *      - SMAA::detectMSAAOrder and

 *      - SMAA::msaaReorder

 *

 *    These functions allow to match the standard multisample patterns by

 *    detecting the subsample order for a specific GPU, and reordering

 *    them appropriately.

 *

 * 2. A shader must be run to output each subsample into a separate buffer

 *    (DX10 is required). You can use SMAASeparate for this purpose, or just do

 *    it in an existing pass (for example, in the tone mapping pass).

 *

 * 3. The full SMAA 1x pipeline must be run for each separated buffer, storing

 *    the results in the final buffer. The second run should alpha blend with

 *    the existing final buffer using a blending factor of 0.5.

 *    'subsampleIndices' must be adjusted as in the SMAA T2x case (see point

 *    b).

 *

 * d) If you want to enable temporal supersampling on top of SMAA S2x

 *    (which actually is SMAA 4x):

 *

 * 1. SMAA 4x consists on temporally jittering SMAA S2x, so the first step is

 *    to calculate SMAA S2x for current frame. In this case, 'subsampleIndices'

 *    must be set as follows:

 *

 *    | F# | S# |   Camera Jitter    |    Net Jitter     |  subsampleIndices  |

 *    +----+----+--------------------+-------------------+--------------------+

 *    |  0 |  0 |  ( 0.125,  0.125)  |  ( 0.375, -0.125) |  int4(5, 3, 1, 3)  |

 *    |  0 |  1 |  ( 0.125,  0.125)  |  (-0.125,  0.375) |  int4(4, 6, 2, 3)  |

 *    +----+----+--------------------+-------------------+--------------------+

 *    |  1 |  2 |  (-0.125, -0.125)  |  ( 0.125, -0.375) |  int4(3, 5, 1, 4)  |

 *    |  1 |  3 |  (-0.125, -0.125)  |  (-0.375,  0.125) |  int4(6, 4, 2, 4)  |

 *

 *    These jitter positions assume a bottom-to-top y axis. F# stands for the

 *    frame number. S# stands for the sample number.

 *

 * 2. After calculating SMAA S2x for current frame (with the new subsample

 *    indices), previous frame must be reprojected as in SMAA T2x mode (see

 *    point b).

 *

 * e) If motion blur is used, you may want to do the edge detection pass

 *    together with motion blur. This has two advantages:

 *

 * 1. Pixels under heavy motion can be omitted from the edge detection process.

 *    For these pixels we can just store "no edge", as motion blur will take

 *    care of them.

 * 2. The center pixel tap is reused.

 *

 * Note that in this case depth testing should be used instead of stenciling,

 * as we have to write all the pixels in the motion blur pass.

 *

 * That's it!

 */
# 274 "SMAA.h"
//-----------------------------------------------------------------------------
// SMAA Presets

/**

 * Note that if you use one of these presets, the corresponding macros below

 * won't be used.

 */
# 309 "SMAA.h"
//-----------------------------------------------------------------------------
// Configurable Defines

/**

 * SMAA_THRESHOLD specifies the threshold or sensitivity to edges.

 * Lowering this value you will be able to detect more edges at the expense of

 * performance. 

 *

 * Range: [0, 0.5]

 *   0.1 is a reasonable value, and allows to catch most visible edges.

 *   0.05 is a rather overkill value, that allows to catch 'em all.

 *

 *   If temporal supersampling is used, 0.2 could be a reasonable value, as low

 *   contrast edges are properly filtered by just 2x.

 */
# 328 "SMAA.h"
/**

 * SMAA_DEPTH_THRESHOLD specifies the threshold for depth edge detection.

 * 

 * Range: depends on the depth range of the scene.

 */
/**

 * SMAA_MAX_SEARCH_STEPS specifies the maximum steps performed in the

 * horizontal/vertical pattern searches, at each side of the pixel.

 *

 * In number of pixels, it's actually the double. So the maximum line length

 * perfectly handled by, for example 16, is 64 (by perfectly, we meant that

 * longer lines won't look as good, but still antialiased).

 *

 * Range: [0, 98]

 */
# 351 "SMAA.h"
/**

 * SMAA_MAX_SEARCH_STEPS_DIAG specifies the maximum steps performed in the

 * diagonal pattern searches, at each side of the pixel. In this case we jump

 * one pixel at time, instead of two.

 *

 * Range: [0, 20]; set it to 0 to disable diagonal processing.

 *

 * On high-end machines it is cheap (between a 0.8x and 0.9x slower for 16 

 * steps), but it can have a significant impact on older machines.

 */
# 365 "SMAA.h"
/**

 * SMAA_CORNER_ROUNDING specifies how much sharp corners will be rounded.

 *

 * Range: [0, 100]; set it to 100 to disable corner detection.

 */
/**

 * Predicated thresholding allows to better preserve texture details and to

 * improve performance, by decreasing the number of detected edges using an

 * additional buffer like the light accumulation buffer, object ids or even the

 * depth buffer (the depth buffer usage may be limited to indoor or short range

 * scenes).

 *

 * It locally decreases the luma or color threshold if an edge is found in an

 * additional buffer (so the global threshold can be higher).

 *

 * This method was developed by Playstation EDGE MLAA team, and used in 

 * Killzone 3, by using the light accumulation buffer. More information here:

 *     http://iryoku.com/aacourse/downloads/06-MLAA-on-PS3.pptx 

 */
# 392 "SMAA.h"
/**

 * Threshold to be used in the additional predication buffer. 

 *

 * Range: depends on the input, so you'll have to find the magic number that

 * works for you.

 */
# 402 "SMAA.h"
/**

 * How much to scale the global threshold used for luma or color edge

 * detection when using predication.

 *

 * Range: [1, 5]

 */
# 412 "SMAA.h"
/**

 * How much to locally decrease the threshold.

 *

 * Range: [0, 1]

 */
/**

 * Temporal reprojection allows to remove ghosting artifacts when using

 * temporal supersampling. We use the CryEngine 3 method which also introduces

 * velocity weighting. This feature is of extreme importance for totally

 * removing ghosting. More information here:

 *    http://iryoku.com/aacourse/downloads/13-Anti-Aliasing-Methods-in-CryENGINE-3.pdf

 *

 * Note that you'll need to setup a velocity buffer for enabling reprojection.

 * For static geometry, saving the previous depth buffer is a viable

 * alternative.

 */
# 436 "SMAA.h"
/**

 * SMAA_REPROJECTION_WEIGHT_SCALE controls the velocity weighting. It allows to

 * remove ghosting trails behind the moving object, which are not removed by

 * just using reprojection. Using low values will exhibit ghosting, while using

 * high values will disable temporal supersampling under motion.

 *

 * Behind the scenes, velocity weighting removes temporal supersampling when

 * the velocity of the subsamples differs (meaning they are different objects).

 *

 * Range: [0, 80]

 */
# 449 "SMAA.h"
/**

 * In the last pass we leverage bilinear filtering to avoid some lerps.

 * However, bilinear filtering is done in gamma space in DX9, under DX9

 * hardware (but not in DX9 code running on DX10 hardware), which gives

 * inaccurate results.

 *

 * So, if you are in DX9, under DX9 hardware, and do you want accurate linear

 * blending, you must set this flag to 1.

 *

 * It's ignored when using SMAA_HLSL_4, and of course, only has sense when

 * using sRGB read and writes on the last pass.

 */
# 465 "SMAA.h"
/**

 * On ATI compilers, discard cannot be used in vertex shaders. Thus, they need

 * to be compiled separately. These macros allow to easily accomplish it.

 */




//-----------------------------------------------------------------------------
// Non-Configurable Defines
# 488 "SMAA.h"
//-----------------------------------------------------------------------------
// Porting Functions
# 553 "SMAA.h"
//-----------------------------------------------------------------------------
// Misc functions

/**

 * Gathers current pixel, and the top-left neighbors.

 */
# 559 "SMAA.h"
vec3 SMAAGatherNeighbours(vec2 texcoord,
                            vec4 offset[3],
                            sampler2D tex) {



    float P = texture(tex, texcoord).r;
    float Pleft = texture(tex, offset[0].xy).r;
    float Ptop = texture(tex, offset[0].zw).r;
    return vec3(P, Pleft, Ptop);

}

/**

 * Adjusts the threshold by means of predication.

 */
# 575 "SMAA.h"
vec2 SMAACalculatePredicatedThreshold(vec2 texcoord,
                                        vec4 offset[3],
                                        sampler2D colorTex,
                                        sampler2D predicationTex) {
    vec3 neighbours = SMAAGatherNeighbours(texcoord, offset, predicationTex);
    vec2 delta = abs(neighbours.xx - neighbours.yz);
    vec2 edges = step(0.01, delta);
    return 2.0 * 0.1 * (1.0 - 0.4 * edges);
}


//-----------------------------------------------------------------------------
// Vertex Shaders

/**

 * Edge Detection Vertex Shader

 */
# 592 "SMAA.h"
void SMAAEdgeDetectionVS(vec4 position,
                         out vec4 svPosition,
                         inout vec2 texcoord,
                         out vec4 offset[3]) {
    svPosition = position;

    offset[0] = texcoord.xyxy + vec2(1.0 / 1280.0, 1.0 / 800.0).xyxy * vec4(-1.0, 0.0, 0.0, -1.0);
    offset[1] = texcoord.xyxy + vec2(1.0 / 1280.0, 1.0 / 800.0).xyxy * vec4( 1.0, 0.0, 0.0, 1.0);
    offset[2] = texcoord.xyxy + vec2(1.0 / 1280.0, 1.0 / 800.0).xyxy * vec4(-2.0, 0.0, 0.0, -2.0);
}

/**

 * Blend Weight Calculation Vertex Shader

 */
# 606 "SMAA.h"
void SMAABlendingWeightCalculationVS(vec4 position,
                                     out vec4 svPosition,
                                     inout vec2 texcoord,
                                     out vec2 pixcoord,
                                     out vec4 offset[3]) {
    svPosition = position;

    pixcoord = texcoord / vec2(1.0 / 1280.0, 1.0 / 800.0);

    // We will use these offsets for the searches later on (see @PSEUDO_GATHER4):
    offset[0] = texcoord.xyxy + vec2(1.0 / 1280.0, 1.0 / 800.0).xyxy * vec4(-0.25, -0.125, 1.25, -0.125);
    offset[1] = texcoord.xyxy + vec2(1.0 / 1280.0, 1.0 / 800.0).xyxy * vec4(-0.125, -0.25, -0.125, 1.25);

    // And these for the searches, they indicate the ends of the loops:
    offset[2] = vec4(offset[0].xz, offset[1].yw) +
                vec4(-2.0, 2.0, -2.0, 2.0) *
                vec2(1.0 / 1280.0, 1.0 / 800.0).xxyy * float(8);
}

/**

 * Neighborhood Blending Vertex Shader

 */
# 628 "SMAA.h"
void SMAANeighborhoodBlendingVS(vec4 position,
                                out vec4 svPosition,
                                inout vec2 texcoord,
                                out vec4 offset[2]) {
    svPosition = position;

    offset[0] = texcoord.xyxy + vec2(1.0 / 1280.0, 1.0 / 800.0).xyxy * vec4(-1.0, 0.0, 0.0, -1.0);
    offset[1] = texcoord.xyxy + vec2(1.0 / 1280.0, 1.0 / 800.0).xyxy * vec4( 1.0, 0.0, 0.0, 1.0);
}

/**

 * Resolve Vertex Shader

 */
# 641 "SMAA.h"
void SMAAResolveVS(vec4 position,
                   out vec4 svPosition,
                   inout vec2 texcoord) {
    svPosition = position;
}

/**

 * Separate Vertex Shader

 */
# 650 "SMAA.h"
void SMAASeparateVS(vec4 position,
                    out vec4 svPosition,
                    inout vec2 texcoord) {
    svPosition = position;
}



//-----------------------------------------------------------------------------
// Edge Detection Pixel Shaders (First Pass)

/**

 * Luma Edge Detection

 *

 * IMPORTANT NOTICE: luma edge detection requires gamma-corrected colors, and

 * thus 'colorTex' should be a non-sRGB texture.

 */
# 667 "SMAA.h"
vec4 SMAALumaEdgeDetectionPS(vec2 texcoord,
                               vec4 offset[3],
                               sampler2D colorTex



                               ) {
    // Calculate the threshold:



    vec2 threshold = vec2(0.1, 0.1);


    // Calculate lumas:
    vec3 weights = vec3(0.2126, 0.7152, 0.0722);
    float L = dot(texture(colorTex, texcoord).rgb, weights);
    float Lleft = dot(texture(colorTex, offset[0].xy).rgb, weights);
    float Ltop = dot(texture(colorTex, offset[0].zw).rgb, weights);

    // We do the usual threshold:
    vec4 delta;
    delta.xy = abs(L - vec2(Lleft, Ltop));
    vec2 edges = step(threshold, delta.xy);

    // Then discard if there is no edge:
    if (dot(edges, vec2(1.0, 1.0)) == 0.0)
        discard;

    // Calculate right and bottom deltas:
    float Lright = dot(texture(colorTex, offset[1].xy).rgb, weights);
    float Lbottom = dot(texture(colorTex, offset[1].zw).rgb, weights);
    delta.zw = abs(L - vec2(Lright, Lbottom));

    // Calculate the maximum delta in the direct neighborhood:
    vec2 maxDelta = max(delta.xy, delta.zw);
    maxDelta = max(maxDelta.xx, maxDelta.yy);

    // Calculate left-left and top-top deltas:
    float Lleftleft = dot(texture(colorTex, offset[2].xy).rgb, weights);
    float Ltoptop = dot(texture(colorTex, offset[2].zw).rgb, weights);
    delta.zw = abs(vec2(Lleft, Ltop) - vec2(Lleftleft, Ltoptop));

    // Calculate the final maximum delta:
    maxDelta = max(maxDelta.xy, delta.zw);

    /**

     * Each edge with a delta in luma of less than 50% of the maximum luma

     * surrounding this pixel is discarded. This allows to eliminate spurious

     * crossing edges, and is based on the fact that, if there is too much

     * contrast in a direction, that will hide contrast in the other

     * neighbors.

     * This is done after the discard intentionally as this situation doesn't

     * happen too frequently (but it's important to do as it prevents some 

     * edges from going undetected).

     */
# 723 "SMAA.h"
    edges.xy *= step(0.5 * maxDelta, delta.xy);

    return vec4(edges, 0.0, 0.0);
}

/**

 * Color Edge Detection

 *

 * IMPORTANT NOTICE: color edge detection requires gamma-corrected colors, and

 * thus 'colorTex' should be a non-sRGB texture.

 */
# 734 "SMAA.h"
vec4 SMAAColorEdgeDetectionPS(vec2 texcoord,
                                vec4 offset[3],
                                sampler2D colorTex



                                ) {
    // Calculate the threshold:



    vec2 threshold = vec2(0.1, 0.1);


    // Calculate color deltas:
    vec4 delta;
    vec3 C = texture(colorTex, texcoord).rgb;

    vec3 Cleft = texture(colorTex, offset[0].xy).rgb;
    vec3 t = abs(C - Cleft);
    delta.x = max(max(t.r, t.g), t.b);

    vec3 Ctop = texture(colorTex, offset[0].zw).rgb;
    t = abs(C - Ctop);
    delta.y = max(max(t.r, t.g), t.b);

    // We do the usual threshold:
    vec2 edges = step(threshold, delta.xy);

    // Then discard if there is no edge:
    if (dot(edges, vec2(1.0, 1.0)) == 0.0)
        discard;

    // Calculate right and bottom deltas:
    vec3 Cright = texture(colorTex, offset[1].xy).rgb;
    t = abs(C - Cright);
    delta.z = max(max(t.r, t.g), t.b);

    vec3 Cbottom = texture(colorTex, offset[1].zw).rgb;
    t = abs(C - Cbottom);
    delta.w = max(max(t.r, t.g), t.b);

    // Calculate the maximum delta in the direct neighborhood:
    float maxDelta = max(max(max(delta.x, delta.y), delta.z), delta.w);

    // Calculate left-left and top-top deltas:
    vec3 Cleftleft = texture(colorTex, offset[2].xy).rgb;
    t = abs(C - Cleftleft);
    delta.z = max(max(t.r, t.g), t.b);

    vec3 Ctoptop = texture(colorTex, offset[2].zw).rgb;
    t = abs(C - Ctoptop);
    delta.w = max(max(t.r, t.g), t.b);

    // Calculate the final maximum delta:
    maxDelta = max(max(maxDelta, delta.z), delta.w);

    // Local contrast adaptation in action:
    edges.xy *= step(0.5 * maxDelta, delta.xy);

    return vec4(edges, 0.0, 0.0);
}

/**

 * Depth Edge Detection

 */
# 800 "SMAA.h"
vec4 SMAADepthEdgeDetectionPS(vec2 texcoord,
                                vec4 offset[3],
                                sampler2D depthTex) {
    vec3 neighbours = SMAAGatherNeighbours(texcoord, offset, depthTex);
    vec2 delta = abs(neighbours.xx - vec2(neighbours.y, neighbours.z));
    vec2 edges = step((0.1 * 0.1), delta);

    if (dot(edges, vec2(1.0, 1.0)) == 0.0)
        discard;

    return vec4(edges, 0.0, 0.0);
}

//-----------------------------------------------------------------------------
// Diagonal Search Functions
# 919 "SMAA.h"
//-----------------------------------------------------------------------------
// Horizontal/Vertical Search Functions

/**

 * This allows to determine how much length should we add in the last step

 * of the searches. It takes the bilinearly interpolated edge (see 

 * @PSEUDO_GATHER4), and adds 0, 1 or 2, depending on which edges and

 * crossing edges are active.

 */
# 928 "SMAA.h"
float SMAASearchLength(sampler2D searchTex, vec2 e, float bias, float scale) {
    // Not required if searchTex accesses are set to point:
    // float2 SEARCH_TEX_PIXEL_SIZE = 1.0 / float2(66.0, 33.0);
    // e = float2(bias, 0.0) + 0.5 * SEARCH_TEX_PIXEL_SIZE + 
    //     e * float2(scale, 1.0) * float2(64.0, 32.0) * SEARCH_TEX_PIXEL_SIZE;
    e.r = bias + e.r * scale;
    return 255.0 * textureLod(searchTex, e, 0.0).r;
}

/**

 * Horizontal/vertical search functions for the 2nd pass.

 */
# 940 "SMAA.h"
float SMAASearchXLeft(sampler2D edgesTex, sampler2D searchTex, vec2 texcoord, float end) {
    /**

     * @PSEUDO_GATHER4

     * This texcoord has been offset by (-0.25, -0.125) in the vertex shader to

     * sample between edge, thus fetching four edges in a row.

     * Sampling with different offsets in each direction allows to disambiguate

     * which edges are active from the four fetched ones.

     */
# 948 "SMAA.h"
    vec2 e = vec2(0.0, 1.0);
    while (texcoord.x > end &&
           e.g > 0.8281 && // Is there some edge not activated?
           e.r == 0.0) { // Or is there a crossing edge that breaks the line?
        e = textureLod(edgesTex, texcoord, 0.0).rg;
        texcoord -= vec2(2.0, 0.0) * vec2(1.0 / 1280.0, 1.0 / 800.0);
    }

    // We correct the previous (-0.25, -0.125) offset we applied:
    texcoord.x += 0.25 * vec2(1.0 / 1280.0, 1.0 / 800.0).x;

    // The searches are bias by 1, so adjust the coords accordingly:
    texcoord.x += vec2(1.0 / 1280.0, 1.0 / 800.0).x;

    // Disambiguate the length added by the last step:
    texcoord.x += 2.0 * vec2(1.0 / 1280.0, 1.0 / 800.0).x; // Undo last step
    texcoord.x -= vec2(1.0 / 1280.0, 1.0 / 800.0).x * SMAASearchLength(searchTex, e, 0.0, 0.5);

    return texcoord.x;
}

float SMAASearchXRight(sampler2D edgesTex, sampler2D searchTex, vec2 texcoord, float end) {
    vec2 e = vec2(0.0, 1.0);
    while (texcoord.x < end &&
           e.g > 0.8281 && // Is there some edge not activated?
           e.r == 0.0) { // Or is there a crossing edge that breaks the line?
        e = textureLod(edgesTex, texcoord, 0.0).rg;
        texcoord += vec2(2.0, 0.0) * vec2(1.0 / 1280.0, 1.0 / 800.0);
    }

    texcoord.x -= 0.25 * vec2(1.0 / 1280.0, 1.0 / 800.0).x;
    texcoord.x -= vec2(1.0 / 1280.0, 1.0 / 800.0).x;
    texcoord.x -= 2.0 * vec2(1.0 / 1280.0, 1.0 / 800.0).x;
    texcoord.x += vec2(1.0 / 1280.0, 1.0 / 800.0).x * SMAASearchLength(searchTex, e, 0.5, 0.5);
    return texcoord.x;
}

float SMAASearchYUp(sampler2D edgesTex, sampler2D searchTex, vec2 texcoord, float end) {
    vec2 e = vec2(1.0, 0.0);
    while (texcoord.y > end &&
           e.r > 0.8281 && // Is there some edge not activated?
           e.g == 0.0) { // Or is there a crossing edge that breaks the line?
        e = textureLod(edgesTex, texcoord, 0.0).rg;
        texcoord -= vec2(0.0, 2.0) * vec2(1.0 / 1280.0, 1.0 / 800.0);
    }

    texcoord.y += 0.25 * vec2(1.0 / 1280.0, 1.0 / 800.0).y;
    texcoord.y += vec2(1.0 / 1280.0, 1.0 / 800.0).y;
    texcoord.y += 2.0 * vec2(1.0 / 1280.0, 1.0 / 800.0).y;
    texcoord.y -= vec2(1.0 / 1280.0, 1.0 / 800.0).y * SMAASearchLength(searchTex, e.gr, 0.0, 0.5);
    return texcoord.y;
}

float SMAASearchYDown(sampler2D edgesTex, sampler2D searchTex, vec2 texcoord, float end) {
    vec2 e = vec2(1.0, 0.0);
    while (texcoord.y < end &&
           e.r > 0.8281 && // Is there some edge not activated?
           e.g == 0.0) { // Or is there a crossing edge that breaks the line?
        e = textureLod(edgesTex, texcoord, 0.0).rg;
        texcoord += vec2(0.0, 2.0) * vec2(1.0 / 1280.0, 1.0 / 800.0);
    }

    texcoord.y -= 0.25 * vec2(1.0 / 1280.0, 1.0 / 800.0).y;
    texcoord.y -= vec2(1.0 / 1280.0, 1.0 / 800.0).y;
    texcoord.y -= 2.0 * vec2(1.0 / 1280.0, 1.0 / 800.0).y;
    texcoord.y += vec2(1.0 / 1280.0, 1.0 / 800.0).y * SMAASearchLength(searchTex, e.gr, 0.5, 0.5);
    return texcoord.y;
}

/** 

 * Ok, we have the distance and both crossing edges. So, what are the areas

 * at each side of current edge?

 */
# 1021 "SMAA.h"
vec2 SMAAArea(sampler2D areaTex, vec2 dist, float e1, float e2, float offset) {
    // Rounding prevents precision errors of bilinear filtering:
    vec2 texcoord = float(16) * round(4.0 * vec2(e1, e2)) + dist;

    // We do a scale and bias for mapping to texel space:
    texcoord = (1.0 / vec2(160.0, 560.0)) * texcoord + (0.5 * (1.0 / vec2(160.0, 560.0)));

    // Move to proper place, according to the subpixel offset:
    texcoord.y += (1.0 / 7.0) * offset;

    // Do it!



    return textureLod(areaTex, texcoord, 0.0).rg;

}

//-----------------------------------------------------------------------------
// Corner Detection Functions

void SMAADetectHorizontalCornerPattern(sampler2D edgesTex, inout vec2 weights, vec2 texcoord, vec2 d) {
# 1056 "SMAA.h"
}

void SMAADetectVerticalCornerPattern(sampler2D edgesTex, inout vec2 weights, vec2 texcoord, vec2 d) {
# 1072 "SMAA.h"
}

//-----------------------------------------------------------------------------
// Blending Weight Calculation Pixel Shader (Second Pass)

vec4 SMAABlendingWeightCalculationPS(vec2 texcoord,
                                       vec2 pixcoord,
                                       vec4 offset[3],
                                       sampler2D edgesTex,
                                       sampler2D areaTex,
                                       sampler2D searchTex,
                                       ivec4 subsampleIndices) { // Just pass zero for SMAA 1x, see @SUBSAMPLE_INDICES.
    vec4 weights = vec4(0.0, 0.0, 0.0, 0.0);

    vec2 e = texture(edgesTex, texcoord).rg;

   
    if (e.g > 0.0) { // Edge at north
# 1101 "SMAA.h"
        vec2 d;

        // Find the distance to the left:
        vec2 coords;
        coords.x = SMAASearchXLeft(edgesTex, searchTex, offset[0].xy, offset[2].x);
        coords.y = offset[1].y; // offset[1].y = texcoord.y - 0.25 * SMAA_PIXEL_SIZE.y (@CROSSING_OFFSET)
        d.x = coords.x;

        // Now fetch the left crossing edges, two at a time using bilinear
        // filtering. Sampling at -0.25 (see @CROSSING_OFFSET) enables to
        // discern what value each edge has:
        float e1 = textureLod(edgesTex, coords, 0.0).r;

        // Find the distance to the right:
        coords.x = SMAASearchXRight(edgesTex, searchTex, offset[0].zw, offset[2].y);
        d.y = coords.x;

        // We want the distances to be in pixel units (doing this here allow to
        // better interleave arithmetic and memory accesses):
        d = d / vec2(1.0 / 1280.0, 1.0 / 800.0).x - pixcoord.x;

        // SMAAArea below needs a sqrt, as the areas texture is compressed 
        // quadratically:
        vec2 sqrt_d = sqrt(abs(d));

        // Fetch the right crossing edges:
        float e2 = textureLodOffset(edgesTex, coords, 0.0, ivec2(1, 0)).r;

        // Ok, we know how this pattern looks like, now it is time for getting
        // the actual area:
        weights.rg = SMAAArea(areaTex, sqrt_d, e1, e2, float(subsampleIndices.y));

        // Fix corners:
        SMAADetectHorizontalCornerPattern(edgesTex, weights.rg, texcoord, d);





    }

   
    if (e.r > 0.0) { // Edge at west
        vec2 d;

        // Find the distance to the top:
        vec2 coords;
        coords.y = SMAASearchYUp(edgesTex, searchTex, offset[1].xy, offset[2].z);
        coords.x = offset[0].x; // offset[1].x = texcoord.x - 0.25 * SMAA_PIXEL_SIZE.x;
        d.x = coords.y;

        // Fetch the top crossing edges:
        float e1 = textureLod(edgesTex, coords, 0.0).g;

        // Find the distance to the bottom:
        coords.y = SMAASearchYDown(edgesTex, searchTex, offset[1].zw, offset[2].w);
        d.y = coords.y;

        // We want the distances to be in pixel units:
        d = d / vec2(1.0 / 1280.0, 1.0 / 800.0).y - pixcoord.y;

        // SMAAArea below needs a sqrt, as the areas texture is compressed 
        // quadratically:
        vec2 sqrt_d = sqrt(abs(d));

        // Fetch the bottom crossing edges:
        float e2 = textureLodOffset(edgesTex, coords, 0.0, ivec2(0, 1)).g;

        // Get the area for this direction:
        weights.ba = SMAAArea(areaTex, sqrt_d, e1, e2, float(subsampleIndices.x));

        // Fix corners:
        SMAADetectVerticalCornerPattern(edgesTex, weights.ba, texcoord, d);
    }

    return weights;
}

//-----------------------------------------------------------------------------
// Neighborhood Blending Pixel Shader (Third Pass)

vec4 SMAANeighborhoodBlendingPS(vec2 texcoord,
                                  vec4 offset[2],
                                  sampler2D colorTex,
                                  sampler2D blendTex) {
    // Fetch the blending weights for current pixel:
    vec4 a;
    a.xz = texture(blendTex, texcoord).xz;
    a.y = texture(blendTex, offset[1].zw).g;
    a.w = texture(blendTex, offset[1].xy).a;

    // Is there any blending weight with a value greater than 0.0?
   
    if (dot(a, vec4(1.0, 1.0, 1.0, 1.0)) < 1e-5)
        return textureLod(colorTex, texcoord, 0.0);
    else {
        vec4 color = vec4(0.0, 0.0, 0.0, 0.0);

        // Up to 4 lines can be crossing a pixel (one through each edge). We
        // favor blending by choosing the line with the maximum weight for each
        // direction:
        vec2 offset;
        offset.x = a.a > a.b? a.a : -a.b; // left vs. right 
        offset.y = a.g > a.r? a.g : -a.r; // top vs. bottom

        // Then we go in the direction that has the maximum weight:
        if (abs(offset.x) > abs(offset.y)) // horizontal vs. vertical
            offset.y = 0.0;
        else
            offset.x = 0.0;
# 1230 "SMAA.h"
        // We exploit bilinear filtering to mix current pixel with the chosen
        // neighbor:
        texcoord += offset * vec2(1.0 / 1280.0, 1.0 / 800.0);
        return textureLod(colorTex, texcoord, 0.0);
# 1242 "SMAA.h"
    }
}

//-----------------------------------------------------------------------------
// Temporal Resolve Pixel Shader (Optional Pass)

vec4 SMAAResolvePS(vec2 texcoord,
                     sampler2D colorTexCurr,
                     sampler2D colorTexPrev



                     ) {
# 1273 "SMAA.h"
    // Just blend the pixels:
    vec4 current = texture(colorTexCurr, texcoord);
    vec4 previous = texture(colorTexPrev, texcoord);
    return mix(current, previous, 0.5);

}

//-----------------------------------------------------------------------------
// Separate Multisamples Pixel Shader (Optional Pass)
# 1295 "SMAA.h"
//-----------------------------------------------------------------------------
