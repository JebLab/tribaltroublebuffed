package com.oddlabs.tt.headless;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Headless;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.LandscapeResources;
import com.oddlabs.tt.landscape.NotificationListener;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.AdvancedAI;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Plays an AI-only skirmish to the end without a window, sound or network: builds the world the way a Single-player
 * skirmish does, gives every slot an {@link AdvancedAI} and ticks the simulation until one team is left or the tick
 * limit is reached. AIs act on the world directly, inside the tick, so no player input is involved and the match is a
 * pure function of its {@link HeadlessMatchConfig} and the simulation code.
 * <p>
 * Ported in spirit from the restoration fork's {@code HeadlessMatchRunner}, whose engine runs one world per peer over
 * a loopback router; this engine has one world and no AI traffic on the network, so it ticks that world directly.
 */
public final class HeadlessMatchRunner {
    /** Checksum samples every ten seconds of game time, as often as multiplayer peers compare them. */
    public static final int DEFAULT_SAMPLE_INTERVAL = (int) (10 / AnimationManager.ANIMATION_SECONDS_PER_TICK);

    private static final int PROGRESS_INTERVAL = (int) (300 / AnimationManager.ANIMATION_SECONDS_PER_TICK);

    private final int sample_interval;
    private final boolean verbose;

    public HeadlessMatchRunner() {
        this(DEFAULT_SAMPLE_INTERVAL, true);
    }

    /**
     * @param sample_interval ticks between checksum samples in the trace
     * @param verbose         print the tribes' unit and building counts every five minutes of game time
     */
    public HeadlessMatchRunner(int sample_interval, boolean verbose) {
        this.sample_interval = sample_interval;
        this.verbose = verbose;
    }

    /** Makes this JVM headless and gives it default settings if it has none. Safe to call more than once. */
    public static synchronized void setUp() {
        Headless.enable();
        if (Settings.getSettings() == null)
            Settings.setSettings(new Settings());
    }

    public @NonNull HeadlessMatchResult run(@NonNull HeadlessMatchConfig config) {
        return run(config, _ -> false);
    }

    /**
     * Plays the match.
     *
     * @param stop_after asked after every tick with the tick number; true ends the match there, undecided
     */
    public @NonNull HeadlessMatchResult run(@NonNull HeadlessMatchConfig config, @NonNull IntPredicate stop_after) {
        setUp();
        World world = newWorld(config);
        Player[] players = world.getPlayers();
        List<HeadlessMatchResult.Sample> trace = new ArrayList<>();
        trace.add(new HeadlessMatchResult.Sample(world.getTick(), checksum(world)));
        while (true) {
            world.tick(AnimationManager.ANIMATION_SECONDS_PER_TICK);
            int tick = world.getTick();
            if (tick % sample_interval == 0)
                trace.add(new HeadlessMatchResult.Sample(tick, checksum(world)));
            if (verbose && tick % PROGRESS_INTERVAL == 0)
                IO.println(progress(tick, players));

            Set<Integer> alive_teams = new LinkedHashSet<>();
            List<Integer> survivors = new ArrayList<>();
            for (int i = 0; i < players.length; i++) {
                if (players[i].isAlive()) {
                    survivors.add(i);
                    int team = players[i].getPlayerInfo().getTeam();
                    if (team != PlayerInfo.TEAM_NEUTRAL)
                        alive_teams.add(team);
                }
            }
            boolean decided = alive_teams.size() <= 1;
            if (decided || tick >= config.maxTicks() || stop_after.test(tick)) {
                int winner = alive_teams.size() == 1 && decided ? alive_teams.iterator().next() : -1;
                int checksum = checksum(world);
                if (trace.getLast().tick() != tick)
                    trace.add(new HeadlessMatchResult.Sample(tick, checksum));
                if (verbose)
                    IO.println(progress(tick,
                            players) + (winner >= 0 ? " -> team " + winner + " wins" : " -> undecided"));
                return new HeadlessMatchResult(winner, tick, checksum, survivors, trace);
            }
        }
    }

