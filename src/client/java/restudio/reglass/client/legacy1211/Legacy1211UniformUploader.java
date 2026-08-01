/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client.legacy1211;

import static restudio.reglass.client.legacy1211.ReGlassStd140Layout.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import restudio.reglass.client.api.ReGlassConfig;
import restudio.reglass.client.api.WidgetStyle;
import restudio.reglass.client.gui.LiquidGlassGuiElementRenderState;
import restudio.reglass.client.runtime.ReGlassAnim;

public final class Legacy1211UniformUploader implements AutoCloseable {
    private static final int SAMPLER_INFO_BINDING = 0;
    private static final int CUSTOM_UNIFORMS_BINDING = 1;
    private static final int WIDGET_INFO_BINDING = 2;
    private static final int BG_CONFIG_BINDING = 3;

    private final Map<Long, FadeState> fades = new HashMap<>();
    private final List<Integer> usedBlurRadii = new ArrayList<>(MAX_BLUR_LEVELS);

    private int samplerInfoUbo;
    private int customUniformsUbo;
    private int widgetInfoUbo;
    private int bgConfigUbo;

    public void ensureCreated() {
        if (samplerInfoUbo != 0) {
            return;
        }
        samplerInfoUbo = createUbo(SAMPLER_INFO_SIZE);
        customUniformsUbo = createUbo(CUSTOM_UNIFORMS_SIZE);
        widgetInfoUbo = createUbo(WIDGET_INFO_SIZE);
        bgConfigUbo = createUbo(BG_CONFIG_SIZE);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public void uploadShared(
            int framebufferWidth,
            int framebufferHeight,
            float time,
            float mouseX,
            float mouseY,
            boolean screenWantsBlur
    ) {
        ensureCreated();

        ByteBuffer sampler = direct(SAMPLER_INFO_SIZE);
        sampler.putFloat(0, framebufferWidth);
        sampler.putFloat(4, framebufferHeight);
        sampler.putFloat(8, framebufferWidth);
        sampler.putFloat(12, framebufferHeight);
        upload(samplerInfoUbo, sampler);

        ReGlassConfig cfg = ReGlassConfig.INSTANCE;
        ByteBuffer custom = direct(CUSTOM_UNIFORMS_SIZE);
        custom.putFloat(0, time);
        putVec4(custom, 16, mouseX, mouseY, 0.0f, 0.0f);
        custom.putFloat(32, screenWantsBlur ? 1.0f : 0.0f);
        custom.putFloat(48, cfg.rimLight.direction().x);
        custom.putFloat(52, cfg.rimLight.direction().y);
        custom.putFloat(56, 0.0f);
        putColor(custom, 64, cfg.rimLight.color(), cfg.rimLight.intensity());
        custom.putFloat(80, cfg.pixelEpsilon);
        custom.putFloat(84, ReGlassAnim.INSTANCE.debugStep());
        custom.putFloat(88, cfg.features.pixelatedGrid ? 1.0f : 0.0f);
        custom.putFloat(92, ReGlassAnim.INSTANCE.pixelatedGridSize());
        custom.putFloat(96, ReGlassAnim.INSTANCE.hoverScalePx());
        custom.putFloat(100, ReGlassAnim.INSTANCE.focusScalePx());
        custom.putFloat(104, ReGlassAnim.INSTANCE.focusBorderWidthPx());
        custom.putFloat(108, ReGlassAnim.INSTANCE.focusBorderIntensity());
        custom.putFloat(112, ReGlassAnim.INSTANCE.focusBorderSpeed());
        upload(customUniformsUbo, custom);

        ByteBuffer bg = direct(BG_CONFIG_SIZE);
        bg.putFloat(0, ReGlassAnim.INSTANCE.shadowExpand());
        bg.putFloat(4, ReGlassAnim.INSTANCE.shadowFactor());
        bg.putFloat(8, ReGlassAnim.INSTANCE.shadowOffsetX());
        bg.putFloat(12, ReGlassAnim.INSTANCE.shadowOffsetY());
        upload(bgConfigUbo, bg);
    }

    public List<Integer> uploadWidgets(
            List<LiquidGlassGuiElementRenderState> source,
            int framebufferWidth,
            int framebufferHeight,
            float guiScale,
            double dtSeconds
    ) {
        ensureCreated();
        int count = Math.min(MAX_WIDGETS, source.size());
        rebuildBlurRadii(source, count);

        Map<Integer, Integer> radiusToIndex = new HashMap<>();
        for (int i = 0; i < usedBlurRadii.size(); i++) {
            radiusToIndex.put(usedBlurRadii.get(i), i);
        }

        ByteBuffer widgets = direct(WIDGET_INFO_SIZE);
        widgets.putFloat(COUNT_OFFSET, count);
        for (int i = 0; i < count; i++) {
            LiquidGlassGuiElementRenderState widget = source.get(i);
            WidgetStyle style = widget.style();

            float width = widget.x2() - widget.x1();
            float height = widget.y2() - widget.y1();
            float pixelX = widget.x1() * guiScale;
            float pixelTop = widget.y1() * guiScale;
            float pixelWidth = width * guiScale;
            float pixelHeight = height * guiScale;
            float centerYTop = pixelTop + 0.5f * pixelHeight;
            float rectY = framebufferHeight - centerYTop - 0.5f * pixelHeight;

            putVec4(widgets, elementOffset(RECTS_OFFSET, i), pixelX, rectY, pixelWidth, pixelHeight);
            float radius = widget.cornerRadius() * guiScale;
            putVec4(widgets, elementOffset(RADS_OFFSET, i), radius, radius, radius, radius);
            putColor(widgets, elementOffset(TINTS_OFFSET, i), style.getTintColor(), style.getTintAlpha());
            putVec4(widgets, elementOffset(OPTICS0_OFFSET, i), style.getRefThickness(), style.getRefFactor(), style.getRefDispersion(), style.getRefFresnelRange());
            putVec4(widgets, elementOffset(OPTICS1_OFFSET, i), style.getRefFresnelHardness(), style.getRefFresnelFactor(), style.getGlareRange(), style.getGlareHardness());
            putVec4(widgets, elementOffset(OPTICS2_OFFSET, i), style.getGlareConvergence(), style.getGlareOppositeFactor(), style.getGlareFactor(), style.getGlareAngleRad());
            putVec4(widgets, elementOffset(SMOOTHINGS_OFFSET, i), style.getSmoothing(), 0.0f, 0.0f, 0.0f);

            if (widget.scissorArea() == null) {
                putVec4(widgets, elementOffset(SCISSOR_RECTS_OFFSET, i), 0.0f, 0.0f, framebufferWidth, framebufferHeight);
            } else {
                float left = widget.scissorArea().left() * guiScale;
                float right = widget.scissorArea().right() * guiScale;
                float top = widget.scissorArea().top() * guiScale;
                float bottom = widget.scissorArea().bottom() * guiScale;
                putVec4(widgets, elementOffset(SCISSOR_RECTS_OFFSET, i), left, framebufferHeight - bottom, right, framebufferHeight - top);
            }

            putVec4(widgets, elementOffset(SHADOW0_OFFSET, i), style.getShadowExpand(), style.getShadowFactor(), style.getShadowOffsetX() * guiScale, style.getShadowOffsetY() * guiScale);
            putColor(widgets, elementOffset(SHADOW_COLOR_OFFSET, i), style.getShadowColor(), style.getShadowColorAlpha());

            int requestedRadius = Math.max(0, style.getBlurRadius());
            int blurIndex = radiusToIndex.getOrDefault(requestedRadius, 0);
            FadeState fade = fades.computeIfAbsent(rectKey(widget), ignored -> new FadeState());
            fade.hover = smoothToward(fade.hover, clamp01(widget.hover()), dtSeconds, 0.12f);
            fade.focus = smoothToward(fade.focus, clamp01(widget.focus()), dtSeconds, 0.18f);
            double hash = Math.sin(widget.x1() * 12.9898 + widget.y1() * 78.233 + i * 37.719);
            float seed = (float) (hash - Math.floor(hash));
            putVec4(widgets, elementOffset(EXTRA0_OFFSET, i), blurIndex, fade.hover, fade.focus, seed);
        }
        upload(widgetInfoUbo, widgets);
        return List.copyOf(usedBlurRadii);
    }

    public void bindCompositeBlocks(int shaderProgram) {
        ensureCreated();
        bindBlock(shaderProgram, "SamplerInfo", SAMPLER_INFO_BINDING, samplerInfoUbo);
        bindBlock(shaderProgram, "CustomUniforms", CUSTOM_UNIFORMS_BINDING, customUniformsUbo);
        bindBlock(shaderProgram, "WidgetInfo", WIDGET_INFO_BINDING, widgetInfoUbo);
        bindBlock(shaderProgram, "BgConfig", BG_CONFIG_BINDING, bgConfigUbo);
    }

    public int samplerInfoUbo() {
        ensureCreated();
        return samplerInfoUbo;
    }

    private void rebuildBlurRadii(List<LiquidGlassGuiElementRenderState> source, int count) {
        HashSet<Integer> requested = new HashSet<>();
        for (int i = 0; i < count; i++) {
            requested.add(Math.max(0, source.get(i).style().getBlurRadius()));
        }
        usedBlurRadii.clear();
        requested.stream().sorted().limit(MAX_BLUR_LEVELS).forEach(usedBlurRadii::add);
        if (usedBlurRadii.isEmpty()) {
            usedBlurRadii.add(ReGlassAnim.INSTANCE.blurRadiusInt());
        }
    }

    private static int createUbo(int size) {
        int buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, buffer);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, size, GL15.GL_DYNAMIC_DRAW);
        return buffer;
    }

    private static void upload(int buffer, ByteBuffer data) {
        data.position(0).limit(data.capacity());
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, buffer);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, data);
    }

    private static void bindBlock(int program, String name, int binding, int buffer) {
        int index = GL31.glGetUniformBlockIndex(program, name);
        if (index != GL31.GL_INVALID_INDEX) {
            GL31.glUniformBlockBinding(program, index, binding);
            GL31.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, binding, buffer);
        }
    }

    private static ByteBuffer direct(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    private static void putColor(ByteBuffer target, int offset, int rgb, float alpha) {
        putVec4(target, offset, ((rgb >>> 16) & 0xFF) / 255.0f, ((rgb >>> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f, alpha);
    }

    private static void putVec4(ByteBuffer target, int offset, float x, float y, float z, float w) {
        target.putFloat(offset, x);
        target.putFloat(offset + 4, y);
        target.putFloat(offset + 8, z);
        target.putFloat(offset + 12, w);
    }

    private static long rectKey(LiquidGlassGuiElementRenderState widget) {
        long a = Integer.toUnsignedLong(widget.x1()) | (Integer.toUnsignedLong(widget.y1()) << 32);
        long b = Integer.toUnsignedLong(widget.x2()) | (Integer.toUnsignedLong(widget.y2()) << 32);
        long hash = 1469598103934665603L;
        hash = (hash ^ a) * 1099511628211L;
        return (hash ^ b) * 1099511628211L;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float smoothToward(float current, float target, double dt, float tau) {
        float alpha = (float) (1.0 - Math.exp(-Math.max(0.0, dt) / Math.max(1.0e-4f, tau)));
        float value = current + (target - current) * alpha;
        return Math.abs(value - target) < 1.0e-4f ? target : value;
    }

    @Override
    public void close() {
        int[] buffers = {samplerInfoUbo, customUniformsUbo, widgetInfoUbo, bgConfigUbo};
        for (int buffer : buffers) {
            if (buffer != 0) {
                GL15.glDeleteBuffers(buffer);
            }
        }
        samplerInfoUbo = customUniformsUbo = widgetInfoUbo = bgConfigUbo = 0;
    }

    private static final class FadeState {
        private float hover;
        private float focus;
    }
}
