uniform sampler2D tex;

in vec2 coords;
void main()
{
    gl_FragColor = texture2D(tex, coords);
}
