#version 150
#define BLURKERNEL 4

uniform sampler2D ssao;

varying vec2 texcoords;
uniform vec4 viewOffset = vec4(0.0,0.0,1.0,1.0);
uniform vec2 texelSize = vec2(1.0/1280.0, 1.0/800.0);

void main()
{
    float result = 0.0;

    for (int i=0; i<BLURKERNEL;++i) {
        for(int j=0; j<BLURKERNEL;++j) {
            result += texture2D(ssao, texcoords + texelSize * vec2(j-2,i-2)).r;
        }
    }
    result = result / float(BLURKERNEL*BLURKERNEL);

    gl_FragColor = vec4(result);
}

