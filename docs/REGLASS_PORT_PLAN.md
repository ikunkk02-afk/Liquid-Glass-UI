# ReGlass to Fabric 1.21.1 Port Study

This document records the design study completed before any ReGlass-derived implementation was added to Liquid Glass UI. The reviewed revisions are:

- ReGlass `73822f89c31eb48b8a965d13c45214772b800eb7`
- Blur+ `3507b81754ed93ec744ca5e45d1146b51081fc9f`

## License boundary

ReGlass is MIT licensed by ReStudio By RedxAx. Liquid Glass UI may adapt substantial shader mathematics and the component-collection design when it keeps the original license and attribution. The full license is stored in `licenses/ReGlass-LICENSE.txt`; derived files carry a short attribution header and are enumerated in `THIRD_PARTY_NOTICES.md`.

Blur+ is also MIT licensed. It was inspected only as an implementation reference for the Minecraft 1.21.1 screen-blur lifecycle. No Blur+ source or shader is copied into Liquid Glass UI, so the project does not include a Blur+-derived implementation or redistribute its assets.

No ReGlass screenshot, logo, icon, sound, font, or other art asset is used. ReGlass and Blur+ are not runtime dependencies, Git submodules, or bundled JARs.

## ReGlass modules and disposition

| ReGlass source | Useful design | 1.21.8 dependency | Fabric 1.21.1 disposition |
|---|---|---|---|
| `ReGlassApi` | Widget builder collects geometry, state, style, pose, and scissor instead of drawing immediately | `DrawContext.state.addSpecialElement` | Replace with `GlassFrameCollector`; menu adapters pre-register known buttons and the widget Mixin finalizes pose/scissor while suppressing only their vanilla surface |
| `WidgetStyle` | Per-widget tint, blur, shadow, refraction, Fresnel, glare, hover and focus values | None conceptually | Adapt as version-independent `GlassMaterial` and `GlassOptics`; expose only user-facing controls through YACL |
| `LiquidGlassPipelines` | One fullscreen pipeline with explicit samplers and uniform blocks | `RenderPipeline`, `GpuBuffer`, `RenderSystem.getDevice()` | Rewrite with 1.21.1 `ShaderInstance`, `CoreShaderRegistrationCallback`, `RenderTarget`, `TextureTarget`, and explicit RenderSystem bindings |
| `LiquidGlassUniforms` | Fixed 64-widget budget, frame reset, GUI-to-framebuffer conversion, blur-level selection | std140 UBOs and `GpuBuffer` mapping | Rewrite as a reusable `64 x 12` RGBA32F data texture uploaded once per frame; perform the only Y flip while packing physical rectangles |
| `LiquidGlassPrecomputeRuntime` | Shared blur targets computed once for all widgets | `GpuTexture`, `GpuTextureView`, `RenderPass`, command encoder | Rewrite as full-size capture plus scaled 1.21.1 `TextureTarget` ping-pong blur targets |
| `LiquidGlassGuiElementRenderState` / renderer | GUI extraction records elements and the renderer only adds them to the frame list | `GuiRenderState`, `SpecialGuiElementRenderer` | Rewrite with `ScreenEvents`, an `AbstractWidget.renderWidget` wrapper, and a deferred button-content queue |
| `GameRendererMixin` | One render pass binds the original background and shared blur textures, then draws one fullscreen quad | modern render pass/device API | Rewrite at the target `Screen.render` boundary: capture at HEAD, composite at RETURN, then draw deferred text; no GameRenderer-wide HUD replacement |
| `ReGlassAnim` | Real-frame-time animation | 1.21.8 tick-counter calls | Do not copy; retain Liquid Glass UI's bounded `System.nanoTime()` clock and damped spring integrator |
| `blit_fullscreen.vsh` | Normalized fullscreen triangle/quad mapping | Modern pipeline declaration | Reimplement a project-namespaced GLSL 150 fullscreen vertex shader for `ShaderInstance` |
| `liquid_glass_gui.fsh` | Rounded-box SDF, analytic normal, hard/smooth boolean operations, field evaluation, refraction, dispersion, Fresnel, glare, shadow | UBO layouts and six fixed blur samplers | Adapt the mathematical core, remove unrelated pixel-grid/focus-sweep/Bloom/HUD behavior, add group-aware fusion and data-texture decoding |
| `blur.fsh` | Shared separable blur concept | Modern pipeline resources | Do not copy; implement a compact project-owned Kawase pass normalized to the actual target size |

