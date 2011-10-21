#version 140
#ifdef GL_ARB_texture_rectangle
#extension GL_ARB_texture_rectangle : enable
#else
#extension GL_EXT_texture_rectangle : enable
#endif
uniform sampler2DRect tex;

varying vec2 coords;
void main()
{
    gl_FragColor = texture2DRect(tex, coords);
}
