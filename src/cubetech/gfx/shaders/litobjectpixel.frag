

uniform samplerCube envmap;
uniform sampler2D tex;

varying vec2 coords;
varying vec4 diffuse, ambient;
varying vec3 normal, lightDir, halfVector;
varying vec3 reflectDir;
varying vec4 col;

void main()
{
    // Start with ambient
    vec4 color = ambient * textureCube(envmap, reflectDir);

    vec4 texcol = texture2D(tex, coords);
    //vec4 texcol = vec4(0.8,0.8,0.8,1.0);

    // normalize the interpolated normal
    vec3 n = normalize(normal);

    // compute dot
    float NdotL = max(dot(n, lightDir), 0.0);

    if(NdotL > 0.0) {
        color += diffuse * NdotL;

        vec3 halfV = normalize(halfVector);
        float NdotHV = max(dot(n, halfV), 0.0);
        color += texcol
                * pow(NdotHV, gl_FrontMaterial.shininess);
    }

    gl_FragColor = texcol * color * col;
    //gl_FragColor = textureCube(envmap, reflectDir);
}
