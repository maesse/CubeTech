#version 120
uniform sampler2D tex;

varying vec2 coords;
varying vec4 color;

void main()
{
    vec4 dest = texture2D(tex, coords);
    gl_FragColor = dest * color;
}
