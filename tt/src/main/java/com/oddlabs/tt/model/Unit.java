package com.oddlabs.tt.model;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.behaviour.CastController;
import com.oddlabs.tt.model.behaviour.DefendController;
import com.oddlabs.tt.model.behaviour.DieBehaviour;
import com.oddlabs.tt.model.behaviour.DieController;
import com.oddlabs.tt.model.behaviour.EnterController;
import com.oddlabs.tt.model.behaviour.GatherController;
import com.oddlabs.tt.model.behaviour.HuntController;
import com.oddlabs.tt.model.behaviour.IdleController;
import com.oddlabs.tt.model.behaviour.MagicController;
import com.oddlabs.tt.model.behaviour.PlaceBuildingController;
import com.oddlabs.tt.model.behaviour.RepairController;
import com.oddlabs.tt.model.behaviour.ShipAttackController;
import com.oddlabs.tt.model.behaviour.SittingController;
import com.oddlabs.tt.model.behaviour.SnareController;
import com.oddlabs.tt.model.behaviour.StunController;
import com.oddlabs.tt.model.behaviour.WalkBehaviour;
import com.oddlabs.tt.model.behaviour.WalkController;
import com.oddlabs.tt.model.weapon.Champion;
import com.oddlabs.tt.model.weapon.Drum;
import com.oddlabs.tt.model.weapon.GearFactory;
import com.oddlabs.tt.model.weapon.MagicFactory;
import com.oddlabs.tt.model.weapon.Net;
import com.oddlabs.tt.model.weapon.TargetedMagicFactory;
import com.oddlabs.tt.model.weapon.WeaponFactory;
import com.oddlabs.tt.particle.BalancedParametricEmitter;
import com.oddlabs.tt.particle.StunFunction;
import com.oddlabs.tt.pathfinder.Movable;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.PathFinder;
import com.oddlabs.tt.pathfinder.PathTracker;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.TargetRegionFinder;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.Target;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Unit extends Selectable<UnitTemplate> implements Occupant, Movable {

    private static final float IDLE_SPEED = 1f / 2.5f;
    private static final float TRANSPORT_SPEED_SCALE = 4f / 5f;

    private static final int PENALTY_INCREMENT = 3;
    private static final int INITIAL_PATH_PENALTY = 5;
    private static final float[] MAX_MAGIC_ENERGY = new float[]{40f, 70f};

    public class Animation {
        public static final int IDLING = 0;
        public static final int MOVING = 1;
        public static final int THROWING = 2;
        public static final int DYING = 3;
        public static final int MAGIC = 4;
        public static final int THOR = 5;
        public static final int SITTING = 4;
        public static final int STEERING = 5;
        public static final int ROWING_RIGHT = 6;
        public static final int ROWING_LEFT = 7;
    }

    public static final int SPEAR_RELEASE_FRAME = 29;

    private final @Nullable UnitSupplyContainer supply_container;
    private final @Nullable String name;
    private final @NonNull PathTracker path_tracker;
    // The 2004 spells' charges, then Buffed's third slot (two spells sharing one charge length).
    private final float[] magic_energy = new float[RacesResources.NUM_MAGIC];
    private int last_magic_index = -1;

    private @Nullable BalancedParametricEmitter stun_marker;
    // Buffed: seconds left before Jolly Jungle's vines let the unit walk again.
    private float root_seconds;
    private @Nullable BalancedParametricEmitter root_marker;
    private int hit_points;
    private @NonNull int animation = Animation.IDLING;
    private float anim_speed;
    private float anim_time;
    private int path_penalty;
    private boolean imaginary;
    /**
     * unit is in a tower
     */
    private boolean mounted;
    private boolean on_ship = false;
    private float mount_offset = 0;
    private Building mounted_building;
    private float range_bonus;
    // Buffed: a chicken catcher's snares lying in the world; they go with it.
    private final @NonNull List<@NonNull Snare> snares = new ArrayList<>(0);

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point,
            @NonNull UnitTemplate unit_template) {
        this(owner, x, y, rally_point, unit_template, null);
    }

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point,
            @NonNull UnitTemplate unit_template, @Nullable String name) {
        this(owner, x, y, rally_point, unit_template, name, true);
    }

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point,
            @NonNull UnitTemplate unit_template, @Nullable String name, boolean notify_by_chieftain) {
        this(owner, x, y, rally_point, unit_template, name, notify_by_chieftain, false);
    }

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point,
            @NonNull UnitTemplate unit_template, @Nullable String name, boolean notify_by_chieftain,
            boolean grid_targets_only) {
        this(owner, x, y, rally_point, unit_template, name, notify_by_chieftain, grid_targets_only, false);
    }

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point,
            @NonNull UnitTemplate unit_template, @Nullable String name, boolean notify_by_chieftain,
            boolean grid_targets_only, boolean imaginary) {
        super(owner, unit_template);
        this.name = name;
        this.imaginary = imaginary;
        getAbilities().addAbilities(unit_template.getAbilities());
        register();
        hit_points = unit_template.getMaxHitPoints();
        this.path_tracker = new PathTracker(getUnitGrid(), this);
        UnitSupplyContainerFactory factory = unit_template.getUnitSupplyContainerFactory();
        supply_container = factory != null ? (UnitSupplyContainer) factory.createContainer(this) : null;

        if (!imaginary) {
            findInitialPosition(x, y, grid_targets_only, -1);
            if (isDrummer())
                owner.getWorld().getDrummers().add(this);
        }

        pushController(new IdleController(this, new AttackScanFilter(getOwner(), AttackScanFilter.UNIT_RANGE,
                isWarrior()), true));
        if (!getAbilities().hasAbilities(Abilities.MAGIC) && !imaginary) {
            int result = getOwner().getUnitCountContainer().increaseSupply(1);
            assert (result == 1) : "No room for new unit in player unit container.";
        } else if (notify_by_chieftain) {
            owner.getWorld().getNotificationListener().newSelectableNotification(this);
        }
        if (rally_point != null) {
            Target unit_target;
            if (rally_point instanceof LandscapeTarget) {
                UnitGrid grid = getUnitGrid();
                List<Target> temp_occupants = new ArrayList<>();
                for (var s : getOwner().getUnits().getSet()) {
                    if (s.getCurrentController() instanceof WalkController) {
                        Target target = ((WalkController) s.getCurrentController()).getTarget();
                        if (!grid.isGridOccupied(target.getGridX(), target.getGridY())) {
                            grid.occupyGrid(target.getGridX(), target.getGridY(), this);
                            temp_occupants.add(target);
                        }
                    }
                }
                unit_target = grid.findGridTargets(rally_point.getGridX(), rally_point.getGridY(), 1, true)[0];
                for (Target target : temp_occupants) {
                    grid.freeGrid(target.getGridX(), target.getGridY(), this);
                }
            } else
                unit_target = rally_point;

            boolean aggressive = unit_template.getAbilities().hasAbilities(Abilities.THROW);
            setTarget(unit_target, Action.DEFAULT, aggressive);
        }
    }

    @Override
    protected @NonNull Unit self() {
        return this;
    }

    @Override
    protected final float getZError() {
        if (on_ship) {
            return 0.0f;
        } else {
            return getLandscapeError();
        }
    }

    @Override
    public final void visit(@NonNull ElementVisitor visitor) {
        visitor.visitUnit(this);
    }

    public final @Nullable UnitSupplyContainer getSupplyContainer() {
        return supply_container;
    }

    @Override
    public final String toString() {
        if (!isDead())
            return "Unit: " + hashCode() + " | getOwner() = " + getOwner() + " | mounted = " + mounted + " | getGridX() = " + getGridX() + " | getGridY() = " + getGridY();
        else
            return super.toString();
    }

    public void reposition(Building building) {
        findInitialPosition(getPositionX(), getPositionY(), true, building.getIslandId());
    }

    private void findInitialPosition(float x, float y, boolean grid_targets_only, int island) {
        UnitGrid unit_grid = getUnitGrid();
        Target reserved_target = unit_grid.findGridTargets(UnitGrid.toGridCoordinate(x), UnitGrid.toGridCoordinate(y),
                1, grid_targets_only, island)[0];
        setGridPosition(reserved_target.getGridX(), reserved_target.getGridY());
        setPosition(reserved_target.getPositionX(), reserved_target.getPositionY());

        // Orient initially towards world center
        float center = getOwner().getWorld().getHeightMap().getMetersPerWorld() / 2f;
        float dx = center - reserved_target.getPositionX();
        float dy = center - reserved_target.getPositionY();
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            setDirection(dx / len, dy / len);
        }

        occupy();
        reinsert();
    }

    @Override
    public final int getStatusValue() {
        int tower_factor = 1;
        if (mounted)
            tower_factor = 3;
        return getTemplate().getStatusValue() * tower_factor;
    }

    public final void increaseRange(float amount) {
        assert !isDead();
        range_bonus += amount;
    }

    @Override
    public final AttackScanFilter.@NonNull Priority getAttackPriority() {
        assert !isDead();
        // Buffed: a drummer is everyone's first target among units.
        if (isDrummer())
            return AttackScanFilter.Priority.DRUMMER;
        return getAbilities().hasAbilities(
                Abilities.BUILD) ? AttackScanFilter.Priority.PEON : AttackScanFilter.Priority.WARRIOR;
    }

    @Override
    public final void visit(@NonNull ToolTipVisitor visitor) {
        visitor.visitUnit(this);
    }

    public final @Nullable String getName() {
        return name;
    }

    public final int getHitPoints() {
        return hit_points;
    }

    public final void drown() {
        if (isDead()) {
            return;
        }
        clearControllerStack();
        setReference(null);
        mounted = false;
        on_ship = false;
        mount_offset = 0;
        if (!imaginary) {
            enable();
        }
        if (supply_container != null) {
            supply_container.resetSupply(LeftPaddle.class);
            supply_container.resetSupply(RightPaddle.class);
        }
        mounted_building = null;
        startDying();
    }

    public final void unmount() {
        assert !isDead();
        clearControllerStack();
        swapController(new IdleController(this, new AttackScanFilter(getOwner(), AttackScanFilter.UNIT_RANGE,
                isWarrior()), true));
        mounted = false;
        on_ship = false;
        mount_offset = 0;
        enable();
        Building entrance = mounted_building.getEntrance();
        findInitialPosition(entrance.getPositionX(), entrance.getPositionY(), true, entrance.getIslandId());
        if (supply_container != null) {
            supply_container.resetSupply(LeftPaddle.class);
            supply_container.resetSupply(RightPaddle.class);
        }
        mounted_building = null;
    }

    public final void mount(@NonNull Building building) {
        mountAt(building, building.getPositionX(), building.getPositionY());
    }

    /** Mounts a tower that holds several throwers (Buffed's Great Tower), at a place this far from its centre. */
    public final void mount(@NonNull Building building, float offset_x, float offset_y) {
        mountAt(building, building.getPositionX() + offset_x, building.getPositionY() + offset_y);
    }

    private void mountAt(@NonNull Building building, float x, float y) {
        assert !isDead();
        mounted_building = building;
        mount_offset = building.getTemplate().getMountOffset();
        if (!imaginary) {
            disable();
            free();
        }
        setPosition(x, y);
        mounted = true;
        clearControllerStack();
        swapController(new IdleController(this, new AttackScanFilter(getOwner(), AttackScanFilter.TOWER_RANGE, true),
                false));
    }

    public final void mount(Ship ship, ShipAllocation ship_allocation) {
        assert !isDead();
        mounted_building = ship;
        if (!imaginary) {
            disable();
            free();
        }
        mounted = true;
        on_ship = true;
        setReference(ship);
        clearControllerStack();
        switch (ship_allocation.getRole()) {
            case ShipAllocation.FIGHTING:
                swapController(
                        new ShipAttackController(
                                this,
                                ship,
                                new AttackScanFilter(
                                        getOwner(), AttackScanFilter.TOWER_RANGE + 10),
                                ship_allocation));
                break;
            default:
                swapController(new SittingController(this, ship, ship_allocation));
                break;
        }
    }

    public final boolean isMounted() {
        return mounted;
    }

    @Override
    public final boolean isEnabled() {
        return !isDead() && !mounted;
    }

    public final float getMetersPerSecond() {
        assert !isDead();
        float speed;
        if (getAbilities().hasAbilities(Abilities.HARVEST) && supply_container.getNumSupplies() > 0)
            speed = TRANSPORT_SPEED_SCALE * getTemplate().getMetersPerSecond();
        else
            speed = getTemplate().getMetersPerSecond();
        // Buffed: a drummer of its team nearby; without one the speed is the original, bit for bit.
        float drum_factor = DrumAura.getSpeedFactor(this);
        return drum_factor != 1f ? speed * drum_factor : speed;
    }

    public final void aimAtTarget(@NonNull Target target) {
        assert !isDead();
        float dx = target.getPositionX() - getPositionX();
        float dy = target.getPositionY() - getPositionY();
        float dir_len_inv = 1f / (float) Math.sqrt(dx * dx + dy * dy);
        dx *= dir_len_inv;
        dy *= dir_len_inv;
        setDirection(dx, dy);
    }

    public final void switchToIdleAnimation() {
        assert !isDead();
        switchAnimation(IDLE_SPEED, Animation.IDLING);
    }

    public final void switchToSittingAnimation() {
        assert !isDead();
        switchAnimation(IDLE_SPEED, Animation.SITTING);
    }

    public final void switchToSteeringAnimation() {
        assert !isDead();
        switchAnimation(IDLE_SPEED, Animation.STEERING);
    }

    public final void switchToRowingRightAnimation() {
        assert !isDead();
        assert supply_container != null;
        switchAnimation(IDLE_SPEED, Animation.ROWING_RIGHT);
        supply_container.increaseSupply(1, RightPaddle.class);
    }

    public final void switchToRowingLeftAnimation() {
        assert !isDead();
        assert supply_container != null;
        switchAnimation(IDLE_SPEED, Animation.ROWING_LEFT);
        supply_container.increaseSupply(1, LeftPaddle.class);
    }

    public final @NonNull WeaponFactory getWeaponFactory() {
        assert !isDead();
        return getTemplate().getWeaponFactory();
    }

    public final float getRange(@NonNull Target target) {
        assert !isDead();
        return getWeaponFactory().getRange() + range_bonus + target.getSize();
    }

    @Override
    public final float getSize() {
        return 1.9f;
    }

    @Override
    public final @NonNull SpriteKey getSpriteRenderer() {
        return getTemplate().getSpriteRenderer();
    }

    @Override
    public final void doAnimate(float t) {
        anim_time += anim_speed * t;
        if (isDead() || mounted)
            reinsert();
        getOwner().getWorld().updateGlobalChecksum(animation);
        // Buffed: Jolly Jungle's vines let go.
        if (root_seconds > 0f)
            root_seconds -= t;

        if (getAbilities().hasAbilities(Abilities.MAGIC)) {
            // Buffed: a Lodge of the chieftain's team nearby charges the spells faster; without one, exactly as before.
            float factor = Lodge.getSpellChargeFactor(this);
            float charge = factor != 1f ? t * factor : t;
            for (int i = 0; i < magic_energy.length; i++) {
                increaseMagicEnergy(i, charge);
            }
        }
    }

    public final void increaseMagicEnergy(int index, float amount) {
        magic_energy[index] += amount;
        float max = maxMagicEnergy(index);
        if (magic_energy[index] > max) {
            magic_energy[index] = max;
        }
    }

    /** Seconds a spell takes to charge: 40 and 70 for the 2004 spells, the ruleset's third slot for Buffed's. */
    private float maxMagicEnergy(int index) {
        if (index < MAX_MAGIC_ENERGY.length)
            return MAX_MAGIC_ENERGY[index];
        return getOwner().getWorld().getRuleset().getStats().spells().third_slot_seconds();
    }

    @Override
    public final @NonNull PathTracker getTracker() {
        assert !isDead();
        return path_tracker;
    }

    @Override
    public final void markBlocking() {
        assert !isDead();
        path_penalty = Math.min(path_penalty + PENALTY_INCREMENT, STATIC - 1); // never gets STATIC
    }

    @Override
    public final int getPenalty() {
        assert !isDead();
        return isBlocking() ? Occupant.STATIC : path_penalty;
    }

    @Override
    protected final void removeDying() {
        if (getAbilities().hasAbilities(Abilities.MAGIC)) {
            getOwner().setActiveChieftain(null);
        }
        if (!imaginary) {
            free();
            if (!getAbilities().hasAbilities(Abilities.MAGIC)) {
                int result = getOwner().getUnitCountContainer().increaseSupply(-1);
                assert result == -1;
            }
        }
        if (stun_marker != null) {
            stun_marker.done();
            stun_marker = null;
        }
        if (root_marker != null) {
            root_marker.done();
            root_marker = null;
        }
        // Buffed: a drummer's aura and a catcher's snares end with it (in the world: dead, or gone into a building).
        if (isDrummer())
            getOwner().getWorld().getDrummers().remove(this);
        while (!snares.isEmpty())
            snares.getFirst().pullUp();
        super.removeDying();
    }

    public final void removeNow() {
        assert !isDead();
        removeDying();
        remove();
    }

    @Override
    public final void free() {
        assert !isDead();
        UnitGrid unit_grid = getUnitGrid();
        if (unit_grid.getOccupant(getGridX(), getGridY()) == this) {
            unit_grid.freeGrid(getGridX(), getGridY(), this);
        }
        path_penalty = INITIAL_PATH_PENALTY;
    }

    @Override
    public final void occupy() {
        assert !isDead();
        UnitGrid unit_grid = getUnitGrid();
        unit_grid.occupyGrid(getGridX(), getGridY(), this);

        // stats
        getOwner().unitMoved();
    }

    @Override
    public final boolean isMoving() {
        return getCurrentBehaviour() instanceof WalkBehaviour;
    }

    /*	public final void moveNextAnimate() {
    	WalkBehaviour behaviour = (WalkBehaviour)getCurrentBehaviour();
    	behaviour.moveNextAnimate();
    }
     */
    @Override
    public final void hit(int damage, float direction_x, float direction_y, @NonNull Player owner) {
        super.hit(damage, direction_x, direction_y, owner);
        if (mounted && !on_ship) {
            mounted_building.hit(damage, direction_x, direction_y, owner);
        } else if (!isDead()) {
            takeDamage(damage, direction_x, direction_y, owner);
        }
    }

    /**
     * A blow from one of Buffed's wild animals (docs/design/fauna.md), which strike only units on the ground: like
     * {@link #hit}, but no player is credited with the kill.
     */
    public final void hitByAnimal(int damage, float direction_x, float direction_y) {
        assert !mounted;
        if (isDead())
            return;
        getOwner().getWorld().getNotificationListener().newAttackNotification(this);
        takeDamage(damage, direction_x, direction_y, null);
        if (hit_points == 0)
            getOwner().unitLostToAnimal(getGridX(), getGridY());
    }

    private void takeDamage(int damage, float direction_x, float direction_y, @Nullable Player killer) {
        hit_points = Math.clamp(hit_points - damage, 0, getTemplate().getMaxHitPoints());
        if (hit_points == 0) {
            if (killer != null)
                killer.unitKilled();
            if (mounted_building instanceof Ship ship) {
                ship.getShipHR().removeUnit(this);
                drown();
            } else {
                startDying();
                setDirection(-direction_x, -direction_y);
            }
        }
    }

    /**
     * Buffed's monkeys: the unit's load is taken from it and lost, and it goes back to work at once instead of
     * carrying nothing to a building.
     */
    public final void loseLoad() {
        assert !isDead() && supply_container != null;
        supply_container.increaseSupply(-supply_container.getNumSupplies(), supply_container.getSupplyType());
        redecide();
    }

    /** Whether the unit walks the island carrying a load a monkey could take (wood, rock, iron or a chicken). */
    public final boolean isCarrying() {
        if (isDead() || mounted || supply_container == null || supply_container.getNumSupplies() == 0)
            return false;
        Class<?> type = supply_container.getSupplyType();
        return type != LeftPaddle.class && type != RightPaddle.class;
    }

    public final void startDying() {
        getOwner().unitLost();

        mounted = false;
        on_ship = false;
        mount_offset = 0;

        pushController(new DieController(this));
        forceDecide();
        getOwner().getWorld().getAudio().newAudio(
                new AudioParameters(
                        getTemplate().getDeathSound(),
                        getPositionX(),
                        getPositionY(),
                        getPositionZ(),
                        AudioPlayer.AUDIO_RANK_DEATH,
                        AudioPlayer.AUDIO_DISTANCE_DEATH,
                        AudioPlayer.AUDIO_GAIN_DEATH,
                        AudioPlayer.AUDIO_RADIUS_DEATH,
                        1f + (getOwner().getWorld().getRandom().nextFloat() - .5f) * getTemplate().getDeathPitch()));
        removeDying();
    }

    public final void stun(float time) {
        float x = getPositionX() + getTemplate().getStunX() * getDirectionX() + getTemplate().getStunY() * (-getDirectionY());
        float y = getPositionY() + getTemplate().getStunX() * getDirectionY() + getTemplate().getStunY() * getDirectionX();
        float z = getOwner().getWorld().getHeightMap().getNearestHeight(x, y) + getTemplate().getStunZ() + mount_offset;

        if (stun_marker != null) {
            stun_marker.done();
        }
        stun_marker = createStunStar(x, y, z, time, (float) Math.PI / 2);
        pushController(new StunController(this, time));
        forceDecide();
    }

    /**
     * Buffed's Jolly Jungle (docs/design/spells.md): for {@code seconds} the unit cannot walk, but still fights,
     * gathers and builds within reach and keeps its dodge. Rooting a rooted unit keeps the longer time.
     */
    public final void root(float seconds) {
        assert !isDead();
        if (seconds <= root_seconds)
            return;
        root_seconds = seconds;
        if (root_marker != null)
            root_marker.done();
        // Leaves circling the unit's feet while the vines hold it.
        float z = getOwner().getWorld().getHeightMap().getNearestHeight(getPositionX(), getPositionY()) + .3f;
        root_marker = new BalancedParametricEmitter(getOwner().getWorld(),
                new StunFunction(.7f, .1f), new Vector3f(getPositionX(), getPositionY(), z),
                (float) Math.PI / 3, 5f, (float) Math.PI * 2, (float) Math.PI * 2,
                6, 0f, 2f,
                new Vector4f(1f, 1f, 1f, 1f), new Vector4f(0f, 0f, 0f, 0f),
                new Vector3f(.22f, .22f, .22f), new Vector3f(0f, 0f, 0f), seconds,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                getOwner().getWorld().getRacesResources().getLeafTextures(),
                getOwner().getWorld().getAnimationManagerGameTime());
    }

    /** Whether Jolly Jungle's vines hold the unit where it stands (Buffed). */
    public final boolean isRooted() {
        return root_seconds > 0f;
    }

    /** Whether the unit carries a weapon from the Armory: a thrown one, or Buffed's gear. */
    public final boolean isWarrior() {
        return getWeaponFactory().getType() != null;
    }

    /**
     * Whether the unit fights hand to hand with Buffed's gear. Such a warrior cannot man a tower or board a ship, whose
     * places are for throwers.
     */
    public final boolean isGearWarrior() {
        return getWeaponFactory() instanceof GearFactory;
    }

    public final boolean isChieftain() {
        return getAbilities().hasAbilities(Abilities.MAGIC);
    }

    /** Whether the unit is Buffed's Champion, trained at the Lodge: only a Lodge shelters it. */
    public final boolean isChampion() {
        return getWeaponFactory().getType() == Champion.class;
    }

    /** Whether the unit carries Buffed's Drum / Horn: it never attacks, and lifts its team nearby (DrumAura). */
    public final boolean isDrummer() {
        return getWeaponFactory().getType() == Drum.class;
    }

    /** Whether the unit carries Buffed's Net: it catches chickens in one stroke and lays snares. */
    public final boolean isNetter() {
        return getWeaponFactory().getType() == Net.class;
    }

    /** Buffed: sends a chicken catcher after the nearest chicken, then the next, as a right-click on one would. */
    public final void catchChickens() {
        assert isNetter() && !isDead();
        clearControllerStack();
        pushController(new GatherController<>(this, null, RubberSupply.class));
    }

    /** The snares this chicken catcher has lying, oldest first (Buffed). */
    public final @NonNull List<@NonNull Snare> getSnares() {
        return snares;
    }

    private @NonNull BalancedParametricEmitter createStunStar(float x, float y, float z, float time, float velocity) {
        int num_particles = 5;
        return new BalancedParametricEmitter(getOwner().getWorld(),
                new StunFunction(.4f, .15f), new Vector3f(x, y, z),
                velocity, 5f, (float) Math.PI * 2, (float) Math.PI * 2,
                num_particles, 0f, 2f,
                new Vector4f(1f, 1f, 1f, 1f), new Vector4f(0f, 0f, 0f, 0f),
                new Vector3f(.1f, .1f, .1f), new Vector3f(0f, 0f, 0f), time,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                getOwner().getWorld().getRacesResources().getStarTextures(),
                getOwner().getWorld().getAnimationManagerGameTime());
    }

    public final boolean canAttack(@NonNull Target target, boolean kill_friendly) {
        assert !isDead();
        // Buffed: a warrior, or a thrower in a tower, may be sent at a wild animal that can be hunted.
        if (target instanceof Animal animal)
            return animal.isPrey() && isWarrior() && getAbilities().hasAbilities(Abilities.ATTACK);
        if (!(target instanceof Selectable<?> selectable) || !getAbilities().hasAbilities(Abilities.ATTACK))
            return false;
        Player target_player = selectable.getOwner();
        return kill_friendly || getOwner().isEnemy(target_player);
    }

    private boolean canBuild(@NonNull Target target) {
        return target instanceof Building building &&
                getAbilities().hasAbilities(Abilities.BUILD) &&
                !building.isPlaced();
    }

    private boolean canGather(@NonNull Target target) {
        // Buffed: a chicken catcher gathers chickens only.
        return (target instanceof Supply && getAbilities().hasAbilities(Abilities.BUILD))
                || (target instanceof RubberSupply && isNetter());
    }

    private @Nullable Building nearestSupplyBuilding(@NonNull Supply supply) {
        UnitGrid grid = getUnitGrid();
        BuildingFinder finder = new BuildingFinder(getOwner(), Abilities.SUPPLY_CONTAINER);
        Region region = PathFinder.findPathRegion(grid, new TargetRegionFinder(grid, finder),
                grid.getRegion(supply.getGridX(), supply.getGridY()));
        Building building = region != null ? finder.getOccupantFromRegion(region, true) : null;
        return building != null ? building.getBase() : null;
    }

    private boolean canRepair(@NonNull Target target, boolean action_repair) {
        return target instanceof Building building &&
                getAbilities().hasAbilities(Abilities.BUILD) &&
                (action_repair || !building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)
                        || !building.isComplete()) &&
                // getOwner() == building.getOwner() && building.isPlaced() && building.isDamaged();
                !getOwner().isEnemy(building.getOwner()) && building.isPlaced() && building.hasWork();
    }

    public boolean canEnter(@NonNull Target target) {
        if (!(target instanceof Building building) || building.getUnitContainer() == null
                || getOwner() != building.getOwner() || !building.getUnitContainer().canEnter(this)) {
            return false;
        }

        if (target instanceof LandBuilding) {
            return !isChieftain();
        } else {
            return true;
        }
    }

    @Override
    public final float getDefenseChance() {
        return getCurrentController() instanceof StunController ? 0 : super.getDefenseChance();
    }

    private void walkToTarget(@NonNull Target target, boolean scan_attack) {
        Target walkable_target = getUnitGrid().findGridTargets(target.getGridX(), target.getGridY(), 1, false)[0];
        pushController(new WalkController(this, walkable_target, scan_attack));
    }

    @Override
    public void setTarget(@NonNull Target target, @NonNull Action action, boolean aggressive) {
        if (target == this)
            return;
        assert !target.isDead() : "Setting dead target";
        assert !mounted;
        if (target instanceof Building) {
            target = ((Building) target).getEntrance();
        }
        switch (action) {
            case DEFAULT:
                if (canBuild(target)) {
                    pushController(new PlaceBuildingController(this, (Building) target));
                } else if (canGather(target)) {
                    pushController(new GatherController(this, (Supply) target, ((Supply) target).getClass(),
                            nearestSupplyBuilding((Supply) target)));
                } else if (canRepair(target, false)) {
                    pushController(new RepairController(this, (Building) target));
                } else if (canEnter(target)) {
                    pushController(new EnterController(this, (Building) target));
                } else if (canAttack(target, false)) {
                    pushController(new HuntController(this, (Hittable) target));
                } else {
                    walkToTarget(target, aggressive);
                }
                break;
            case MOVE:
                if (canEnter(target)) {
                    pushController(new EnterController(this, (Building) target));
                } else {
                    walkToTarget(target, false);
                }
                break;
            case ATTACK:
                if (canAttack(target, true)) {
                    pushController(new HuntController(this, (Hittable) target));
                } else {
                    walkToTarget(target, true);
                }
                break;
            case GATHER_REPAIR:
                if (canGather(target)) {
                    pushController(new GatherController(this, (Supply) target, ((Supply) target).getClass(),
                            nearestSupplyBuilding((Supply) target)));
                } else if (canRepair(target, true)) {
                    pushController(new RepairController(this, (Building) target));
                }
                break;
            case DEFEND:
                pushController(new DefendController(this, target));
                break;
            case SNARE:
                // Buffed: only a chicken catcher lays snares; the rest of a selection ignores the order.
                if (isNetter())
                    pushController(new SnareController(this, target));
                break;
            case THOR:
                // Buffed: the chieftain casts Hammer of Thor at an enemy; the rest of a selection attacks it.
                if (isChieftain()) {
                    int magic_index = getTargetedMagicIndex();
                    if (magic_index >= 0 && canDoMagic(magic_index) && target instanceof Selectable<?> selectable
                            && getOwner().isEnemy(selectable.getOwner()))
                        pushController(new CastController(this, magic_index, selectable));
                } else if (canAttack(target, true)) {
                    pushController(new HuntController(this, (Hittable) target));
                } else {
                    walkToTarget(target, true);
                }
                break;
            default:
                IO.println("Invalid action: " + action);
                break;
        }
    }

    public final void printDebugInfo() {
        IO.println("-----------------------------------");
        IO.println("Primary Controller = " + getPrimaryController());
        if (getAbilities().hasAbilities(Abilities.MAGIC)) {
            IO.println("Hit Points = " + hit_points);
            IO.println("Magic Energy 0 = " + magic_energy[0]);
            IO.println("Magic Energy 1 = " + magic_energy[1]);
            IO.println("Controller = " + getPrimaryController());
        }
    }

    public final boolean canDoMagic(int magic_index) {
        return !isDead() && magic_index >= 0 && magic_index < RacesResources.NUM_MAGIC && getOwner().canDoMagic(
                magic_index) && magic_energy[magic_index] == maxMagicEnergy(magic_index);
    }

    /**
     * Casts a spell around the chieftain. A spell cast at a target (Buffed's Hammer of Thor) needs {@link #doMagicAt}.
     */
    public final void doMagic(int magic_index, boolean clear_stack) {
        if (canDoMagic(magic_index)) {
            MagicFactory factory = getOwner().getRace().getMagicFactory(magic_index);
            if (factory instanceof TargetedMagicFactory)
                return;
            cast(magic_index, factory, clear_stack);
        }
    }

    /** Buffed's Hammer of Thor: casts a spell at a target in its range (CastController walks there first). */
    public final void doMagicAt(int magic_index, @NonNull Selectable<?> target) {
        if (canDoMagic(magic_index)
                && getOwner().getRace().getMagicFactory(magic_index) instanceof TargetedMagicFactory factory) {
            cast(magic_index, factory.aimedAt(target), false);
        }
    }

    private void cast(int magic_index, @NonNull MagicFactory factory, boolean clear_stack) {
        if (clear_stack)
            clearControllerStack();
        pushController(new MagicController(this, factory));
        Arrays.fill(magic_energy, 0f);
        last_magic_index = magic_index;

        // stats
        getOwner().magicCast(magic_index);
    }

    /** The spell this chieftain casts at a target (Buffed's Hammer of Thor), or -1 when its race has none. */
    public final int getTargetedMagicIndex() {
        for (int i = 0; i < RacesResources.NUM_MAGIC; i++) {
            if (getOwner().getRace().getMagicFactory(i) instanceof TargetedMagicFactory)
                return i;
        }
        return -1;
    }

    public final int getLastMagicIndex() {
        return last_magic_index; // for tutorial
    }

    public final float getMagicProgress(int magic_index) {
        return magic_energy[magic_index] / maxMagicEnergy(magic_index);
    }

    public final void switchAnimation(float anim_speed, @NonNull int animation) {
        assert !isDead();
        if (supply_container != null) {
            supply_container.resetSupply(LeftPaddle.class);
            supply_container.resetSupply(RightPaddle.class);
        }
        this.anim_speed = anim_speed;
        if (this.animation != animation) {
            this.animation = animation;
            this.anim_time = 0f;
        } else if (getTemplate().getSpriteRenderer().getAnimationType(
                animation) == AnimationInfo.AnimationType.PLAIN.ordinal()) {
                    this.anim_time = 0f;
                }
    }

    @Override
    public final int getAnimation() {
        return animation;
    }

    @Override
    public final float getAnimationTicks() {
        return anim_time;
    }

    public final void setMountOffset(float offset) {
        mount_offset = offset;
    }

    public final float getMountOffset() {
        assert !isDead();
        return mount_offset;
    }

    @Override
    public final float getOffsetZ() {
        if (mounted)
            return mounted_building.getOffsetZ() + mount_offset;
        else {
            if (isDead()) {
                DieBehaviour die_behaviour = (DieBehaviour) getCurrentBehaviour();
                return die_behaviour.getOffsetZ();
            } else
                return calculateSlopeOffset();
        }
    }

    private float calculateSlopeOffset() {
        // Check surrounding heights to lift unit on slopes
        float r = getSize() * 0.2f; // Check closer to center (feet) to avoid excessive floating
        float x = getPositionX();
        float y = getPositionY();
        var hm = getOwner().getWorld().getHeightMap();

        float h_center = hm.getNearestHeight(x, y);
        float h_max = h_center;

        // Axis-aligned
        h_max = Math.max(h_max, hm.getNearestHeight(x + r, y));
        h_max = Math.max(h_max, hm.getNearestHeight(x - r, y));
        h_max = Math.max(h_max, hm.getNearestHeight(x, y + r));
        h_max = Math.max(h_max, hm.getNearestHeight(x, y - r));

        // Diagonals (approx 0.707 * r)
        float d = r * 0.707f;
        h_max = Math.max(h_max, hm.getNearestHeight(x + d, y + d));
        h_max = Math.max(h_max, hm.getNearestHeight(x - d, y + d));
        h_max = Math.max(h_max, hm.getNearestHeight(x + d, y - d));
        h_max = Math.max(h_max, hm.getNearestHeight(x - d, y - d));

        return Math.max(0f, h_max - h_center);
    }

    public final float getHitError() {
        if (on_ship && mounted_building instanceof Ship ship) {
            if (ship.getShipHR().hasChieftain()) {
                return 0.2f;
            } else {
                return 2.2f;
            }
        } else {
            return 0.0f;
        }
    }

    public final void debugRender() {
        path_tracker.debugRender();
    }
}