    /**
     * The state multiplayer peers compare (see {@code PeerHub.sendChecksum}): the tick, the world's running checksum
     * and every animation's own contribution.
     */
    public static int checksum(@NonNull World world) {
        StateChecksum sum = new StateChecksum();
        sum.update(world.getTick());
        sum.update(world.getChecksum());
        world.getAnimationManagerGameTime().updateChecksum(sum);
        world.getAnimationManagerRealTime().updateChecksum(sum);
        return sum.getValue();
    }

    private static @NonNull World newWorld(@NonNull HeadlessMatchConfig config) {
        RenderQueues queues = new RenderQueues();
        LandscapeResources landscape_resources = World.loadCommon(queues);
        RacesResources races_resources = World.loadInGame(queues, config.ruleset().getStats());
        IslandGenerator generator = new IslandGenerator(config.metersPerWorld(), config.terrain(), config.hills(),
                config.vegetation(), config.supplies(), config.seed(), false);
        // spotless:off
        WorldParameters world_params = WorldParameters.builder()
                .initialGameSpeed(Globals.gamespeed)
                .mapcode("headless")
                .initialUnitCount(config.initialUnitCount())
                .maxUnitCount(config.maxUnitCount())
                .mapSize(config.size())
                .maxBuildingCount(config.maxBuildingCount())
                .ships(false)
                .ruleset(config.ruleset())
                .build();
        // spotless:on
        List<HeadlessMatchConfig.PlayerConfig> player_configs = config.players();
        PlayerInfo[] player_infos = new PlayerInfo[player_configs.size()];
        for (int i = 0; i < player_infos.length; i++) {
            HeadlessMatchConfig.PlayerConfig player = player_configs.get(i);
            player_infos[i] = new PlayerInfo(player.team(), player.race(), "AI" + i);
        }
        WorldInfo world_info = generator.generate(player_infos.length, world_params.getInitialUnitCount(), 0f);
        World world = World.newWorld(HeadlessMatchRunner::silence, landscape_resources, races_resources,
                new NotificationListener() {
                }, world_params, world_info, generator.getTerrainType(), player_infos, generator.getFogInfo());
        Player[] players = world.getPlayers();
        for (int i = 0; i < players.length; i++) {
            // A skirmish AI starts with peons only (see Client's unit infos).
            UnitInfo unit_info = new UnitInfo(false, false, 0, false, world_params.getInitialUnitCount(), 0, 0, 0);
            players[i].setAI(new AdvancedAI(players[i], unit_info, toAIDifficulty(player_configs.get(i).difficulty())));
        }
        return world;
    }

    @SuppressWarnings("unchecked")
    private static @NonNull AudioPlayer silence(@NonNull AudioParameters<?> params) {
        return AudioPlayer.silent((AudioParameters<Audio>) params);
    }

    private static int toAIDifficulty(int slot_difficulty) {
        return switch (slot_difficulty) {
            case PlayerSlot.AI_EASY -> AdvancedAI.DIFFICULTY_EASY;
            case PlayerSlot.AI_NORMAL -> AdvancedAI.DIFFICULTY_NORMAL;
            case PlayerSlot.AI_HARD -> AdvancedAI.DIFFICULTY_HARD;
            default -> throw new IllegalArgumentException("not a skirmish AI difficulty: " + slot_difficulty);
        };
    }

    private static @NonNull String progress(int tick, Player @NonNull [] players) {
        StringBuilder line = new StringBuilder(String.format("tick %d (%d:%02d)", tick, tick / 3000,
                tick / 50 % 60));
        for (Player player : players) {
            line.append(String.format(" | %s T%d: %d units, %d buildings%s", player.getPlayerInfo().getName(),
                    player.getPlayerInfo().getTeam(), player.getUnitCountContainer().getNumSupplies(),
                    player.getBuildingCountContainer().getNumSupplies(), player.isAlive() ? "" : " (out)"));
        }
        return line.toString();
    }
}
