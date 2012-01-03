// Shadow mapping uses all this stuff
#extension GL_EXT_texture_array : enable
#define SHADOW_SAMPLES 4
uniform sampler2DArrayShadow shadows;
uniform vec4 cascadeDistances;
uniform mat4 shadowMatrix[4];
uniform vec4 pcfOffsets[4];
uniform float shadow_bias;
uniform float shadow_factor;

uniform samplerCube envmap;
uniform sampler2D tex;
uniform sampler2D normalmap;
uniform sampler2D specularmap;

#pragma include "lighting.glsl"

// Shadow in
in vec4 vPosition;
in float vDepth;
in vec3 bonedebug;

// Light in
in vec2 coords;
in vec3 reflectDir;
in vec3 lightVec, halfVec, eyeVec;
in mat3 invTan;

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
    vec4 ambientcolor = gl_LightModel.ambient * textureCube(envmap, reflectDir);
    vec4 texcol = texture2D(tex, coords);
    vec3 mnormal = normalize(2.0 * texture2D(normalmap, coords.st).rgb - 1.0);
    vec4 speccolor = texture2D(specularmap, coords.st);
    vec3 n = mnormal; // normalize the interpolated normal

    //speccolor = vec4(1,1,1,1);
    n = vec3(0,0,1);
    
    float NdotL = (max(dot(n, lightVec), 0.0));
    vec4 diff = texcol * NdotL;

    //diff = vec4(NdotL);
    //diff.w = 1.0;

    vec4 spec = vec4(0,0,0,1);
    // Blinn-Phong
    if(NdotL > 0.0) {
        vec3 halfV = normalize(halfVec);
        float NdotHV = max(dot(n, halfV), 0.0);
        spec = pow(NdotHV, 32.0) * speccolor * 2.0;
    }

    gl_FragColor.rgb = ambientcolor.rgb * texcol.rgb + (diff.rgb + spec.rgb) * getShadowFraction();
    //gl_FragColor.rgb *= 0.0001;
    //gl_FragColor.rgb += (invTan * n);
    //gl_FragColor.rgb += vec3(spec.rgb);
    gl_FragColor.a = 1.0;
}