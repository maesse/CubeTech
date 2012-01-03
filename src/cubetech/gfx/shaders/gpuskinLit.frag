uniform samplerCube envmap;
uniform sampler2D tex;
#pragma include "lighting.glsl"

in vec3 reflectDir; // for cubemap
in vec2 coords;

void main()
{
    // Start with ambient
    vec4 ambientcolor = ambient * textureCube(envmap, reflectDir);
     
    vec4 texcol = texture2D(tex, coords);
    
    // get light
    vec4 lightColor = getLighting(texcol);
    

    gl_FragColor.rgb = texcol.rgb * (ambientcolor.rgb + lightColor.rgb) * gl_Color.rgb;
    gl_FragColor.a = 1.0;
}