package com.oddlabs.tt.gamemode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.GameMode;
import com.oddlabs.matchmaking.Preset;
import com.oddlabs.matchmaking.RosterTemplate;
import com.oddlabs.matchmaking.StandardOptions;
import com.oddlabs.matchmaking.WorldConfig;
import com.oddlabs.tt.ruleset.Ruleset;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Presets, in memory plus JSON load/save. User-saved presets live in {@code presets.json}; the single-player menu also
 * offers one built-in preset per {@link Ruleset}, which is listed first, never saved and cannot be deleted.
 */
public final class PresetLibrary {
    private static final Logger logger = Logger.getLogger(PresetLibrary.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final TypeReference<List<Preset>> PRESET_LIST_TYPE = new TypeReference<>() {
    };
    private static final String BUILT_IN_ID_PREFIX = "builtin-";
    private static final int BUILT_IN_SLOTS = 6;
    private static final int SLIDER_MIDDLE = 5;

    private final @NonNull List<@NonNull Preset> presets = new ArrayList<>();
    private final @NonNull List<@NonNull Preset> built_ins = new ArrayList<>();

    /**
     * Adds the built-in ruleset presets: a medium island with middle sliders, the default limits, no ships, and the
     * player against one easy AI, under each ruleset.
     */
    public void addRulesetPresets() {
        built_ins.clear();
        RosterTemplate.Slot[] slots = new RosterTemplate.Slot[BUILT_IN_SLOTS];
        slots[0] = new RosterTemplate.Slot(RosterTemplate.Fill.HOST, null, 0);
        slots[1] = new RosterTemplate.Slot(RosterTemplate.Fill.EASY_AI, null, 1);
        for (int i = 2; i < slots.length; i++) {
            slots[i] = new RosterTemplate.Slot(RosterTemplate.Fill.CLOSED, null, i);
        }
        RosterTemplate roster = new RosterTemplate(slots);
        for (Ruleset ruleset : Ruleset.values()) {
            // spotless:off
            WorldConfig world = WorldConfig.builder()
                    .gamespeed(Game.GAMESPEED_NORMAL - Game.GAMESPEED_SLOW)
                    .islandSize(Game.SIZE_MEDIUM)
                    .terrainType(Game.TERRAIN_TYPE_NATIVE)
                    .hills(SLIDER_MIDDLE)
                    .vegetation(SLIDER_MIDDLE)
                    .supplies(SLIDER_MIDDLE)
                    .ruleset(ruleset.getId())
                    .build();
            // spotless:on
            built_ins.add(new Preset(BUILT_IN_ID_PREFIX + ruleset.getId(), ruleset.getDisplayName(), world,
                    StandardOptions.defaults(), roster, true));
        }
    }

    /** User-saved presets only; these are what {@link #save} writes. */
    public @NonNull List<@NonNull Preset> all() {
        return Collections.unmodifiableList(presets);
    }

    /** Built-in presets first, then the user's, for the given mode. */
    public @NonNull List<@NonNull Preset> forMode(@NonNull GameMode mode) {
        List<Preset> filtered = new ArrayList<>();
        for (Preset preset : everything()) {
            if (preset.getMode() == mode) {
                filtered.add(preset);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public @Nullable Preset findById(@NonNull String id) {
        for (Preset preset : everything()) {
            if (preset.getId().equals(id)) {
                return preset;
            }
        }
        return null;
    }

    public boolean hasName(@NonNull String name) {
        for (Preset preset : everything()) {
            if (preset.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private @NonNull List<@NonNull Preset> everything() {
        List<Preset> result = new ArrayList<>(built_ins);
        result.addAll(presets);
        return result;
    }

    public void add(@NonNull Preset preset) {
        presets.add(preset);
    }

    /** Removes a user preset. Built-in presets cannot be removed. */
    public boolean remove(@NonNull Preset preset) {
        return presets.removeIf(p -> p.getId().equals(preset.getId()));
    }

    public void load(@NonNull Path file) {
        presets.clear();
        if (!Files.exists(file)) {
            return;
        }
        try {
            List<Preset> loaded = MAPPER.readValue(file.toFile(), PRESET_LIST_TYPE);
            presets.addAll(loaded);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read presets from " + file + "; starting empty.", e);
        }
    }

    public void save(@NonNull Path file) {
        try {
            MAPPER.writeValue(file.toFile(), presets);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to write presets to " + file, e);
        }
    }
}
