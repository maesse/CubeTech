// Shadow mapping uses all this stuff
#extension GL_EXT_texture_array : enable
#define SHADOW_SAMPLES 4
uniform sampler2DArrayShadow shadows;
uniform vec4 cascadeDistances;
uniform mat4 shadowMatrix[4];
uniform vec4 pcfOffsets[4];
uniform float shadow_bias = 0.002;
uniform float shadow_factor = 0.75;
varying vec4 vPosition;
varying float vDepth;

uniform samplerCube envmap;
uniform sampler2D tex;

varying vec2 coords;
varying vec4 diffuse, ambient;
varying vec3 normal, lightDir, halfVector;
varying vec3 reflectDir;
varying vec4 col;

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
        lightDist = shadow2DArray(shadows, shadowCoords).r;
    } else {
        for(int i=0; i<SHADOW_SAMPLES;i++) {
            lightDist += shadow2DArray(shadows, shadowCoords + pcfOffsets[i]).r;
        }
        lightDist /= SHADOW_SAMPLES;
    }
    return lightDist;
}

vec4 getLighting(in vec4 texcol)
{
    vec3 n = normalize(normal); // normalize the interpolated normal
    float NdotL = (max(dot(n, lightDir), 0.0)*shadow_factor)+(1.0-shadow_factor);
    vec3 halfV = normalize(halfVector);
    float NdotHV = max(dot(n, halfV), 0.0);

    vec4 lightColor = diffuse * NdotL;
    lightColor += texcol
            * pow(NdotHV, gl_FrontMaterial.shininess) * texcol.a * 5.0;
    return lightColor;
}

void main()
{
    // Start with ambient
    vec4 color = ambient * textureCube(envmap, reflectDir);

    vec4 texcol = texture2D(tex, coords);

    vec4 lightColor = getLighting(texcol);
    lightColor.rgb *= getShadowFraction();

    gl_FragColor.rgb = texcol.rgb * (color.rgb + lightColor.rgb) * col.rgb;
    gl_FragColor.a = 1.0;
}