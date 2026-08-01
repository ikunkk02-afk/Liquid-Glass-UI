#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform vec2 Direction;
uniform float StepSize;
uniform int SamplePairs;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 halfTexel = TexelSize * 0.5;
    vec2 uv = clamp(texCoord, halfTexel, vec2(1.0) - halfTexel);
    int pairs = clamp(SamplePairs, 1, 6);
    float sigma = max(0.75, float(pairs) * 0.58);
    vec4 color = texture(Sampler0, uv);
    float totalWeight = 1.0;
    for (int tap = 1; tap <= 6; tap++) {
        if (tap > pairs) break;
        float offset = float(tap);
        float weight = exp(-0.5 * offset * offset / (sigma * sigma));
        vec2 delta = TexelSize * Direction * StepSize * offset;
        color += texture(Sampler0, clamp(uv + delta, halfTexel, vec2(1.0) - halfTexel)) * weight;
        color += texture(Sampler0, clamp(uv - delta, halfTexel, vec2(1.0) - halfTexel)) * weight;
        totalWeight += weight * 2.0;
    }
    fragColor = color / totalWeight;
}
