#version 120
attribute vec3 v_position;
attribute vec2 v_coords;
//attribute vec3 v_normal;
attribute vec4 v_color;

varying vec2 coords;
varying vec4 color;

void main()
{
    // Get vertex normal
    vec3 normal = normalize(gl_NormalMatrix * gl_Normal);

    // Get light direction
    vec3 lightDirection = normalize(vec3(gl_LightSource[0].position));

    // get angle
    float NdotL = max(dot(normal, lightDirection), 0.0);

    // compute diffuse term
    vec4 diffuse = gl_LightSource[0].diffuse; // gl_FrontMaterial.diffuse
    color = NdotL * diffuse + gl_LightModel.ambient;
    color.a = 1.0;
    

    gl_Position = gl_ModelViewProjectionMatrix * vec4(v_position, 1.0);
    coords = v_coords;
}