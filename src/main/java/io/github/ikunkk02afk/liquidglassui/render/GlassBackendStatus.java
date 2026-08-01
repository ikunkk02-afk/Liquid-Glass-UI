package io.github.ikunkk02afk.liquidglassui.render;

public record GlassBackendStatus(boolean safeMode, String reason) {
    public static GlassBackendStatus ready() { return new GlassBackendStatus(false, ""); }
    public static GlassBackendStatus degraded(String reason) { return new GlassBackendStatus(true, reason == null ? "" : reason); }
}
