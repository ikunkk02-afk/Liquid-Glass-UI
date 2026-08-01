package io.github.ikunkk02afk.liquidglassui.config;

import java.util.Locale;

public final class LiquidGlassConfigData {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public Appearance appearance = new Appearance();
    public Optics optics = new Optics();
    public Animation animation = new Animation();
    public Fusion fusion = new Fusion();
    public InterfaceOptions ui = new InterfaceOptions();
    public Performance performance = new Performance();
    public boolean highQualityWarningAcknowledged;

    public static LiquidGlassConfigData defaults() {
        return new LiquidGlassConfigData();
    }

    public LiquidGlassConfigData sanitizedCopy() {
        LiquidGlassConfigData copy = copy();
        copy.sanitize();
        return copy;
    }

    public LiquidGlassConfigData copy() {
        LiquidGlassConfigData copy = new LiquidGlassConfigData();
        copy.schemaVersion = schemaVersion;
        copy.appearance = appearance == null ? null : appearance.copy();
        copy.optics = optics == null ? null : optics.copy();
        copy.animation = animation == null ? null : animation.copy();
        copy.fusion = fusion == null ? null : fusion.copy();
        copy.ui = ui == null ? null : ui.copy();
        copy.performance = performance == null ? null : performance.copy();
        copy.highQualityWarningAcknowledged = highQualityWarningAcknowledged;
        return copy;
    }

