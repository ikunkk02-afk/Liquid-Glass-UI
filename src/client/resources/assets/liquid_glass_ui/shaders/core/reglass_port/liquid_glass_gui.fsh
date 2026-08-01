/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
#version 150

uniform sampler2D RawSampler;
uniform sampler2D BlurSampler0;
uniform sampler2D BlurSampler1;
uniform sampler2D BlurSampler2;
uniform sampler2D BlurSampler3;
uniform sampler2D BlurSampler4;
uniform sampler2D WidgetDataSampler;
uniform vec2 FramebufferSize;
uniform int WidgetCount;
uniform int DebugMode;
uniform float Time;
uniform float HoverScalePx;
uniform float FocusScalePx;
uniform float FocusBorderWidthPx;
uniform float FocusBorderIntensity;
uniform float FocusBorderSpeed;

in vec2 texCoord;
out vec4 fragColor;

#define MAX_WIDGETS 64

struct SDFResult {
    float dist;
    vec2 normal;
    float aspect;
    int index;
};

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
    float l = length(q);
    float distanceToBox = (g > 0.0) ? l - r : g - r;
    vec2 normal = (g > 0.0) ? q / max(l, 1e-6)
                              : ((w.x > w.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0));
    return vec3(distanceToBox, s * normal);
}

SDFResult opSmoothUnion(in SDFResult a, in SDFResult b, in float k) {
    if (k <= 1e-6) return (a.dist < b.dist) ? a : b;
    float h = clamp(0.5 + 0.5 * (a.dist - b.dist) / k, 0.0, 1.0);
    float distanceToUnion = mix(a.dist, b.dist, h) - k * h * (1.0 - h);
    vec2 unionNormal = normalize(mix(a.normal, b.normal, h));
    int materialIndex = (a.dist < b.dist) ? a.index : b.index;
    return SDFResult(distanceToUnion, unionNormal, mix(a.aspect, b.aspect, h), materialIndex);
}

SDFResult opHardUnion(SDFResult a, SDFResult b) {
    return (a.dist < b.dist) ? a : b;
}

SDFResult opHardSubtract(SDFResult a, SDFResult b) {
    float distanceToSubtraction = max(a.dist, -b.dist);
    if (distanceToSubtraction == a.dist) return a;
    return SDFResult(distanceToSubtraction, -b.normal, a.aspect, a.index);
}

vec4 sampleBlur(int index, vec2 uv) {
    vec2 safeUv = clamp(uv, vec2(0.0), vec2(1.0));
    if (index <= 0) return texture(BlurSampler0, safeUv);
    if (index == 1) return texture(BlurSampler1, safeUv);
    if (index == 2) return texture(BlurSampler2, safeUv);
    if (index == 3) return texture(BlurSampler3, safeUv);
    return texture(BlurSampler4, safeUv);
}

bool widgetContainsCoordinate(int index, vec2 fragmentCoord) {
    vec4 scissor = widgetData(index, 7);
    return fragmentCoord.x >= scissor.x && fragmentCoord.y >= scissor.y
            && fragmentCoord.x <= scissor.z && fragmentCoord.y <= scissor.w;
}

SDFResult widgetShape(int index, vec2 fragmentCoord) {
    vec4 rectangle = widgetData(index, 0);
    vec4 radii = widgetData(index, 1);
    vec4 interaction = widgetData(index, 10);
    vec2 center = rectangle.xy + rectangle.zw * 0.5;
    vec3 box = sdgBox(fragmentCoord - center, rectangle.zw * 0.5, radii);
    float expansion = HoverScalePx * interaction.y + FocusScalePx * interaction.z + 0.8 * interaction.w;
    return SDFResult(box.x - expansion, box.yz,
            min(rectangle.z, rectangle.w) / max(rectangle.z, rectangle.w), index);
}

SDFResult fieldWidgets(vec2 fragmentCoord) {
    SDFResult field = SDFResult(1e20, vec2(0.0), 1.0, -1);
    bool fieldInitialized = false;
    for (int i = 0; i < MAX_WIDGETS; ++i) {
        if (i >= WidgetCount) break;
        if (!widgetContainsCoordinate(i, fragmentCoord)) continue;
        int group = int(widgetData(i, 6).y + 0.5);
        bool groupAlreadyProcessed = false;
        for (int previous = 0; previous < MAX_WIDGETS; ++previous) {
            if (previous >= i) break;
            if (widgetContainsCoordinate(previous, fragmentCoord)
                    && int(widgetData(previous, 6).y + 0.5) == group) {
                groupAlreadyProcessed = true;
                break;
            }
        }
        if (groupAlreadyProcessed) continue;

        SDFResult groupField = widgetShape(i, fragmentCoord);
        float groupSmoothing = max(0.0, widgetData(i, 6).x) * widgetData(i, 11).y;
        for (int other = 0; other < MAX_WIDGETS; ++other) {
            if (other <= i) continue;
            if (other >= WidgetCount) break;
            if (!widgetContainsCoordinate(other, fragmentCoord)) continue;
            if (int(widgetData(other, 6).y + 0.5) != group) continue;
            float smoothing = max(groupSmoothing,
                    max(0.0, widgetData(other, 6).x) * widgetData(other, 11).y);
            groupField = opSmoothUnion(groupField, widgetShape(other, fragmentCoord), smoothing);
        }
        field = fieldInitialized ? opHardUnion(field, groupField) : groupField;
        fieldInitialized = true;
    }
    return field;
}

