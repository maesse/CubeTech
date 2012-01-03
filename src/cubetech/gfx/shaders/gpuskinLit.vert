#version 150
layout(std140) uniform animdata
{
   uniform vec4 bonemats[130*3];
};
uniform mat4 ModelView;
uniform mat4 ModelViewProjection;
uniform mat4 Modell;
uniform vec3 lightDirection;


in vec4 vposition;
in vec2 vcoords;
in vec3 vnormal;
in vec4 vweights;
in vec4 vbones;
in vec4 vtangent;

// Light out
out vec4 diffuse, ambient;
out vec3 normal, lightDir, halfVector;
out vec3 reflectDir;
out vec2 coords;


void main(void)
{
   ivec4 offsets = ivec4(vbones)*3;
   mat3x4 m = mat3x4(bonemats[offsets.x],bonemats[offsets.x+1],bonemats[offsets.x+2]) * vweights.x;
   m += mat3x4(bonemats[offsets.y],bonemats[offsets.y+1],bonemats[offsets.y+2]) * vweights.y;
   m += mat3x4(bonemats[offsets.z],bonemats[offsets.z+1],bonemats[offsets.z+2]) * vweights.z;
   m += mat3x4(bonemats[offsets.w],bonemats[offsets.w+1],bonemats[offsets.w+2]) * vweights.w;
   vec4 mpos = Modell * vec4(vposition * m, vposition.w);

   gl_Position = ModelViewProjection * mpos;
   coords = vcoords;
   
    // Light out
    mat3 madjtrans = mat3(cross(m[1].xyz, m[2].xyz), cross(m[2].xyz, m[0].xyz), cross(m[0].xyz, m[1].xyz));
    vec3 mnormal = vec3(Modell * vec4(vnormal * madjtrans,0.0));
    reflectDir = mnormal;

    vec3 vertexPos = vec3(ModelView * mpos);
    mat3 normalmatrix = transpose(inverse(mat3(ModelView)));
    vec3 lightnormal = normalize( normalmatrix * mnormal);
    lightDir = lightDirection;
    diffuse = vec4(1,1,1,1);
    ambient = vec4(0,0,0,0);
    halfVector = normalize( normalize(lightDir)-normalize(vertexPos));
}

