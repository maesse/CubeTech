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
out vec4 diffuse;
out vec3 normal, lightDir, halfVector;
out vec3 reflectDir;
out vec3 bonedebug;

// Shadow out
out vec4 vPosition;
out float vDepth;
out vec2 coords;
out vec3 lightVec, halfVec, eyeVec;
out mat3 invTan;
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

    vec3 mnormal = normalize( vec3(Modell * vec4(vnormal * madjtrans, 0)));
    vec3 mtangent = normalize(vec3(Modell * vec4(vtangent.xyz * madjtrans, 0)));
    reflectDir = mnormal;

    mat3 normalMatrix = transpose(inverse(mat3(ModelView)));

    normal = normalMatrix * mnormal;
    vec3 tangent = normalMatrix * (mtangent); // tangent not used, just here as an example
    vec3 bitangent = cross(normal, tangent) * vtangent.w; // bitangent not used, just here as an example

    lightDir = vec3( (ModelView) * vec4(-lightDirection, 0.0));
    //lightDir = vec3(0,0,1);
    diffuse = vec4(1,1,1,1); // gl_FrontMaterial.diffuse
    
    invTan = transpose(mat3(tangent, bitangent, normal));
    vec3 v;
    
    v.x = dot(lightDir, tangent);
    v.y = dot(lightDir, bitangent);
    v.z = dot(lightDir, normal);
    lightVec = normalize(v);
    
    vec3 vertexposition = vec3(ModelView * mpos);
    v.x = dot(vertexposition, tangent);
    v.y = dot(vertexposition, bitangent);
    v.z = dot(vertexposition, normal);
    eyeVec = normalize(v);
    

    // Get halfvector

    //halfVector = normalize( normalize(vertexposition)+normalize(lightDir));
    //v.x = dot(halfVector, tangent);
    //v.y = dot(halfVector, bitangent);
    //v.z = dot(halfVector, normal);
    
    halfVec = normalize(normalize(-eyeVec)+lightVec );

    // shadow out
    vPosition = mpos;
    vDepth = gl_Position.z;
    gl_FrontColor = vec4(1,1,1,1);
}