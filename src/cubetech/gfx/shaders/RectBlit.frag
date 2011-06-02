#version 140
#extension GL_EXT_texture_rectangle : enable
#extension GL_ARB_texture_rectangle : enable
uniform sampler2DRect tex;

varying vec2 coords;
void main()
{
    gl_FragColor = texture2DRect(tex, coords);
}
