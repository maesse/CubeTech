#version 120
uniform sampler2D tex;

varying vec2 coords;
varying vec4 color;
void main()
{
    vec4 dest = texture2D(tex, coords) * color;
    float fogFactor = exp(-pow((0.001 * gl_FogFragCoord), 2.0));
    gl_FragColor = mix(vec4(0.823,0.7,0.486,1), dest, fogFactor);
    //gl_FragColor = vec4(coords,1.0,1.0) ;
}
