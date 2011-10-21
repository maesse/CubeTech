attribute vec3 v_position;
uniform mat4 Modell;

void main()
{
    gl_Position =  gl_ModelViewProjectionMatrix * Modell * vec4( v_position, 1.0);
}