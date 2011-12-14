#version 120

attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec4 v_color;
attribute vec2 v_normal;

uniform sampler2D tex;

uniform float near;
uniform float far;

varying vec2 coords;
varying vec4 color;
varying vec3 normal;
varying float depth;
varying vec4 position;

vec3 decode (vec4 enc, vec3 view)
{
    vec4 nn = enc*vec4(2,2,0,0) + vec4(-1,-1,1,-1);
    float l = dot(nn.xyz,-nn.xyw);
    nn.z = l;
    nn.xy *= sqrt(l);
    return nn.xyz * 2 + vec3(0,0,-1);
}

vec3 decodeSphereMap(vec2 enc, vec3 view)
{
    vec2 fenc = enc * 4.0 - 2.0;
    float f = dot(fenc, fenc);
    float g = sqrt(1-f/4.0);
    vec3 n = vec3(fenc * g, 1-f/2.0);
    return n;
}
void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * vec4(v_position, 1.0);
    coords = v_coords;
    color = v_color;
    vec4 vnormal = vec4(v_normal.x, v_normal.y, .0, .0);
    normal = gl_NormalMatrix * decodeSphereMap(vnormal.xy, normal);
    depth = gl_Position.z;
    //depth = gl_Position.z  / gl_Position.w;
    //depth = ((depth)-near) / (far-near);
    
    position = gl_ModelViewMatrix * vec4(v_position, 1.0);
    depth = position.z;
}