vec3 applyShadows(vec2 fragmentCoord, vec3 base) {
    vec3 shadowed = base;
    for (int i = 0; i < MAX_WIDGETS; ++i) {
        if (i >= WidgetCount) break;
        vec4 scissor = widgetData(i, 7);
        if (fragmentCoord.x < scissor.x || fragmentCoord.y < scissor.y
                || fragmentCoord.x > scissor.z || fragmentCoord.y > scissor.w) continue;
        vec4 rectangle = widgetData(i, 0);
        vec4 radii = widgetData(i, 1);
        vec4 shadow = widgetData(i, 8);
        vec4 shadowColor = widgetData(i, 9);
        vec2 center = rectangle.xy + rectangle.zw * 0.5;
        vec3 box = sdgBox(fragmentCoord + shadow.zw - center, rectangle.zw * 0.5, radii);
        float expand = max(shadow.x, 1e-4);
        float falloff = exp(-max(box.x, 0.0) / expand);
        float outside = smoothstep(-1.0, 1.0, box.x);
        float amount = clamp(falloff * outside * 0.6 * shadow.y * shadowColor.a, 0.0, 0.75);
        shadowed = mix(shadowed, shadowColor.rgb, amount);
    }
    return shadowed;
}

void main() {
    vec2 coord = gl_FragCoord.xy;
    vec2 uv = coord / FramebufferSize;
    vec3 raw = texture(RawSampler, clamp(uv, vec2(0.0), vec2(1.0))).rgb;

    if (DebugMode == 3) {
        fragColor = vec4(raw, 1.0);
        return;
    }

    SDFResult field = fieldWidgets(coord);
    float merged = field.dist;
    int index = field.index;

    if (DebugMode == 1) {
        vec3 color = merged > 0.0 ? vec3(0.9, 0.28, 0.12) : vec3(0.14, 0.55, 1.0);
        color *= 1.0 - exp(-0.04 * abs(merged));
        fragColor = vec4(color, 1.0);
        return;
    }
    if (DebugMode == 2) {
        fragColor = vec4(field.normal * 0.5 + 0.5, 0.5, 1.0);
        return;
    }

    int blurIndex = index >= 0 ? int(widgetData(index, 10).x + 0.5) : 0;
    if (DebugMode == 4) {
        fragColor = sampleBlur(blurIndex, uv);
        return;
    }

    vec3 shadowed = applyShadows(coord, raw);
    if (index < 0 || merged >= 0.0) {
        fragColor = vec4(shadowed, 1.0);
        return;
    }

    vec4 optics0 = widgetData(index, 3);
    vec4 optics1 = widgetData(index, 4);
    vec4 optics2 = widgetData(index, 5);
    vec4 tint = widgetData(index, 2);
    vec4 interaction = widgetData(index, 10);

    vec2 normal = field.normal;
    float normalLength = length(normal);
    if (normalLength > 1e-6) normal /= normalLength;

    float refractionThickness = max(optics0.x, 1e-6);
    float refractionFactor = max(optics0.y, 1e-6);
    float dispersion = optics0.z;
    float interiorDepth = -merged;
    float normalizedEdge = clamp(1.0 - interiorDepth / refractionThickness, 0.0, 1.0);
    float thetaI = asin(clamp(normalizedEdge * normalizedEdge, -0.999, 0.999));
    float thetaT = asin(clamp(sin(thetaI) / refractionFactor, -0.999, 0.999));
    float edgeFactor = interiorDepth < refractionThickness ? -tan(thetaT - thetaI) : 0.0;
    float hoverBoost = 1.0 + 0.35 * interaction.y * exp(-abs(merged) / 9.0);
    vec2 aspectCorrection = vec2(FramebufferSize.y / FramebufferSize.x, 1.0);
    vec2 refractionOffset = -normal * edgeFactor * 0.08 * hoverBoost * aspectCorrection;

    const float NR = 0.985;
    const float NG = 1.000;
    const float NB = 1.015;
    vec4 dispersed;
    dispersed.r = sampleBlur(blurIndex, uv + refractionOffset * (1.0 - (NR - 1.0) * dispersion)).r;
    dispersed.g = sampleBlur(blurIndex, uv + refractionOffset * (1.0 - (NG - 1.0) * dispersion)).g;
    dispersed.b = sampleBlur(blurIndex, uv + refractionOffset * (1.0 - (NB - 1.0) * dispersion)).b;
    dispersed.a = 1.0;

    if (DebugMode == 5) {
        fragColor = dispersed;
        return;
    }

    vec4 outColor = mix(dispersed, vec4(tint.rgb, 1.0), tint.a * 0.8);
    float fresnelHardness = optics1.x / 100.0;
    float fresnelFactor = clamp(pow(1.0 + merged / 1500.0
            * pow(500.0 / max(optics0.w, 1e-6), 2.0) + fresnelHardness, 5.0), 0.0, 1.0);
    float fresnelStrength = optics1.y / 100.0;

    if (DebugMode == 6) {
        fragColor = vec4(vec3(fresnelFactor * fresnelStrength), 1.0);
        return;
    }

    vec3 fresnelTint = mix(vec3(1.0), tint.rgb, tint.a * 0.5);
    outColor = mix(outColor, vec4(fresnelTint, 1.0),
            fresnelFactor * fresnelStrength * 0.7 * normalLength);

    float glareHardness = optics1.w / 100.0;
    float glareGeometry = clamp(pow(1.0 + merged / 1500.0
            * pow(500.0 / max(optics1.z, 1e-6), 2.0) + glareHardness, 5.0), 0.0, 1.0);
    vec2 configuredGlareDirection = vec2(cos(optics2.w), sin(optics2.w));
    vec2 trackedGlareDirection = normalize(widgetData(index, 11).zw + vec2(1e-5, 0.0));
    vec2 glareDirection = normalize(mix(configuredGlareDirection, trackedGlareDirection, interaction.y * 0.65));
    float directionality = 0.5 + 0.5 * dot(normal, glareDirection);
    float convergence = max(0.25, optics2.x / 50.0);
    float facing = pow(clamp(directionality, 0.0, 1.0), convergence);
    float opposite = (optics2.y / 100.0) * pow(clamp(1.0 - directionality, 0.0, 1.0), convergence * 1.5);
    float glareStrength = glareGeometry * clamp(facing + 0.18 * opposite, 0.0, 1.0)
            * (optics2.z / 100.0) * normalLength;
    vec3 blurred = sampleBlur(blurIndex, uv + refractionOffset).rgb;
    vec3 glareColor = mix(blurred, mix(vec3(1.0), tint.rgb, tint.a * 0.35), 0.72);
    outColor.rgb = mix(outColor.rgb, glareColor, 0.48 * glareStrength);

    float edgeProximity = exp(-abs(merged) / 9.0);
    if (interaction.y > 0.0001) {
        vec2 tangent = normalize(vec2(-normal.y, normal.x));
        vec2 chroma = tangent * 0.0008 * interaction.y * edgeProximity * aspectCorrection;
        vec3 hoverChromatic = vec3(
                sampleBlur(blurIndex, uv + refractionOffset + chroma).r,
                sampleBlur(blurIndex, uv + refractionOffset).g,
                sampleBlur(blurIndex, uv + refractionOffset - chroma).b);
        outColor.rgb = clamp(outColor.rgb
                + mix(hoverChromatic, vec3(1.0), 0.08) * (0.08 + 0.22 * edgeProximity) * interaction.y,
                0.0, 1.0);
    }

    if (interaction.z > 0.0001) {
        vec4 rectangle = widgetData(index, 0);
        vec2 center = rectangle.xy + rectangle.zw * 0.5;
        float angle = atan(coord.y - center.y, coord.x - center.x);
        float sweep = smoothstep(0.75, 1.0, 0.5 + 0.5 * cos(angle - Time * FocusBorderSpeed));
        float band = exp(-pow(abs(merged) / max(FocusBorderWidthPx, 1e-3), 2.0));
        vec3 ringTint = mix(vec3(1.0), tint.rgb, 0.2);
        outColor.rgb = clamp(outColor.rgb + ringTint * band * sweep
                * FocusBorderIntensity * interaction.z, 0.0, 1.0);
    }

    vec3 finalColor = mix(outColor.rgb, shadowed, smoothstep(-1.0, 1.0, merged));
    fragColor = vec4(finalColor, 1.0);
}
