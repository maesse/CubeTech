#version 150
uniform mat4 ModelView;
uniform mat4 Projection;

in vec4 vposition;
in vec2 vcoords;

out vec2 coords;

void main(void)
{
    vec4 mpos = ModelView * vposition;
    gl_Position = Projection * mpos;
    coords = vcoords;
}