package com.oddlabs.tt.headless;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The same seeded match must produce the same checksums wherever it runs. Lockstep multiplayer, replays and save games
 * all depend on it.
 */
final class DeterminismTest {
    private static final String MATCH = "buffed-6p";

    @Test
    void sameMatchTwiceInOneJvm() {
        HeadlessMatchResult first = Matches.play(MATCH);
        HeadlessMatchResult again = new HeadlessMatchRunner(false).run(
                Matches.ALL.get(MATCH));

        assertEquals(first.trace(), again.trace());
    }

    /**
     * Nondeterminism that depends on identity hash codes (iteration order of a {@code HashMap} keyed by objects
     * without their own {@code hashCode}) is stable inside one JVM and often between runs too, so the match is played
     * in two fresh JVMs, the second with every identity hash code equal ({@code -XX:hashCode=2}), and both traces must
     * equal each other and the one played in this JVM after other matches. With the fix of the AI unit grouping bug
     * (PR #1) reverted, the second JVM ends this match about three minutes of game time later; a JVM with another
     * hash generator ({@code -XX:hashCode=0}) did not show the bug in this match.
     */
    @Test
    void sameMatchInTwoJvms(@TempDir Path dir) throws Exception {
        List<String> args = Matches.ALL.get(MATCH).toArguments();
        Process plain = start(args, List.of(), dir.resolve("plain.out"));
        Process equal_hashes = start(args, List.of("-XX:+UnlockExperimentalVMOptions", "-XX:hashCode=2"), dir.resolve(
                "equal_hashes.out"));

        List<String> plain_trace = finish(plain, dir.resolve("plain.out"));
        List<String> equal_trace = finish(equal_hashes, dir.resolve("equal_hashes.out"));

        assertTrue(plain_trace.size() > 10, "too few samples: " + plain_trace);
        assertEquals(plain_trace, equal_trace, "the two JVMs disagree");
        List<String> here = Matches.play(MATCH).trace().stream().map(sample -> "checksum " + sample).toList();
        assertEquals(here, plain_trace.subList(0, plain_trace.size() - 1), "this JVM disagrees with a fresh one");
    }

    private static Process start(List<String> args, List<String> jvm_options, Path output) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(jvm_options);
        command.addAll(List.of("-ea", "--enable-native-access=ALL-UNNAMED", "-cp", System.getProperty(
                "java.class.path"), HeadlessMain.class.getName(), "--quiet"));
        command.addAll(args);
        return new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
    }

    /** The {@code checksum} lines and the {@code result} line of a finished run. */
    private static List<String> finish(Process process, Path output) throws Exception {
        assertTrue(process.waitFor(5, TimeUnit.MINUTES), "timed out");
        List<String> lines = Files.readAllLines(output);
        assertEquals(0, process.exitValue(), () -> String.join("\n", lines));
        return lines.stream().filter(line -> line.startsWith("checksum ") || line.startsWith("result ")).toList();
    }
}
