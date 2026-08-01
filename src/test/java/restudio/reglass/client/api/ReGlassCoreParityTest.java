package restudio.reglass.client.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import restudio.reglass.client.runtime.ReGlassAnim;

class ReGlassCoreParityTest {
    @Test
    void upstreamDefaultsRemainExact() {
        ReGlassConfig cfg = ReGlassConfig.INSTANCE;
        assertEquals(0x000000, cfg.defaultTintColor);
        assertEquals(0.0f, cfg.defaultTintAlpha);
        assertEquals(0.003f, cfg.defaultSmoothing);
        assertEquals(12, cfg.defaultBlurRadius);
        assertEquals(30.0f, cfg.defaultShadowExpand);
        assertEquals(0.25f, cfg.defaultShadowFactor);
        assertEquals(20.0f, cfg.defaultRefThickness);
        assertEquals(1.4f, cfg.defaultRefFactor);
        assertEquals(7.0f, cfg.defaultRefDispersion);
        assertEquals(30.0f, cfg.defaultRefFresnelRange);
        assertEquals(20.0f, cfg.defaultRefFresnelHardness);
        assertEquals(20.0f, cfg.defaultRefFresnelFactor);
        assertEquals(90.0f, cfg.defaultGlareFactor);
        assertEquals(1.5f, cfg.hoverScalePx);
        assertEquals(2.5f, cfg.focusScalePx);
    }

    @Test
    void widgetStyleKeepsCompleteOpticalOverrides() {
        WidgetStyle style = WidgetStyle.create()
                .tint(0x123456, 0.4f)
                .smoothing(0.05f)
                .blurRadius(17)
                .shadow(25.0f, 0.2f, 1.0f, 3.0f)
                .shadowColor(0x010203, 0.8f)
                .refractionThickness(21.0f)
                .refractionFactor(1.45f)
                .refractionDispersion(8.0f)
                .fresnelRange(31.0f)
                .fresnelHardness(22.0f)
                .fresnelFactor(23.0f)
                .glareRange(32.0f)
                .glareHardness(24.0f)
                .glareConvergence(51.0f)
                .glareOppositeFactor(81.0f)
                .glareFactor(91.0f)
                .glareAngleRad(-0.5f);

        assertEquals(0x123456, style.getTintColor());
        assertEquals(0.4f, style.getTintAlpha());
        assertEquals(0.05f, style.getSmoothing());
        assertEquals(17, style.getBlurRadius());
        assertEquals(8.0f, style.getRefDispersion());
        assertEquals(23.0f, style.getRefFresnelFactor());
        assertEquals(91.0f, style.getGlareFactor());
    }

    @Test
    void animationInitializesToUpstreamConfiguration() {
        ReGlassAnim.INSTANCE.update(ReGlassConfig.INSTANCE, 1.0 / 60.0);
        assertEquals(12, ReGlassAnim.INSTANCE.blurRadiusInt());
        assertEquals(1.4f, ReGlassAnim.INSTANCE.refFactor());
        assertEquals(90.0f, ReGlassAnim.INSTANCE.glareFactor());
    }
}
