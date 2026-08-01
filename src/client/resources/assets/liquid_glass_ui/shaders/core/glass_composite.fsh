/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License. Adapted for Minecraft Fabric 1.21.1.
 */
#version 150

uniform sampler2D RawSampler;
uniform sampler2D LightBlurSampler;
uniform sampler2D FullBlurSampler;
uniform sampler2D WidgetDataSampler;
uniform vec2 FramebufferSize;
uniform int WidgetCount;
uniform int DebugMode;
uniform int SampleCount;
uniform int RefractionQuality;

out vec4 fragColor;

struct SDFResult {
    float distance;
    vec2 normal;
    int index;
    int group;
    float smoothing;
};

vec4 widgetRow(int widget, int row) {
    return texelFetch(WidgetDataSampler, ivec2(widget, row), 0);
}

SDFResult sdgBox(vec2 point, vec4 rect, float radius, int index, int group, float smoothing) {
    vec2 center = rect.xy + rect.zw * 0.5;
    vec2 local = point - center;
    vec2 halfSize = max(vec2(0.5), rect.zw * 0.5);
    float r = min(radius, min(halfSize.x, halfSize.y));
    vec2 q = abs(local) - halfSize + r;
    vec2 outside = max(q, vec2(0.0));
    float distance = length(outside) + min(max(q.x, q.y), 0.0) - r;
    vec2 normal;
    if (outside.x > 0.0 || outside.y > 0.0) {
        normal = normalize(max(outside, vec2(0.0001)) * sign(local));
    } else if (q.x > q.y) {
        normal = vec2(sign(local.x), 0.0);
    } else {
        normal = vec2(0.0, sign(local.y));
    }
    return SDFResult(distance, normal, index, group, smoothing);
}

SDFResult sdgCapsule(vec2 point, vec2 startPoint, vec2 endPoint, float radius,
                     int index, int group, float smoothing) {
    vec2 segment = endPoint - startPoint;
    float divisor = max(dot(segment, segment), 0.0001);
    float along = clamp(dot(point - startPoint, segment) / divisor, 0.0, 1.0);
    vec2 delta = point - (startPoint + segment * along);
    float lengthDelta = max(length(delta), 0.0001);
    return SDFResult(lengthDelta - radius, delta / lengthDelta, index, group, smoothing);
}

SDFResult opHardUnion(SDFResult a, SDFResult b) {
    return a.distance <= b.distance ? a : b;
}

SDFResult opSmoothUnion(SDFResult a, SDFResult b, float amount) {
    float k = max(amount, 0.001);
    float blend = clamp(0.5 + 0.5 * (b.distance - a.distance) / k, 0.0, 1.0);
    SDFResult result = blend >= 0.5 ? a : b;
    result.distance = mix(b.distance, a.distance, blend) - k * blend * (1.0 - blend);
    result.normal = normalize(mix(b.normal, a.normal, blend));
    return result;
}

bool insideScissor(vec2 point, vec4 scissor) {
    return point.x >= scissor.x && point.y >= scissor.y && point.x <= scissor.z && point.y <= scissor.w;
}

SDFResult fieldWidgets(vec2 point) {
    SDFResult field = SDFResult(1.0e20, vec2(0.0, 1.0), -1, -1, 0.0);
    for (int i = 0; i < 64; i++) {
        if (i >= WidgetCount) break;
        vec4 state = widgetRow(i, 5);
        if (state.w < 0.5 || !insideScissor(point, widgetRow(i, 7))) continue;
        vec4 identity = widgetRow(i, 6);
        int group = int(identity.x + 0.5);
        int shape = int(identity.y + 0.5);
        float smoothing = identity.z;
        SDFResult item;
        if (shape == 1) {
            vec4 capsule = widgetRow(i, 8);
            item = sdgCapsule(point, capsule.xy, capsule.zw, widgetRow(i, 9).x, i, group, smoothing);
        } else {
            item = sdgBox(point, widgetRow(i, 0), widgetRow(i, 1).x, i, group, smoothing);
        }
        if (field.index >= 0 && field.group == item.group && max(field.smoothing, item.smoothing) > 0.001) {
            field = opSmoothUnion(field, item, max(field.smoothing, item.smoothing));
        } else {
            field = opHardUnion(field, item);
        }
    }
    return field;
}

