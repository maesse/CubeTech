#version 120
uniform sampler2D tex;

varying vec2 coords;
varying vec4 color;
varying vec3 normal;
varying vec4 position;
varying float depth;
uniform float far;

void main()
{
    gl_FragData[0] = vec4(texture2D(tex, coords).rgb * color.rgb, 0.1);
    //gl_FragData[1] = vec4(position);
    gl_FragData[1] = gl_FragData[0] * 0.5;
    gl_FragData[2] = vec4(normal, -depth/far);
}
