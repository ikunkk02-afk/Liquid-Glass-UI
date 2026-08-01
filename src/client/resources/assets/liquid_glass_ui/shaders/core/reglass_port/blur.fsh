/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform vec2 Direction;
uniform int Radius;
uniform float Weights[65];

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 delta = Direction / OutSize;
    vec4 sum = texture(DiffuseSampler, texCoord) * Weights[0];
    for (int i = 1; i <= 64; ++i) {
        if (i > Radius) break;
        vec2 offset = delta * float(i);
        float weight = Weights[i];
        sum += texture(DiffuseSampler, texCoord + offset) * weight;
        sum += texture(DiffuseSampler, texCoord - offset) * weight;
    }
    fragColor = sum;
}
