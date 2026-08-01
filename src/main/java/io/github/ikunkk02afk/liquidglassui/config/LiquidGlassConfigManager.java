package io.github.ikunkk02afk.liquidglassui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class LiquidGlassConfigManager {
    private static final DateTimeFormatter BROKEN_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path path;
    private final Logger logger;
    private LiquidGlassConfigData current = LiquidGlassConfigData.defaults();

    public LiquidGlassConfigManager(Path path, Logger logger) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public synchronized LiquidGlassConfigData load() {
        if (Files.notExists(path)) {
            current = LiquidGlassConfigData.defaults();
            saveInternal(current);
            return current.copy();
        }

        try {
            LiquidGlassConfigData loaded;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                migrateLegacyDebugView(root);
                loaded = GSON.fromJson(root, LiquidGlassConfigData.class);
            }
            if (loaded == null) throw new JsonParseException("Configuration root is null");
            loaded.sanitize();
            current = loaded;
            saveInternal(current);
        } catch (IOException | RuntimeException exception) {
            logger.warn("Liquid Glass UI configuration is damaged; preserving it and restoring defaults", exception);
            preserveBrokenFile();
            current = LiquidGlassConfigData.defaults();
            saveInternal(current);
        }
        return current.copy();
    }

    private static void migrateLegacyDebugView(JsonElement root) {
        if (!root.isJsonObject()) return;
        JsonObject performance = root.getAsJsonObject().getAsJsonObject("performance");
        if (performance == null || !performance.has("debugView") || !performance.get("debugView").isJsonPrimitive()) return;
        String value = performance.get("debugView").getAsString();
        if ("SOLID_MASK".equals(value) || "UV_GRID".equals(value)) performance.addProperty("debugView", "OFF");
    }

    public synchronized LiquidGlassConfigData get() {
        return current.copy();
    }

    public synchronized void replaceAndSave(LiquidGlassConfigData data) {
        LiquidGlassConfigData sanitized = Objects.requireNonNull(data, "data").sanitizedCopy();
        saveInternal(sanitized);
        current = sanitized;
    }

    public synchronized LiquidGlassConfigData reset() {
        LiquidGlassConfigData defaults = LiquidGlassConfigData.defaults();
        saveInternal(defaults);
        current = defaults;
        return defaults.copy();
    }

    public Path path() {
        return path;
    }

    private void preserveBrokenFile() {
        if (Files.notExists(path)) return;
        String base = "liquid_glass_ui.broken-" + BROKEN_TIMESTAMP.format(LocalDateTime.now());
        Path target = path.resolveSibling(base + ".json");
        int suffix = 1;
        while (Files.exists(target)) target = path.resolveSibling(base + "-" + suffix++ + ".json");
        try {
            Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException moveFailure) {
            try {
                Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException copyFailure) {
                moveFailure.addSuppressed(copyFailure);
                logger.error("Could not preserve damaged Liquid Glass UI configuration at {}", path, moveFailure);
            }
        }
    }

    private void saveInternal(LiquidGlassConfigData data) {
        Path parent = path.getParent();
        try {
            if (parent != null) Files.createDirectories(parent);
            String fileName = path.getFileName().toString();
            Path temporary = Files.createTempFile(parent, fileName + ".", ".tmp");
            try {
                try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                    GSON.toJson(data, writer);
                }
                try {
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save Liquid Glass UI configuration to " + path, exception);
        }
    }
}
