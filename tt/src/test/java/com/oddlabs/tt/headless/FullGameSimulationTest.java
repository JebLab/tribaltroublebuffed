package com.oddlabs.tt.headless;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plays AI-only matches to the end under every ruleset, without a window: the simulation must run to a victory
 * within the tick limit without throwing. Ported from the restoration fork's test of the same name.
 */
final class FullGameSimulationTest {

    static Set<String> matches() {
        return Matches.ALL.keySet();
    }

    @ParameterizedTest
    @MethodSource("matches")
    void runsToVictory(String name) {
        HeadlessMatchConfig config = Matches.ALL.get(name);
        HeadlessMatchResult result = Matches.play(name);

        String summary = name + " ended at tick " + result.finalTick() + ", survivors " + result.survivors();
        assertTrue(result.victory(), "no winner: " + summary);
        assertTrue(result.finalTick() <= config.maxTicks(), summary);
        assertFalse(result.survivors().isEmpty(), summary);
        for (int player : result.survivors()) {
            assertTrue(config.players().get(player).team() == result.winningTeam(),
                    "player " + player + " survived but is not on team " + result.winningTeam() + ": " + summary);
        }
    }
}
