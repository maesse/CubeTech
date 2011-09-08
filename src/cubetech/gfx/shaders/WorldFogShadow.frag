#version 120
// Shadow mapping uses all this stuff
#extension GL_EXT_texture_array : enable
#define SHADOW_SAMPLES 1
uniform sampler2D tex;

// Shadow
uniform sampler2DArrayShadow shadows;
uniform float shadow_bias = 0.002;
uniform float shadow_factor = 0.75;
uniform vec4 cascadeDistances;
uniform mat4 shadowMatrix[4];
uniform vec4 pcfOffsets[4];


// Regular stuff
uniform vec4 fog_color = vec4(0.823,0.7,0.486,1);
uniform float fog_factor = 0.001;

varying vec4 vPosition;
varying float vDepth;
varying vec2 coords;
varying vec4 color;

float getShadowFraction()
{
    // Shadow
    vec4 compare1 = vec4(greaterThan(vec4(vDepth), cascadeDistances));
    float fIndex = dot(vec4(1,1,1,1), compare1);
    int index = int(fIndex);

    vec4 shadowCoords = shadowMatrix[index] * vPosition;
    shadowCoords.w = (shadowCoords.z - shadow_bias);
    shadowCoords.z = fIndex;

    // Sample four times
    float lightDist = shadow_bias;
    if(SHADOW_SAMPLES == 1) {
        lightDist = shadow2DArray(shadows, shadowCoords).r + shadow_bias;
    } else {
        for(int i=0; i<SHADOW_SAMPLES;i++) {
            lightDist += shadow2DArray(shadows, shadowCoords + pcfOffsets[i]).r;
        }
        lightDist /= SHADOW_SAMPLES;
    }
    return lightDist;
}

void main()
{
    
    float shadowCo = (getShadowFraction() * shadow_factor) + (1.0 - shadow_factor);

    vec4 dest = texture2D(tex, coords) * color;
    dest.rgb *= shadowCo;

    float fogFactor = exp(-pow((fog_factor * gl_FogFragCoord), 2.0));
    gl_FragColor = mix(fog_color, dest, fogFactor);
}
