#version 150

uniform vec4 Rect;
uniform float CornerRadius;
uniform vec3 Tint;
uniform float Alpha;
uniform float EdgeAlpha;

in vec2 screenPosition;
out vec4 fragColor;

float roundedRectangle(vec2 point, vec4 rectangle, float radius) {
    vec2 center = rectangle.xy + rectangle.zw * 0.5;
    vec2 halfSize = max(vec2(0.5), rectangle.zw * 0.5 - vec2(radius));
    vec2 corner = abs(point - center) - halfSize;
    return length(max(corner, 0.0)) + min(max(corner.x, corner.y), 0.0) - radius;
}

void main() {
    float field = roundedRectangle(screenPosition, Rect, CornerRadius);
    float mask = 1.0 - smoothstep(-0.25, 0.85, field);
    if (mask <= 0.001) discard;
    float edge = 1.0 - smoothstep(0.0, 1.15, abs(field));
    vec3 color = Tint + vec3(edge * 0.08);
    fragColor = vec4(clamp(color, 0.0, 1.0), (Alpha + edge * EdgeAlpha) * mask);
}
