#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform float Offset;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 halfTexel = TexelSize * 0.5;
    vec2 uv = clamp(texCoord, halfTexel, vec2(1.0) - halfTexel);
    vec2 stepUv = TexelSize * Offset;
    vec4 color = texture(Sampler0, uv) * 0.20;
    color += texture(Sampler0, clamp(uv + stepUv, halfTexel, vec2(1.0) - halfTexel)) * 0.20;
    color += texture(Sampler0, clamp(uv + vec2(-stepUv.x, stepUv.y), halfTexel, vec2(1.0) - halfTexel)) * 0.20;
    color += texture(Sampler0, clamp(uv + vec2(stepUv.x, -stepUv.y), halfTexel, vec2(1.0) - halfTexel)) * 0.20;
    color += texture(Sampler0, clamp(uv - stepUv, halfTexel, vec2(1.0) - halfTexel)) * 0.20;
    fragColor = color;
}
