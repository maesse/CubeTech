uniform sampler2D tex;
uniform sampler2D normalmap;
uniform sampler2D specularmap;

varying vec3 normal;
varying mat3 invTan;
varying float scaledDepth;

void main()
{
    // Start with ambient
    vec3 mnormal = normalize(2.0 * texture2D(normalmap, gl_TexCoord[0].st).rgb - 1.0);
    vec4 texcol = texture2D(tex, gl_TexCoord[0].xy);
    vec3 specularcolor = texture2D(specularmap, gl_TexCoord[0].xy).rgb;
    //mnormal = vec3(0,0,1);
    gl_FragData[0].rgb = texcol.rgb;
    gl_FragData[0].a = texcol.a;
    //gl_FragData[1] = vec4(specularcolor, 0.0);
    gl_FragData[1] = vec4(0.0);
    gl_FragData[2] = vec4(invTan * mnormal, scaledDepth);
}