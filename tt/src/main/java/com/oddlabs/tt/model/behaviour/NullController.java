package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class NullController extends Controller {
    private final @NonNull Selectable<?> selectable;

    public NullController(@NonNull Selectable<?> s) {
        super(0);
        this.selectable = s;
    }

    @Override
    public @NonNull Object getKey() {
        Abilities abilities = selectable.getAbilities();
        // Walls have no job: their template tells them apart, finished or not.
        return List.of(super.getKey(), abilities.hasAbilities(Abilities.BUILD_ARMIES),
                abilities.hasAbilities(Abilities.REPRODUCE), abilities.hasAbilities(Abilities.ATTACK),
                abilities.hasAbilities(Abilities.SAIL), abilities.hasAbilities(Abilities.BREED),
                abilities.hasAbilities(Abilities.AURA), abilities.hasAbilities(Abilities.TRADE),
                selectable instanceof Building building && building.isWall());
    }

    @Override
    public void decide() {
        selectable.setBehaviour(new NullBehaviour());
    }
}
