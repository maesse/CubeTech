#version 150
uniform sampler2D tex0; // color & specular power
uniform sampler2D tex1; // unused
uniform sampler2D tex2; // normal & depth

uniform vec4 lightPosition;
uniform vec4 lightAttenuation;
uniform vec3 viewBL;
uniform vec3 viewBR;
uniform vec3 viewTL;
uniform vec3 viewTR;

uniform float near;
uniform float far;

in noperspective vec2 texcoords;
in float depthin;
in noperspective vec3 viewpos;
in vec3 viewlight;

void main()
{
    vec4 vtex0 = texture2D(tex0, texcoords);
    vec4 vtex2 = texture2D(tex2, texcoords);
    float depth = vtex2.w;
    if(depth < depthin) discard;

    vec3 albedo = vec3(vtex0.r, vtex0.g, vtex0.b);
    float specularFactor = vtex0.a;
    vec3 normal = vec3(vtex2.r, vtex2.g, vtex2.b);
    
    vec3 position = vec3(viewpos.xy, depth * (far-near));
    vec3 color = vec3(0,0,0);

    
    
    // Calculate eye -> light vector
    vec3 aux = vec3(viewlight - position);
    vec3 lightDir = normalize(aux);
    float NdotL = max(dot(normal,lightDir),0.0); // dot(N,L)

    if (NdotL > 0.0) {
        // Direct light
        float dist = length(aux);
        float att = 1.0 /   (lightAttenuation.x +
                            lightAttenuation.y * dist +
                            lightAttenuation.z * dist * dist);
        color += att * (albedo * NdotL);

        // Specular
        vec3 halfVector = vec3(normalize(lightDir-normalize(position)));
        float NdotHV = max(dot(normal,halfVector),0.0);
        color += att * pow(NdotHV,specularFactor * 128.0) * 0.5 * albedo;
    }

    gl_FragColor.rgb = color;
    //gl_FragColor.rgb = abs(viewpos) * 0.0001;
    gl_FragColor.a = 1.0;
    gl_FragColor.r = length(position.z - viewlight.z) / 1000.0;
}
