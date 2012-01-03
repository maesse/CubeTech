#version 140
layout(std140) uniform animdata
{
   uniform vec4 bonemats[130*3];
};
uniform mat4 ModelView;
uniform mat4 Projection;
in vec4 vposition;
in vec2 vcoords;
in vec4 vweights;
in vec4 vbones;
in vec4 vtangent;

out float viewDepth;
void main(void)
{
   ivec4 offsets = ivec4(vbones)*3;
   mat3x4 m = mat3x4(bonemats[offsets.x],bonemats[offsets.x+1],bonemats[offsets.x+2]) * vweights.x;
   m += mat3x4(bonemats[offsets.y],bonemats[offsets.y+1],bonemats[offsets.y+2]) * vweights.y;
   m += mat3x4(bonemats[offsets.z],bonemats[offsets.z+1],bonemats[offsets.z+2]) * vweights.z;
   m += mat3x4(bonemats[offsets.w],bonemats[offsets.w+1],bonemats[offsets.w+2]) * vweights.w;
   vec4 mpos = ModelView * vec4(vposition * m, vposition.w);

   gl_Position = Projection * mpos;
   viewDepth = length(mpos.xyz);
}