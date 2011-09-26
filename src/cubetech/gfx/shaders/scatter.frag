#version 120
#extension GL_ARB_texture_rectangle : require
uniform sampler2DRect tex;
uniform sampler2D normalmap;

const float exposure = 0.2f;
const float decay = 0.99f;
const float density = 1.3f;
const float weight = 0.6f;
uniform vec2 lightPositionOnScreen = vec2(100,700);
const int NUM_SAMPLES = 100 ;

varying vec2 coords;
varying vec2 coords2;
varying vec4 color;
void main()
{
  //  vec2 distOffset = (texture2D(normalmap, coords2.xy).xy * 2 - 1);
  //  distOffset *= 10.0f;
  //  vec4 dest = texture2DRect(tex, coords.xy + distOffset);

  vec2 deltaTextCoord = vec2( coords.xy - lightPositionOnScreen.xy );
  vec2 textCoo = coords.xy;
  deltaTextCoord *= 1.0 / float(NUM_SAMPLES) * density;
  float illuminationDecay = 1.0;

  vec4 destColor = vec4(0,0,0,1);

  for(int i=0; i < NUM_SAMPLES ; i++)
   {
     textCoo -= deltaTextCoord;
     vec4 sample = texture2DRect(tex, textCoo );
          sample *= illuminationDecay * weight;
          destColor += sample;
          illuminationDecay *= decay;
  }
  
  gl_FragColor = destColor * exposure;
  //gl_FragColor = dest*color;
  //gl_FragColor = vec4(coords,1.0,1.0) ;
}
