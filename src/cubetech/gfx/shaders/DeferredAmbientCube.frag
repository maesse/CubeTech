#version 150
uniform sampler2D tex0; // color & specular power
uniform sampler2D tex1; // unused
uniform sampler2D tex2; // normal & depth
uniform samplerCube envmap; // normal & depth

uniform mat4 viewmatrix;
uniform float ambientFactor = 1.0;

in noperspective vec2 texcoords;

void main()
{
    vec4 vtex0 = texture2D(tex0, texcoords);
    vec4 vtex2 = texture2D(tex2, texcoords);

    vec3 albedo = vec3(vtex0.r, vtex0.g, vtex0.b);
    vec3 normal = vec3(vtex2.r, vtex2.g, vtex2.b);
    
    gl_FragColor.rgb =  ambientFactor  * textureCube(envmap, mat3(viewmatrix) * normal).rgb * albedo;
    gl_FragColor.a = 1.0;
}