vec2 safeUv(vec2 pixel, sampler2D source) {
    vec2 size = vec2(textureSize(source, 0));
    vec2 halfTexel = 0.5 / size;
    return clamp(pixel / FramebufferSize, halfTexel, vec2(1.0) - halfTexel);
}

float hashNoise(vec2 point) {
    return fract(sin(dot(point, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
}

vec3 groupColor(int group) {
    float seed = float(max(group, 0));
    return 0.45 + 0.45 * cos(vec3(0.0, 2.1, 4.2) + seed * 1.73);
}

vec3 highQualityRefraction(vec2 point, vec2 normal, float offsetPixels, float dispersionPixels) {
    int taps = clamp(SampleCount, 2, 12);
    float tapsFloat = float(taps);
    vec3 accumulated = vec3(0.0);
    float totalWeight = 0.0;
    for (int sampleIndex = 0; sampleIndex < 12; sampleIndex++) {
        if (sampleIndex >= taps) break;
        float phase = (float(sampleIndex) + 0.5) / tapsFloat - 0.5;
        float weight = 1.0 - abs(phase) * 0.55;
        vec2 samplePoint = point + normal * offsetPixels * (1.0 + phase * 0.32);
        vec2 chroma = normal * dispersionPixels * 0.5;
        accumulated += vec3(
                texture(RawSampler, safeUv(samplePoint + chroma, RawSampler)).r,
                texture(RawSampler, safeUv(samplePoint, RawSampler)).g,
                texture(RawSampler, safeUv(samplePoint - chroma, RawSampler)).b) * weight;
        totalWeight += weight;
    }
    return accumulated / max(totalWeight, 0.0001);
}

void main() {
    vec2 point = gl_FragCoord.xy;
    SDFResult field = fieldWidgets(point);
    if (field.index < 0) discard;
    vec4 rect = widgetRow(field.index, 0);
    vec4 interaction = widgetRow(field.index, 1);
    vec4 tint = widgetRow(field.index, 2);
    vec4 opticsA = widgetRow(field.index, 3);
    vec4 opticsB = widgetRow(field.index, 4);
    vec4 pointer = widgetRow(field.index, 5);
    vec4 surface = widgetRow(field.index, 9);
    vec4 detail = widgetRow(field.index, 10);
    vec4 material = widgetRow(field.index, 11);
    float shadowRadius = surface.y;
    float animationOpacity = clamp(pointer.z, 0.0, 1.0);

    if (DebugMode == 1) {
        if (field.distance > shadowRadius * 4.0) discard;
        float band = 0.5 + 0.5 * cos(field.distance * 0.45);
        vec3 signColor = field.distance <= 0.0 ? vec3(0.15, 0.72, 1.0) : vec3(1.0, 0.24, 0.18);
        fragColor = vec4(mix(signColor * 0.35, signColor, band), 0.92);
        return;
    }
    if (DebugMode == 2) {
        if (field.distance > 0.0) discard;
        fragColor = vec4(field.normal * 0.5 + 0.5, 0.5, 1.0);
        return;
    }
    if (DebugMode == 3) {
        if (field.distance > 0.0) discard;
        fragColor = vec4(groupColor(field.group), 0.9);
        return;
    }
    float aa = max(fwidth(field.distance), 0.75);
    if (field.distance > shadowRadius) discard;
    if (field.distance > aa) {
        float shadow = (1.0 - smoothstep(aa, shadowRadius, field.distance)) * opticsB.z;
        fragColor = vec4(0.0, 0.0, 0.0, shadow * 0.28 * animationOpacity);
        return;
    }
    float coverage = 1.0 - smoothstep(-aa, aa, field.distance);
    if (coverage <= 0.0) discard;

    vec2 baseUv = safeUv(point, RawSampler);
    if (DebugMode == 4) {
        fragColor = vec4(texture(RawSampler, baseUv).rgb, coverage * animationOpacity);
        return;
    }
    if (DebugMode == 5) {
        fragColor = vec4(texture(FullBlurSampler, safeUv(point, FullBlurSampler)).rgb,
                coverage * animationOpacity);
        return;
    }

    float thickness = max(opticsA.x, 0.5);
    float surfaceEdge = 1.0 - smoothstep(0.0, thickness, -field.distance);
    float edgeRangePixels = mix(thickness, max(thickness, min(rect.z, rect.w) * 0.5),
            clamp(material.y, 0.0, 1.0));
    float refractionEdge = 1.0 - smoothstep(0.0, edgeRangePixels, -field.distance);
    float refractionPixels = refractionEdge * opticsA.y * thickness * (2.0 + interaction.y * 0.25);
    vec2 refractedPoint = point + field.normal * refractionPixels;
    vec3 rawBase = texture(RawSampler, baseUv).rgb;
    vec3 rawColor = rawBase;
    if (RefractionQuality == 1) {
        rawColor = texture(RawSampler, safeUv(refractedPoint, RawSampler)).rgb;
    } else if (RefractionQuality >= 2) {
        rawColor = highQualityRefraction(point, field.normal, refractionPixels,
                refractionEdge * opticsA.z);
    }
    vec3 lightBlur = texture(LightBlurSampler, safeUv(refractedPoint, LightBlurSampler)).rgb;
    vec3 fullBlur = texture(FullBlurSampler, safeUv(refractedPoint, FullBlurSampler)).rgb;
    float materialOpacity = clamp(material.x, 0.0, 1.0);
    float densityCurve = sqrt(materialOpacity);
    float densityFactor = mix(0.45, 1.0, densityCurve);
    float clarity = clamp(opticsB.w, 0.0, 1.0);
    vec3 structuredBlur = mix(fullBlur, lightBlur, clarity);
    float blurWeight = clamp(surface.z, 0.0, 1.0) * mix(0.30, 1.0, densityCurve)
            * mix(1.0, 0.55, clarity);
    vec3 backdrop = mix(rawColor, structuredBlur, clamp(blurWeight, 0.0, 1.0));
    if (DebugMode == 6) {
        fragColor = vec4(abs(rawColor - rawBase) * 7.0, coverage * animationOpacity);
        return;
    }

    float fineEdge = 1.0 - smoothstep(0.0, max(detail.z, aa), -field.distance);
    float fresnel = pow(clamp(surfaceEdge, 0.0, 1.0), 1.65) * opticsA.w;
    vec2 lightDirection = normalize(vec2(-0.62, 0.78));
    float directional = pow(max(dot(field.normal, lightDirection), 0.0), 2.2);
    float edgeHighlight = (fresnel * 0.62 + fineEdge * 0.18) * opticsB.x
            * (0.42 + directional * 0.95 + interaction.y * 0.10) * densityFactor;
    float innerDark = (1.0 - directional) * surfaceEdge * opticsB.y * densityFactor;
    vec2 local = clamp((point - rect.xy) / max(rect.zw, vec2(1.0)), 0.0, 1.0);
    float pointerDistance = length((point - pointer.xy) / max(rect.zw, vec2(1.0)));
    float pointerRange = max(material.z, 0.02);
    float pointerLight = (1.0 - smoothstep(pointerRange * 0.15, pointerRange, pointerDistance))
            * detail.w * (0.25 + interaction.x * 0.75) * (1.0 + interaction.y * 0.12)
            * 0.16 * densityFactor;
    float verticalSheen = (1.0 - smoothstep(0.0, 0.58, local.y)) * 0.008 * densityFactor;
    float luminance = dot(backdrop, vec3(0.2126, 0.7152, 0.0722));
    float adaptive = detail.y > 0.5 ? mix(1.12, 0.88, luminance) : 1.0;
    if (DebugMode == 7) {
        fragColor = vec4(vec3(clamp(fresnel + edgeHighlight, 0.0, 1.0)),
                coverage * animationOpacity);
        return;
    }

    float tintWeight = clamp(tint.a * (0.35 + densityCurve * 1.30), 0.0, 1.0);
    vec3 color = mix(backdrop, tint.rgb, tintWeight);
    vec3 highlightColor = mix(vec3(1.0), tint.rgb, 0.28);
    color += highlightColor * (edgeHighlight * adaptive + pointerLight + verticalSheen);
    color *= 1.0 - min(innerDark * 0.10, 0.08);
    color += hashNoise(point) * detail.x * densityFactor;
    fragColor = vec4(clamp(color, 0.0, 1.0), coverage * animationOpacity);
}
