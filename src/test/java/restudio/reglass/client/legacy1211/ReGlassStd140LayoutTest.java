package restudio.reglass.client.legacy1211;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReGlassStd140LayoutTest {
    @Test
    void widgetBlockRetainsUpstreamCapacityAndOffsets() {
        assertEquals(64, ReGlassStd140Layout.MAX_WIDGETS);
        assertEquals(5, ReGlassStd140Layout.MAX_BLUR_LEVELS);
        assertEquals(12_304, ReGlassStd140Layout.WIDGET_INFO_SIZE);
        assertEquals(16, ReGlassStd140Layout.RECTS_OFFSET);
        assertEquals(1_040, ReGlassStd140Layout.RADS_OFFSET);
        assertEquals(10_256, ReGlassStd140Layout.EXTRA0_OFFSET);
        assertEquals(11_264, ReGlassStd140Layout.elementOffset(ReGlassStd140Layout.EXTRA0_OFFSET, 63));
    }

    @Test
    void sharedBlocksMatchStd140Sizes() {
        assertEquals(16, ReGlassStd140Layout.SAMPLER_INFO_SIZE);
        assertEquals(128, ReGlassStd140Layout.CUSTOM_UNIFORMS_SIZE);
        assertEquals(16, ReGlassStd140Layout.BG_CONFIG_SIZE);
    }

    @Test
    void widgetIndexCannotEscapeFixedBlock() {
        assertThrows(IndexOutOfBoundsException.class, () -> ReGlassStd140Layout.elementOffset(ReGlassStd140Layout.RECTS_OFFSET, 64));
    }
}
