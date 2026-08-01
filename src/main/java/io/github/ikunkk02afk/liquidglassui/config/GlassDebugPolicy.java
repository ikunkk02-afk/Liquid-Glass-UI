package io.github.ikunkk02afk.liquidglassui.config;

/** Keeps diagnostic render modes behind an explicit, opt-in JVM property. */
public final class GlassDebugPolicy {
    public static final String SYSTEM_PROPERTY = "liquidglass.debug";

    private GlassDebugPolicy() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean(SYSTEM_PROPERTY);
    }

    public static GlassDebugView constrain(GlassDebugView requested) {
        return enabled() && requested != null ? requested : GlassDebugView.OFF;
    }

    public static void enforce(LiquidGlassConfigData data) {
        if (data != null && data.performance != null) {
            data.performance.debugView = constrain(data.performance.debugView);
        }
    }
}
