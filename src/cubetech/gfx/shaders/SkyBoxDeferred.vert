#version 120

attribute vec3 v_position;
attribute vec2 v_coords;

varying vec2 coords;
varying vec4 glcoords;
void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * vec4(v_position, 1.0);
    glcoords = gl_Position;
    coords = v_coords;
}