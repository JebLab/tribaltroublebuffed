package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.ruleset.RulesetStats.PredatorStats;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Random;

/**
 * A boar (tropical islands) or a wolf (northern islands) of Buffed's fauna (docs/design/fauna.md). It lives in a patch
 * of deep forest, roaming a few cells. When a peon comes within its sight with no other unit of the peon's team near
 * it, it charges and strikes once, like a peon's blow; then it goes back to its patch and rests, whatever the outcome.
 * It gives up when the peon reaches company or gets too far from the patch, and never attacks warriors, chieftains,
 * buildings or groups. A kill counts for no player. Warriors and towers may kill it.
 */
public final class Predator extends Animal {
    private static final int ROAM_CELLS = 2;
    private static final float ROAM_CHANCE = .1f;
    private static final float ROAM_SPEED_FACTOR = .4f;

    private enum State {
        ROAM,
        CHARGE,
        RETURN,
        REST
    }

    private @NonNull State state = State.ROAM;
    private @Nullable Unit prey;
    private int strikes;

    public Predator(@NonNull World world, @NonNull Species species, int grid_x, int grid_y) {
        super(world, species, grid_x, grid_y);
        assert species == Species.BOAR || species == Species.WOLF;
    }

    @Override
    public boolean isPrey() {
        return true;
    }

    /** A peon on the ground with no other unit of its team within the company radius. */
    private boolean isLonePeon(@NonNull Unit unit) {
        if (unit.isDead() || !unit.getAbilities().hasAbilities(Abilities.BUILD))
            return false;
        int team = unit.getOwner().getPlayerInfo().getTeam();
        return findUnit(unit.getGridX(), unit.getGridY(), getStats().predator().company_radius(),
                other -> other != unit && other.getOwner().getPlayerInfo().getTeam() == team) == null;
    }

    @Override
    protected void think() {
        PredatorStats stats = getStats().predator();
        switch (state) {
            case ROAM -> {
                prey = findUnit(stats.sight(), this::isLonePeon);
                if (prey != null) {
                    state = State.CHARGE;
                    walkTo(prey, stats.speed());
                } else if (!isWalking()) {
                    Random random = getWorld().getRandom();
                    if (random.nextFloat() < ROAM_CHANCE)
                        walkToCell(getHomeX() + random.nextInt(2 * ROAM_CELLS + 1) - ROAM_CELLS,
                                getHomeY() + random.nextInt(2 * ROAM_CELLS + 1) - ROAM_CELLS,
                                stats.speed() * ROAM_SPEED_FACTOR);
                }
            }
            case CHARGE -> {
                Unit target = prey;
                if (target == null || !isLonePeon(target)
                        || !isWithin(getHomeX(), getHomeY(), target.getGridX(), target.getGridY(), stats.leash())) {
                    goHome(stats);
                } else if (isInReach(target)) {
                    attack(target);
                } else {
                    walkTo(target, stats.speed());
                }
            }
            case RETURN -> {
                if (!isWalking()) {
                    state = State.REST;
                    rest(stats.rest_seconds());
                }
            }
            case REST -> {
                if (!isResting())
                    state = State.ROAM;
            }
        }
    }

    @Override
    protected void strike() {
        Unit target = prey;
        if (target == null || target.isDead() || !isInReach(target))
            return;
        strikes++;
        PredatorStats stats = getStats().predator();
        if (getWorld().getRandom().nextFloat() < stats.hit_chance() * (1 - target.getDefenseChance())) {
            float dx = target.getPositionX() - getPositionX();
            float dy = target.getPositionY() - getPositionY();
            float length = Math.max((float) Math.sqrt(dx * dx + dy * dy), .01f);
            target.hitByAnimal(1, dx / length, dy / length);
        }
    }

    @Override
    protected void attackDone() {
        goHome(getStats().predator());
    }

    /** Blows it has struck at a peon in reach, hit or miss. */
    public int getStrikes() {
        return strikes;
    }

    private void goHome(@NonNull PredatorStats stats) {
        prey = null;
        state = State.RETURN;
        walkHome(stats.speed());
    }
}
