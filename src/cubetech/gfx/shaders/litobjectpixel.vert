
attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec4 v_color;

uniform mat4 Modell;
uniform vec3 lightDirection;

varying vec2 coords;
varying vec4 diffuse, ambient;
varying vec3 normal, lightDir, halfVector;
varying vec3 reflectDir;
varying vec4 col;

// Shadow out
varying vec4 vPosition;
varying float vDepth;

void main()
{
    // Get vertex normal
    normal = normalize( vec3(Modell * vec4(gl_Normal,0.0)));
    reflectDir = normal;

    // Get light direction
    // vec3(gl_LightSource[0].position)
    lightDir = lightDirection;

    // Get halfvector
    vec3 vertexPos = vec3(gl_ModelViewMatrix * vec4( v_position, 1.0));
    //halfVector = normalize(gl_LightSource[0].halfVector.xyz);
    halfVector = normalize(normalize(vertexPos) + lightDirection);

    // compute diffuse term
    diffuse = gl_LightSource[0].diffuse; // gl_FrontMaterial.diffuse
    ambient = gl_LightModel.ambient;
    col = v_color;

    gl_Position =  gl_ModelViewProjectionMatrix * Modell *  vec4( v_position, 1.0);
    coords = v_coords;

    // shadow out
    vPosition = Modell * vec4( v_position, 1.0);
    vDepth = (gl_ModelViewProjectionMatrix * Modell *  vec4(  v_position, 1.0)).z;
}