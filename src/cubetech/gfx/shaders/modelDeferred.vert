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
out mat3 invTan;
out float scaledDepth;
out vec2 coords;

void main(void)
{
    vec4 mpos = ModelView * vposition;
    gl_Position = Projection * mpos;
    coords = vcoords;

    vec3 mnormal = normalize( vec3( vec4(vnormal, 0)));
    vec3 mtangent = normalize(vec3( vec4(vtangent.xyz, 0)));
    mat3 normalmatrix = transpose(inverse(mat3(ModelView)));
    normal = normalmatrix * mnormal;
    vec3 tangent = normalmatrix * (mtangent); // tangent not used, just here as an example
    vec3 bitangent = cross(normal, tangent) * vtangent.w; // bitangent not used, just here as an example

    invTan = (mat3(tangent, bitangent, normal));
    scaledDepth = -mpos.z / far;
}