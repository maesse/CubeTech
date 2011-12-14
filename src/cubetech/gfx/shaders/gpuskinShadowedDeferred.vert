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
varying vec4 diffuse, ambient;
varying vec3 normal, lightDir, halfVector;
varying vec3 reflectDir;

// Shadow out
varying vec4 vPosition;
varying float vDepth;
varying float scaledDepth;

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

void main(void)
{
   ivec4 offsets = ivec4(vbones)*3;
   mat3x4 m = mat3x4(bonemats[offsets.x],bonemats[offsets.x+1],bonemats[offsets.x+2]) * vweights.x;
   m += mat3x4(bonemats[offsets.y],bonemats[offsets.y+1],bonemats[offsets.y+2]) * vweights.y;
   m += mat3x4(bonemats[offsets.z],bonemats[offsets.z+1],bonemats[offsets.z+2]) * vweights.z;
   m += mat3x4(bonemats[offsets.w],bonemats[offsets.w+1],bonemats[offsets.w+2]) * vweights.w;
   vec4 mpos = Modell * vec4(gl_Vertex * m, gl_Vertex.w);
   vec4 fpos = gl_ModelViewProjectionMatrix * mpos;
   gl_Position = fpos;
   gl_TexCoord[0] = gl_MultiTexCoord0;
   
   //vec3 mtangent = vtangent.xyz * madjtrans; // tangent not used, just here as an example
   //vec3 mbitangent = cross(mnormal, mtangent) * vtangent.w; // bitangent not used, just here as an example
    gl_FrontColor = vec4(1,1,1,1);

    // Light out
    mat3 madjtrans = mat3(cross(m[1].xyz, m[2].xyz), cross(m[2].xyz, m[0].xyz), cross(m[0].xyz, m[1].xyz));
    vec3 mnormal = normalize( vec3(Modell * vec4(gl_Normal * madjtrans, 0)));
    reflectDir = mnormal;
    setLightVars(gl_NormalMatrix * mnormal, vec3(gl_ModelViewMatrix * mpos));

    // shadow out
    vPosition = mpos;
    vDepth = fpos.z;
    scaledDepth = (gl_Position.z-near) / (far-near);
    //scaledDepth = gl_Position.z;
}