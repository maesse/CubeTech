#version 120
// Shadow mapping uses all this stuff
#extension GL_EXT_texture_array : enable
uniform sampler2DArrayShadow shadows;
uniform vec4 cascadeDistances[2];
uniform mat4 shadowMatrix[8];
uniform vec4 pcfOffsets[4];
uniform float shadow_bias = 0.002;
varying vec4 vPosition;
varying float vDepth;

// Regular stuff
uniform sampler2D tex;
uniform vec4 fog_color = vec4(0.823,0.7,0.486,1);
uniform float fog_factor = 0.001;
//uniform vec4 cascadeColors[8];
varying vec2 coords;
varying vec4 color;

void main()
{
    // Shadow
    vec4 pixelDepth = vec4(vDepth,vDepth,vDepth,vDepth);
    vec4 compare1 = vec4(greaterThan(pixelDepth, cascadeDistances[0]));
    vec4 compare2 = vec4(greaterThan(pixelDepth, cascadeDistances[1]));
    float fIndex = dot(vec4(1,1,1,1), compare1) + dot(vec4(1,1,1,1), compare2);
    int index = int(fIndex);

    vec4 shadowCoords = shadowMatrix[index] * vPosition;
    shadowCoords.w = shadowCoords.z + shadow_bias;



    shadowCoords.z = fIndex;
    // Sample four times
    float lightDist = shadow2DArray(shadows, shadowCoords + pcfOffsets[0]).r;
    lightDist += shadow2DArray(shadows, shadowCoords + pcfOffsets[1]).r;
    lightDist += shadow2DArray(shadows, shadowCoords + pcfOffsets[2]).r;
    lightDist += shadow2DArray(shadows, shadowCoords + pcfOffsets[3]).r;
    lightDist /= 4.0;
    float shadowCo = (lightDist + 0.25)/1.25;

    vec4 dest = texture2D(tex, coords) * color;// * cascadeColors[index];
    dest.rgb *= shadowCo;
    float fogFactor = exp(-pow((fog_factor * gl_FogFragCoord), 2.0));
    gl_FragColor = mix(fog_color, dest, fogFactor);
}
