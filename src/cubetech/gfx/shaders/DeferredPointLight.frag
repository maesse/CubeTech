#version 130
uniform sampler2D tex0; // color & specular power
uniform sampler2D tex1; // unused
uniform sampler2D tex2; // normal & depth

uniform vec4 lightPosition;
uniform vec4 attenuation;


in vec2 texcoords;
in float depthin;
in vec3 viewpos;
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

    gl_FragColor.rgb = color;
    //gl_FragColor.rgb *= 0.0001;
    //gl_FragColor.rgb = abs(crypos - vtex1.rgb) * 0.001;
    //gl_FragColor.rgb = vec3(att);
    gl_FragColor.a = 1.0;
    //gl_FragColor.r = length(position.z - viewlight.z) / 1000.0;
}
