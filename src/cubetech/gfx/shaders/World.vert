#version 120

attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec4 v_color;

uniform sampler2D tex;

varying vec2 coords;
varying vec4 color;

void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * vec4(v_position, 1.0);
    coords = v_coords;
    color = v_color;
}