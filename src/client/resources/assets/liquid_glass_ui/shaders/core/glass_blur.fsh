#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform vec2 Direction;
uniform float Radius;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 offset = Direction * TexelSize * max(1.0, Radius);
    vec4 color = texture(Sampler0, texCoord) * 0.30;
    color += texture(Sampler0, texCoord + offset * 0.75) * 0.24;
    color += texture(Sampler0, texCoord - offset * 0.75) * 0.24;
    color += texture(Sampler0, texCoord + offset * 1.75) * 0.11;
    color += texture(Sampler0, texCoord - offset * 1.75) * 0.11;
    fragColor = color;
}
