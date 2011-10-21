
attribute vec3 v_position;
attribute vec3 v_normal;
attribute vec2 v_coords;

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

void setLightVars(in vec3 in_normal,in vec3 vertexPos)
{
    normal = in_normal;

    lightDir = vec3(gl_LightSource[0].position);
    diffuse = gl_LightSource[0].diffuse; // gl_FrontMaterial.diffuse
    ambient = gl_LightModel.ambient;

    // Get halfvector
#ifdef PHONG
    halfVector = normalize( vertexPos);
#else
    halfVector = normalize( normalize(lightDir)-normalize(vertexPos));
#endif
}

void main()
{
    // Get vertex normal
    normal = normalize( vec3(Modell * vec4(v_normal,0.0)));
    reflectDir = normal;

    setLightVars(gl_NormalMatrix * normal, vec3(gl_ModelViewMatrix * (Modell * vec4( v_position, 1.0))));

    gl_Position =  gl_ModelViewProjectionMatrix * Modell *  vec4( v_position, 1.0);
    coords = v_coords;
    col = vec4(1,1,1,1);

    // shadow out
    vPosition = Modell * vec4( v_position, 1.0);
    vDepth = (gl_ModelViewProjectionMatrix * Modell *  vec4(  v_position, 1.0)).z;
}