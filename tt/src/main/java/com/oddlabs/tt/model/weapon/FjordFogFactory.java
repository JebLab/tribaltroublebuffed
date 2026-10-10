package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

/** Buffed's Fjord Fog (Vikings, docs/design/spells.md): a mist in which enemies hit less often. */
public final class FjordFogFactory extends TimedMagicFactory {
    private final float hit_radius;
    private final float seconds;
    private final float hit_penalty;

    public FjordFogFactory(float hit_radius, float seconds, float hit_penalty, float seconds_per_anim,
            float init_ratio, float release_ratio) {
        super(seconds_per_anim, init_ratio, release_ratio);
        this.hit_radius = hit_radius;
        this.seconds = seconds;
        this.hit_penalty = hit_penalty;
    }

    @Override
    public float getHitRadius() {
        return hit_radius;
    }

    @Override
    public @NonNull Magic execute(@NonNull Unit src) {
        return new FjordFog(hit_radius, seconds, hit_penalty, src);
    }
}
