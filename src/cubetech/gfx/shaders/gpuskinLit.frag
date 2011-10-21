uniform samplerCube envmap;
uniform sampler2D tex;
#pragma include "lighting.glsl"

varying vec3 reflectDir; // for cubemap


void main()
{
    // Start with ambient
    vec4 ambientcolor = ambient * textureCube(envmap, reflectDir);
     
    vec4 texcol = texture2D(tex, gl_TexCoord[0].xy);
    
    // get light
    vec4 lightColor = getLighting(texcol);
    

    gl_FragColor.rgb = texcol.rgb * (ambientcolor.rgb + lightColor.rgb) * gl_Color.rgb;
    gl_FragColor.a = 1.0;
}