#version 130
#define PACKEDCOORDS

attribute vec2 v_position;
#ifdef PACKEDCOORDS // coords packed into a single integer
attribute float v_coords;
#else
attribute vec2 v_coords;
#endif
attribute vec4 v_color;

uniform sampler2D tex;

varying vec2 coords;
varying vec4 color;

void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * vec4(v_position, 0.0, 1.0);
#ifdef PACKEDCOORDS
    float t = mod(v_coords,1.0f);
    float s = mod(v_coords * 4096.0,1.0f);
    coords = vec2(s,t);
#else
    coords = v_coords;
#endif
    color = v_color;
}