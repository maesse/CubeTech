#version 140
layout(std140) uniform animdata
{
   uniform vec4 bonemats[100*3];
};
uniform mat4 Modell;
uniform vec3 lightDirection;

uniform float near;
uniform float far;

attribute vec4 vweights;
attribute vec4 vbones;
attribute vec4 vtangent;

// Light out
varying vec3 normal;
varying mat3 invTan;
varying float scaledDepth;

void main(void)
{
   ivec4 offsets = ivec4(vbones)*3;
   mat3x4 m = mat3x4(bonemats[offsets.x],bonemats[offsets.x+1],bonemats[offsets.x+2]) * vweights.x;
   m += mat3x4(bonemats[offsets.y],bonemats[offsets.y+1],bonemats[offsets.y+2]) * vweights.y;
   m += mat3x4(bonemats[offsets.z],bonemats[offsets.z+1],bonemats[offsets.z+2]) * vweights.z;
   m += mat3x4(bonemats[offsets.w],bonemats[offsets.w+1],bonemats[offsets.w+2]) * vweights.w;
   vec4 mpos = Modell * vec4(gl_Vertex * m, gl_Vertex.w);
   
   gl_Position = gl_ModelViewProjectionMatrix * mpos;
   gl_TexCoord[0] = gl_MultiTexCoord0;

    // Light out
    mat3 madjtrans = mat3(cross(m[1].xyz, m[2].xyz), cross(m[2].xyz, m[0].xyz), cross(m[0].xyz, m[1].xyz));

    vec3 mnormal = normalize( vec3(Modell * vec4(gl_Normal * madjtrans, 0)));
    vec3 mtangent = normalize(vec3(Modell * vec4(vtangent.xyz * madjtrans, 0)));

    normal = gl_NormalMatrix * mnormal;
    vec3 tangent = gl_NormalMatrix * (mtangent); // tangent not used, just here as an example
    vec3 bitangent = cross(normal, tangent) * vtangent.w; // bitangent not used, just here as an example

    invTan = (mat3(tangent, bitangent, normal));
    scaledDepth = -(gl_ModelViewMatrix * mpos).z / far;(-gl_Position.z) / (far);
}