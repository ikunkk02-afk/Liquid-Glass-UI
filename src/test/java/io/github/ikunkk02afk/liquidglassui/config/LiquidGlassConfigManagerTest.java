package io.github.ikunkk02afk.liquidglassui.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LiquidGlassConfigManagerTest {
    @TempDir Path directory;

    @Test
    void createsDefaultsWhenMissing() {
        Path file = directory.resolve("liquid_glass_ui.json");
        LiquidGlassConfigManager manager = manager(file);
        LiquidGlassConfigData data = manager.load();

        assertTrue(Files.isRegularFile(file));
        assertEquals(GlassQualityPreset.MEDIUM, data.performance.preset);
        assertEquals("#DDE7F2", data.appearance.mainColor);
        assertEquals(0.10f, data.appearance.opacity);
        assertEquals(2, data.schemaVersion);
        assertFalse(data.highQualityWarningAcknowledged);
    }

    @Test
    void fillsMissingFieldsAndClampsValues() throws IOException {
        Path file = directory.resolve("liquid_glass_ui.json");
        Files.writeString(file, "{\"appearance\":{\"opacity\":4,\"cornerRadius\":-9},\"performance\":{\"customBufferScale\":0.68,\"customBlurPasses\":99,\"customSampleCount\":0}}", StandardCharsets.UTF_8);
        LiquidGlassConfigData data = manager(file).load();

        assertEquals(1.0f, data.appearance.opacity);
        assertEquals(2.0f, data.appearance.cornerRadius);
        assertEquals(0.75f, data.performance.customBufferScale);
        assertEquals(6, data.performance.customBlurPasses);
        assertEquals(2, data.performance.customSampleCount);
        assertNotNull(data.optics);
        assertNotNull(data.animation);
        assertNotNull(data.fusion);
    }

    @Test
    void migratesSchemaOneAndRemovedDebugModes() throws IOException {
        Path file = directory.resolve("liquid_glass_ui.json");
        Files.writeString(file, "{\"schemaVersion\":1,\"optics\":{\"blurRadius\":9},"
                + "\"performance\":{\"debugView\":\"SOLID_MASK\"}}", StandardCharsets.UTF_8);
        LiquidGlassConfigData data = manager(file).load();

        assertEquals(2, data.schemaVersion);
        assertEquals(9.0f, data.optics.blurRadius);
        assertEquals(GlassDebugView.OFF, data.performance.debugView);
        assertEquals(32.0f, data.fusion.distance);
    }

    @Test
    void preservesBrokenFileAndRecovers() throws IOException {
        Path file = directory.resolve("liquid_glass_ui.json");
        Files.writeString(file, "{ definitely broken", StandardCharsets.UTF_8);
        LiquidGlassConfigData data = manager(file).load();

        assertEquals(GlassQualityPreset.MEDIUM, data.performance.preset);
        assertTrue(Files.isRegularFile(file));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.filter(path -> path.getFileName().toString().startsWith("liquid_glass_ui.broken-")).count());
        }
    }

    @Test
    void atomicSaveLeavesNoTemporaryFile() throws IOException {
        Path file = directory.resolve("liquid_glass_ui.json");
        LiquidGlassConfigManager manager = manager(file);
        LiquidGlassConfigData data = manager.load();
        data.optics.blurRadius = 11.0f;
        manager.replaceAndSave(data);

        assertEquals(11.0f, manager(file).load().optics.blurRadius);
        try (var files = Files.list(directory)) {
            assertEquals(0, files.filter(path -> path.getFileName().toString().endsWith(".tmp")).count());
        }
    }

    @Test
    void highQualityAcknowledgementPersistsAndResetClearsIt() {
        Path file = directory.resolve("liquid_glass_ui.json");
        LiquidGlassConfigManager manager = manager(file);
        LiquidGlassConfigData data = manager.load();
        assertTrue(HighQualityPolicy.requiresWarning(data, GlassQualityPreset.HIGH));
        HighQualityPolicy.acknowledgeAndSelect(data);
        manager.replaceAndSave(data);

        assertFalse(HighQualityPolicy.requiresWarning(manager.get(), GlassQualityPreset.HIGH));
        assertFalse(manager.reset().highQualityWarningAcknowledged);
    }

    private LiquidGlassConfigManager manager(Path file) {
        return new LiquidGlassConfigManager(file, LoggerFactory.getLogger("LiquidGlassConfigManagerTest"));
    }
}
