#version 150
#define MAXLIGHTS 32

#define BLURKERNEL 4

uniform sampler2D tex0; // color & specular power
uniform sampler2D tex1; // unused
uniform sampler2D tex2; // normal & depth
uniform sampler2D ssao; // ssao contribution

uniform vec2 pixelOffset;

struct LightInfo {
    vec4 position;
    vec3 attenuation;
};

layout(std140) uniform LightDataSrc {
    int nLights;
    LightInfo lights[MAXLIGHTS];
} LightData;


in vec2 texcoords;
in vec3 view; // texture -> view space

vec3 findclosest(vec3 a, vec3 b, vec3 c)
{
    float u = (c.x-a.x)*(b.x-a.x) + (c.y-a.y)*(b.y-a.y) + (c.z-a.z)*(b.z-a.z);
    float v = length(b-a);
    u /= v*v;
    vec3 result = a + u * (b-a);
    return result;
}

void main()
{
    vec4 vtex0 = texture2D(tex0, texcoords);
    vec4 vtex1 = texture2D(tex1, texcoords);
    vec4 vtex2 = texture2D(tex2, texcoords);

#ifdef BOXBLUR
    vec3 ssaos = vec3(0,0,0);

    for (int i=0; i<BLURKERNEL;i++) {
        for(int j=0; j<BLURKERNEL;j++) {
            ssaos += texture2D(ssao, texcoords + pixelOffset * vec2(i,j)).xyz;
        }
    }
    ssaos /= float(BLURKERNEL*BLURKERNEL);
#else
    vec3 ssaos = texture2D(ssao, texcoords).xyz;
    ssaos += texture2D(ssao, texcoords + pixelOffset * vec2(1.5, -0.5)).xyz;
    ssaos += texture2D(ssao, texcoords + pixelOffset * vec2(-0.5, -1.5)).xyz;
    ssaos += texture2D(ssao, texcoords + pixelOffset * vec2(-1.5, 0.5)).xyz;
    ssaos += texture2D(ssao, texcoords + pixelOffset * vec2(0.5, 1.5)).xyz;

    ssaos /= 5.0;
#endif
    
    vec3 albedo = vec3(vtex0.r, vtex0.g, vtex0.b);
    float specularFactor = vtex0.a;
    vec3 normal = vec3(vtex2.r, vtex2.g, vtex2.b);
    float depth = vtex2.w;

    vec3 position = vec3(view) * depth;

    vec3 color = vec3(0,0,0);
    
    for(int i=0; i<LightData.nLights; i++) {
        LightInfo info = LightData.lights[i];
        // Calculate eye -> light vector

        vec3 lightpos = info.position.xyz;
        
        vec3 aux = vec3(lightpos - position);
        vec3 lightDir = normalize(aux);
        // dot(N,L)
        float NdotL = max(dot(normal,lightDir),0.0);
        // color output
        
        if (NdotL > 0.0) {
            // Direct light
            float dist = length(aux);
            float att = 1.0 /   (info.attenuation.x +
                                info.attenuation.y * dist +
                                info.attenuation.z * dist * dist);
            color += att * (albedo * NdotL);

            // Specular
            vec3 halfVector = vec3(normalize(lightDir-normalize(position)));
            float NdotHV = max(dot(normal,halfVector),0.0);
            color += att * pow(NdotHV,specularFactor * 128.0) * 0.5 * albedo;
        }
    }

    

    // Debug: Eye -> World position
    //vec4 wpos = vec4(position.xy, position.z, 1.0);
    //wpos = invView * wpos;

    gl_FragColor.rgb = color *  ssaos;
    gl_FragColor.a = 1.0;
    //gl_FragColor.r = length((LightData.lights[0].position.xyz - position.xyz) / 100.0);

    // Specular test
    //gl_FragColor.rgb = specularFactor;
}
