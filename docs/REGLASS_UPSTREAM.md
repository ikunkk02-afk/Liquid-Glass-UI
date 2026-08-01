# ReGlass Upstream Baseline

- Upstream repository: https://github.com/RedxAx/ReGlass
- Pinned commit: `73822f89c31eb48b8a965d13c45214772b800eb7`
- Commit title: `ReGlass 1.1.0`
- Retrieved: 2026-08-01
- Upstream Minecraft version: 1.21.8
- Target Minecraft version: 1.21.1
- Target loader: Fabric
- Target Java: 21
- Target mappings: official Mojang mappings

The upstream checkout is kept outside this repository at
`../ReGlass-upstream` in detached-HEAD state. Its Git metadata, Gradle caches,
run directory, and build outputs are not vendored.

## Compatibility boundary

The ReGlass optical model, defaults, widget data model, animation, Gaussian
blur, SDF operations, refraction, dispersion, Fresnel, glare, shadow, scissor,
hover, focus, and smooth-union behavior are the parity baseline.

Only the Minecraft 1.21.8 rendering infrastructure is replaced. The 1.21.1
bridge uses `ShaderInstance`, `RenderTarget`/`TextureTarget`, `GuiGraphics`,
`VertexBuffer`, Fabric shader registration, and OpenGL uniform buffer objects.
All compatibility edits to upstream shaders must be listed in the final
backport report.
