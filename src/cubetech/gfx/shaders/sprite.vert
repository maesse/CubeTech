#version 120
#define PACKEDCOORDS

in vec2 v_position;
#ifdef PACKEDCOORDS // coords packed into a single integer
in float v_coords;
#else
in vec2 v_coords;
#endif
in vec4 v_color;

uniform sampler2D tex;

varying out vec2 coords;
varying out vec4 color;

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