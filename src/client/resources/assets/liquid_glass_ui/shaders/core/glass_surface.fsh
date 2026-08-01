#version 150

uniform sampler2D Sampler0;
uniform vec2 GuiSize;
uniform vec4 Rect;
uniform vec4 PreviousRect;
uniform float CornerRadius;
uniform float Merge;
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
    vec2 distanceToCorner = abs(point - center) - halfSize;
    return length(max(distanceToCorner, 0.0)) + min(max(distanceToCorner.x, distanceToCorner.y), 0.0) - radius;
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

void main() {
    float field = distanceField(screenPosition);
    float mask = 1.0 - smoothstep(-0.3, 1.0, field);
    if (mask <= 0.001) discard;

    float epsilon = 0.75;
    vec2 gradient = vec2(
        distanceField(screenPosition + vec2(epsilon, 0.0)) - distanceField(screenPosition - vec2(epsilon, 0.0)),
        distanceField(screenPosition + vec2(0.0, epsilon)) - distanceField(screenPosition - vec2(0.0, epsilon))
    );
    vec2 normal = normalize(gradient + vec2(0.0001));
    float refractionBand = 1.0 - smoothstep(0.0, mix(1.0, 7.0, RefractionRange), abs(field));
    vec2 refractedUv = backgroundUv + vec2(normal.x, -normal.y) * Refraction * refractionBand * 0.012;
    vec3 background = vec3(0.0);
    int samples = clamp(SampleCount, 1, 12);
    for (int sampleIndex = 0; sampleIndex < 12; sampleIndex++) {
        if (sampleIndex >= samples) break;
        float angle = 6.2831853 * (float(sampleIndex) + 0.5) / float(samples);
        vec2 sampleOffset = vec2(cos(angle), sin(angle)) / GuiSize * (float(samples) / 12.0);
        background += texture(Sampler0, clamp(refractedUv + sampleOffset, vec2(0.001), vec2(0.999))).rgb;
    }
    background /= float(samples);
    float luminance = dot(background, vec3(0.2126, 0.7152, 0.0722));

    vec3 glass = mix(background, Tint, TintIntensity);
    float vertical = clamp((screenPosition.y - Rect.y) / max(1.0, Rect.w), 0.0, 1.0);
    glass += (0.035 - vertical * 0.055);

    float edge = 1.0 - smoothstep(0.0, max(0.25, EdgeWidth + 0.75), abs(field));
    float adaptiveHighlight = mix(1.15, 0.72, luminance) * mix(1.0, 1.35, AdaptBrightness);
    glass += vec3(edge * EdgeHighlight * 0.20 * adaptiveHighlight);

    float innerEdge = smoothstep(-4.0, -0.1, field);
    float adaptiveShadow = mix(0.8, 1.3, luminance) * mix(1.0, 1.25, AdaptBrightness);
    glass -= vec3(innerEdge * InnerShadow * 0.14 * adaptiveShadow);

    vec2 local = (screenPosition - Rect.xy) / max(Rect.zw, vec2(1.0));
    float highlightDistance = distance(local, HighlightPosition);
    float highlightRadius = mix(0.18, 0.85, MouseRange);
    float pointerHighlight = 1.0 - smoothstep(0.0, highlightRadius, highlightDistance);
    glass += vec3(pointerHighlight * MouseHighlight * 0.14);
    glass += randomNoise(screenPosition) * Noise * 0.07;

    float contrastAlpha = Opacity + mix(0.03, 0.10, luminance) * AdaptBrightness;
    fragColor = vec4(clamp(glass, 0.0, 1.0), clamp(contrastAlpha, 0.0, 1.0) * mask);
}
