#version 120
uniform sampler2D tex;

uniform vec4 fog_color = vec4(0.823,0.7,0.486,1);
uniform float fog_factor = 0.0005;

varying vec2 coords;
varying vec4 color;
void main()
{
    vec4 dest = texture2D(tex, coords) * color;
    float fogFactor = exp(-pow((fog_factor * gl_FogFragCoord), 2.0));
    gl_FragColor = mix(fog_color, dest, fogFactor);
}