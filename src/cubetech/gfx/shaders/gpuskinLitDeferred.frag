uniform sampler2D tex;
uniform sampler2D normalmap;
uniform sampler2D specularmap;

in vec3 normal;
in mat3 invTan;
in float scaledDepth;
in vec2 coords;

void main()
{
    // Start with ambient
    vec3 mnormal = normalize(2.0 * texture2D(normalmap, coords).rgb - 1.0);
    vec4 texcol = texture2D(tex, coords);
    vec4 specularcolor = texture2D(specularmap, coords);
    //mnormal = vec3(0,0,1);
    gl_FragData[0] = texcol;
    gl_FragData[1] = specularcolor;
    gl_FragData[2] = vec4(invTan * mnormal, scaledDepth);
}