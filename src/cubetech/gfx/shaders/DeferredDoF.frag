#version 150
#define NSAMPLES 32

uniform sampler2D noise;
uniform sampler2D tex0;
uniform sampler2D tex2;


varying vec2 texcoord;
varying vec3 view;
uniform vec2 noiseScale;
uniform vec3 kernel[NSAMPLES];
uniform float kernelRadius;
uniform mat4 projectionMatrix;




void main()
{
    
    vec3 color = texture2D(tex0, texcoord).rgb;
    vec4 vtex2 = texture2D(tex2, texcoord);
    vec3 normal = vec3(vtex2.r, vtex2.g, vtex2.b);
    float depth = vtex2.w ;

    vec4 projPos = vec4(texcoord, depth, 1.0);
    projPos.xy = projPos.xy * 2.0 - 1.0;
    projPos = inverse(projectionMatrix) * projPos;
    projPos.xyz /= projPos.w;
    vec3 position = vec3(projPos);

    vec3 rnd = (texture2D(noise, texcoord * noiseScale).rgb - 0.5) * 2.0;
    rnd = vec3(0,0,-1);
    normal = normalize(normal);
    vec3 tangent = normalize(rnd - normal * dot(rnd, normal));
    vec3 bitan = cross(normal,tangent);

    mat3 tbn = mat3(tangent, bitan, normal);

    float occlusion = 0.0;
    vec3 samplesum = vec3(0,0,0);
    for(int i=0; i < NSAMPLES; i++) {
        vec3 sample = vec3(tbn * kernel[i]);
        
        sample = sample  * kernelRadius + position;
        //sample = vec3(0.01,0,0) + position;
        // View space -> screen space
        vec4 offset = vec4(sample, 1.0);
        
        // Get clip coordinates
        offset = projectionMatrix * offset;

        // To normalized device coordinates
        offset /= offset.w;
        offset.xy = offset.xy * 0.5 + 0.5;

        // sample depth at point
        float sampleDepth = texture2D(tex2, offset.xy).w;
        

        vec4 projPos2 = vec4(offset.xy, sampleDepth, 1.0);
        projPos2.xy = projPos2.xy * 2.0 - 1.0;
        projPos2 = inverse(projectionMatrix) * projPos2;
        projPos2.xyz /= projPos2.w;
        float rangeCheck = ((position.z) - projPos2.z) > 0.0 ? 1.0 : 0.0;
        //occlusion += (projPos2.z  <= position.z ? 1.0 : 0.0) ;
        occlusion += (projPos2.z  <= sample.z ? 1.0 : 0.0) ;
    }

    occlusion =   (occlusion / float(NSAMPLES));

    gl_FragColor.rgb = color * occlusion;
    
    
    gl_FragColor.a = 1.0;
}

