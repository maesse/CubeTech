#version 150
uniform mat4 ModelView;
uniform mat4 Projection;
uniform vec3 lightDirection;
uniform float near;
uniform float far;

in vec4 vposition;
in vec2 vcoords;
in vec3 vnormal;
in vec4 vtangent;

// Light out
out vec3 normal;
out float scaledDepth;
out vec2 coords;

void main(void)
{
    vec4 mpos = ModelView * vposition;
    gl_Position = Projection * mpos;
    coords = vcoords;

    mat3 normalmatrix = mat3(ModelView);
    normal = normalmatrix * vnormal;

    scaledDepth = -mpos.z / far;
}