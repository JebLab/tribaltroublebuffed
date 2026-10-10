package com.oddlabs.tt.player;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.stream.StreamSupport;

public abstract class ChieftainAI {
    /** Decides the 2004 spells only. */
    public final void decide(Unit chieftain) {
        decide(chieftain, false);
    }

    /**
     * @param new_spells whether to consider Buffed's third slot too (docs/design/spells.md): for a Normal or Hard AI
     *                   under a ruleset that has it
     */
    public abstract void decide(Unit chieftain, boolean new_spells);

    protected final int numEnemyUnits(@NonNull Player owner) {
        Player[] players = owner.getWorld().getPlayers();
        int count = Arrays.stream(players).filter(owner::isEnemy).mapToInt(p -> p.getUnits().size()).sum();
        return count;
    }

    /** Living units or buildings of this type within the radius of the chieftain, of its enemies or of its side. */
    protected static <S extends Selectable<?>> int countClose(@NonNull Unit chieftain, float radius,
            @NonNull Class<S> type, boolean enemies) {
        FindOccupantFilter<S> filter = new FindOccupantFilter<>(chieftain.getPositionX(), chieftain.getPositionY(),
                radius, chieftain, type);
        chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
        return (int) StreamSupport.stream(filter.getResult().spliterator(), false).filter(Selectable::isAlive).filter(
                s -> chieftain.getOwner().isEnemy(s.getOwner()) == enemies).count();
    }
}
