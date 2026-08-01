package io.github.ikunkk02afk.liquidglassui.reglassport;

import io.github.ikunkk02afk.liquidglassui.reglassport.screen.ReGlassMenuGroups;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReGlassMenuGroupsTest {
    @Test
    void titleGroupsAreTranslationKeyBased() {
        assertEquals(ReGlassMenuGroups.TITLE_PLAY, ReGlassMenuGroups.titleGroup("menu.singleplayer"));
        assertEquals(ReGlassMenuGroups.TITLE_PLAY, ReGlassMenuGroups.titleGroup("menu.online"));
        assertEquals(ReGlassMenuGroups.TITLE_SYSTEM, ReGlassMenuGroups.titleGroup("menu.options"));
        assertEquals(ReGlassMenuGroups.LIQUID_GLASS_SETTINGS,
                ReGlassMenuGroups.titleGroup("liquid_glass_ui.open_settings"));
        assertEquals(ReGlassMenuGroups.UNKNOWN, ReGlassMenuGroups.titleGroup("third.party.button"));
    }

    @Test
    void pauseNativeButtonsShareOneGroupAndUnknownButtonsStayUnassigned() {
        assertEquals(ReGlassMenuGroups.PAUSE_NATIVE, ReGlassMenuGroups.pauseGroup("menu.returnToGame"));
        assertEquals(ReGlassMenuGroups.PAUSE_NATIVE, ReGlassMenuGroups.pauseGroup("menu.options"));
        assertEquals(ReGlassMenuGroups.LIQUID_GLASS_SETTINGS,
                ReGlassMenuGroups.pauseGroup("liquid_glass_ui.open_settings"));
        assertEquals(ReGlassMenuGroups.UNKNOWN, ReGlassMenuGroups.pauseGroup("third.party.button"));
    }
}
