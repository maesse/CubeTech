#version 150
#define NSAMPLES 32

uniform sampler2D noise;
uniform sampler2D tex2;

varying vec2 texcoord;
varying vec3 view;
uniform vec2 noiseScale;
uniform vec3 kernel[NSAMPLES];
uniform float kernelRadius;
uniform mat4 projectionMatrix;
uniform float far;

uniform vec4 viewOffset = vec4(0.0,0.0,1.0,1.0);

void main()
{
    vec4 vtex2 = texture2D(tex2, texcoord);
    vec3 normal = vec3(vtex2.r, vtex2.g, vtex2.b);
    float depth = vtex2.w ;
    
    vec3 position = vec3(view) * depth;

    vec3 rnd = (texture2D(noise, texcoord * noiseScale).rgb - 0.5) * 2.0;
    
    normal = normalize(normal);
    vec3 tangent = normalize(rnd - normal * dot(rnd, normal));
    vec3 bitan = cross(normal,tangent);

    mat3 tbn = mat3(tangent, bitan, normal);

    float occlusion = 0.0;
    vec3 samplesum = vec3(0,0,0);
    for(int i=0; i < NSAMPLES; i++) {
        vec3 sample = vec3(tbn * kernel[i]) * kernelRadius * (1+depth*2.0) + position;
        // View space -> screen space
        vec4 offset = vec4(sample, 1.0);
        
        // Get clip coordinates
        offset = projectionMatrix * offset;

        // To normalized device coordinates
        offset /= offset.w;
        offset.xy = offset.xy * 0.5 + 0.5;

        offset.xy = viewOffset.xy + offset.xy * viewOffset.zw;

        // sample depth at point
        float sampleDepth = texture2D(tex2, offset.xy).w * -far;
        
        float rangeCheck = abs(position.z - sampleDepth) - kernelRadius;
        if(rangeCheck < 0) { rangeCheck = 1.0; } else { rangeCheck = 1.0 - min(rangeCheck/kernelRadius, 1.0); }
        float occFrac = 1.0;// - (length(kernel[i]))*0.7;
        occlusion += (sampleDepth   >= sample.z ? 1.0 : 0.0) * occFrac * rangeCheck;
    }
    occlusion =   abs(1.0 - (occlusion / float(NSAMPLES)));

    gl_FragColor.rgb = vec3(0,0,0);
    gl_FragColor.rgb = vec3(occlusion * occlusion );
    //gl_FragColor.rgb = vec3(abs(sampleDepth - depth) * 1.0);
    //gl_FragColor.rg = (abs( offset.xy - texcoord) ) * 100.0;

    //gl_FragColor.rgb = rnd.rgb;
    
    
    gl_FragColor.a = 1.0;
}

