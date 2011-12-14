#version 120
attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec3 v_view;

varying vec3 view;
varying vec2 texcoord;

void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * vec4(v_position, 1.0);
    texcoord = v_coords;
    view = v_view;
}