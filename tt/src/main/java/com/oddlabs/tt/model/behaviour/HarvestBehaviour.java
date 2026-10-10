package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class HarvestBehaviour implements Behaviour {
    private static final float SECONDS_PER_ANIMATION_CYCLE = 1f;
    private final @NonNull Supply supply;
    private final @NonNull Unit unit;
    private float anim_time;
    private boolean sound;

    public HarvestBehaviour(@NonNull Unit unit, @NonNull Supply supply) {
        this.unit = unit;
        this.supply = supply;
        unit.aimAtTarget(supply);
        restartAnimation();
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public @NonNull State animate(float t) {
        anim_time += t;
        if (anim_time > unit.getWeaponFactory().getSecondsPerRelease(1f / SECONDS_PER_ANIMATION_CYCLE) && !sound) {
            sound = true;
            unit.getOwner().getWorld().getAudio().newAudio(new AudioParameters<>(
                    unit.getOwner().getWorld().getRacesResources().getHarvestSound(supply.getClass(),
                            unit.getOwner().getWorld().getRandom()),
                    unit.getPositionX(), unit.getPositionY(), unit.getPositionZ(),
                    AudioPlayer.AUDIO_RANK_HARVEST,
                    AudioPlayer.AUDIO_DISTANCE_HARVEST,
                    AudioPlayer.AUDIO_GAIN_HARVEST,
                    AudioPlayer.AUDIO_RADIUS_HARVEST));
            if (stroke()) {
                unit.getSupplyContainer().increaseSupply(1, supply.getClass());
                unit.getOwner().harvested(supply.getClass());
            }
        }

        if (anim_time > SECONDS_PER_ANIMATION_CYCLE) {
            restartAnimation();
            if (unit.getSupplyContainer().isSupplyFull() || supply.isEmpty())
                return State.DONE;
        }

        return State.INTERRUPTIBLE;
    }

    /** One stroke at the supply; whether it yielded a load. A net (Buffed) catches a chicken in one stroke. */
    private boolean stroke() {
        if (unit.isNetter() && supply instanceof RubberSupply) {
            for (int i = 0; i < Supply.HITS_PER_HARVEST; i++) {
                if (supply.hit())
                    return true;
            }
            return false;
        }
        return supply.hit();
    }

    private void restartAnimation() {
        unit.switchAnimation(1f / SECONDS_PER_ANIMATION_CYCLE, Unit.Animation.THROWING);
        anim_time = 0;
        sound = false;
    }

    @Override
    public void forceInterrupted() {
    }
}
