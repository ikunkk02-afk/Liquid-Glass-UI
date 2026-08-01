# Third-Party Notices

## ReGlass

- Project: ReGlass
- Author: ReStudio By RedxAx
- Original repository: https://github.com/RedxAx/ReGlass
- Reviewed revision: `73822f89c31eb48b8a965d13c45214772b800eb7`
- License: MIT (`licenses/ReGlass-LICENSE.txt`)

Liquid Glass UI contains code adapted and modified from ReGlass for Minecraft Fabric 1.21.1. ReGlass's original copyright and MIT terms are preserved.

The following files contain code adapted from ReGlass's API, widget data model, or shader implementation:

- `src/main/java/io/github/ikunkk02afk/liquidglassui/reglassport/api/GlassPortConfig.java`
- `src/main/java/io/github/ikunkk02afk/liquidglassui/reglassport/api/GlassOptics.java`
- `src/main/java/io/github/ikunkk02afk/liquidglassui/reglassport/api/GlassStyle.java`
- `src/main/java/io/github/ikunkk02afk/liquidglassui/reglassport/render/ReGlassFrameCollector.java`
- `src/main/java/io/github/ikunkk02afk/liquidglassui/reglassport/render/ReGlassUniformData.java`
- `src/client/java/io/github/ikunkk02afk/liquidglassui/reglassport/api/ReGlassPortApi.java`
- `src/client/java/io/github/ikunkk02afk/liquidglassui/reglassport/widget/LiquidGlassPortWidget.java`
- `src/client/java/io/github/ikunkk02afk/liquidglassui/reglassport/render/ReGlassCompositePass.java`
- `src/client/java/io/github/ikunkk02afk/liquidglassui/reglassport/render/ReGlassBlurRuntime.java`
- `src/main/java/io/github/ikunkk02afk/liquidglassui/reglassport/animation/ReGlassAnimationRuntime.java`
- `src/client/resources/assets/liquid_glass_ui/shaders/core/reglass_port/blit_fullscreen.vsh`
- `src/client/resources/assets/liquid_glass_ui/shaders/core/reglass_port/blur.fsh`
- `src/client/resources/assets/liquid_glass_ui/shaders/core/reglass_port/liquid_glass_gui.fsh`

The following 1.21.1 integration files are original compatibility implementations rather than copied upstream code:

- `src/client/java/io/github/ikunkk02afk/liquidglassui/reglassport/ReGlassPortClient.java`
- `src/client/java/io/github/ikunkk02afk/liquidglassui/reglassport/render/ReGlassFramebufferManager.java`
- `src/client/java/io/github/ikunkk02afk/liquidglassui/reglassport/render/ReGlassLegacyRenderer.java`
- `src/client/java/io/github/ikunkk02afk/liquidglassui/reglassport/render/ReGlassRenderStateGuard.java`
- `src/client/java/io/github/ikunkk02afk/liquidglassui/reglassport/render/ReGlassShaderManager.java`
- `src/client/java/io/github/ikunkk02afk/liquidglassui/reglassport/screen/ReGlassPlaygroundScreen.java`

No ReGlass logo, screenshot, icon, font, sound, or other visual/media asset is redistributed. ReGlass is not a runtime dependency, bundled JAR, or Git submodule.

## Blur+

Blur+ was consulted by the earlier legacy-renderer implementation only as a lifecycle reference. No Blur+ source, shader, configuration, or artwork is copied into the ReGlass Legacy Port, and the new port does not depend on it.
