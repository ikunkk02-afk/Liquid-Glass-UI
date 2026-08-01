package io.github.ikunkk02afk.liquidglassui.reglassport.screen;

import java.util.Set;

/** Translation-key based grouping policy that is independent of the active language. */
public final class ReGlassMenuGroups {
    public static final int UNKNOWN = -1;
    public static final int TITLE_PLAY = 1;
    public static final int TITLE_SYSTEM = 2;
    public static final int LIQUID_GLASS_SETTINGS = 3;
    public static final int PAUSE_NATIVE = 1;

    private static final Set<String> TITLE_PLAY_KEYS = Set.of(
            "menu.singleplayer",
            "menu.multiplayer",
            "menu.online",
            "menu.online.disabled",
            "menu.realms"
    );
    private static final Set<String> TITLE_SYSTEM_KEYS = Set.of(
            "menu.options",
            "menu.quit"
    );
    private static final Set<String> PAUSE_NATIVE_KEYS = Set.of(
            "menu.returnToGame",
            "gui.advancements",
            "gui.stats",
            "menu.sendFeedback",
            "menu.reportBugs",
            "menu.options",
            "menu.shareToLan",
            "menu.returnToMenu",
            "menu.disconnect",
            "menu.playerReporting"
    );

    private ReGlassMenuGroups() {
    }

    public static int titleGroup(String translationKey) {
        if (isSettings(translationKey)) return LIQUID_GLASS_SETTINGS;
        if (TITLE_PLAY_KEYS.contains(translationKey)) return TITLE_PLAY;
        if (TITLE_SYSTEM_KEYS.contains(translationKey)) return TITLE_SYSTEM;
        return UNKNOWN;
    }

    public static int pauseGroup(String translationKey) {
        if (isSettings(translationKey)) return LIQUID_GLASS_SETTINGS;
        return PAUSE_NATIVE_KEYS.contains(translationKey) ? PAUSE_NATIVE : UNKNOWN;
    }

    private static boolean isSettings(String translationKey) {
        return "liquid_glass_ui.open_settings".equals(translationKey);
    }
}
