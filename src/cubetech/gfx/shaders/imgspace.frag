#version 120
#extension GL_ARB_texture_rectangle : require
uniform sampler2DRect tex;
uniform sampler2D normalmap;

varying vec2 coords;
varying vec2 coords2;
varying vec4 color;
void main()
{
    vec2 distOffset = (texture2D(normalmap, coords2.xy).xy * 2 - 1);
    distOffset *= 10.0f;
    vec4 dest = texture2DRect(tex, coords.xy + distOffset);
    gl_FragColor = dest*color;
    
    //gl_FragColor = vec4(coords,1.0,1.0) ;
}
