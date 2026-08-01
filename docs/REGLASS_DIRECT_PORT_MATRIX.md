# ReGlass Direct Port Matrix

Baseline: ReGlass `73822f89c31eb48b8a965d13c45214772b800eb7`.

Status meanings: **COPY** preserves implementation, **REMAP** changes mapped
Minecraft names/imports only, **ADAPT** preserves behavior with a narrow API
adjustment, **REWRITE-BRIDGE** replaces only version-specific rendering
infrastructure, and **SKIP** requires an explicit reason.

| Upstream path | Status | 1.21.1 disposition |
|---|---|---|
| `src/main/java/restudio/reglass/ReGlass.java` | COPY | Preserve initializer and attribution. |
| `src/main/java/restudio/reglass/client/Config.java` | COPY | Preserve upstream flag, including spelling. |
| `src/main/java/restudio/reglass/client/LiquidGlassPipelines.java` | REWRITE-BRIDGE | Facade over `legacy1211` shader/pipeline bridge. |
| `src/main/java/restudio/reglass/client/LiquidGlassPrecomputeRuntime.java` | REWRITE-BRIDGE | Facade over RenderTarget Gaussian runtime. |
| `src/main/java/restudio/reglass/client/LiquidGlassUniforms.java` | REWRITE-BRIDGE | Preserve data packing through OpenGL UBO uploader. |
| `src/main/java/restudio/reglass/client/LiquidGlassWidget.java` | REMAP | Mojang GUI component names and narration signature. |
| `src/main/java/restudio/reglass/client/ReGlassClient.java` | REMAP | Mojang key/screen names; behavior retained. |
| `src/main/java/restudio/reglass/client/ReGlassOptOut.java` | COPY | Marker interface unchanged. |
| `src/main/java/restudio/reglass/client/api/ReGlassApi.java` | ADAPT | Preserve Builder; submit through 1.21.1 collector. |
| `src/main/java/restudio/reglass/client/api/ReGlassConfig.java` | COPY | Preserve every upstream default and feature. |
| `src/main/java/restudio/reglass/client/api/WidgetStyle.java` | COPY | Preserve the complete optical parameter API. |
| `src/main/java/restudio/reglass/client/api/model/RimLight.java` | COPY | Record unchanged. |
| `src/main/java/restudio/reglass/client/config/ReGlassSettingsIO.java` | COPY | Preserve `reglass.json` semantics. |
| `src/main/java/restudio/reglass/client/gui/LiquidGlassGuiElementRenderState.java` | REWRITE-BRIDGE | Replaced by immutable legacy collector element. |
| `src/main/java/restudio/reglass/client/gui/LiquidGlassGuiElementRenderer.java` | REWRITE-BRIDGE | 1.21.1 has no SpecialGuiElementRenderer. |
| `src/main/java/restudio/reglass/client/runtime/ReGlassAnim.java` | COPY | Preserve interpolation and timing constants. |
| `src/main/java/restudio/reglass/client/screen/config/ReGlassConfigScreen.java` | REMAP | Mojang widgets and drawing names only. |
| `src/main/java/restudio/reglass/client/screen/widget/ClickableEntryWidget.java` | REMAP | Mojang widget/narration names only. |
| `src/main/java/restudio/reglass/client/screen/widget/ScrollableListWidget.java` | ADAPT | Preserve scrolling behavior on 1.21.1 input API. |
| `src/main/java/restudio/reglass/client/screen/widget/world/WorldListEntryWidget.java` | ADAPT | Compile against 1.21.1; redesign remains disabled. |
| `src/main/java/restudio/reglass/client/screen/world/CustomWorldSelectScreen.java` | ADAPT | Compile against 1.21.1; upstream WIP gate retained. |
| `src/main/java/restudio/reglass/client/ui/MappedSlider.java` | REMAP | Mojang slider names only. |
| `src/main/java/restudio/reglass/mixin/accessor/DrawContextAccessor.java` | REWRITE-BRIDGE | Scissor captured by Legacy1211GuiHook. |
| `src/main/java/restudio/reglass/mixin/accessor/GuiRenderStateAccessor.java` | REWRITE-BRIDGE | No GuiRenderState in 1.21.1. |
| `src/main/java/restudio/reglass/mixin/accessor/GuiRendererAccessor.java` | REWRITE-BRIDGE | No GuiRenderer special-element path in 1.21.1. |
| `src/main/java/restudio/reglass/mixin/accessor/SliderWidgetAccessor.java` | REMAP | Access 1.21.1 slider value. |
| `src/main/java/restudio/reglass/mixin/accessor/TextIconButtonWidgetAccessor.java` | SKIP | Unused upstream accessor with no parity behavior. |
| `src/main/java/restudio/reglass/mixin/client/GuiRendererMixin.java` | REWRITE-BRIDGE | Replaced by Legacy1211GuiHook. |
| `src/main/java/restudio/reglass/mixin/client/InGameHudMixin.java` | ADAPT | Preserve hotbar behavior after Playground gate. |
| `src/main/java/restudio/reglass/mixin/client/MinecraftClientMixin.java` | ADAPT | Preserve upstream disabled WIP condition. |
| `src/main/java/restudio/reglass/mixin/client/ScreenMixin.java` | REWRITE-BRIDGE | Recreate background/composite ordering in 1.21.1. |
| `src/main/java/restudio/reglass/mixin/client/TitleScreenMixin.java` | ADAPT | Apply only after Playground parity gate. |
| `src/main/java/restudio/reglass/mixin/logical/GameRendererMixin.java` | REWRITE-BRIDGE | Own frame begin/final composite timing. |
| `src/main/java/restudio/reglass/mixin/widgets/DrawContextMixin.java` | REWRITE-BRIDGE | Replace sprite interception with Mojang GUI hook. |
| `src/main/java/restudio/reglass/mixin/widgets/SliderWidgetMixin.java` | ADAPT | Preserve slider glass/knob/text semantics. |
| `src/main/java/restudio/reglass/mixin/widgets/TooltipStateMixin.java` | SKIP | All executable injections are commented upstream. |
| `src/main/resources/assets/reglass/icon.png` | SKIP | Upstream logo/icon is explicitly not copied. |
| `src/main/resources/assets/reglass/shaders/core/blit_fullscreen.vsh` | COPY | Formula unchanged; attribution header added. |
| `src/main/resources/assets/reglass/shaders/post/liquid_glass_ingame.json` | COPY | Dormant upstream resource retained, not registered. |
| `src/main/resources/assets/reglass/shaders/post/liquid_glass_pre.json` | COPY | Dormant upstream resource retained, not registered. |
| `src/main/resources/assets/reglass/shaders/program/bg.fsh` | COPY | Formula unchanged. |
| `src/main/resources/assets/reglass/shaders/program/bloom.fsh` | COPY | Formula unchanged. |
| `src/main/resources/assets/reglass/shaders/program/blur.fsh` | COPY | Full Gaussian formula unchanged. |
| `src/main/resources/assets/reglass/shaders/program/liquid_glass_gui.fsh` | COPY | Sole visual reference; math unchanged. |
| `src/main/resources/fabric.mod.json` | ADAPT | Keep Liquid Glass UI identity and target dependencies. |
| `src/main/resources/reglass.accesswidener` | SKIP | Upstream file is empty. |
| `src/main/resources/reglass.mixins.json` | ADAPT | Register only mapped/bridge mixins that own behavior. |

## Repository-level files

Upstream Gradle files, wrapper, workflow, README, `.gitignore`, and Git history
are not copied because the target Fabric 1.21.1 template owns its build and
repository history. The exact upstream build metadata is recorded in
`REGLASS_UPSTREAM.md`; the upstream MIT license is copied verbatim.

The pinned upstream contains no language resources. Liquid Glass UI English
and Chinese translations added later belong to the extension layer, not to
ReGlass.
