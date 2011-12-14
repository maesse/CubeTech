#version 150
uniform sampler2D tex0; // color & specular power
uniform sampler2D tex1; // unused
uniform sampler2D tex2; // normal & depth

uniform vec4 lightPosition;


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
    if(depth < depthin) discard;

    vec3 albedo = vec3(vtex0.r, vtex0.g, vtex0.b);
    float specularFactor = vtex0.a;
    vec3 normal = vec3(vtex2.r, vtex2.g, vtex2.b);
    
    vec3 position = view * depth;
    
    
    // Calculate eye -> light vector
    float NdotL = max(dot(normal,lightPosition.xyz),0.0); // dot(N,L)
    vec3 color = NdotL * albedo;
    if (NdotL > 0.0) {
        vec3 halfV = normalize(normalize(-position)+normalize(lightPosition.xyz));
        float NdotHV = max(dot(normal, halfV), 0.0);
        color += pow(NdotHV, 32.0) * vtex1.rgb;
    }

    gl_FragColor.rgb = color;
    //gl_FragColor.rgb *= 0.0001;
    //gl_FragColor.rgb = abs(crypos - vtex1.rgb) * 0.001;
    //gl_FragColor.rgb = vec3(vtex2.rgb);
    gl_FragColor.a = 1.0;
    //gl_FragColor.r = length(position.z - viewlight.z) / 1000.0;
}
