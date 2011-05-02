//
// Atmospheric scattering fragment shader
//
// Author: Sean O'Neil
//
// Copyright (c) 2004 Sean O'Neil
//

//uniform sampler2D s2Tex1;
//uniform sampler2D s2Tex2;
#version 120
uniform sampler2D tex;

varying vec2 coords;
varying vec4 color;

void main (void)
{
// texture2D(tex, coords) *
        vec4 dest =   gl_Color;
        gl_FragColor = dest;
	//gl_FragColor =  gl_SecondaryColor;
        gl_FragColor.a = 1;
	//gl_FragColor = gl_Color + texture2D(s2Tex1, gl_TexCoord[0].st) * texture2D(s2Tex2, gl_TexCoord[1].st) * gl_SecondaryColor;
}
