package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Unit;

public interface MagicFactory {
    float getHitRadius();

    float getSecondsPerAnim();

    float getSecondsPerInit();

    float getSecondsPerRelease();

    Magic execute(Unit src);

    /** The chieftain's animation while casting: {@code Unit.Animation.MAGIC}, or the Hammer of Thor's {@code THOR}. */
    default int getAnimation() {
        return Unit.Animation.MAGIC;
    }
}
