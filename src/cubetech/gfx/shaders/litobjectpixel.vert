attribute vec3 v_position;
attribute vec2 v_coords;
attribute vec4 v_color;

uniform mat4 Modell;
uniform vec3 lightDirection;

varying vec4 diffuse, ambient;
varying vec3 normal, lightDir, halfVector;
varying vec3 reflectDir;

// Shadow out
varying vec4 vPosition;
varying float vDepth;

vec3 getHalfVector(in vec4 pos, in vec3 lightDir)
{
    vec3 vertexPos = vec3(gl_ModelViewMatrix * pos);
    return normalize(normalize(vertexPos) + lightDir);
}

vec3 getLightDir()
{
    // vec3(gl_LightSource[0].position)
    return lightDirection;
}

void main()
{
    // Get vertex normal
    normal = normalize( vec3(Modell * vec4(gl_Normal,0.0)));
    reflectDir = normal;

    // Get light direction
    lightDir = getLightDir();

    // Get halfvector
    vec4 pos = vec4( v_position, 1.0);
    halfVector = getHalfVector(pos, lightDirection);

    // compute diffuse term
    diffuse = gl_LightSource[0].diffuse; // gl_FrontMaterial.diffuse
    ambient = gl_LightModel.ambient;
    gl_FrontColor = v_color;

    // shadow & position out
    gl_TexCoord[0].xy = v_coords;
    vPosition = Modell * pos;
    gl_Position =  gl_ModelViewProjectionMatrix * vPosition;
    vDepth = gl_Position.z;
}