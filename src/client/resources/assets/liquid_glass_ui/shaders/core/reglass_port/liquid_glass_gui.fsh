/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
#version 150

uniform sampler2D RawSampler;
uniform sampler2D WidgetDataSampler;
uniform vec2 FramebufferSize;
uniform int WidgetCount;
uniform int DebugMode;

in vec2 texCoord;
out vec4 fragColor;

#define MAX_WIDGETS 64

vec4 widgetData(int index, int row) {
    return texelFetch(WidgetDataSampler, ivec2(index, row), 0);
}

vec3 sdgBox(in vec2 p, in vec2 b, vec4 ra) {
    ra.xy = (p.x > 0.0) ? ra.xy : ra.zw;
    float r = (p.y > 0.0) ? ra.x : ra.y;
    vec2 w = abs(p) - (b - r);
    vec2 s = vec2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    float g = max(w.x, w.y);
    vec2 q = max(w, 0.0);
    float lengthQ = length(q);
    float distanceToBox = (g > 0.0) ? lengthQ - r : g - r;
    vec2 normal = (g > 0.0) ? q / max(lengthQ, 1e-6)
                              : ((w.x > w.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0));
    return vec3(distanceToBox, s * normal);
}

void main() {
    vec2 coord = gl_FragCoord.xy;
    vec2 uv = coord / FramebufferSize;
    vec3 raw = texture(RawSampler, uv).rgb;
    float bestDistance = 1e20;
    vec2 bestNormal = vec2(0.0);
    int bestIndex = -1;

    for (int i = 0; i < MAX_WIDGETS; ++i) {
        if (i >= WidgetCount) break;
        vec4 scissor = widgetData(i, 7);
        if (coord.x < scissor.x || coord.y < scissor.y || coord.x > scissor.z || coord.y > scissor.w) continue;
        vec4 rect = widgetData(i, 0);
        vec4 radii = widgetData(i, 1);
        vec2 center = rect.xy + rect.zw * 0.5;
        vec3 sdf = sdgBox(coord - center, rect.zw * 0.5, radii);
        if (sdf.x < bestDistance) {
            bestDistance = sdf.x;
            bestNormal = sdf.yz;
            bestIndex = i;
        }
    }

    if (bestIndex < 0) discard;
    vec4 shadow0 = widgetData(bestIndex, 8);
    float shadowRange = max(1.0, shadow0.x);
    if (bestDistance > shadowRange) discard;

    if (DebugMode == 1) {
        vec3 debugColor = bestDistance > 0.0 ? vec3(0.9, 0.25, 0.12) : vec3(0.15, 0.55, 1.0);
        debugColor *= 1.0 - exp(-0.08 * abs(bestDistance));
        fragColor = vec4(debugColor, 1.0);
        return;
    }
    if (DebugMode == 2) {
        fragColor = vec4(bestNormal * 0.5 + 0.5, 0.5, 1.0);
        return;
    }

    if (bestDistance > 0.0) {
        vec4 shadowColor = widgetData(bestIndex, 9);
        float shadow = (1.0 - smoothstep(0.0, shadowRange, bestDistance))
                     * 0.8 * shadow0.y * shadowColor.a;
        fragColor = vec4(mix(raw, shadowColor.rgb, clamp(shadow, 0.0, 0.65)), 1.0);
        return;
    }

    vec4 tint = widgetData(bestIndex, 2);
    vec4 interaction = widgetData(bestIndex, 10);
    float edge = exp(-abs(bestDistance) / 4.0);
    float response = 1.0 + interaction.x * 0.35 + interaction.y * 0.25;
    vec3 centerGlass = mix(raw, tint.rgb, tint.a * 0.8);
    vec3 edgeLight = mix(centerGlass, vec3(1.0), 0.18 * edge * response);
    fragColor = vec4(edgeLight, 1.0);
}