    public void sanitize() {
        schemaVersion = CURRENT_SCHEMA_VERSION;
        if (appearance == null) appearance = new Appearance();
        if (optics == null) optics = new Optics();
        if (animation == null) animation = new Animation();
        if (fusion == null) fusion = new Fusion();
        if (ui == null) ui = new InterfaceOptions();
        if (performance == null) performance = new Performance();
        appearance.sanitize();
        optics.sanitize();
        animation.sanitize();
        fusion.sanitize();
        performance.sanitize();
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Appearance {
        public boolean enabled = true;
        public String mainColor = "#DDE7F2";
        public float opacity = 0.10f;
        public float tintIntensity = 0.10f;
        public float cornerRadius = 9.0f;
        public float edgeWidth = 0.65f;
        public float edgeHighlightIntensity = 0.30f;
        public float innerShadowIntensity = 0.12f;
        public boolean adaptToBackgroundBrightness = true;

        private Appearance copy() {
            Appearance copy = new Appearance();
            copy.enabled = enabled;
            copy.mainColor = mainColor;
            copy.opacity = opacity;
            copy.tintIntensity = tintIntensity;
            copy.cornerRadius = cornerRadius;
            copy.edgeWidth = edgeWidth;
            copy.edgeHighlightIntensity = edgeHighlightIntensity;
            copy.innerShadowIntensity = innerShadowIntensity;
            copy.adaptToBackgroundBrightness = adaptToBackgroundBrightness;
            return copy;
        }

        private void sanitize() {
            if (mainColor == null || !mainColor.matches("#[0-9a-fA-F]{6}")) mainColor = "#DDE7F2";
            mainColor = mainColor.toUpperCase(Locale.ROOT);
            opacity = clamp(opacity, 0.0f, 1.0f);
            tintIntensity = clamp(tintIntensity, 0.0f, 1.0f);
            cornerRadius = clamp(cornerRadius, 2.0f, 10.0f);
            edgeWidth = clamp(edgeWidth, 0.0f, 3.0f);
            edgeHighlightIntensity = clamp(edgeHighlightIntensity, 0.0f, 1.0f);
            innerShadowIntensity = clamp(innerShadowIntensity, 0.0f, 1.0f);
        }
    }

    public static final class Optics {
        public float blurIntensity = 0.70f;
        public float blurRadius = 6.0f;
        public float refractionIntensity = 0.025f;
        public float edgeRefractionRange = 0.28f;
        public float mouseHighlightIntensity = 0.20f;
        public float mouseHighlightRange = 0.78f;
        public float surfaceNoiseIntensity = 0.008f;
        public float glassThickness = 5.5f;
        public float fresnelStrength = 0.32f;
        public float dispersionStrength = 0.65f;
        public float shadowStrength = 0.18f;
        public float backgroundClarity = 0.62f;

        private Optics copy() {
            Optics copy = new Optics();
            copy.blurIntensity = blurIntensity;
            copy.blurRadius = blurRadius;
            copy.refractionIntensity = refractionIntensity;
            copy.edgeRefractionRange = edgeRefractionRange;
            copy.mouseHighlightIntensity = mouseHighlightIntensity;
            copy.mouseHighlightRange = mouseHighlightRange;
            copy.surfaceNoiseIntensity = surfaceNoiseIntensity;
            copy.glassThickness = glassThickness;
            copy.fresnelStrength = fresnelStrength;
            copy.dispersionStrength = dispersionStrength;
            copy.shadowStrength = shadowStrength;
            copy.backgroundClarity = backgroundClarity;
            return copy;
        }

        private void sanitize() {
            blurIntensity = clamp(blurIntensity, 0.0f, 1.0f);
            blurRadius = clamp(blurRadius, 0.0f, 16.0f);
            refractionIntensity = clamp(refractionIntensity, 0.0f, 1.0f);
            edgeRefractionRange = clamp(edgeRefractionRange, 0.0f, 1.0f);
            mouseHighlightIntensity = clamp(mouseHighlightIntensity, 0.0f, 1.0f);
            mouseHighlightRange = clamp(mouseHighlightRange, 0.0f, 1.0f);
            surfaceNoiseIntensity = clamp(surfaceNoiseIntensity, 0.0f, 1.0f);
            glassThickness = clamp(glassThickness, 0.5f, 16.0f);
            fresnelStrength = clamp(fresnelStrength, 0.0f, 1.0f);
            dispersionStrength = clamp(dispersionStrength, 0.0f, 2.0f);
            shadowStrength = clamp(shadowStrength, 0.0f, 1.0f);
            backgroundClarity = clamp(backgroundClarity, 0.0f, 1.0f);
        }
    }

    public static final class Fusion {
        public boolean enabled = true;
        public float distance = 32.0f;
        public float softness = 8.0f;
        public float hoverExpansion = 1.5f;
        public float connectionDurationSeconds = 0.18f;
        public boolean staticConnection;

        private Fusion copy() {
            Fusion copy = new Fusion();
            copy.enabled = enabled;
            copy.distance = distance;
            copy.softness = softness;
            copy.hoverExpansion = hoverExpansion;
            copy.connectionDurationSeconds = connectionDurationSeconds;
            copy.staticConnection = staticConnection;
            return copy;
        }

        private void sanitize() {
            distance = clamp(distance, 0.0f, 64.0f);
            softness = clamp(softness, 0.0f, 24.0f);
            hoverExpansion = clamp(hoverExpansion, 0.0f, 4.0f);
            connectionDurationSeconds = clamp(connectionDurationSeconds, 0.05f, 0.6f);
        }
    }

    public static final class Animation {
        public boolean enabled = true;
        public float speed = 1.0f;
        public float springStiffness = 220.0f;
        public float damping = 28.0f;
        public float hoverScale = 1.015f;
        public float pressedScale = 0.975f;
        public float mergeStrength = 0.55f;
        public boolean mouseFollow = true;
        public boolean reduceMotion;

        private Animation copy() {
            Animation copy = new Animation();
            copy.enabled = enabled;
            copy.speed = speed;
            copy.springStiffness = springStiffness;
            copy.damping = damping;
            copy.hoverScale = hoverScale;
            copy.pressedScale = pressedScale;
            copy.mergeStrength = mergeStrength;
            copy.mouseFollow = mouseFollow;
            copy.reduceMotion = reduceMotion;
            return copy;
        }

        private void sanitize() {
            speed = clamp(speed, 0.25f, 2.5f);
            springStiffness = clamp(springStiffness, 20.0f, 500.0f);
            damping = clamp(damping, 4.0f, 80.0f);
            hoverScale = clamp(hoverScale, 1.0f, 1.08f);
            pressedScale = clamp(pressedScale, 0.90f, 1.0f);
            mergeStrength = clamp(mergeStrength, 0.0f, 1.0f);
        }
    }

    public static final class InterfaceOptions {
        public boolean mainMenu = true;
        public boolean pauseMenu = true;
        public boolean confirmDialogs = true;
        public boolean replaceCommonButtons = true;

        private InterfaceOptions copy() {
            InterfaceOptions copy = new InterfaceOptions();
            copy.mainMenu = mainMenu;
            copy.pauseMenu = pauseMenu;
            copy.confirmDialogs = confirmDialogs;
            copy.replaceCommonButtons = replaceCommonButtons;
            return copy;
        }
    }

    public static final class Performance {
        public GlassQualityPreset preset = GlassQualityPreset.MEDIUM;
        public float customBufferScale = 0.5f;
        public int customBlurPasses = 4;
        public GlassRefractionQuality customRefractionQuality = GlassRefractionQuality.LOW;
        public int customSampleCount = 4;
        public int maxGlassComponents = 64;
        public GlassDebugView debugView = GlassDebugView.OFF;

        private Performance copy() {
            Performance copy = new Performance();
            copy.preset = preset;
            copy.customBufferScale = customBufferScale;
            copy.customBlurPasses = customBlurPasses;
            copy.customRefractionQuality = customRefractionQuality;
            copy.customSampleCount = customSampleCount;
            copy.maxGlassComponents = maxGlassComponents;
            copy.debugView = debugView;
            return copy;
        }

        private void sanitize() {
            if (preset == null) preset = GlassQualityPreset.MEDIUM;
            if (customRefractionQuality == null) customRefractionQuality = GlassRefractionQuality.LOW;
            if (debugView == null) debugView = GlassDebugView.OFF;
            float[] allowed = {0.25f, 0.5f, 0.75f, 1.0f};
            float nearest = allowed[0];
            for (float option : allowed) {
                if (Math.abs(customBufferScale - option) < Math.abs(customBufferScale - nearest)) nearest = option;
            }
            customBufferScale = nearest;
            customBlurPasses = clamp(customBlurPasses, 1, 6);
            customSampleCount = clamp(customSampleCount, 2, 12);
            maxGlassComponents = clamp(maxGlassComponents, 8, 64);
        }
    }
}
