uniform sampler2D tex;

in vec2 coords;
in vec4 color;
varying vec4 diffuse, ambientGlobal, ambient;
varying vec3 normal, lightDir, halfVector;
varying float dist;
varying vec3 v;

void main()
{
    //vec4 dest = texture2D(tex, coords) * diffuse;
    vec4 dest = ambientGlobal;

    
    

    vec3 n = normalize(normal);
    float NdotL = max(dot(n, normalize(lightDir)),0.0);

    if(NdotL > 0.0) {
        float att = 1.0 / (gl_LightSource[0].constantAttenuation +
                            gl_LightSource[0].linearAttenuation * dist +
                            gl_LightSource[0].quadraticAttenuation * dist * dist);
        dest += att * (diffuse * NdotL + ambient);
        //att = abs(dist) / 200.0;
        dest.xyz = vec3(att,att,att);

        vec3 halfV = normalize(halfVector);
        float NdotHV = max(dot(n, halfV),0.0);
        //dest.xyz += halfV;
        dest +=gl_LightSource[0].specular * pow(NdotHV, 100.0);
    }

    //dest = vec4(normalize(lightDir),1);
    gl_FragColor = dest;
}
