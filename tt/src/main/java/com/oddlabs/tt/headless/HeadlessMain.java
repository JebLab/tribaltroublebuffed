package com.oddlabs.tt.headless;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plays one headless match from the command line and prints its checksum trace, one {@code checksum <tick> <hex>}
 * line per sample, then a {@code result} line. The options are those of {@link HeadlessMatchConfig#toArguments()},
 * plus {@code --quiet}. Without {@code --player} options it plays a 1v1 under
 * the skirmish default ruleset.
 */
public final class HeadlessMain {
    private HeadlessMain() {
    }

    static void main(String @NonNull [] args) {
        List<String> match_args = new ArrayList<>();
        boolean verbose = true;
        for (String arg : args) {
            if (arg.equals("--quiet"))
                verbose = false;
            else
                match_args.add(arg);
        }
        HeadlessMatchConfig config = HeadlessMatchConfig.fromArguments(match_args);
        if (config.players().isEmpty()) {
            List<String> with_players = new ArrayList<>(match_args);
            with_players.addAll(Arrays.asList("--player", "0:0:3", "--player", "1:1:3"));
            config = HeadlessMatchConfig.fromArguments(with_players);
        }
        HeadlessMatchResult result = new HeadlessMatchRunner(verbose).run(config);
        for (HeadlessMatchResult.Sample sample : result.trace()) {
            IO.println("checksum " + sample);
        }
        IO.println(
                "result winner=" + result.winningTeam() + " tick=" + result.finalTick() + " checksum=" + Integer.toHexString(
                        result.finalChecksum()) + " survivors=" + result.survivors());
        for (int i = 0; i < result.census().size(); i++) {
            HeadlessMatchResult.Census census = result.census().get(i);
            IO.println(
                    "census player=" + i + " buildings=" + census.completedBuildings() + " coop_bred=" + census.chickenCoopBred() + " units=" + census.unitTypes() + " fires=" + census.firesLit() + " snares=" + census.snaresLaid() + "/" + census.snaresSprung() + " lost_to_animals=" + census.unitsLostToAnimals() + " loads_stolen=" + census.loadsStolen() + " animals_killed=" + census.animalsKilled());
        }
    }
}
