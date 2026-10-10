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
        // Walls have no job: their template tells them apart, finished or not. A finished Great Tower has the Tower's
        // job, and its template tells them apart.
        return List.of(super.getKey(), abilities.hasAbilities(Abilities.BUILD_ARMIES),
                abilities.hasAbilities(Abilities.REPRODUCE), abilities.hasAbilities(Abilities.ATTACK),
                abilities.hasAbilities(Abilities.SAIL), abilities.hasAbilities(Abilities.BREED),
                abilities.hasAbilities(Abilities.AURA), abilities.hasAbilities(Abilities.TRADE),
                selectable instanceof Building wall && wall.isWall(),
                abilities.hasAbilities(Abilities.SHELTER),
                abilities.hasAbilities(Abilities.ATTACK) && selectable instanceof Building tower
                        && tower.isGreatTower());
    }

    @Override
    public void decide() {
        selectable.setBehaviour(new NullBehaviour());
    }
}
