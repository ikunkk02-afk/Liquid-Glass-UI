#version 150

uniform vec2 GuiSize;

in vec3 Position;
out vec2 screenPosition;

void main() {
    vec2 normalized = Position.xy / GuiSize;
    gl_Position = vec4(normalized.x * 2.0 - 1.0, 1.0 - normalized.y * 2.0, 0.0, 1.0);
    screenPosition = Position.xy;
}
