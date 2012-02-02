#version 120
uniform sampler2D tex;
uniform sampler2D tex2;
uniform vec4 depthOffset = vec4(0.0,0.0,1.0,1.0);
varying vec2 coords;
varying vec4 glcoords;
void main()
{

    vec2 viewcoords = vec2(glcoords.xy / glcoords.w) * 0.5 + 0.5;
    viewcoords = vec2(depthOffset.xy) + viewcoords * depthOffset.zw;
    float depth = texture2D(tex2, viewcoords).w;
    if(depth < 1.0) discard;
    gl_FragColor = texture2D(tex, coords);
    gl_FragColor.a = 0.0;
}

