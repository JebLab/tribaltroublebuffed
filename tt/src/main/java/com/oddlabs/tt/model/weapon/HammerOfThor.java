package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.particle.Lightning;
import com.oddlabs.tt.player.Player;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

/**
 * Hammer of Thor (Buffed, docs/design/spells.md): when released, one bolt strikes the target for its damage, with no
 * roll: a unit of one hit point dies, a building or a chieftain loses the damage. A dead target is not struck.
 */
public final class HammerOfThor implements Magic {
    private static final float BOLT_HEIGHT = 30f;
    private static final int FLASHES = 3;
    private static final float SECONDS_BETWEEN_FLASHES = .1f;
    private static final float LIGHTNING_TIME = .15f;
    private static final Vector4fc DELTA_COLOR = new Vector4f(0f, 0f, 0f, -1f / LIGHTNING_TIME);

    private final int damage;
    private final @NonNull Unit src;
    private final @NonNull Player owner;
    private final @NonNull Selectable<?> target;

    private float time = 0f;
    private int flashes = 0;
    private float strike_x;
    private float strike_y;

    public HammerOfThor(int damage, @NonNull Unit src, @NonNull Selectable<?> target) {
        this.damage = damage;
        this.src = src;
        this.owner = src.getOwner();
        this.target = target;
    }

    @Override
    public void animate(float t) {
        World world = owner.getWorld();
        if (flashes == 0) {
            if (target.isDead()) {
                world.getAnimationManagerGameTime().removeAnimation(this);
                return;
            }
            float dx = target.getPositionX() - src.getPositionX();
            float dy = target.getPositionY() - src.getPositionY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 0f) {
                dx /= dist;
                dy /= dist;
            } else {
                dx = 0f;
                dy = 1f;
            }
            strike_x = target.getPositionX();
            strike_y = target.getPositionY();
            target.hit(damage, dx, dy, owner);
            float x = strike_x;
            float y = strike_y;
            world.getAudio().newAudio(new AudioParameters<>(
                    world.getRacesResources().getLightningSound(), x, y,
                    world.getHeightMap().getNearestHeight(x, y),
                    AudioPlayer.AUDIO_RANK_MAGIC,
                    AudioPlayer.AUDIO_DISTANCE_MAGIC,
                    AudioPlayer.AUDIO_GAIN_LIGHTNING,
                    AudioPlayer.AUDIO_RADIUS_LIGHTNING));
        }
        time += t;
        if (time >= flashes * SECONDS_BETWEEN_FLASHES) {
            flash();
            flashes++;
        }
        if (flashes >= FLASHES)
            world.getAnimationManagerGameTime().removeAnimation(this);
    }

    /** A bolt from the sky onto where the target stood when it was struck. */
    private void flash() {
        World world = owner.getWorld();
        float x = strike_x;
        float y = strike_y;
        float z = world.getHeightMap().getNearestHeight(x, y);
        new Lightning(world, new Vector3f(x, y, z + BOLT_HEIGHT), new Vector3f(x, y, z), 2f,
                15, Color.WHITE, DELTA_COLOR,
                world.getRacesResources().getLightningTexture(), LIGHTNING_TIME,
                world.getAnimationManagerGameTime());
    }

    @Override
    public void interrupt() {
    }
}
