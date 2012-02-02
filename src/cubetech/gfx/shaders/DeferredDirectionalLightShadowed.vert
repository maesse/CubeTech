#version 130
attribute vec3 v_position;
attribute vec3 v_view;
attribute vec2 v_coords;

uniform float near;
uniform float far;
uniform vec4 lightPosition;
uniform mat4 invProjectionMatrix;

out vec3 viewpos;
out vec2 texcoords;
out float depthin;
out vec3 viewlight; 
out vec3 view;

void main()
{
    vec4 viewposition = gl_ModelViewMatrix * vec4(v_position, 1.0);
    vec4 viewlightcalc = gl_ModelViewMatrix * lightPosition;

    gl_Position = gl_ProjectionMatrix * (viewposition);
    viewlight = viewlightcalc.xyz;
    viewpos = viewposition.xyz;
    depthin = ((gl_Position.z)-near) / (far-near);
    texcoords = gl_Position.xy / gl_Position.w;
    texcoords = texcoords * 0.5 + 0.5;
    texcoords = v_coords;
    view = v_view;
}