## API mapping

| Minecraft 1.21.8 / ReGlass | Minecraft 1.21.1 Mojang mappings |
|---|---|
| `MinecraftClient` | `Minecraft` |
| `DrawContext` | `GuiGraphics` |
| `GuiRenderState` / special elements | No equivalent; use target-screen collection and deferred widget contents |
| `RenderPipeline` | `ShaderInstance` registered by Fabric's core shader callback |
| `RenderPass` | Bind a `RenderTarget`, set viewport/state, draw through `Tesselator`/`BufferUploader` |
| `GpuBuffer` / std140 widget UBO | RGBA32F data texture with `texelFetch` |
| `GpuTexture` / `GpuTextureView` | `RenderTarget` / `TextureTarget` color texture IDs |
| `RenderSystem.getDevice()` command encoder | 1.21.1 RenderSystem plus narrowly contained LWJGL calls in the legacy backend |
| `Matrix3x2f` GUI pose | `PoseStack` / `Matrix4f`; transform corners before packing |

The 1.21.8 Java renderer cannot be copied into 1.21.1: the render-state extraction system, special GUI element renderer, pipeline builder, device abstraction, command encoder, GPU buffers and texture views do not exist in the target runtime. Reflection or compatibility stubs would be fragile and would defeat the planned version-backend boundary.

## Target frame flow

1. Before the target screen renders, reset a fixed-capacity collector and update all spring states.
2. Pre-register visible supported buttons with logical geometry, group and material IDs.
3. After the title panorama or rendered world is present, capture the main color target once before any widget, font or tooltip.
4. Downsample once and compute one light and one final shared blur level for the selected quality profile.
5. During normal widget traversal, finalize pose/scissor and defer the registered button surface/text; unsupported widgets render normally.
6. At `Screen.render` return, upload all widget records once and execute one fullscreen glass composite pass. Pixels outside the glass/shadow field are discarded.
7. Draw deferred button text/content in original order. Later title, pause, confirmation and tooltip rendering remains vanilla.

The compositor explicitly binds raw capture, light blur, final blur and widget data on every invocation. Font or other GUI texture bindings are never treated as implicit input.

## Blur+ findings

Blur+'s multiversion source shows that its 1.21.1 path calls `GameRenderer.processBlurEffect(...)` and rebinds `Minecraft.getMainRenderTarget()` afterwards. Its screen mixins determine whether a screen requested a blurred background, coordinate fade animations, handle title screens specially and account for multiple render calls.

Liquid Glass UI reuses only these lifecycle lessons:

- always restore/rebind the main target after off-screen work;
- make blur invocation idempotent within one frame;
- reset frame state at a stable render boundary;
- recreate resources after resize/resource reload;
- keep screen eligibility explicit.

Liquid Glass UI does not call Blur+'s renderer and does not reproduce its full-screen blur, gradients, screen allow/deny lists, configuration, animations or mixins. The vanilla full-screen menu blur is cancelled for an enabled target screen; only the glass SDF samples the project's private blur texture.

## Files replaced by the port

The old one-widget-at-a-time `LegacyGlassRenderBackend`, `GlassButtonRenderer`, `GlassSurface` draw path and `glass_surface` shader are replaced by the frame collector, legacy framebuffer/shader/blur/composite services, deferred content renderer and unified composite shaders. The existing configuration manager, animation clock/springs, failure latch and target-screen registration are retained and adapted rather than discarded.

