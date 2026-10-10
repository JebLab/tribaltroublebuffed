package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Selectable;
import org.jspecify.annotations.NonNull;

/**
 * A spell cast at a chosen target within a range (Buffed's Hammer of Thor, docs/design/spells.md). The chieftain walks
 * into range first ({@code CastController}); {@link #aimedAt} gives the factory for one cast at that target.
 */
public interface TargetedMagicFactory extends MagicFactory {
    /** Meters from the chieftain to the target's edge within which it casts. */
    float getRange();

    @NonNull
    MagicFactory aimedAt(@NonNull Selectable<?> target);
}
