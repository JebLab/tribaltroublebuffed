package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Hittable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.ruleset.RulesetStats.TorchStats;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Buffed's gear from the Armory, which fights hand to hand: a blow at range 0, like a peon's or a chieftain's
 * ({@link InstantHitFactory}), but with a weapon type, so the Armory stocks, deploys and takes back the gear as it does
 * a thrown weapon. A torch's blow on a building always hits for the torch's damage and sets the building on fire.
 */
public final class GearFactory extends WeaponFactory {
    private final @NonNull Class<?> type;
    private final @Nullable TorchStats torch;
    private final @NonNull Audio @NonNull [] sounds;

    /**
     * @param type          the gear's key ({@link Shield} or {@link Torch})
     * @param release_ratio when in the attack animation the blow lands
     * @param torch         the torch's numbers, or null for gear that strikes buildings like a peon
     */
    public GearFactory(@NonNull Class<?> type, float hit_chance, float release_ratio, @Nullable TorchStats torch,
            @NonNull Audio @NonNull [] sounds) {
        super(hit_chance, 0f, release_ratio);
        this.type = type;
        this.torch = torch;
        this.sounds = sounds;
    }

    @Override
    protected void doAttack(boolean hit, @NonNull Unit src, @NonNull Hittable target) {
        if (torch != null && target instanceof Building building) {
            InstantHitFactory.strike(src, target, torch.building_damage(), sounds);
            if (!building.isDead())
                building.ignite(torch, src.getOwner());
        } else {
            InstantHitFactory.blow(hit, src, target, sounds);
        }
    }

    @Override
    public @NonNull Class<?> getType() {
        return type;
    }
}
