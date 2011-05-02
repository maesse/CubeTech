#version 120
//in vec3 v_position;
//in vec2 v_coords;
//in vec4 v_color;

attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec2 v_coords2;
attribute vec4 v_color;

varying vec2 coords;
varying vec4 color;
varying vec2 coords2;
void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * vec4(v_position, 1.0);
    coords = v_coords;
    coords2 = v_coords2;
    color = v_color;
}