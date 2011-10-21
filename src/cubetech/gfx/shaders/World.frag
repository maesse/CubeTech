#version 120
uniform sampler2D tex;

varying vec2 coords;
varying vec4 color;
void main()
{
    gl_FragColor = texture2D(tex, coords) * color;
    //gl_FragColor.rgb = vec3(coords.y, 0.0, 0.0);
}
