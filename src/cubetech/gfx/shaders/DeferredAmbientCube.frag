#version 130
uniform sampler2D tex0; // color & specular power
uniform sampler2D tex2; // normal & depth
uniform samplerCube envmap; // normal & depth
uniform sampler2D ssao; // ssao

uniform mat4 viewmatrix;
uniform float ambientFactor = 1.0;

in vec2 texcoords;

void main()
{
    vec4 vtex0 = texture2D(tex0, texcoords);
    vec4 vtex2 = texture2D(tex2, texcoords);
    float ssaoFactor = texture2D(ssao, texcoords).r;
    if(vtex2.w >= 1.0) discard;

    vec3 albedo = vec3(vtex0.r, vtex0.g, vtex0.b);
    vec3 normal = vec3(vtex2.r, vtex2.g, vtex2.b);

    vec3 cubeTex = vec3(textureCube(envmap, mat3(viewmatrix) * normal).rgb) ;
    
    gl_FragColor.rgb = vec3(ambientFactor * ssaoFactor * cubeTex * vtex0.rgb);
    gl_FragColor.a = 1.0;
}
