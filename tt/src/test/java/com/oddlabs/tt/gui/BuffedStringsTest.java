package com.oddlabs.tt.gui;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * M8's and M9's strings exist in all six languages and format with the arguments the game passes (the tooltips are
 * built on
 * hover, so a missing key or a broken pattern would only show in game).
 */
final class BuffedStringsTest {
    // English is the base file: never the JVM's own locale instead.
    private static final ResourceBundle.Control NO_FALLBACK = ResourceBundle.Control.getNoFallbackControl(
            ResourceBundle.Control.FORMAT_PROPERTIES);
    private static final Map<String, Object[]> PANEL = Map.ofEntries(
            Map.entry("great_tower_tip", new Object[]{"Great Tower", "W", 3, 50, 10}),
            Map.entry("lodge_tip", new Object[]{"Mead Hall", "L", 30, "Berserker", 35, 5}),
            Map.entry("lodge_units_tip", new Object[]{}),
            Map.entry("lodge_champions_tip", new Object[]{"Berserker"}),
            Map.entry("lodge_leave_tip", new Object[]{}),
            Map.entry("train_champion_tip", new Object[]{"Berserker", 5}),
            Map.entry("drum_weapons_tip_natives", new Object[]{}),
            Map.entry("drum_weapons_tip_vikings", new Object[]{}),
            Map.entry("net_weapons_tip", new Object[]{}),
            Map.entry("build_drum_tip", new Object[]{"Hornblower", 12f}),
            Map.entry("build_net_tip", new Object[]{"Fowler"}),
            Map.entry("lay_snare_tip", new Object[]{"D", 3, 4f}));
    private static final String[] NAMES = {"great_tower", "lodge_natives", "lodge_vikings", "champion_natives", "champion_vikings", "drum_warrior_natives", "drum_warrior_vikings", "net_warrior_natives", "net_warrior_vikings", "snare"};
    private static final String[] ACTIONS = {"UNIT_BUILD_GREAT_TOWER", "UNIT_BUILD_LODGE", "TRAIN_CHAMPION", "TRAIN_CHAMPION_DEC", "TRAIN_CHAMPION_BATCH", "TRAIN_CHAMPION_BATCH_DEC", "CHEAT_12", "RES_DRUM", "RES_DRUM_DEC", "RES_DRUM_BATCH", "RES_DRUM_BATCH_DEC", "RES_NET", "RES_NET_DEC", "RES_NET_BATCH", "RES_NET_BATCH_DEC", "UNIT_LAY_SNARE", "CHEAT_13", "CHEAT_14"};

    @ParameterizedTest
    @ValueSource(strings = {"en", "da", "de", "es", "it", "pt"})
    void everyLanguageHasThem(String language) {
        Locale locale = Locale.of(language);
        ResourceBundle panel = ResourceBundle.getBundle("com.oddlabs.tt.gui.ActionButtonPanel", locale, NO_FALLBACK);
        ResourceBundle races = ResourceBundle.getBundle("com.oddlabs.tt.model.RacesResources", locale, NO_FALLBACK);
        ResourceBundle options = ResourceBundle.getBundle("com.oddlabs.tt.form.OptionsMenu", locale, NO_FALLBACK);
        for (Map.Entry<String, Object[]> entry : PANEL.entrySet()) {
            String text = assertDoesNotThrow(() -> MessageFormat.format(panel.getString(entry.getKey()),
                    entry.getValue()), language + " " + entry.getKey());
            assertFalse(text.contains("{"), language + " " + entry.getKey() + ": " + text);
        }
        for (String name : NAMES)
            assertFalse(MessageFormat.format(races.getString(name), new Object[0]).isBlank(), language + " " + name);
        for (String action : ACTIONS)
            assertFalse(options.getString("action." + action).isBlank(), language + " " + action);
    }
}
