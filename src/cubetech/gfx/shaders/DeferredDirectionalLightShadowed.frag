#version 150
#extension GL_EXT_texture_array : enable
#define SHADOW_SAMPLES 4
uniform sampler2D tex0; // color & specular power
uniform sampler2D tex1; // unused
uniform sampler2D tex2; // normal & depth

// Shadow
uniform sampler2DArrayShadow shadows;
uniform float shadow_bias = 0.002;
uniform float shadow_factor = 0.75;
uniform vec4 cascadeDistances;
uniform mat4 shadowMatrix[4];
uniform vec4 pcfOffsets[4];
uniform mat4 projectionMatrix;

uniform vec4 lightPosition;
uniform mat4 invModelView;
uniform vec3 lightDiffuse;
uniform vec3 lightSpecular;


in noperspective vec2 texcoords;
in float depthin;
in noperspective vec3 viewpos;
in vec3 viewlight;
in vec3 view;

float getShadowFraction(float vDepth, vec4 vPosition)
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
    vec4 vtex0 = texture2D(tex0, texcoords);
    vec4 vtex1 = texture2D(tex1, texcoords);
    vec4 vtex2 = texture2D(tex2, texcoords);
    float depth = vtex2.w;
    if(depth < depthin) discard;

    vec3 albedo = vec3(vtex0.r, vtex0.g, vtex0.b);
    float specularFactor = vtex0.a;
    vec3 normal = vec3(vtex2.r, vtex2.g, vtex2.b);
    
    vec3 position = view * depth;
    
    // Calculate eye -> light vector
    float NdotL = max(dot(normal,lightPosition.xyz),0.0); // dot(N,L)
    vec3 color = NdotL * albedo * lightDiffuse;
    if (NdotL > 0.0) {
        vec3 halfV = normalize(normalize(-position)+normalize(lightPosition.xyz));
        float NdotHV = max(dot(normal, halfV), 0.0);
        color += pow(NdotHV, 32.0) * vtex1.rgb * lightSpecular;
    }

    float shadow = getShadowFraction((projectionMatrix * vec4(position, 1.0)).z, invModelView * vec4(position.xyz, 1.0));
    
    gl_FragColor.rgb = color * (shadow * shadow_factor + (1.0 - shadow_factor));
    //gl_FragColor.rgb *= 0.0001;
    //gl_FragColor.rgb = abs(crypos - vtex1.rgb) * 0.001;
    //gl_FragColor.rgb = vec3(depth);
    gl_FragColor.a = 1.0;
    //gl_FragColor.r = length(position.z - viewlight.z) / 1000.0;
}
