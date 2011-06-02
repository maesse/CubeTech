#version 120
//#extension GL_ARB_texture_rectangle : require
uniform sampler2DRect tex;

varying vec2 coords;
void main()
{
    gl_FragColor = texture2DRect(tex, coords);
}
