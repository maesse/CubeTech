#version 130
#extension GL_EXT_texture_array : enable
#define SHADOW_SAMPLES 0
uniform sampler2D tex0; // color & specular power
uniform sampler2D tex1; // unused
uniform sampler2D tex2; // normal & depth
uniform sampler2D ssao; // ambient occlusion
uniform sampler2D randomRot; // ambient occlusion

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


in vec2 texcoords;
in float depthin;
in vec3 viewpos;
in vec3 viewlight;
in vec3 view;

float getShadowFraction(float vDepth, vec4 vPosition, vec3 realpos)
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
    if(SHADOW_SAMPLES == 0 && index == 0) {
        float kernelScale = 0.002;
        vec2 texscale = vec2(mod(vPosition.x/10.0 + vPosition.z/10.0, 1.0),mod(vPosition.y/10.0 + vPosition.z/10.0, 1.0));
        vec2 rndTex = texture2D(randomRot, texscale).rg;
        vec4 rndRot = vec4(rndTex*kernelScale,0.0,0.0);
        lightDist = shadow2DArray(shadows, shadowCoords + vec4(-0.5040303, -0.7851074,0.0,0.0)*rndRot).r;
        lightDist += shadow2DArray(shadows, shadowCoords + vec4(0.1344586, -0.006443536,0.0,0.0)*rndRot).r;
        lightDist += shadow2DArray(shadows, shadowCoords + vec4(-0.7923665, 0.03883154,0.0,0.0)*rndRot).r;
        lightDist += shadow2DArray(shadows, shadowCoords + vec4(0.1178484, -0.8978863,0.0,0.0)*rndRot).r;
        lightDist += shadow2DArray(shadows, shadowCoords + vec4(-0.4307679, 0.5938465,0.0,0.0)*rndRot).r;
        lightDist += shadow2DArray(shadows, shadowCoords + vec4(0.5729364, 0.5509189,0.0,0.0)*rndRot).r;
        lightDist += shadow2DArray(shadows, shadowCoords + vec4(0.7030579, -0.3134302,0.0,0.0)*rndRot).r;
        lightDist /= 7.0;
        //lightDist = rndTex.r;
    }
    else if(SHADOW_SAMPLES <= 1) {
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
    float ssaoFactor = texture2D(ssao, texcoords).r;
    float depth = vtex2.w;
    if(depth < depthin) discard;

    vec3 albedo = vec3(vtex0.r, vtex0.g, vtex0.b);
    float specularFactor = vtex0.a;
    vec3 normal = normalize(vec3(vtex2.r, vtex2.g, vtex2.b));
    
    vec3 position = view * depth;

    float shadow = getShadowFraction((projectionMatrix * vec4(position, 1.0)).z, invModelView * vec4(position.xyz, 1.0), position);
    
    // Calculate eye -> light vector
    float NdotL = max(dot(normal,lightPosition.xyz),0.0); // dot(N,L)
    vec3 color = NdotL * albedo * lightDiffuse;
    vec3 halfV = normalize(normalize(-position)+normalize(lightPosition.xyz));
    float NdotHV = max(dot(normal, halfV), 0.0);
    if (NdotL > 0.0) {
        //  
        color += pow(NdotHV, 40.0) * vtex1.rgb * 4.0 * lightSpecular * shadow;
    }

    

    float litfac = 0.7;
    float litssao = (1.0 - litfac) + ssaoFactor * litfac;
    //color = vec3(0.5,0.5,0.5);

    ssaoFactor = 1.0 - ssaoFactor;
    ssaoFactor = ssaoFactor * (1.0 - shadow * litfac);
    ssaoFactor = 1.0 - ssaoFactor;
    
    gl_FragColor.rgb = color *  (shadow * shadow_factor + (1.0 - shadow_factor)) * (ssaoFactor);
    //gl_FragColor.rgb *= 0.0001;
    //gl_FragColor.rgb = abs(crypos - vtex1.rgb) * 0.001;
    // Depth debug:
    //gl_FragColor.rgb = vec3(depth);
    // Normal debug:
    //gl_FragColor.rgb = abs(normal);
    // ssao debug:
    //gl_FragColor.rgb = vec3(ssaoFrac);
    // specular debug:
    //gl_FragColor.rgb = vec3(vtex1.rgb);
    // dot debug:
    //gl_FragColor.rgb = vec3(pow(NdotHV, 30.0));
    gl_FragColor.a = 1.0;
    //gl_FragColor.r = length(position.z - viewlight.z) / 1000.0;
}
