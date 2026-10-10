package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

/**
 * Buffed's Hammer of Thor (Vikings, docs/design/spells.md): the chieftain raises the hammer (its {@code thor}
 * animation) and one bolt strikes a chosen enemy within range. It is only ever cast at a target ({@link #aimedAt}).
 */
public final class HammerOfThorFactory extends TimedMagicFactory implements TargetedMagicFactory {
    private final float range;
    private final int damage;

    public HammerOfThorFactory(float range, int damage, float seconds_per_anim, float init_ratio,
            float release_ratio) {
        super(seconds_per_anim, init_ratio, release_ratio);
        this.range = range;
        this.damage = damage;
    }

    @Override
    public float getHitRadius() {
        return range;
    }

    @Override
    public float getRange() {
        return range;
    }

    @Override
    public int getAnimation() {
        return Unit.Animation.THOR;
    }

    @Override
    public @NonNull Magic execute(@NonNull Unit src) {
        throw new IllegalStateException("Hammer of Thor is cast at a target");
    }

    @Override
    public @NonNull MagicFactory aimedAt(@NonNull Selectable<?> target) {
        HammerOfThorFactory factory = this;
        return new MagicFactory() {
            @Override
            public float getHitRadius() {
                return factory.getHitRadius();
            }

            @Override
            public float getSecondsPerAnim() {
                return factory.getSecondsPerAnim();
            }

            @Override
            public float getSecondsPerInit() {
                return factory.getSecondsPerInit();
            }

            @Override
            public float getSecondsPerRelease() {
                return factory.getSecondsPerRelease();
            }

            @Override
            public int getAnimation() {
                return factory.getAnimation();
            }

            @Override
            public @NonNull Magic execute(@NonNull Unit src) {
                return new HammerOfThor(damage, src, target);
            }
        };
    }
}
