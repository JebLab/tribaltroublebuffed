package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

/**
 * A peon working one load into a building. A log adds a hit point per animation cycle, five in all; any other
 * material (the Totem's rock, a chicken for the Chicken Coop) is handed over whole after one cycle.
 */
public final class RepairBehaviour implements Behaviour {
    public static final int REPAIRS_PER_SUPPLY = 5;
    private static final float SECONDS_PER_ANIMATION_CYCLE = 1f;
    private final @NonNull Building building;
    private final @NonNull Unit unit;
    private final @NonNull Class<? extends Supply> material;

    private float anim_time;
    private int repairs;
    private boolean sound;

    public RepairBehaviour(@NonNull Unit unit, @NonNull Building building) {
        this(unit, building, TreeSupply.class);
    }

    public RepairBehaviour(@NonNull Unit unit, @NonNull Building building,
            @NonNull Class<? extends Supply> material) {
        this.unit = unit;
        this.building = building;
        this.material = material;
        unit.aimAtTarget(building);
        restartAnimation();
        unit.getSupplyContainer().increaseSupply(-1, material);
        repairs = 0;
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
                    unit.getOwner().getWorld().getRacesResources().getHarvestSound(material,
                            unit.getOwner().getWorld().getRandom()), unit.getPositionX(), unit.getPositionY(),
                    unit.getPositionZ(),
                    AudioPlayer.AUDIO_RANK_HARVEST,
                    AudioPlayer.AUDIO_DISTANCE_HARVEST,
                    AudioPlayer.AUDIO_GAIN_HARVEST,
                    AudioPlayer.AUDIO_RADIUS_HARVEST));
        }

        if (anim_time > SECONDS_PER_ANIMATION_CYCLE) {
            restartAnimation();
            repairs++;
            if (material != TreeSupply.class) {
                if (!building.isDead() && building.needsMaterial(material))
                    building.deliverMaterial(material);
                return State.DONE;
            }
            if (building.isDead() || !building.needsMaterial(TreeSupply.class)) {
                return State.DONE;
            } else
                building.repair(1);
        }

        return repairs == REPAIRS_PER_SUPPLY ? State.DONE : State.INTERRUPTIBLE;
    }

    private void restartAnimation() {
        anim_time = 0;
        sound = false;
        unit.switchAnimation(1f / SECONDS_PER_ANIMATION_CYCLE, Unit.Animation.THROWING);
    }

    @Override
    public void forceInterrupted() {
    }
}
