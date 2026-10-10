package com.oddlabs.tt.headless;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * How a headless match ended.
 *
 * @param winningTeam   the last team standing, or -1 when the tick limit ended the match
 * @param finalTick     the tick the match ended on
 * @param finalChecksum {@link HeadlessMatchRunner#checksum} at that tick
 * @param survivors     indices of the players still alive at the end
 * @param trace         the checksum at every sampled tick, in order
 */
public record HeadlessMatchResult(int winningTeam, int finalTick, int finalChecksum,
                                  @NonNull List<@NonNull Integer> survivors, @NonNull List<@NonNull Sample> trace) {

    /** The world checksum at one tick. */
    public record Sample(int tick, int checksum) {
        @Override
        public @NonNull String toString() {
            return tick + " " + Integer.toHexString(checksum);
        }
    }

    public HeadlessMatchResult {
        survivors = List.copyOf(survivors);
        trace = List.copyOf(trace);
    }

    public boolean victory() {
        return winningTeam >= 0;
    }
}
