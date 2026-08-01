# ReGlass Legacy Port for Fabric 1.21.1

This document records the source audit that gates implementation of the ReGlass-derived renderer. The reference checkout is intentionally outside this repository and pinned to ReGlass commit `73822f89c31eb48b8a965d13c45214772b800eb7` (ReGlass 1.1.0).

## License boundary

ReGlass is distributed under the MIT License, Copyright (c) 2025 ReStudio By RedxAx. The complete upstream text is preserved at `licenses/ReGlass-LICENSE.txt`; derivative files carry the required attribution header and are enumerated in `THIRD_PARTY_NOTICES.md` as they are introduced.

No ReGlass logo, screenshot, icon, font, sound, or other artwork is copied. The reference repository, its `.git` directory, build output, and visual-reference media remain outside Liquid Glass UI and are never packaged in the mod.

## 1. Source-to-port mapping

| ReGlass 1.21.8 source | ReGlass Legacy Port counterpart | Disposition |
|---|---|---|
| `client/api/ReGlassApi.java` | `reglassport/api/ReGlassPortApi.java` | Retain the fluent builder and deferred submission contract; translate `DrawContext`/`ClickableWidget` to Mojang-mapped `GuiGraphics`/`AbstractWidget`. |
| `client/api/ReGlassConfig.java` | `reglassport/api/GlassPortConfig.java` | Retain ReGlass parameter names and defaults; omit redesign features outside the Playground core. |
| `client/api/WidgetStyle.java` | `reglassport/api/GlassStyle.java`, `GlassOptics.java` | Retain optional fluent overrides and resolve them against `GlassPortConfig`; do not collapse the optics into legacy project names. |
| `client/LiquidGlassPipelines.java` | `reglassport/render/ReGlassShaderManager.java` | Rewrite around 1.21.1 `ShaderInstance` and Fabric core-shader registration. |
| `client/LiquidGlassUniforms.java` | `ReGlassFrameCollector.java`, `ReGlassUniformData.java` | Preserve 64 widgets, five blur levels, GUI-to-framebuffer conversion, per-widget optics, scissor, hover, and focus; replace UBOs with one `64 x 12` RGBA32F data texture. |
| `client/LiquidGlassPrecomputeRuntime.java` | `ReGlassBlurRuntime.java`, `ReGlassFramebufferManager.java` | Preserve one separable Gaussian blur per requested radius; rewrite GPU resource management with 1.21.1 `TextureTarget`. |
| `client/LiquidGlassWidget.java` | `reglassport/widget/LiquidGlassPortWidget.java` | Retain draggable widget behavior and narration-safe vanilla widget participation; use stable IDs and smoothed drag targets. |
| `gui/LiquidGlassGuiElementRenderState.java` | immutable records owned by `ReGlassFrameCollector` | The 1.21.8 special GUI render-state API has no 1.21.1 equivalent. |
| `gui/LiquidGlassGuiElementRenderer.java` | `ReGlassFrameCollector` plus the Screen render boundary | Replace special-element extraction with explicit per-frame registration. |
| `runtime/ReGlassAnim.java` | `ReGlassAnimationRuntime.java`, `ReGlassSpringState.java` | Preserve real-time parameter smoothing; use bounded `System.nanoTime()` delta and stable widget identity. |
| `mixin/logical/GameRendererMixin.java` | `reglassport/mixin/ReGlassPortScreenMixin.java` | Replace the 1.21.8 device render pass with capture-after-background and composite-after-widget collection. |
| `mixin/client/ScreenMixin.java` | `ReGlassPortScreenMixin.java` | Route background capture without applying whole-screen blur or darkening cancellation. |
| `mixin/widgets/SliderWidgetMixin.java` | no phase-1 counterpart | Studied for deferred text ordering; slider replacement is outside the accepted scope. |
| `shaders/core/blit_fullscreen.vsh` | `shaders/core/reglass_port/blit_fullscreen.vsh` | Direct GLSL 150 adaptation under the Liquid Glass UI namespace. |
| `shaders/program/liquid_glass_gui.fsh` | `shaders/core/reglass_port/liquid_glass_gui.fsh` | Adapt the complete SDF/optics core to data-texture decoding and group-aware fusion. |
| `shaders/program/blur.fsh` | `shaders/core/reglass_port/blur.fsh` | Adapt the normalized separable Gaussian shader to ordinary uniforms. |
| `shaders/program/bg.fsh` | folded into the unified composite pass | Preserve soft per-widget shadow math without a redundant whole-screen pass. |

