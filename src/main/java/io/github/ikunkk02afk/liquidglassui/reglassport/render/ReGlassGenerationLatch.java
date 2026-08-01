package io.github.ikunkk02afk.liquidglassui.reglassport.render;

/** Latches one runtime failure only for the resource generation that produced it. */
public final class ReGlassGenerationLatch {
    private int failedGeneration = Integer.MIN_VALUE;
    private boolean reported;

    public boolean fail(int generation) {
        boolean firstReport = failedGeneration != generation || !reported;
        failedGeneration = generation;
        reported = true;
        return firstReport;
    }

    public boolean failed(int generation) {
        return failedGeneration == generation;
    }

    public void successfulReload() {
        failedGeneration = Integer.MIN_VALUE;
        reported = false;
    }
}
