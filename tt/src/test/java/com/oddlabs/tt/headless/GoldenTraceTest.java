package com.oddlabs.tt.headless;

import com.oddlabs.util.Compatibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Compares each headless match with the checksum trace recorded in {@code golden-traces.txt}. CI runs this on Windows
 * and Linux, so the file is the recording made on one system that the other must replay exactly. It also catches a
 * simulation change that did not bump {@link Compatibility#SIM_VERSION}, and a change meant for one ruleset that moves
 * another (Classic must stay the 2004 game).
 * <p>
 * After an intended simulation change, bump {@code SIM_VERSION} and regenerate the file with
 * {@code gradlew tt:test --tests com.oddlabs.tt.headless.GoldenTraceTest -PupdateGoldenTraces}; the diff shows which
 * matches moved.
 */
final class GoldenTraceTest {
    /** One sample per minute of game time keeps the file short. */
    private static final int GOLDEN_INTERVAL = 3000;
    private static final String RESOURCE = "golden-traces.txt";
    private static final String UPDATE_PROPERTY = "headless.goldenTraces";

    static Set<String> matches() {
        return Matches.ALL.keySet();
    }

    @ParameterizedTest
    @MethodSource("matches")
    void matchesGoldenTrace(String name) throws IOException {
        assumeFalse(isUpdating(), "regenerating the golden traces");
        Golden golden = Golden.read();
        assertEquals(Compatibility.SIM_VERSION, golden.sim_version,
                "SIM_VERSION changed since the golden traces were recorded: regenerate them (see GoldenTraceTest)");
        List<String> expected = golden.traces.get(name);
        assertNotNull(expected, "no golden trace for " + name + ": regenerate them (see GoldenTraceTest)");
        List<String> actual = trace(Matches.play(name));
        for (int i = 0; i < Math.max(expected.size(), actual.size()); i++) {
            String want = i < expected.size() ? expected.get(i) : "(end)";
            String got = i < actual.size() ? actual.get(i) : "(end)";
            if (!want.equals(got)) {
                fail(name + " left its golden trace: expected '" + want + "', got '" + got + "'. If the simulation was" + " meant to change, bump SIM_VERSION and regenerate the traces (see GoldenTraceTest); if not," + " this change alters the simulation, or it runs differently on this system.");
            }
        }
    }

    @Test
    void regenerate() throws IOException {
        assumeTrue(isUpdating(), "set -PupdateGoldenTraces to regenerate");
        Path file = Path.of(System.getProperty(UPDATE_PROPERTY));
        List<String> lines = new ArrayList<>(List.of(
                "# Checksum traces of the headless matches in Matches.java: the result, then the world checksum once",
                "# a minute of game time. Written by GoldenTraceTest; regenerate them after an intended simulation",
                "# change, together with a SIM_VERSION bump.",
                "sim_version " + Compatibility.SIM_VERSION));
        for (String name : Matches.ALL.keySet()) {
            lines.add("match " + name);
            lines.addAll(trace(Matches.play(name)));
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
        IO.println("Wrote " + file);
    }

    private static boolean isUpdating() {
        return System.getProperty(UPDATE_PROPERTY) != null;
    }

    private static List<String> trace(HeadlessMatchResult result) {
        List<String> lines = new ArrayList<>();
        lines.add("winner " + result.winningTeam() + " tick " + result.finalTick());
        for (HeadlessMatchResult.Sample sample : result.trace()) {
            if (sample.tick() % GOLDEN_INTERVAL == 0 || sample.tick() == result.finalTick())
                lines.add(sample.toString());
        }
        return lines;
    }

    private record Golden(int sim_version, Map<String, List<String>> traces) {
        static Golden read() throws IOException {
            InputStream in = GoldenTraceTest.class.getResourceAsStream(RESOURCE);
            assertNotNull(in, RESOURCE + " is missing: generate it (see GoldenTraceTest)");
            int sim_version = -1;
            Map<String, List<String>> traces = new LinkedHashMap<>();
            List<String> current = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#"))
                        continue;
                    if (line.startsWith("sim_version ")) {
                        sim_version = Integer.parseInt(line.substring("sim_version ".length()));
                    } else if (line.startsWith("match ")) {
                        current = new ArrayList<>();
                        traces.put(line.substring("match ".length()), current);
                    } else if (current != null) {
                        current.add(line);
                    }
                }
            }
            return new Golden(sim_version, traces);
        }
    }
}
