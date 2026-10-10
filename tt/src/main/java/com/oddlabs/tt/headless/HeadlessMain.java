package com.oddlabs.tt.headless;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plays one headless match from the command line and prints its checksum trace, one {@code checksum <tick> <hex>}
 * line per sample, then a {@code result} line. The options are those of {@link HeadlessMatchConfig#toArguments()},
 * plus {@code --sample-interval <ticks>} and {@code --quiet}. Without {@code --player} options it plays a 1v1 under
 * the skirmish default ruleset.
 */
public final class HeadlessMain {
    private HeadlessMain() {
    }

    static void main(String @NonNull [] args) {
        List<String> match_args = new ArrayList<>();
        int sample_interval = HeadlessMatchRunner.DEFAULT_SAMPLE_INTERVAL;
        boolean verbose = true;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--sample-interval" -> sample_interval = Integer.parseInt(args[++i]);
                case "--quiet" -> verbose = false;
                default -> match_args.add(args[i]);
            }
        }
        HeadlessMatchConfig config = HeadlessMatchConfig.fromArguments(match_args);
        if (config.players().isEmpty()) {
            List<String> with_players = new ArrayList<>(match_args);
            with_players.addAll(Arrays.asList("--player", "0:0:3", "--player", "1:1:3"));
            config = HeadlessMatchConfig.fromArguments(with_players);
        }
        HeadlessMatchResult result = new HeadlessMatchRunner(sample_interval, verbose).run(config);
        for (HeadlessMatchResult.Sample sample : result.trace()) {
            IO.println("checksum " + sample);
        }
        IO.println(
                "result winner=" + result.winningTeam() + " tick=" + result.finalTick() + " checksum=" + Integer.toHexString(
                        result.finalChecksum()) + " survivors=" + result.survivors());
    }
}
