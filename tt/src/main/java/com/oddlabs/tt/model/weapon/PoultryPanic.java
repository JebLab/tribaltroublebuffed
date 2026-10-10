package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.RubberGroup;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.util.Target;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 * Poultry Panic (Buffed, docs/design/spells.md): when released, every enemy unit on the ground within the radius of the
 * chieftain is knocked flat, stunned as by Terrifying Toot, and chickens fly out from the chieftain to free cells 5 to
 * 8 m around it, one in each direction, in a flock of their own that anyone may catch (like the Chicken Coop's).
 */
public final class PoultryPanic implements Magic {
    private static final float MIN_CHICKEN_DISTANCE = 5f;
    private static final float MAX_CHICKEN_DISTANCE = 8f;
    private static final int FEATHERS = 60;

    private final float hit_radius;
    private final float stun_seconds;
    private final int chickens;
    private final @NonNull Unit src;
    private final @NonNull Player owner;
    private final float x;
    private final float y;
    private final float z;

    public PoultryPanic(float hit_radius, float stun_seconds, int chickens, @NonNull Unit src) {
        this.hit_radius = hit_radius;
        this.stun_seconds = stun_seconds;
        this.chickens = chickens;
        this.src = src;
        this.owner = src.getOwner();
        // The chieftain stands still while it casts.
        x = src.getPositionX();
        y = src.getPositionY();
        z = src.getPositionZ();
    }

    @Override
    public void animate(float t) {
        World world = owner.getWorld();
        var filter = new FindOccupantFilter<>(x, y, hit_radius, src, Unit.class);
        world.getUnitGrid().scan(filter, UnitGrid.toGridCoordinate(x), UnitGrid.toGridCoordinate(y));
        for (Unit unit : filter.getResult()) {
            if (!unit.isDead() && owner.isEnemy(unit.getOwner()))
                unit.stun(stun_seconds);
        }

        RubberGroup flock = RubberGroup.newCoopFlock(world);
        int last_cell = world.getUnitGrid().getGridSize() - 1;
        float first_angle = world.getRandom().nextFloat() * (float) Math.PI * 2;
        for (int i = 0; i < chickens; i++) {
            float angle = first_angle + i * (float) Math.PI * 2 / chickens;
            float distance = MIN_CHICKEN_DISTANCE + world.getRandom().nextFloat() * (MAX_CHICKEN_DISTANCE - MIN_CHICKEN_DISTANCE);
            int grid_x = Math.clamp(UnitGrid.toGridCoordinate(x + (float) Math.cos(angle) * distance), 0, last_cell);
            int grid_y = Math.clamp(UnitGrid.toGridCoordinate(y + (float) Math.sin(angle) * distance), 0, last_cell);
            Target target = world.getUnitGrid().findGridTargets(grid_x, grid_y, 1, true)[0];
            if (target != null)
                flock.spawn(target, x, y);
        }

        float alpha = 4f;
        float energy = 2.5f;
        new RandomVelocityEmitter(world, new Vector3f(x, y, z), .5f, 0f,
                2f, .5f, 1.3f, (float) Math.PI,
                FEATHERS, 200f,
                new Vector3f(0f, 0f, 6f), new Vector3f(0f, 0f, -3f),
                new Vector4f(1f, 1f, 1f, alpha), new Vector4f(0f, 0f, 0f, -alpha / energy),
                new Vector3f(.25f, .25f, .25f), new Vector3f(0f, 0f, 0f), energy, 1f,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                world.getRacesResources().getFeatherTextures(),
                world.getAnimationManagerGameTime());
        for (int i = 0; i < 3; i++) {
            world.getAudio().newAudio(new AudioParameters<>(
                    world.getLandscapeResources().getBirdIdleSound(world.getRandom()), x, y, z,
                    AudioPlayer.AUDIO_RANK_MAGIC,
                    AudioPlayer.AUDIO_DISTANCE_MAGIC,
                    AudioPlayer.AUDIO_GAIN_LIGHTNING,
                    AudioPlayer.AUDIO_RADIUS_LIGHTNING,
                    .8f + .2f * i));
        }
        world.getAnimationManagerGameTime().removeAnimation(this);
    }

    @Override
    public void interrupt() {
    }
}
