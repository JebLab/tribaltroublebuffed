package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 * Jolly Jungle (Buffed, docs/design/spells.md): when released, every enemy unit on the ground within the radius of the
 * chieftain is rooted ({@link Unit#root}). Leaves spring up around the chieftain while it casts. Units in towers and on
 * ships are out of the unit grid, so the scan passes them by, and so are animals (not units).
 */
public final class JollyJungle implements Magic {
    private final float hit_radius;
    private final float seconds;
    private final @NonNull Unit src;
    private final @NonNull Player owner;
    private final float x;
    private final float y;
    private final float z;
    private final @NonNull RandomVelocityEmitter emitter;

    public JollyJungle(float hit_radius, float seconds, @NonNull Unit src) {
        this.hit_radius = hit_radius;
        this.seconds = seconds;
        this.src = src;
        this.owner = src.getOwner();
        // The chieftain stands still while it casts.
        x = src.getPositionX();
        y = src.getPositionY();
        z = src.getPositionZ();
        float alpha = 6f;
        float energy = 2f;
        emitter = new RandomVelocityEmitter(owner.getWorld(),
                new Vector3f(x, y, z), .2f, 0f,
                3f, .2f, 1f, (float) Math.PI,
                -1, 30f,
                new Vector3f(0f, 0f, 3f), new Vector3f(0f, 0f, -2f),
                new Vector4f(1f, 1f, 1f, alpha), new Vector4f(0f, 0f, 0f, -alpha / energy),
                new Vector3f(.3f, .3f, .3f), new Vector3f(0f, 0f, 0f), energy, 1f,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                owner.getWorld().getRacesResources().getLeafTextures(),
                owner.getWorld().getAnimationManagerGameTime());
    }

    @Override
    public void animate(float t) {
        var filter = new FindOccupantFilter<>(x, y, hit_radius, src, Unit.class);
        owner.getWorld().getUnitGrid().scan(filter, UnitGrid.toGridCoordinate(x), UnitGrid.toGridCoordinate(y));
        for (Unit unit : filter.getResult()) {
            if (!unit.isDead() && owner.isEnemy(unit.getOwner()))
                unit.root(seconds);
        }
        owner.getWorld().getAudio().newAudio(new AudioParameters<>(
                owner.getWorld().getRacesResources().getGasSound(), x, y, z,
                AudioPlayer.AUDIO_RANK_MAGIC,
                AudioPlayer.AUDIO_DISTANCE_MAGIC,
                AudioPlayer.AUDIO_GAIN_BUBBLING,
                AudioPlayer.AUDIO_RADIUS_BUBBLING,
                1f));
        interrupt();
    }

    @Override
    public void interrupt() {
        emitter.done();
        owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
    }
}
