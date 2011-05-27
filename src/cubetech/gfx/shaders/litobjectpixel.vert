#version 120
attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec4 v_color;

uniform mat3 Modell;

varying vec2 coords;
varying vec4 diffuse, ambient;
varying vec3 normal, lightDir, halfVector;
varying vec3 reflectDir;

void main()
{
    // Get vertex normal
    normal = normalize( Modell * gl_Normal);
    reflectDir = Modell * gl_Normal;

    // Get light direction
    // vec3(gl_LightSource[0].position)
    lightDir = normalize( vec3(-0.8, 0.8, 1.0) );

    // Get halfvector
    halfVector = normalize(gl_LightSource[0].halfVector.xyz);

    // compute diffuse term
    diffuse = gl_LightSource[0].diffuse; // gl_FrontMaterial.diffuse
    ambient = gl_LightModel.ambient;

    gl_Position =  gl_ModelViewProjectionMatrix * vec4(Modell * v_position, 1.0);
    coords = v_coords;
}