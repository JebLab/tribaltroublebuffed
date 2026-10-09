package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
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
        return List.of(super.getKey(), abilities.hasAbilities(Abilities.BUILD_ARMIES),
                abilities.hasAbilities(Abilities.REPRODUCE), abilities.hasAbilities(Abilities.ATTACK),
                abilities.hasAbilities(Abilities.SAIL));
    }

    @Override
    public void decide() {
        selectable.setBehaviour(new NullBehaviour());
    }
}
