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
uniform vec2 CaptureSize;
uniform vec2 WidgetDataSize;
uniform int WidgetCount;
uniform int DebugMode;

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
    float shadowRadius = surface.y;

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
    if (field.distance > shadowRadius) discard;
    if (field.distance > 0.0) {
        float shadow = (1.0 - smoothstep(0.0, shadowRadius, field.distance)) * opticsB.z;
        fragColor = vec4(0.0, 0.0, 0.0, shadow * 0.34);
        return;
    }

    vec2 baseUv = safeUv(point, RawSampler);
    if (DebugMode == 4) {
        fragColor = vec4(texture(RawSampler, baseUv).rgb, pointer.z);
        return;
    }
    if (DebugMode == 5) {
        fragColor = vec4(texture(FullBlurSampler, safeUv(point, FullBlurSampler)).rgb, pointer.z);
        return;
    }

    float thickness = max(opticsA.x, 0.5);
    float edge = 1.0 - smoothstep(0.0, thickness, -field.distance);
    float refractionPixels = edge * opticsA.y * thickness * (2.0 + interaction.y);
    vec2 refractedPoint = point + field.normal * refractionPixels;
    float dispersion = edge * opticsA.z;
    vec2 refractedUv = safeUv(refractedPoint, RawSampler);
    vec2 redUv = safeUv(refractedPoint + field.normal * dispersion, RawSampler);
    vec2 blueUv = safeUv(refractedPoint - field.normal * dispersion, RawSampler);
    vec3 rawColor = vec3(texture(RawSampler, redUv).r,
                         texture(RawSampler, refractedUv).g,
                         texture(RawSampler, blueUv).b);
    vec3 lightBlur = texture(LightBlurSampler, safeUv(refractedPoint, LightBlurSampler)).rgb;
    vec3 fullBlur = texture(FullBlurSampler, safeUv(refractedPoint, FullBlurSampler)).rgb;
    vec3 blurred = mix(lightBlur, fullBlur, clamp(surface.z, 0.0, 1.0));
    vec3 backdrop = mix(blurred, rawColor, clamp(opticsB.w, 0.0, 1.0));
    if (DebugMode == 6) {
        fragColor = vec4(abs(rawColor - texture(RawSampler, baseUv).rgb) * 7.0, 1.0);
        return;
    }

    float fresnel = pow(clamp(edge, 0.0, 1.0), 1.65) * opticsA.w;
    vec2 lightDirection = normalize(vec2(-0.62, 0.78));
    float directional = pow(max(dot(field.normal, lightDirection), 0.0), 2.2);
    float edgeHighlight = fresnel * opticsB.x * (0.48 + directional * 1.15 + interaction.y * 0.18);
    float innerDark = (1.0 - directional) * edge * opticsB.y;
    vec2 local = clamp((point - rect.xy) / max(rect.zw, vec2(1.0)), 0.0, 1.0);
    float verticalLight = (local.y - 0.5) * 0.055;
    float pointerDistance = length((point - pointer.xy) / max(rect.zw, vec2(1.0)));
    float pointerLight = (1.0 - smoothstep(0.05, 0.82, pointerDistance)) * detail.w * interaction.y * 0.22;
    float luminance = dot(backdrop, vec3(0.2126, 0.7152, 0.0722));
    float adaptive = detail.y > 0.5 ? mix(1.18, 0.82, luminance) : 1.0;
    if (DebugMode == 7) {
        fragColor = vec4(vec3(clamp(fresnel + edgeHighlight, 0.0, 1.0)), 1.0);
        return;
    }

    vec3 color = mix(backdrop, tint.rgb, clamp(tint.a, 0.0, 1.0));
    color += tint.rgb * (edgeHighlight * adaptive + pointerLight + verticalLight);
    color -= vec3(innerDark);
    color += hashNoise(point) * detail.x;
    float alpha = clamp(0.70 + tint.a * 0.55 + edge * 0.10 + interaction.y * 0.04, 0.62, 0.92) * pointer.z;
    fragColor = vec4(clamp(color, 0.0, 1.0), alpha);
}
