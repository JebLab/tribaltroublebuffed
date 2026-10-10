package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

/** Buffed's Jolly Jungle (Natives, docs/design/spells.md): vines root every enemy nearby. */
public final class JollyJungleFactory extends TimedMagicFactory {
    private final float hit_radius;
    private final float seconds;

    public JollyJungleFactory(float hit_radius, float seconds, float seconds_per_anim, float init_ratio,
            float release_ratio) {
        super(seconds_per_anim, init_ratio, release_ratio);
        this.hit_radius = hit_radius;
        this.seconds = seconds;
    }

    @Override
    public float getHitRadius() {
        return hit_radius;
    }

    @Override
    public @NonNull Magic execute(@NonNull Unit src) {
        return new JollyJungle(hit_radius, seconds, src);
    }
}
