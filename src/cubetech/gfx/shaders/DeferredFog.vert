#version 130
attribute vec3 v_position;
attribute vec2 v_coords;
out vec2 texcoords;

void main()
{
    vec4 position = vec4(v_position, 1.0);
    gl_Position = gl_ModelViewProjectionMatrix * position;
    texcoords = v_coords;
}