package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.player.Player;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Fjord Fog (Buffed, docs/design/spells.md): when released, a mist settles where the chieftain stands for its
 * seconds. An enemy of the caster whose throw or blow starts inside it hits with {@code hit_penalty} less chance
 * ({@link #getHitPenalty}, read by {@code WeaponFactory.attack}). Fogs do not add up. The mist is in the world's list
 * ({@code World.getMists}) only while it lasts, so with none the hit roll reads an empty list and nothing else.
 */
public final class FjordFog implements Magic {
    private static final float OFFSET_Z = 1.5f;
    private static final int PARTICLES_PER_BURST = 4;
    private static final float SECONDS_BETWEEN_BURSTS = .1f;
    private static final float BURST_RADIUS = 3f;

    private final float hit_radius;
    private final float total_time;
    private final float hit_penalty;
    private final @NonNull Player owner;
    private final float x;
    private final float y;

    private float time = 0f;
    private int bursts = 0;
    private boolean first_run = true;
    private @Nullable AbstractAudioPlayer wind_sound;

    public FjordFog(float hit_radius, float seconds, float hit_penalty, @NonNull Unit src) {
        this.hit_radius = hit_radius;
        this.total_time = seconds;
        this.hit_penalty = hit_penalty;
        this.owner = src.getOwner();
        // The chieftain stands still while it casts.
        x = src.getPositionX();
        y = src.getPositionY();
    }

    /**
     * The hit chance an enemy's fog takes from a unit attacking from inside it, or 0. Several fogs do not add up; the
     * first in the world's list that covers the unit counts.
     */
    public static float getHitPenalty(@NonNull Selectable<?> unit) {
        List<FjordFog> mists = unit.getOwner().getWorld().getMists();
        if (mists.isEmpty())
            return 0f;
        for (FjordFog fog : mists) {
            if (!fog.owner.isEnemy(unit.getOwner()))
                continue;
            float dx = fog.x - unit.getPositionX();
            float dy = fog.y - unit.getPositionY();
            if (dx * dx + dy * dy <= fog.hit_radius * fog.hit_radius)
                return fog.hit_penalty;
        }
        return 0f;
    }

    @Override
    public void animate(float t) {
        World world = owner.getWorld();
        if (first_run) {
            first_run = false;
            world.getMists().add(this);
            wind_sound = world.getAudio().newAudio(new AudioParameters<>(
                    world.getRacesResources().getFogSound(), x, y, world.getHeightMap().getNearestHeight(x, y),
                    AudioPlayer.AUDIO_RANK_MAGIC,
                    AudioPlayer.AUDIO_DISTANCE_MAGIC,
                    AudioPlayer.AUDIO_GAIN_CLOUD,
                    AudioPlayer.AUDIO_RADIUS_CLOUD,
                    1f, true, false));
        }
        time += t;
        if (time >= total_time) {
            world.getMists().remove(this);
            world.getAnimationManagerGameTime().removeAnimation(this);
            interrupt();
            return;
        }
        // Puffs of pale mist, spread evenly over the area while it lasts.
        if (bursts * SECONDS_BETWEEN_BURSTS < time) {
            float r = (float) Math.sqrt(world.getRandom().nextFloat()) * (hit_radius - BURST_RADIUS);
            float a = world.getRandom().nextFloat() * (float) Math.PI * 2;
            float px = x + (float) Math.cos(a) * r;
            float py = y + (float) Math.sin(a) * r;
            float pz = world.getHeightMap().getNearestHeight(px, py);
            float alpha = 3f;
            float energy = 3f;
            new RandomVelocityEmitter(world, new Vector3f(px, py, pz), OFFSET_Z,
                    world.getRandom().nextFloat() * (float) Math.PI * 2,
                    BURST_RADIUS, 0f, 0f, 0f,
                    PARTICLES_PER_BURST, PARTICLES_PER_BURST,
                    new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f),
                    new Vector4f(.9f, .93f, 1f, alpha), new Vector4f(0f, 0f, 0f, -alpha / energy),
                    new Vector3f(0f, 0f, .25f), new Vector3f(3f, 3f, 0f), energy, 1f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    world.getRacesResources().getSmokeTextures(),
                    world.getAnimationManagerGameTime());
            bursts++;
        }
    }

    @Override
    public void interrupt() {
        if (wind_sound != null)
            wind_sound.stop(1f, Settings.getSettings().sound_gain);
    }
}
