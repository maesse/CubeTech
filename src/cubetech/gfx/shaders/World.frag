#version 120
uniform sampler2D tex;

varying vec2 coords;
varying float viewDepth;
void main()
{
    //gl_FragColor = texture2D(tex, coords) * color;
    gl_FragColor = vec4(viewDepth);
    //gl_FragColor = vec4(127.0);
}
