package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

/** Buffed's Poultry Panic (Natives, docs/design/spells.md): a chicken stampede knocks enemies flat. */
public final class PoultryPanicFactory extends TimedMagicFactory {
    private final float hit_radius;
    private final float stun_seconds;
    private final int chickens;

    public PoultryPanicFactory(float hit_radius, float stun_seconds, int chickens, float seconds_per_anim,
            float init_ratio, float release_ratio) {
        super(seconds_per_anim, init_ratio, release_ratio);
        this.hit_radius = hit_radius;
        this.stun_seconds = stun_seconds;
        this.chickens = chickens;
    }

    @Override
    public float getHitRadius() {
        return hit_radius;
    }

    @Override
    public @NonNull Magic execute(@NonNull Unit src) {
        return new PoultryPanic(hit_radius, stun_seconds, chickens, src);
    }
}
