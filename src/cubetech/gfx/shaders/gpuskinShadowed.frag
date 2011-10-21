// Shadow mapping uses all this stuff
#extension GL_EXT_texture_array : enable
#define SHADOW_SAMPLES 4
uniform sampler2DArrayShadow shadows;
uniform vec4 cascadeDistances;
uniform mat4 shadowMatrix[4];
uniform vec4 pcfOffsets[4];
uniform float shadow_bias = 0.002;
uniform float shadow_factor = 0.75;

uniform samplerCube envmap;
uniform sampler2D tex;

#pragma include "lighting.glsl"

// Shadow in
varying vec4 vPosition;
varying float vDepth;

// Light in

varying vec3 reflectDir;

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
    // Start with ambient
    vec4 ambientcolor = ambient * textureCube(envmap, reflectDir);
    vec4 texcol = texture2D(tex, gl_TexCoord[0].xy);

    // get light
    vec4 lightColor = getLighting(texcol);
    // multiply by shadow
    lightColor.rgb *= getShadowFraction();

    gl_FragColor.rgb = texcol.rgb * (ambientcolor.rgb + lightColor.rgb) * gl_Color.rgb;
    gl_FragColor.a = 1.0;
}