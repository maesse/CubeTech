#version 150
attribute vec3 v_position;

uniform float near;
uniform float far;
uniform vec4 lightPosition;

out noperspective vec3 viewpos;
out noperspective vec2 texcoords;
out float depthin;
out vec3 viewlight; 

void main()
{
    vec4 viewposition = gl_ModelViewMatrix * vec4(v_position + lightPosition.xyz, 1.0);
    vec4 viewlightcalc = gl_ModelViewMatrix * lightPosition;

    gl_Position = gl_ProjectionMatrix * (viewposition);
    viewlight = viewlightcalc.xyz;
    viewpos = viewposition.xyz;
    depthin = ((gl_Position.z)-near) / (far-near);
    texcoords = gl_Position.xy / gl_Position.w;
    texcoords = texcoords * 0.5 + 0.5;
}