#version 150

uniform vec2 GuiSize;
uniform vec2 FramebufferSize;
uniform vec2 GuiToFramebufferScale;

in vec3 Position;

out vec2 screenPosition;
out vec2 backgroundUv;

void main() {
    vec2 normalized = Position.xy / GuiSize;
    gl_Position = vec4(normalized.x * 2.0 - 1.0, 1.0 - normalized.y * 2.0, 0.0, 1.0);
    screenPosition = Position.xy;
    vec2 framebufferPosition = Position.xy * GuiToFramebufferScale;
    backgroundUv = vec2(framebufferPosition.x / FramebufferSize.x,
                        1.0 - framebufferPosition.y / FramebufferSize.y);
}
