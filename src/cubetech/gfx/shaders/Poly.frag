uniform sampler2D tex;

in vec2 coords;

void main()
{
    // Start with ambient
    //vec3 mnormal = normalize(2.0 * texture2D(normalmap, coords).rgb - 1.0);
    vec4 texcol = texture2D(tex, coords);
    //vec3 specularcolor = texture2D(specularmap, coords).rgb;
    gl_FragColor.rgb = texcol.rgb * texcol.a;
    gl_FragColor.a = texcol.a;
}
