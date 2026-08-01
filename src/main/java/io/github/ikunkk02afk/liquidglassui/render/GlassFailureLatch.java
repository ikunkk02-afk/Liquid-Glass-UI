package io.github.ikunkk02afk.liquidglassui.render;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class GlassFailureLatch {
    private final BiConsumer<String, Throwable> reporter;
    private boolean failed;
    private String reason = "";

    public GlassFailureLatch(BiConsumer<String, Throwable> reporter) {
        this.reporter = Objects.requireNonNull(reporter, "reporter");
    }

    public synchronized boolean trip(String reason, Throwable throwable) {
        if (failed) return false;
        failed = true;
        this.reason = reason == null ? "Unknown rendering failure" : reason;
        reporter.accept(this.reason, throwable);
        return true;
    }

    public synchronized boolean failed() { return failed; }
    public synchronized String reason() { return reason; }
}
