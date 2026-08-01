package io.github.ikunkk02afk.liquidglassui.config;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import io.github.ikunkk02afk.liquidglassui.LiquidGlassUIClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LiquidGlassConfigScreen {
    private LiquidGlassConfigScreen() {
    }

    public static Screen create(Screen parent) {
        LiquidGlassConfigManager manager = LiquidGlassUIClient.configManager();
        LiquidGlassConfigData working = manager.get();
        GlassQualityPreset originalPreset = working.performance.preset;

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("liquid_glass_ui.config.title"));
        if (GlassDebugPolicy.enabled()) builder.category(developer(working));
        builder.category(appearance(working))
                .category(optics(working))
                .category(animation(working))
                .category(interfaceOptions(working))
                .category(performance(parent, working, manager))
                .save(() -> save(parent, working, originalPreset, manager));
        return builder.build().generateScreen(parent);
    }

    private static void save(Screen parent, LiquidGlassConfigData working, GlassQualityPreset originalPreset,
                             LiquidGlassConfigManager manager) {
        if (working.performance.preset == GlassQualityPreset.HIGH
                && originalPreset != GlassQualityPreset.HIGH
                && HighQualityPolicy.requiresWarning(working, GlassQualityPreset.HIGH)) {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(
                    new HighQualityWarningScreen(parent, () -> {
                        HighQualityPolicy.acknowledgeAndSelect(working);
                        manager.replaceAndSave(working);
                    })));
            return;
        }
        manager.replaceAndSave(working);
    }

    private static ConfigCategory appearance(LiquidGlassConfigData data) {
        LiquidGlassConfigData defaults = LiquidGlassConfigData.defaults();
        return ConfigCategory.createBuilder().name(tr("category.appearance"))
                .option(bool("appearance.enabled", defaults.appearance.enabled, () -> data.appearance.enabled, value -> data.appearance.enabled = value))
                .option(color("appearance.main_color", defaults.appearance.mainColor, () -> data.appearance.mainColor, value -> data.appearance.mainColor = value))
                .option(decimal("appearance.opacity", defaults.appearance.opacity, 0, 1, 0.01f, () -> data.appearance.opacity, value -> data.appearance.opacity = value))
                .option(decimal("appearance.tint_intensity", defaults.appearance.tintIntensity, 0, 1, 0.01f, () -> data.appearance.tintIntensity, value -> data.appearance.tintIntensity = value))
                .option(decimal("appearance.corner_radius", defaults.appearance.cornerRadius, 2, 10, 0.5f, () -> data.appearance.cornerRadius, value -> data.appearance.cornerRadius = value))
                .option(decimal("appearance.edge_width", defaults.appearance.edgeWidth, 0, 3, 0.05f, () -> data.appearance.edgeWidth, value -> data.appearance.edgeWidth = value))
                .option(decimal("appearance.edge_highlight", defaults.appearance.edgeHighlightIntensity, 0, 1, 0.01f, () -> data.appearance.edgeHighlightIntensity, value -> data.appearance.edgeHighlightIntensity = value))
                .option(decimal("appearance.inner_shadow", defaults.appearance.innerShadowIntensity, 0, 1, 0.01f, () -> data.appearance.innerShadowIntensity, value -> data.appearance.innerShadowIntensity = value))
                .option(bool("appearance.adapt_brightness", defaults.appearance.adaptToBackgroundBrightness, () -> data.appearance.adaptToBackgroundBrightness, value -> data.appearance.adaptToBackgroundBrightness = value))
                .build();
    }

    private static ConfigCategory optics(LiquidGlassConfigData data) {
        LiquidGlassConfigData defaults = LiquidGlassConfigData.defaults();
        return ConfigCategory.createBuilder().name(tr("category.optics"))
                .option(decimal("optics.blur_intensity", defaults.optics.blurIntensity, 0, 1, 0.01f, () -> data.optics.blurIntensity, value -> data.optics.blurIntensity = value))
                .option(decimal("optics.blur_radius", defaults.optics.blurRadius, 0, 16, 0.5f, () -> data.optics.blurRadius, value -> data.optics.blurRadius = value))
                .option(decimal("optics.refraction_intensity", defaults.optics.refractionIntensity, 0, 1, 0.01f, () -> data.optics.refractionIntensity, value -> data.optics.refractionIntensity = value))
                .option(decimal("optics.edge_refraction_range", defaults.optics.edgeRefractionRange, 0, 1, 0.01f, () -> data.optics.edgeRefractionRange, value -> data.optics.edgeRefractionRange = value))
                .option(decimal("optics.mouse_highlight", defaults.optics.mouseHighlightIntensity, 0, 1, 0.01f, () -> data.optics.mouseHighlightIntensity, value -> data.optics.mouseHighlightIntensity = value))
                .option(decimal("optics.mouse_highlight_range", defaults.optics.mouseHighlightRange, 0, 1, 0.01f, () -> data.optics.mouseHighlightRange, value -> data.optics.mouseHighlightRange = value))
                .option(decimal("optics.surface_noise", defaults.optics.surfaceNoiseIntensity, 0, 1, 0.005f, () -> data.optics.surfaceNoiseIntensity, value -> data.optics.surfaceNoiseIntensity = value))
                .option(decimal("optics.glass_thickness", defaults.optics.glassThickness, 0.5f, 16, 0.5f, () -> data.optics.glassThickness, value -> data.optics.glassThickness = value))
                .option(decimal("optics.fresnel_strength", defaults.optics.fresnelStrength, 0, 1, 0.01f, () -> data.optics.fresnelStrength, value -> data.optics.fresnelStrength = value))
                .option(decimal("optics.dispersion_strength", defaults.optics.dispersionStrength, 0, 2, 0.01f, () -> data.optics.dispersionStrength, value -> data.optics.dispersionStrength = value))
                .option(decimal("optics.shadow_strength", defaults.optics.shadowStrength, 0, 1, 0.01f, () -> data.optics.shadowStrength, value -> data.optics.shadowStrength = value))
                .option(decimal("optics.background_clarity", defaults.optics.backgroundClarity, 0, 1, 0.01f, () -> data.optics.backgroundClarity, value -> data.optics.backgroundClarity = value))
                .build();
    }

    private static ConfigCategory animation(LiquidGlassConfigData data) {
        LiquidGlassConfigData defaults = LiquidGlassConfigData.defaults();
        return ConfigCategory.createBuilder().name(tr("category.animation"))
                .option(bool("animation.enabled", defaults.animation.enabled, () -> data.animation.enabled, value -> data.animation.enabled = value))
                .option(decimal("animation.speed", defaults.animation.speed, 0.25f, 2.5f, 0.05f, () -> data.animation.speed, value -> data.animation.speed = value))
                .option(decimal("animation.spring_stiffness", defaults.animation.springStiffness, 20, 500, 5, () -> data.animation.springStiffness, value -> data.animation.springStiffness = value))
                .option(decimal("animation.damping", defaults.animation.damping, 4, 80, 1, () -> data.animation.damping, value -> data.animation.damping = value))
                .option(decimal("animation.hover_scale", defaults.animation.hoverScale, 1, 1.08f, 0.001f, () -> data.animation.hoverScale, value -> data.animation.hoverScale = value))
                .option(decimal("animation.pressed_scale", defaults.animation.pressedScale, 0.90f, 1, 0.001f, () -> data.animation.pressedScale, value -> data.animation.pressedScale = value))
                .option(decimal("animation.merge_strength", defaults.animation.mergeStrength, 0, 1, 0.01f, () -> data.animation.mergeStrength, value -> data.animation.mergeStrength = value))
                .option(bool("animation.mouse_follow", defaults.animation.mouseFollow, () -> data.animation.mouseFollow, value -> data.animation.mouseFollow = value))
                .option(bool("animation.reduce_motion", defaults.animation.reduceMotion, () -> data.animation.reduceMotion, value -> data.animation.reduceMotion = value))
                .build();
    }

    private static ConfigCategory interfaceOptions(LiquidGlassConfigData data) {
        LiquidGlassConfigData defaults = LiquidGlassConfigData.defaults();
        return ConfigCategory.createBuilder().name(tr("category.interface"))
                .option(bool("interface.main_menu", defaults.ui.mainMenu, () -> data.ui.mainMenu, value -> data.ui.mainMenu = value))
                .option(bool("interface.pause_menu", defaults.ui.pauseMenu, () -> data.ui.pauseMenu, value -> data.ui.pauseMenu = value))
                .option(bool("interface.confirm_dialogs", defaults.ui.confirmDialogs, () -> data.ui.confirmDialogs, value -> data.ui.confirmDialogs = value))
                .option(bool("interface.replace_common", defaults.ui.replaceCommonButtons, () -> data.ui.replaceCommonButtons, value -> data.ui.replaceCommonButtons = value))
                .build();
    }

    private static ConfigCategory performance(Screen parent, LiquidGlassConfigData data, LiquidGlassConfigManager manager) {
        LiquidGlassConfigData defaults = LiquidGlassConfigData.defaults();
        ConfigCategory.Builder category = ConfigCategory.createBuilder().name(tr("category.performance"));
        if (LiquidGlassUIClient.renderBackend().status().safeMode()) {
            category.option(LabelOption.create(tr("performance.safe_mode_active")));
        }
        return category
                .option(enumeration("performance.preset", GlassQualityPreset.class, defaults.performance.preset,
                        () -> data.performance.preset, value -> data.performance.preset = value, "quality"))
                .option(decimal("performance.custom_buffer_scale", defaults.performance.customBufferScale, 0.25f, 1, 0.25f,
                        () -> data.performance.customBufferScale, value -> data.performance.customBufferScale = value))
                .option(integer("performance.custom_blur_passes", defaults.performance.customBlurPasses, 1, 5, 1,
                        () -> data.performance.customBlurPasses, value -> data.performance.customBlurPasses = value))
                .option(enumeration("performance.custom_refraction", GlassRefractionQuality.class, defaults.performance.customRefractionQuality,
                        () -> data.performance.customRefractionQuality, value -> data.performance.customRefractionQuality = value, "refraction"))
                .option(integer("performance.custom_samples", defaults.performance.customSampleCount, 2, 12, 1,
                        () -> data.performance.customSampleCount, value -> data.performance.customSampleCount = value))
                .option(ButtonOption.createBuilder().name(tr("performance.reset"))
                        .text(tr("performance.reset_button"))
                        .description(description("performance.reset"))
                        .action(screen -> {
                            manager.reset();
                            Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(create(parent)));
                        }).build())
                .build();
    }

    private static ConfigCategory developer(LiquidGlassConfigData data) {
        LiquidGlassConfigData defaults = LiquidGlassConfigData.defaults();
        ConfigCategory.Builder category = ConfigCategory.createBuilder().name(tr("category.developer"));
        if (data.performance.debugView != GlassDebugView.OFF) {
            category.option(LabelOption.create(tr("developer.debug_warning").copy()
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        }
        return category
                .option(enumeration("performance.debug_view", GlassDebugView.class, defaults.performance.debugView,
                        () -> data.performance.debugView, value -> data.performance.debugView = value, "debug_view"))
                .build();
    }

    private static Option<Boolean> bool(String key, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder().name(tr(key)).description(description(key))
                .binding(defaultValue, getter, setter).controller(BooleanControllerBuilder::create).build();
    }

    private static Option<Float> decimal(String key, float defaultValue, float min, float max, float step,
                                         Supplier<Float> getter, Consumer<Float> setter) {
        return Option.<Float>createBuilder().name(tr(key)).description(description(key))
                .binding(defaultValue, getter, setter)
                .controller(option -> FloatSliderControllerBuilder.create(option).range(min, max).step(step)).build();
    }

    private static Option<Integer> integer(String key, int defaultValue, int min, int max, int step,
                                           Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder().name(tr(key)).description(description(key))
                .binding(defaultValue, getter, setter)
                .controller(option -> IntegerSliderControllerBuilder.create(option).range(min, max).step(step)).build();
    }

    private static Option<Color> color(String key, String defaultValue, Supplier<String> getter, Consumer<String> setter) {
        return Option.<Color>createBuilder().name(tr(key)).description(description(key))
                .binding(Color.decode(defaultValue), () -> Color.decode(getter.get()), color -> setter.accept(String.format("#%06X", color.getRGB() & 0xFFFFFF)))
                .controller(option -> ColorControllerBuilder.create(option).allowAlpha(false)).build();
    }

    private static <T extends Enum<T>> Option<T> enumeration(String key, Class<T> enumClass, T defaultValue,
                                                              Supplier<T> getter, Consumer<T> setter, String translationGroup) {
        return Option.<T>createBuilder(enumClass).name(tr(key)).description(description(key))
                .binding(defaultValue, getter, setter)
                .controller(option -> EnumControllerBuilder.create(option).enumClass(enumClass)
                        .valueFormatter(value -> tr(translationGroup + "." + value.name().toLowerCase(Locale.ROOT))))
                .build();
    }

    private static Component tr(String suffix) {
        return Component.translatable("liquid_glass_ui.config." + suffix);
    }

    private static OptionDescription description(String suffix) {
        return OptionDescription.of(Component.translatable("liquid_glass_ui.config." + suffix + ".description"));
    }
}
