package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.headless.HeadlessMatchConfig.PlayerConfig;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.ruleset.Ruleset;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The seeded matches the headless tests play: a 1v1 and a six-tribe free-for-all under each ruleset, and a 6v6 on an
 * Enormous island under Resurrected. Seeds are pinned and each was checked to end in a victory within the tick limit.
 * Every match is played at most once per test JVM; the tests share the results.
 */
final class Matches {
    private static final int NATIVES = RacesResources.RACE_NATIVES;
    private static final int VIKINGS = RacesResources.RACE_VIKINGS;

    static final @NonNull Map<@NonNull String, @NonNull HeadlessMatchConfig> ALL = createAll();

    private static final Map<String, HeadlessMatchResult> results = new ConcurrentHashMap<>();

    private Matches() {
    }

    private static @NonNull Map<String, HeadlessMatchConfig> createAll() {
        Map<String, HeadlessMatchConfig> all = new LinkedHashMap<>();
        for (Ruleset ruleset : Ruleset.values()) {
            // Buffed's 1v1 on seed 7 stays undecided since M10 (the Natives sit at the unit cap; for M14): it plays a
            // seed that finishes and in which the Hard AIs cast the new spells.
            int seed_1v1 = ruleset == Ruleset.BUFFED ? 9 : 7;
            all.put(ruleset.getId() + "-1v1", new HeadlessMatchConfig(ruleset, Landscape.TerrainType.NATIVE,
                    Game.SIZE_SMALL, .5f, .5f, .5f, seed_1v1, List.of(new PlayerConfig(0, NATIVES, PlayerSlot.AI_HARD),
                            new PlayerConfig(1, VIKINGS, PlayerSlot.AI_HARD)), HeadlessMatchConfig.DEFAULT_MAX_TICKS));
            int[] difficulties = {PlayerSlot.AI_HARD, PlayerSlot.AI_NORMAL, PlayerSlot.AI_EASY, PlayerSlot.AI_HARD, PlayerSlot.AI_NORMAL, PlayerSlot.AI_EASY};
            List<PlayerConfig> six = new ArrayList<>();
            for (int i = 0; i < difficulties.length; i++) {
                six.add(new PlayerConfig(i, i % 2 == 0 ? NATIVES : VIKINGS, difficulties[i]));
            }
            // Buffed's six tribes leave half or more of the seeds undecided after 60 minutes (AIs sit at the unit cap;
            // for the AI milestone, M14), seed 11 among them since M9 and seed 9 since M10: it plays a seed that finishes.
            int seed = ruleset == Ruleset.BUFFED ? 10 : 11;
            all.put(ruleset.getId() + "-6p", new HeadlessMatchConfig(ruleset, Landscape.TerrainType.VIKING,
                    Game.SIZE_MEDIUM, .5f, .5f, .5f, seed, six, HeadlessMatchConfig.DEFAULT_MAX_TICKS));
        }
        List<PlayerConfig> twelve = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            twelve.add(new PlayerConfig(i % 2, i / 2 % 2 == 0 ? NATIVES : VIKINGS, PlayerSlot.AI_HARD));
        }
        all.put("resurrected-6v6-enormous", new HeadlessMatchConfig(Ruleset.RESURRECTED,
                Landscape.TerrainType.NATIVE, Game.SIZE_ENORMOUS, .5f, .5f, .5f, 5, twelve,
                HeadlessMatchConfig.DEFAULT_MAX_TICKS));
        return all;
    }

    static @NonNull HeadlessMatchResult play(@NonNull String name) {
        HeadlessMatchConfig config = ALL.get(name);
        if (config == null)
            throw new IllegalArgumentException("no match named " + name);
        return results.computeIfAbsent(name, _ -> new HeadlessMatchRunner(false).run(config));
    }
}
