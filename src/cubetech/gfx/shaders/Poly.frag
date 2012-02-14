uniform sampler2D tex;

in vec2 coords;
in vec4 color;

void main()
{
    // Start with ambient
    //vec3 mnormal = normalize(2.0 * texture2D(normalmap, coords).rgb - 1.0);
    vec4 texcol = texture2D(tex, coords);
    //vec3 specularcolor = texture2D(specularmap, coords).rgb;
    gl_FragColor = vec4(texcol.rgb * color.a * color.rgb, color.a* texcol.a) ;
}
