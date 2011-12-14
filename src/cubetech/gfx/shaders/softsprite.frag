#version 120
#ifdef GL_ARB_texture_rectangle
#extension GL_ARB_texture_rectangle : enable
#else
#extension GL_EXT_texture_rectangle : enable
#endif

uniform sampler2D tex;
uniform sampler2DRect depth;
uniform vec2 res = vec2(1024.0, 768.0);
uniform float scale = 50.0;

varying vec2 coords;
varying vec4 color;
varying vec4 pos;

// eh?
float LinearizeDepth(float zoverw) {
	float n = 1.0; // camera z near
	float f = 3000.0; // camera z far
	return (2.0 * n) / (f + n - zoverw * (f - n));
}

void main()
{
    gl_FragColor = texture2D(tex, coords) * color;
    vec3 fragCoord = ((pos.xyz / pos.w) * 0.5) + 0.5;
    float sceneDepth = texture2DRect(depth, (fragCoord.xy) * res).x;
    float col = (LinearizeDepth(sceneDepth)-LinearizeDepth(fragCoord.z)) * scale;
    gl_FragColor.a *= clamp(col, 0, 1);
}


