# Third-Party Notices

## ReGlass

- Project: ReGlass
- Author: ReStudio By RedxAx
- Original repository: https://github.com/RedxAx/ReGlass
- Reviewed revision: `73822f89c31eb48b8a965d13c45214772b800eb7`
- License: MIT (`licenses/ReGlass-LICENSE.txt`)

Liquid Glass UI studies and adapts ReGlass's component-collection/single-composite design and portions of the mathematical approach used for rounded-box signed-distance fields, analytic edge normals, smooth/hard boolean operations, edge refraction, subtle dispersion, Fresnel response, glare and soft shadowing.

Adapted derivative implementations are project-namespaced and rewritten for Minecraft Fabric 1.21.1. The principal derivative resources are the unified liquid-glass composite fragment shader and its associated frame/material packing model. Each source file containing substantial adapted implementation carries an attribution header.

ReGlass's Minecraft 1.21.8 Java renderer is not copied. Liquid Glass UI replaces its RenderPipeline, RenderPass, GuiRenderState, SpecialGuiElementRenderer, GpuBuffer and GpuTexture code with a separate Minecraft 1.21.1 legacy backend using ShaderInstance, RenderTarget, TextureTarget and a widget-data texture.

No ReGlass visual artwork, screenshots, icons, logos, fonts, sounds or other media are redistributed.

## Blur+

- Project: Blur+
- Repository: https://github.com/Motschen/Blur
- Reviewed revision: `3507b81754ed93ec744ca5e45d1146b51081fc9f`
- License: MIT

Blur+ was consulted as an implementation reference for the Minecraft 1.21.1 screen-blur lifecycle, shader/main-target rebinding, resize/resource behavior and multiversion boundaries. No Blur+ source, shader, configuration or art asset is copied into Liquid Glass UI, and Blur+ is not a runtime dependency.