## 2. GLSL that can be adapted directly

The GLSL 150 mathematical implementations of `SDFResult`, `screenToUV`, `sdgBox`, `opSmoothUnion`, `opHardUnion`, `opHardSubtract`, `fieldWidgets`, Gaussian sampling, edge-normal refraction, chromatic dispersion, Fresnel response, glare, shadow, hover enhancement, and focus enhancement are suitable for direct adaptation.

Changes are limited to sampler names, uniform declarations, component decoding, group isolation, explicit capture sampling, output/discard behavior, and the debug-mode selector. The port must not replace these operations with transparent rectangles, four-corner averaging, bridge geometry, or a white-gradient highlight.

## 3. Java that can be reused after package/type translation

- The fluent builder shape and input clamping from `ReGlassApi`.
- ReGlass configuration defaults and the optional-override behavior of `WidgetStyle`.
- The draggable widget input contract from `LiquidGlassWidget`.
- Exponential real-time interpolation and Gaussian-kernel generation.
- Per-frame reset, unique-radius selection, 64-widget limit, and five-blur-level limit from `LiquidGlassUniforms`.

All reused portions remain attributed. Mojang-mapped names replace Yarn names, and no compatibility reflection is introduced.

## 4. Rendering code that must be rewritten for 1.21.1

The complete resource submission layer is version-specific and must be rewritten: shader registration and reload, framebuffer ownership, main-target capture, blur target allocation, float data-texture upload, sampler binding, fullscreen drawing, render-state restoration, resize/fullscreen recovery, and failure fallback. The 1.21.8 special GUI element and device abstractions cannot be copied.

## 5. Minecraft API differences

| ReGlass / Minecraft 1.21.8 Yarn | Fabric 1.21.1 Mojang mappings |
|---|---|
| `MinecraftClient` | `Minecraft` |
| `DrawContext` | `GuiGraphics` |
| `ClickableWidget` | `AbstractWidget` / `AbstractButton` |
| `Matrix3x2f` GUI pose | `PoseStack` / `Matrix4f` |
| `Framebuffer` attachment views | `RenderTarget` / `TextureTarget` texture IDs |
| `RenderPipeline`, `RenderPass` | `ShaderInstance`, `RenderSystem`, `Tesselator`, `BufferUploader` |
| `GuiRenderState`, `SpecialGuiElementRenderer` | no equivalent; explicit Screen-boundary collection |
| `GpuBuffer`, std140 UBO mapping | fixed uniforms plus `64 x 12` RGBA32F data texture |
| `GpuTexture`, `GpuTextureView` | OpenGL texture IDs owned by `TextureTarget` or the data-texture service |
| command encoder | render-thread calls guarded and restored through 1.21.1 RenderSystem/OpenGL state |

`ShaderInstance` loads mod shaders from `assets/<namespace>/shaders/core/<path>`, so the runtime-correct location is `assets/liquid_glass_ui/shaders/core/reglass_port/`.

## 6. Port sequence

1. Preserve and push the last working legacy renderer, create and push `feat/reglass-backport-1.21.1`, then pin the external reference checkout.
2. Land this license/documentation gate and the independent package skeleton.
3. Add the deferred API, stable widget records, H key, and a single-widget Playground proof.
4. Add capture, separable blur, complete ReGlass optical compositing, debug modes, resource lifecycle, and automatic failure fallback.
5. Add group-aware smooth union, bounded real-time animation, full draggable Playground, right-click creation, focused YACL mapping, and visual evidence.
6. Stop for explicit visual acceptance. Only after acceptance may menu widgets be routed to the port renderer.

## 7. Attribution and notices

`licenses/ReGlass-LICENSE.txt` is shipped unchanged. `THIRD_PARTY_NOTICES.md` identifies ReGlass, its author, repository, license, pinned revision, derivative status, and the exact adapted files. Substantially adapted Java and GLSL files begin with:

```text
/*
 * Portions adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Adapted for Minecraft Fabric 1.21.1.
 */
```
