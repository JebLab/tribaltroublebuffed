package com.oddlabs.tt.model;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Movable;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.PathTracker;
import com.oddlabs.tt.pathfinder.ScanFilter;
import com.oddlabs.tt.pathfinder.TargetTrackerAlgorithm;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.ruleset.RulesetStats.FaunaStats;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * One of Buffed's wild animals (docs/design/fauna.md): a world object of its own, like a chicken, that belongs to no
 * player, so it never counts as anyone's unit and no AI commands it. It walks the unit grid with a {@link PathTracker}
 * from a home cell and decides what to do twice a second, from the world's random numbers, in the world's animation
 * order. Like everyone it has one hit point: warriors and towers may kill the animals that are {@link #isPrey prey},
 * and a kill counts in no player's statistics. A dead animal falls over and sinks into the ground.
 */
public abstract class Animal extends Model implements Animated, Movable, Occupant, Hittable, ModelToolTip {
    /** The animations, by their position in the animal's sprite. */
    protected enum Animation {
        IDLE,
        RUN,
        ATTACK
    }

    /** The kinds of animal, with the numbers tied to their models. */
    public enum Species {
        // Prey are as big as a unit to aim at; thrown weapons aim at about half their height.
        CRAB(1f, .2f),
        MONKEY(1.9f, .6f),
        BOAR(1.9f, .55f),
        WOLF(1.9f, .65f);

        private final float size;
        private final float hit_offset_z;

        Species(float size, float hit_offset_z) {
            this.size = size;
            this.hit_offset_z = hit_offset_z;
        }
    }

    private static final float IDLE_CYCLES_PER_SECOND = .5f;
    // The attack animation plays once in this time; the blow lands half way.
    private static final float ATTACK_SECONDS = .6f;
    // A dead animal falls onto its side, lies there a while, then sinks out of sight.
    private static final float FALL_SECONDS = .4f;
    private static final float LIE_SECONDS = 1.5f;
    private static final float SINK_SECONDS = 3f;
    private static final float SINK_DEPTH = 1f;
    private static final float THINK_SECONDS = .5f;
    // Like a unit that has not been in the way yet: others wait for it or walk around it.
    private static final int PATH_PENALTY = 5;

    private final @NonNull Species species;
    private final @NonNull SpriteKey sprite;
    private final @NonNull PathTracker tracker;
    private final int home_x;
    private final int home_y;

    private int grid_x;
    private int grid_y;
    private @NonNull Animation animation = Animation.IDLE;
    private float anim_time;
    private boolean walking;
    private float speed;
    // The tracker is between two cells: no new path until it reaches the next one.
    private boolean between_cells;
    private float think_time;
    private float rest_time;
    // At least 0 while the attack animation plays.
    private float attack_time = -1;
    private boolean struck;
    private boolean dead;
    private float dead_time;

    protected Animal(@NonNull World world, @NonNull Species species, int grid_x, int grid_y) {
        super(world);
        this.species = species;
        this.sprite = world.getRacesResources().getAnimalSprite(species);
        this.tracker = new PathTracker(world.getUnitGrid(), this);
        this.grid_x = grid_x;
        this.grid_y = grid_y;
        this.home_x = grid_x;
        this.home_y = grid_y;
        float angle = world.getRandom().nextFloat() * 2f * (float) Math.PI;
        setDirection((float) Math.cos(angle), (float) Math.sin(angle));
        think_time = world.getRandom().nextFloat() * THINK_SECONDS;
        setPosition(UnitGrid.coordinateFromGrid(grid_x), UnitGrid.coordinateFromGrid(grid_y));
        world.getUnitGrid().occupyGrid(grid_x, grid_y, this);
        world.getNotificationListener().registerTarget(this);
        register();
        reinsert();
        world.getAnimationManagerGameTime().registerAnimation(this);
        world.getAnimals().add(this);
    }

    public final @NonNull Species getSpecies() {
        return species;
    }

    /** Whether warriors and towers may attack it (every animal but the crab). */
    public abstract boolean isPrey();

    /** What the animal does next; called twice a second while it is on a cell, and when it stops walking. */
    protected abstract void think();

    /** The blow of the attack animation lands. */
    protected void strike() {
    }

    /** The attack animation has ended. */
    protected void attackDone() {
    }

    protected final @NonNull FaunaStats getStats() {
        return Fauna.getStats(getWorld());
    }

    @Override
    public final void animate(float t) {
        if (dead) {
            dead_time += t;
            if (dead_time >= FALL_SECONDS + LIE_SECONDS + SINK_SECONDS) {
                getWorld().getAnimationManagerGameTime().removeAnimation(this);
                remove();
            } else {
                reinsert();
            }
            return;
        }
        if (rest_time > 0)
            rest_time -= t;
        if (attack_time >= 0) {
            attack_time += t;
            anim_time = Math.min(1f, attack_time / ATTACK_SECONDS);
            if (!struck && attack_time >= ATTACK_SECONDS / 2) {
                struck = true;
                strike();
            }
            if (attack_time >= ATTACK_SECONDS) {
                attack_time = -1;
                setAnimation(Animation.IDLE);
                attackDone();
            }
            return;
        }
        if (walking) {
            float meters = speed * t;
            anim_time += meters;
            PathTracker.State state = tracker.animate(meters);
            switch (state) {
                case OK -> between_cells = true;
                case OK_INTERRUPTIBLE -> between_cells = false;
                case DONE, BLOCKED, SOFTBLOCKED -> {
                    between_cells = false;
                    walking = false;
                    setAnimation(Animation.IDLE);
                    think_time = 0;
                }
            }
        } else {
            anim_time += IDLE_CYCLES_PER_SECOND * t;
        }
        think_time -= t;
        if (think_time <= 0 && !between_cells) {
            think_time = THINK_SECONDS;
            think();
        }
    }

    // Moving and acting, for the kinds of animal

    /** Walks next to the target, or onto it if it is a free cell, at this many meters per second. */
    protected final void walkTo(@NonNull Target target, float meters_per_second) {
        tracker.setTarget(new TargetTrackerAlgorithm(getWorld().getUnitGrid(), 0f, target));
        walking = true;
        speed = meters_per_second;
        if (animation != Animation.RUN)
            setAnimation(Animation.RUN);
    }

    /** Walks to the free cell nearest to the grid point. */
    protected final void walkToCell(int x, int y, float meters_per_second) {
        UnitGrid grid = getWorld().getUnitGrid();
        int size = grid.getGridSize();
        Target target = grid.findGridTargets(Math.clamp(x, 0, size - 1), Math.clamp(y, 0, size - 1), 1, false)[0];
        walkTo(target, meters_per_second);
    }

    /** Walks back to its home cell. */
    protected final void walkHome(float meters_per_second) {
        walkToCell(home_x, home_y, meters_per_second);
    }

    protected final boolean isWalking() {
        return walking;
    }

    /** Plays the attack animation facing the target: {@link #strike} half way, then {@link #attackDone}. */
    protected final void attack(@NonNull Target target) {
        walking = false;
        float dx = target.getPositionX() - getPositionX();
        float dy = target.getPositionY() - getPositionY();
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 0)
            setDirection(dx / length, dy / length);
        setAnimation(Animation.ATTACK);
        attack_time = 0;
        struck = false;
    }

    /** Does nothing new for this long. */
    protected final void rest(float seconds) {
        rest_time = seconds;
    }

    protected final boolean isResting() {
        return rest_time > 0;
    }

    protected final int getHomeX() {
        return home_x;
    }

    protected final int getHomeY() {
        return home_y;
    }

    /** Whether two cells are at most this many meters apart. */
    protected static boolean isWithin(int x0, int y0, int x1, int y1, float meters) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        float cells = meters / HeightMap.METERS_PER_UNIT_GRID;
        return dx * dx + dy * dy <= cells * cells;
    }

    /** Whether the target is on a cell next to this animal's, or the one beyond (a lunge reaches it). */
    protected final boolean isInReach(@NonNull Target target) {
        int dx = target.getGridX() - grid_x;
        int dy = target.getGridY() - grid_y;
        return dx * dx + dy * dy <= 4;
    }

    /**
     * The first living unit of any player on the ground within this many meters that passes the test, nearest first.
     */
    protected final @Nullable Unit findUnit(int center_x, int center_y, float meters, @NonNull Predicate<Unit> test) {
        UnitFinder finder = new UnitFinder(center_x, center_y, meters, test);
        getWorld().getUnitGrid().scan(finder, center_x, center_y);
        return finder.found;
    }

    protected final @Nullable Unit findUnit(float meters, @NonNull Predicate<Unit> test) {
        return findUnit(grid_x, grid_y, meters, test);
    }

    private static final class UnitFinder implements ScanFilter {
        private final int center_x;
        private final int center_y;
        private final float meters;
        private final int radius;
        private final @NonNull Predicate<Unit> test;
        private @Nullable Unit found;

        UnitFinder(int center_x, int center_y, float meters, @NonNull Predicate<Unit> test) {
            this.center_x = center_x;
            this.center_y = center_y;
            this.meters = meters;
            this.radius = (int) Math.ceil(meters / HeightMap.METERS_PER_UNIT_GRID);
            this.test = test;
        }

        @Override
        public int getMinRadius() {
            return 1;
        }

        @Override
        public int getMaxRadius() {
            return radius;
        }

        @Override
        public boolean filter(int grid_x, int grid_y, @Nullable Occupant occupant) {
            if (occupant instanceof Unit unit && !unit.isDead()
                    && isWithin(center_x, center_y, grid_x, grid_y, meters) && test.test(unit)) {
                found = unit;
                return true;
            }
            return false;
        }
    }

    // Being hit

    @Override
    public final void hit(int damage, float direction_x, float direction_y, @NonNull Player attacker) {
        if (dead || damage <= 0 || !isPrey())
            return;
        attacker.animalKilled();
        dead = true;
        walking = false;
        attack_time = -1;
        UnitGrid grid = getWorld().getUnitGrid();
        if (grid.getOccupant(grid_x, grid_y) == this)
            grid.freeGrid(grid_x, grid_y, this);
        getWorld().getNotificationListener().unregisterTarget(this);
        getWorld().getAnimals().remove(this);
        setDirection(-direction_x, -direction_y);
        setAnimation(Animation.IDLE);
        getWorld().getAudio().newAudio(new AudioParameters<>(getWorld().getLandscapeResources().getBirdDeathSound(),
                getPositionX(), getPositionY(), getPositionZ(),
                AudioPlayer.AUDIO_RANK_DEATH,
                AudioPlayer.AUDIO_DISTANCE_DEATH,
                AudioPlayer.AUDIO_GAIN_CHICKEN_DEATH,
                AudioPlayer.AUDIO_RADIUS_CHICKEN_DEATH));
    }

    @Override
    public final boolean isDead() {
        return dead;
    }

    @Override
    public final float getDefenseChance() {
        return getStats().defense_chance();
    }

    @Override
    public final float getHitOffsetZ() {
        return species.hit_offset_z;
    }

    @Override
    public final float getSize() {
        return species.size;
    }

    @Override
    public final int getPenalty() {
        return PATH_PENALTY;
    }

    // Movable, for the path tracker

    @Override
    public final boolean isMoving() {
        // Like a chicken: never part of the units' deadlock solving.
        return false;
    }

    @Override
    public final int getGridX() {
        return grid_x;
    }

    @Override
    public final int getGridY() {
        return grid_y;
    }

    @Override
    public final @NonNull PathTracker getTracker() {
        return tracker;
    }

    @Override
    public final void free() {
        getWorld().getUnitGrid().freeGrid(grid_x, grid_y, this);
    }

    @Override
    public final void occupy() {
        getWorld().getUnitGrid().occupyGrid(grid_x, grid_y, this);
    }

    @Override
    public final void setGridPosition(int grid_x, int grid_y) {
        this.grid_x = grid_x;
        this.grid_y = grid_y;
    }

    @Override
    public final void markBlocking() {
    }

    /** Its name in the player's language, for the tooltip. */
    public final @NonNull String getName() {
        return getWorld().getRacesResources().getAnimalName(species);
    }

    @Override
    public final void visit(@NonNull ToolTipVisitor visitor) {
        visitor.visitAnimal(this);
    }

    // Drawing

    private void setAnimation(@NonNull Animation animation) {
        this.animation = animation;
        anim_time = 0;
    }

    @Override
    public final int getAnimation() {
        // Called by the super constructor, before the field is set.
        return animation != null ? animation.ordinal() : 0;
    }

    @Override
    public final float getAnimationTicks() {
        return anim_time;
    }

    @Override
    public final @NonNull SpriteKey getSpriteRenderer() {
        return sprite;
    }

    @Override
    public final float getShadowDiameter() {
        return species.size * .8f;
    }

    @Override
    protected final float getZError() {
        return getLandscapeError();
    }

    @Override
    public final float getOffsetZ() {
        float sinking = dead ? dead_time - FALL_SECONDS - LIE_SECONDS : 0f;
        return sinking <= 0f ? 0f : -SINK_DEPTH * Math.min(1f, sinking / SINK_SECONDS);
    }

    /** How far a dead animal has fallen onto its side, in radians (0 while it lives). */
    public final float getFallAngle() {
        return dead ? (float) Math.PI / 2 * Math.min(1f, dead_time / FALL_SECONDS) : 0f;
    }

    @Override
    public final void updateChecksum(@NonNull StateChecksum checksum) {
        checksum.update(grid_x);
        checksum.update(grid_y);
    }

    @Override
    public final void visit(@NonNull ElementVisitor visitor) {
        visitor.visitAnimal(this);
    }
}
