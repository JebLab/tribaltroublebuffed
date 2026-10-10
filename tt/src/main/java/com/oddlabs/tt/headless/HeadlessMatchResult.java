package com.oddlabs.tt.headless;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * How a headless match ended.
 *
 * @param winningTeam   the last team standing, or -1 when the tick limit ended the match
 * @param finalTick     the tick the match ended on
 * @param finalChecksum {@link HeadlessMatchRunner#checksum} at that tick
 * @param survivors     indices of the players still alive at the end
 * @param trace         the checksum at every sampled tick, in order
 * @param census        per player, what it had built at any sampled tick
 */
public record HeadlessMatchResult(int winningTeam, int finalTick, int finalChecksum,
                                  @NonNull List<@NonNull Integer> survivors, @NonNull List<@NonNull Sample> trace,
                                  @NonNull List<@NonNull Census> census) {

    /** The world checksum at one tick. */
    public record Sample(int tick, int checksum) {
        @Override
        public @NonNull String toString() {
            return tick + " " + Integer.toHexString(checksum);
        }
    }

    /**
     * What one player had at the sampled ticks.
     *
     * @param completedBuildings template ids ({@code Race.BUILDING_*}) of the finished buildings it had
     * @param chickenCoopBred    whether one of its Chicken Coops had let out a chicken
     * @param unitTypes          {@code Race.UNIT_*} ids of the units it had in the field
     * @param firesLit           buildings its torches set on fire (a fire started again counts again)
     * @param tradesMade         trades its Markets made
     */
    public record Census(@NonNull Set<@NonNull Integer> completedBuildings, boolean chickenCoopBred,
                         @NonNull Set<@NonNull Integer> unitTypes, int firesLit, int tradesMade) {
        public Census {
            completedBuildings = Collections.unmodifiableSortedSet(new TreeSet<>(completedBuildings));
            unitTypes = Collections.unmodifiableSortedSet(new TreeSet<>(unitTypes));
        }
    }

    public HeadlessMatchResult {
        survivors = List.copyOf(survivors);
        trace = List.copyOf(trace);
        census = List.copyOf(census);
    }

    public boolean victory() {
        return winningTeam >= 0;
    }
}
