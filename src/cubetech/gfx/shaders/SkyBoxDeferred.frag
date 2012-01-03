#version 120
uniform sampler2D tex;
uniform sampler2D tex2;
varying vec2 coords;
varying vec4 glcoords;
void main()
{

    vec2 viewcoords = vec2(glcoords.xy / glcoords.w) * 0.5 + 0.5;
    float depth = texture2D(tex2, viewcoords).w;
    if(depth < 1.0) discard;
    gl_FragColor = texture2D(tex, coords);
    gl_FragColor.a = 0.0;
}

