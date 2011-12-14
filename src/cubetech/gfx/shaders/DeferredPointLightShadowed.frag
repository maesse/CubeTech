#version 150
uniform sampler2D tex0; // color & specular power
uniform sampler2D tex1; // unused
uniform sampler2D tex2; // normal & depth

uniform samplerCube shadows;
uniform float shadow_bias = 0.002;
uniform float shadow_factor = 0.75;
uniform mat4 projectionMatrix;
uniform mat4 invModelView;

uniform vec4 lightPosition;
uniform vec4 attenuation;


in noperspective vec2 texcoords;
in float depthin;
in noperspective vec3 viewpos;
in vec3 viewlight;
in vec3 view;


void main()
{
    vec4 vtex0 = texture2D(tex0, texcoords);
    vec4 vtex1 = texture2D(tex1, texcoords);
    vec4 vtex2 = texture2D(tex2, texcoords);
    float depth = vtex2.w;
    //if(depth < depthin) discard;

    vec3 albedo = vec3(vtex0.r, vtex0.g, vtex0.b);
    float specularFactor = vtex0.a;
    vec3 normal = vec3(vtex2.r, vtex2.g, vtex2.b);
    
    vec3 position = view * depth;
    
    
    // Calculate eye -> light vector
    vec3 vecToLight = vec3(lightPosition.xyz - position.xyz);
    vec3 lightdir = normalize(vecToLight);
    float NdotL = max(dot(normal,lightdir),0.0); // dot(N,L)
    if(NdotL < 0.0) discard;
    float dist = length(vecToLight);
    
    float att = 1.0 / (attenuation.x + attenuation.y * dist + attenuation.z * dist * dist);
    
    vec3 halfV = normalize(normalize(-position)+normalize(vecToLight));
    float NdotHV = max(dot(normal, halfV), 0.0);
    vec3 spec = pow(NdotHV, 32.0) * vtex1.rgb;
    vec3 color = (NdotL * albedo + spec) * att;

    vec3 worldSpacePosition = vec3(invModelView * vec4(position, 1.0));
    vec3 worldSpaceLight = vec3(invModelView * vec4(lightPosition.xyz, 1.0));
    //worldSpaceLight = vec3(-100,500,200);

    vec4 worldLightToPos = vec4(worldSpacePosition - worldSpaceLight, 1.0);
    vec3 worldLightDir = normalize(vec3( worldLightToPos ));
    float shadowdepth = textureCube(shadows, worldLightDir).a ;
    
    float surfaceToLightDepth = length(worldLightToPos.xyz);
    float finalshadowdepth = surfaceToLightDepth + 0.002  > shadowdepth  ? 0.0 : 1.0;
    if(shadowdepth < 1.0) finalshadowdepth = 1.0;
    gl_FragColor.rgb = color * (finalshadowdepth);

    //gl_FragColor.rgb *= 0.0001;
    //gl_FragColor.rgb = vec3( -shadowdepth / 2.0);
    //gl_FragColor.rgb = vec3(surfaceToLightDepth - 200.0) / 100.0;
    //gl_FragColor.rgb = vec3( surfaceToLightDepth-shadowdepth) / 100.0;
    //gl_FragColor.rgb = vec3(shadowdepth - 200) / 100.0;
    //gl_FragColor.rgb = abs(worldSpacePosition - worldSpaceLight) / 100.0;

    //gl_FragColor.rgb += vec3(shadowdepth) / 10.0;
    gl_FragColor.a = 1.0;
    //gl_FragColor.r = length(position.z - viewlight.z) / 1000.0;
}
