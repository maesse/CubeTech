#version 120

attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec4 v_color;

varying vec2 coords;
varying vec4 color;

// Shadow out
varying vec4 vPosition;
varying float vDepth;

void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * vec4(v_position, 1.0);
    coords = v_coords;
    color = v_color;
    gl_FogFragCoord = abs(gl_Position.z);

    // shadow out
    vPosition = vec4(v_position, 1.0);
    vDepth = (gl_ModelViewProjectionMatrix * vec4(v_position, 1.0)).z;    
}