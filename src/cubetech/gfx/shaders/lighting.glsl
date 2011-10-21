//#define PHONG


varying vec4 diffuse, ambient;
varying vec3 normal, lightDir, halfVector;

vec4 getLighting(in vec4 texcol)
{
    vec3 n = normalize(normal); // normalize the interpolated normal
    float NdotL = (max(dot(n, lightDir), 0.0))*0.80 + 0.20;
    vec4 lightColor = diffuse * NdotL;

#ifdef PHONG
    // Phong
    vec3 halfV = reflect(lightDir, n);
    float NdotHV = max(dot(halfV, normalize(halfVector)), 0.0);
    lightColor += pow(NdotHV, gl_FrontMaterial.shininess) * texcol.a * 10.0;
#else
    // Blinn-Phong
    if(NdotL > 0.0) {
        vec3 halfV = normalize(halfVector);
        float NdotHV = max(dot(n, halfV), 0);
        lightColor += pow(NdotHV, gl_FrontMaterial.shininess) * texcol.a * 5.0;
    }
#endif
    
    return lightColor;
}