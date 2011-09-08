#version 120
#ifdef GL_ARB_uniform_buffer_object
  #extension GL_ARB_uniform_buffer_object : enable
  layout(std140) uniform animdata
  {
     uniform mat3x4 bonemats[80];
  };
#else
  uniform mat3x4 bonemats[80];
#endif
uniform mat4 Modell;
attribute vec4 vweights;
attribute vec4 vbones;
attribute vec4 vtangent;
void main(void)
{
   mat3x4 m = bonemats[int(vbones.x)] * vweights.x;
   m += bonemats[int(vbones.y)] * vweights.y;
   m += bonemats[int(vbones.z)] * vweights.z;
   m += bonemats[int(vbones.w)] * vweights.w;
   vec4 mpos = Modell * vec4(gl_Vertex * m, gl_Vertex.w);
   gl_Position = gl_ModelViewProjectionMatrix * mpos;
   //gl_Position.z -= 0.002;
    //gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
   gl_TexCoord[0] = gl_MultiTexCoord0;
   //mat3 madjtrans = mat3(cross(m[1].xyz, m[2].xyz), cross(m[2].xyz, m[0].xyz), cross(m[0].xyz, m[1].xyz));
   //vec3 mnormal = (Modell * vec4((gl_Normal * madjtrans),0)).xyz;
   //gl_Position.z += mnormal.z * 0.02;
   //vec3 mtangent = vtangent.xyz * madjtrans; // tangent not used, just here as an example
   //vec3 mbitangent = cross(mnormal, mtangent) * vtangent.w; // bitangent not used, just here as an example
   //gl_FrontColor = gl_Color
//* (clamp(dot(normalize(gl_NormalMatrix * mnormal), gl_LightSource[0].position.xyz), 0.0, 1.0)
//* gl_LightSource[0].diffuse + gl_LightSource[0].ambient);
    gl_FrontColor = vec4(1,1,1,1);
}