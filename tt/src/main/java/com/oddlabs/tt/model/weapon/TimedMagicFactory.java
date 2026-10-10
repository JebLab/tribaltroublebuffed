package com.oddlabs.tt.model.weapon;

/**
 * A spell's timing on the chieftain's animation, shared by Buffed's third-slot spells (docs/design/spells.md): the
 * animation lasts {@code seconds_per_anim}; the spell is made at {@code init_ratio} of it and takes effect at
 * {@code release_ratio}.
 */
abstract class TimedMagicFactory implements MagicFactory {
    private final float seconds_per_anim;
    private final float init_ratio;
    private final float release_ratio;

    TimedMagicFactory(float seconds_per_anim, float init_ratio, float release_ratio) {
        this.seconds_per_anim = seconds_per_anim;
        this.init_ratio = init_ratio;
        this.release_ratio = release_ratio;
    }

    @Override
    public final float getSecondsPerAnim() {
        return seconds_per_anim;
    }

    @Override
    public final float getSecondsPerInit() {
        return init_ratio * seconds_per_anim;
    }

    @Override
    public final float getSecondsPerRelease() {
        return release_ratio * seconds_per_anim;
    }
}
