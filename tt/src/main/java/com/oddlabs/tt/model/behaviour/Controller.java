package com.oddlabs.tt.model.behaviour;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;

public abstract class Controller {
    private static final int MAX_TRIES = 1;
    private final int @NonNull [] give_up_counters;

    protected Controller(int num_states) {
        give_up_counters = new int[num_states];
    }

    public final void resetGiveUpCounters() {
        Arrays.fill(give_up_counters, 0);
    }

    public final void resetGiveUpCounter(int state_index) {
        give_up_counters[state_index] = 0;
    }

    protected final boolean shouldGiveUp(int state_index) {
        if (give_up_counters[state_index] != MAX_TRIES) {
            give_up_counters[state_index]++;
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns the key that {@link com.oddlabs.tt.player.Player#classifyUnits()} groups units by. Two controllers share
     * a group when their keys are {@code equals}. Keys must not be built from identity hash codes: those differ
     * between JVM runs, so lockstep clients and event-log replays would group units differently.
     */
    public @NonNull Object getKey() {
        return getClass();
    }

    public abstract void decide();
}
