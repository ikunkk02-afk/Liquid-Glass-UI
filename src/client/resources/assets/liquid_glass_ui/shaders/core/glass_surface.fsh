#version 150

uniform sampler2D Sampler0;
uniform vec2 SampleTextureSize;
uniform vec4 Rect;
uniform vec4 PreviousRect;
uniform float CornerRadius;
uniform float Merge;
uniform int DebugMode;
uniform float Opacity;
uniform vec3 Tint;
uniform float TintIntensity;
uniform float EdgeWidth;
uniform float EdgeHighlight;
uniform float InnerShadow;
uniform float MouseHighlight;
uniform float MouseRange;
uniform float Refraction;
uniform float RefractionRange;
uniform int SampleCount;
uniform float Noise;
uniform float AdaptBrightness;
uniform vec2 HighlightPosition;

in vec2 screenPosition;
in vec2 backgroundUv;
out vec4 fragColor;

float roundedRectangle(vec2 point, vec4 rectangle, float radius) {
    vec2 center = rectangle.xy + rectangle.zw * 0.5;
    vec2 halfSize = max(vec2(0.5), rectangle.zw * 0.5 - vec2(radius));
    vec2 corner = abs(point - center) - halfSize;
    return length(max(corner, 0.0)) + min(max(corner.x, corner.y), 0.0) - radius;
}

float smoothUnion(float first, float second, float radius) {
    if (radius <= 0.001) return min(first, second);
    float blend = max(radius - abs(first - second), 0.0) / radius;
    return min(first, second) - blend * blend * radius * 0.25;
}

float distanceField(vec2 point) {
    float current = roundedRectangle(point, Rect, CornerRadius);
    float previous = roundedRectangle(point, PreviousRect, CornerRadius);
    return smoothUnion(current, previous, Merge * min(Rect.w, 18.0));
}

float randomNoise(vec2 point) {
    return fract(sin(dot(point, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
}

vec3 uvGrid(vec2 uv) {
    vec3 top = mix(vec3(0.12, 0.35, 1.0), vec3(1.0, 0.22, 0.16), uv.x);
    vec3 bottom = mix(vec3(0.16, 0.95, 0.30), vec3(1.0, 0.82, 0.08), uv.x);
    vec3 color = mix(bottom, top, uv.y);
    vec2 lines = abs(fract(uv * 10.0) - 0.5);
    float gridLine = 1.0 - smoothstep(0.43, 0.49, max(lines.x, lines.y));
    return mix(color, vec3(1.0), gridLine * 0.38);
}

void main() {
    float field = distanceField(screenPosition);
    float mask = 1.0 - smoothstep(-0.25, 0.85, field);
    if (mask <= 0.001) discard;

    vec2 halfTexel = 0.5 / max(SampleTextureSize, vec2(1.0));
    vec2 safeUv = clamp(backgroundUv, halfTexel, vec2(1.0) - halfTexel);

    if (DebugMode == 3) {
        fragColor = vec4(uvGrid(safeUv), mask);
        return;
    }

    vec3 rawBackground = texture(Sampler0, safeUv).rgb;
    if (DebugMode == 1 || DebugMode == 2) {
        fragColor = vec4(rawBackground, mask);
        return;
    }

    float epsilon = 0.75;
    vec2 gradient = vec2(
        distanceField(screenPosition + vec2(epsilon, 0.0)) - distanceField(screenPosition - vec2(epsilon, 0.0)),
        distanceField(screenPosition + vec2(0.0, epsilon)) - distanceField(screenPosition - vec2(0.0, epsilon))
    );
    vec2 normal = normalize(gradient + vec2(0.0001));
    float refractionBand = 1.0 - smoothstep(0.0, mix(1.0, 7.0, RefractionRange), abs(field));
    vec2 refractedUv = safeUv + vec2(normal.x, -normal.y) * Refraction * refractionBand * 0.006;

    vec3 background = vec3(0.0);
    int samples = clamp(SampleCount, 1, 12);
    for (int sampleIndex = 0; sampleIndex < 12; sampleIndex++) {
        if (sampleIndex >= samples) break;
        float angle = 6.2831853 * (float(sampleIndex) + 0.5) / float(samples);
        vec2 sampleOffset = vec2(cos(angle), sin(angle)) * halfTexel * min(float(samples), 4.0) * 0.35;
        vec2 sampleUv = clamp(refractedUv + sampleOffset, halfTexel, vec2(1.0) - halfTexel);
        background += texture(Sampler0, sampleUv).rgb;
    }
    background /= float(samples);
    float luminance = dot(background, vec3(0.2126, 0.7152, 0.0722));

    vec3 glass = mix(background, Tint, TintIntensity);
    float vertical = clamp((screenPosition.y - Rect.y) / max(1.0, Rect.w), 0.0, 1.0);
    glass += 0.018 - vertical * 0.030;

    float edge = 1.0 - smoothstep(0.0, max(0.25, EdgeWidth + 0.55), abs(field));
    float adaptiveHighlight = mix(1.08, 0.76, luminance) * mix(1.0, 1.18, AdaptBrightness);
    glass += vec3(edge * EdgeHighlight * 0.11 * adaptiveHighlight);

    float innerEdge = smoothstep(-3.5, -0.1, field);
    float adaptiveShadow = mix(0.82, 1.16, luminance) * mix(1.0, 1.12, AdaptBrightness);
    glass -= vec3(innerEdge * InnerShadow * 0.08 * adaptiveShadow);

    vec2 local = (screenPosition - Rect.xy) / max(Rect.zw, vec2(1.0));
    float highlightDistance = distance(local, HighlightPosition);
    float highlightRadius = mix(0.32, 0.92, MouseRange);
    float pointerHighlight = 1.0 - smoothstep(0.0, highlightRadius, highlightDistance);
    glass += vec3(pointerHighlight * MouseHighlight * 0.07);
    glass += randomNoise(screenPosition) * Noise * 0.025;

    float surfaceAlpha = clamp(Opacity, 0.06, 0.55);
    fragColor = vec4(clamp(glass, 0.0, 1.0), surfaceAlpha * mask);
}
