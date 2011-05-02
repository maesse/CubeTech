in vec3 v_position;
in vec2 v_coords;
in vec4 v_color;

out vec2 coords;
out vec4 color;
varying vec3 normal, lightDir, halfVector;
varying vec4 diffuse, ambientGlobal, ambient;
varying vec3 v;
varying float dist;
void main()
{
    normal = normalize(gl_NormalMatrix * gl_Normal);

    vec4 ecPos = vec4(gl_LightSource[0].position - vec4(v_position,1.0));

    //vec4 ecPos = vec4(gl_ModelViewMatrix * vec4(v_position,1.0));
    //vec4 derp = vec4(gl_ModelViewMatrix * gl_LightSource[0].position);
    vec3 aux = vec4(gl_ModelViewMatrix * ecPos).xyz;
    v = vec3(ecPos.xyz);
    lightDir = normalize(aux);
    dist = float(length(aux));

    //halfVector = normalize(gl_LightSource[0].halfVector.xyz);
    halfVector = normalize(normalize(gl_LightSource[0].position.xyz) + vec3(0,0,1));
    diffuse = gl_LightSource[0].diffuse;

    ambient = gl_LightSource[0].ambient;
    ambientGlobal = gl_LightModel.ambient;

    gl_Position = gl_ModelViewProjectionMatrix * vec4(v_position, 1.0);
    coords = v_coords;
    color = v_color;
}