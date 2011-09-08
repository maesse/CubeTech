attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec4 v_color;

uniform mat4 Modell;
varying vec4 vColor;

void main()
{
    gl_Position =  gl_ModelViewProjectionMatrix * Modell * vec4( v_position, 1.0);
    //vColor = v_color;
}