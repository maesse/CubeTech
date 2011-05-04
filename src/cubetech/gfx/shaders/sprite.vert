#version 120
//in vec3 v_position;
//in vec2 v_coords;
//in vec4 v_color;

attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec4 v_color;

varying vec2 coords;
varying vec4 color;
uniform sampler2D tex;
void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * vec4(v_position, 1.0);
    coords = v_coords;
    color = v_color;
    gl_FogFragCoord = abs(gl_Position.z);
}