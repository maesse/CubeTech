#version 120
attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec4 v_color;

varying vec2 coords;
varying vec4 color;
varying vec4 pos;

void main()
{
    pos = gl_ModelViewProjectionMatrix * vec4(v_position, 1.0);
    gl_Position = pos;
    coords = v_coords;
    color = v_color;
}