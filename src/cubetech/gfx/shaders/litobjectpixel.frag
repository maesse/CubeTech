// Shadow mapping uses all this stuff
#extension GL_EXT_texture_array : enable
uniform sampler2DArrayShadow shadows;
uniform vec4 cascadeDistances;
uniform mat4 shadowMatrix[4];
uniform vec4 pcfOffsets[4];
uniform float shadow_bias = 0.002;
varying vec4 vPosition;
varying float vDepth;

uniform samplerCube envmap;
uniform sampler2D tex;

varying vec2 coords;
varying vec4 diffuse, ambient;
varying vec3 normal, lightDir, halfVector;
varying vec3 reflectDir;
varying vec4 col;

void main()
{
    // Shadow
    vec4 compare1 = vec4(greaterThan(vec4(vDepth), cascadeDistances));
    float fIndex = dot(vec4(1,1,1,1), compare1);
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

    // Start with ambient
    vec4 color = ambient * textureCube(envmap, reflectDir);

    vec4 texcol = texture2D(tex, coords);
    //vec4 texcol = vec4(0.8,0.8,0.8,1.0);

    // normalize the interpolated normal
    vec3 n = normalize(normal);

    // compute dot
    float NdotL = (max(dot(n, lightDir), 0.0)*0.75)+0.25;

    //vec4 lightColor = vec4(0,0,0,1);
    //if(NdotL > 0.0) {
      vec4 lightColor = diffuse * NdotL ;

        vec3 halfV = normalize(halfVector);
        float NdotHV = max(dot(n, halfV), 0.0);
        lightColor += texcol
                * pow(NdotHV, gl_FrontMaterial.shininess) * texcol.a * 5.0;
    //}
    lightColor.rgb *= lightDist;

    gl_FragColor.rgb = texcol.rgb * (color.rgb + lightColor.rgb ) * col.rgb;
    gl_FragColor.a = 1.0;
    //gl_FragColor.rgb *= lightDist;
    //gl_FragColor.rgb = vec3(vDepth/100.0);

    //gl_FragColor = textureCube(envmap, reflectDir);
}
