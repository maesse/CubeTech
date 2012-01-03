#version 130
uniform sampler2D tex2; // normal & depth

uniform mat4 viewmatrix;

in vec2 texcoords;

void main()
{
    vec4 vtex2 = texture2D(tex2, texcoords);
    if(vtex2.w >= 1.0) discard;

    vec3 fogColor = vec3( (95/255.0),(87/255.0),(67/255.0)); 
    float fogFactor = max((vtex2.w) - 0.1, 0.0) / 2.0;
    gl_FragColor.rgb = fogColor;
    gl_FragColor.a = fogFactor;
}
