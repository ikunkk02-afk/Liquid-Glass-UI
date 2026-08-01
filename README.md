# Liquid Glass UI

Liquid Glass UI is a pure client-side Fabric mod that gives selected vanilla menu buttons a clean, restrained liquid-glass treatment. It samples and blurs the current screen once per frame, then combines that shared background with rounded SDF masks, subtle tint, highlights, inner shading, refraction, pointer response, and procedural surface variation. It does not contain Apple artwork, fonts, logos, sounds, or other Apple resources.

## Current support

Phase one currently targets **Minecraft 1.21.1 on Fabric only**. It styles the vanilla:

- Title screen buttons
- Pause screen buttons
- Confirmation-dialog buttons

The title and pause screens include a bottom-left **Liquid Glass Settings** entry. The YACL screen can also be opened directly in code with `LiquidGlassConfigScreen.create(parent)`; Mod Menu is not required.

## Requirements and installation

Required runtime dependencies:

- Fabric Loader 0.19.3 or newer compatible release
- Fabric API 0.116.15+1.21.1
- YetAnotherConfigLib (YACL) 3.8.2+1.21.1-fabric

Install Fabric Loader for Minecraft 1.21.1, place Fabric API, YACL, and the Liquid Glass UI JAR in the same `mods` folder, then start the Fabric profile. The mod is declared client-only and must not be installed on a dedicated server.

The configuration is stored at `config/liquid_glass_ui.json`. Missing fields are restored from defaults, values are validated, and a damaged file is preserved as `liquid_glass_ui.broken-<timestamp>.json` before defaults are regenerated. Saves use a same-directory temporary file and atomic replacement when the file system supports it.

## Quality presets

- **LOW:** quarter-resolution shared blur buffer, two fast blur passes, dynamic refraction disabled, simplified sampling.
- **MEDIUM (default):** half-resolution shared blur buffer, three passes, light edge refraction, full pointer highlight and merging.
- **HIGH:** full-resolution shared blur buffer, five passes, high-quality refraction and an eight-sample surface budget.
- **CUSTOM:** selectable buffer scale, one to five blur passes, refraction quality, and a 2–12 sample surface budget.

The first switch to HIGH shows a GPU warning. HIGH can noticeably increase GPU use and reduce frame rate, especially at large window sizes. If the interface flickers, frame rate falls, or a shader error is reported, switch back to MEDIUM or LOW. Resetting the configuration also resets the warning acknowledgement.

Shader compilation, framebuffer creation, or render-pass failures are logged once and latch safe mode for the rest of the run. Safe mode uses ordinary translucent rounded panels without the custom shader/framebuffer path. If even that fallback fails, the original Minecraft button renderer is allowed to run.

## Animation and controls

Hover, press, menu entry, pointer highlight, and same-group merge motion use real frame time and damped springs. Long frame gaps and window-focus changes are frozen to avoid jumps, and spring integration is subdivided for comparable behavior at common refresh rates. Reduced-motion mode removes stretching, pronounced rebound, and pointer-follow deformation while keeping the interface usable and retaining a basic fade.

Only visuals are replaced. Original button messages, click callbacks, tooltips, active/visible state, keyboard activation, Tab navigation, and narration remain owned by Minecraft.

## Not implemented in phase one

This phase does not style inventory/HUD bars, containers, chat, third-party mod screens, or arbitrary vanilla screens. Dye contamination and dye flow direction/speed are planned for a later phase. Other Fabric Minecraft versions (1.21.2 through 26.2), NeoForge 1.21.1, and Forge 1.20.1 are **planned**, not currently supported.

The template icon is still used as a placeholder and must be replaced with original project artwork before a polished release.

## Architecture and future ports

Configuration data, disk I/O, animation springs, surface models, quality budgets, and the render-backend contract live in the common source set without Minecraft client, Blaze3D, or LWJGL imports. `LegacyGlassRenderBackend` contains the Fabric 1.21.1 GUI, shader, framebuffer, RenderSystem, and OpenGL adapter. A future Minecraft 26.2 port can add a modern backend without rewriting configuration, animation, or surface state.

## Building

Use JDK 21:

```powershell
.\gradlew.bat test
.\gradlew.bat clean build
```

The remapped mod JAR is written to `build/libs/`. Fabric API and YACL remain external runtime dependencies and are not included in the Liquid Glass UI JAR.

## Known compatibility notes

- The custom capture/shader path may conflict with other mods that replace menu rendering or framebuffer state; safe mode is intended to preserve usability when this is detected.
- Only the explicit vanilla screen types listed above are registered. Third-party screens and subclasses with unusual custom button content are intentionally outside phase-one support.
- Visual quality and interaction still need to be checked across GPU drivers, window/fullscreen transitions, GUI scales, and resource reloads before a public release.
