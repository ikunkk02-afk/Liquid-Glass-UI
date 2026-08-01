package restudio.reglass.client.legacy1211;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GaussianParityTest {
    @Test
    void radiusZeroIsIdentity() {
        assertEquals(1.0f, Legacy1211BlurRuntime.gaussian(0)[0]);
    }

    @Test
    void upstreamKernelIsSymmetricallyNormalized() {
        float[] weights = Legacy1211BlurRuntime.gaussian(12);
        float sum = weights[0];
        for (int i = 1; i < weights.length; i++) sum += 2.0f * weights[i];
        assertEquals(1.0f, sum, 1.0e-5f);
        for (int i = 1; i < weights.length; i++) assertTrue(weights[i] <= weights[i - 1]);
    }
}
