#version 150
attribute vec3 v_position;
out noperspective vec2 texcoords;

void main()
{
    vec4 position = vec4(v_position, 1.0);
    gl_Position = gl_ModelViewProjectionMatrix * position;
    texcoords = gl_Position.xy / gl_Position.w;
    texcoords = texcoords * 0.5 + 0.5;
}