uniform sampler2D tex;
varying float viewDepth;
void main()
{
    //gl_FragColor = gl_Color * texture2D(tex, gl_TexCoord[0].xy);
    gl_FragColor = vec4(viewDepth);
}