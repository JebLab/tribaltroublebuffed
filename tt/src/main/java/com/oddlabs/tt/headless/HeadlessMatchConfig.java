package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.ruleset.Ruleset;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * An AI-only skirmish, set up the way the Single-player menu sets one up.
 *
 * @param ruleset    the rules the world plays under
 * @param terrain    native (tropical) or viking (northern) island
 * @param size       {@link Game#SIZE_SMALL} to {@link Game#SIZE_ENORMOUS}
 * @param hills      the menu's hills slider, 0 to 1
 * @param vegetation the menu's vegetation slider, 0 to 1
 * @param supplies   the menu's supplies slider, 0 to 1
 * @param seed       the island generator seed (the menu passes the square of its seed field)
 * @param players    one entry per AI tribe, in slot order
 * @param maxTicks   the match stops undecided after this many ticks
 */
public record HeadlessMatchConfig(@NonNull Ruleset ruleset, Landscape.@NonNull TerrainType terrain, int size,
                                  float hills, float vegetation, float supplies, int seed,
                                  @NonNull List<@NonNull PlayerConfig> players,
                                  int maxTicks) {

    /** Island side in meters for each {@link Game} size; the menu's table. */
    private static final int[] METERS_PER_WORLD = {256, 512, 1024, 2048};

    /** Sixty minutes of game time at normal speed. */
    public static final int DEFAULT_MAX_TICKS = 60 * 60 * 50;

    /**
     * @param team       players on the same team win together
     * @param race       {@link com.oddlabs.tt.model.RacesResources#RACE_NATIVES} or
     *                   {@link com.oddlabs.tt.model.RacesResources#RACE_VIKINGS}
     * @param difficulty {@link PlayerSlot#AI_EASY}, {@link PlayerSlot#AI_NORMAL} or {@link PlayerSlot#AI_HARD}
     */
    public record PlayerConfig(int team, int race, int difficulty) {
    }

    public HeadlessMatchConfig {
        players = List.copyOf(players);
        if (size < Game.SIZE_SMALL || size > Game.SIZE_ENORMOUS)
            throw new IllegalArgumentException("size must be small to enormous: " + size);
    }

    /**
     * Every player on a team of its own, races alternating natives and vikings, all at one difficulty; medium island
     * with the menu's default sliders.
     */
    public static @NonNull HeadlessMatchConfig freeForAll(@NonNull Ruleset ruleset, int num_players, int size,
            int seed, int difficulty) {
        List<PlayerConfig> players = new ArrayList<>();
        for (int i = 0; i < num_players; i++) {
            players.add(new PlayerConfig(i, i % 2, difficulty));
        }
        Landscape.TerrainType terrain = (seed & 1) == 0 ? Landscape.TerrainType.NATIVE : Landscape.TerrainType.VIKING;
        return new HeadlessMatchConfig(ruleset, terrain, size, .5f, .5f, .5f, seed, players, DEFAULT_MAX_TICKS);
    }

    public int metersPerWorld() {
        return METERS_PER_WORLD[size];
    }

    public int initialUnitCount() {
        return Player.INITIAL_UNIT_COUNT;
    }

    public int maxUnitCount() {
        return Player.DEFAULT_MAX_UNIT_COUNT;
    }

    public int maxBuildingCount() {
        return Game.DEFAULT_MAX_BUILDING_COUNT;
    }

    public @NonNull HeadlessMatchConfig withMaxTicks(int max_ticks) {
        return new HeadlessMatchConfig(ruleset, terrain, size, hills, vegetation, supplies, seed, players, max_ticks);
    }

    /** Arguments for {@link HeadlessMain}: the same match in another JVM. */
    public @NonNull List<@NonNull String> toArguments() {
        List<String> args = new ArrayList<>(List.of("--ruleset", ruleset.getId(), "--terrain", terrain.name(),
                "--size", Integer.toString(size), "--hills", Float.toString(hills), "--vegetation",
                Float.toString(vegetation), "--supplies", Float.toString(supplies), "--seed", Integer.toString(seed),
                "--max-ticks", Integer.toString(maxTicks)));
        for (PlayerConfig player : players) {
            args.add("--player");
            args.add(player.team() + ":" + player.race() + ":" + player.difficulty());
        }
        return args;
    }

    /** Reads what {@link #toArguments()} writes. */
    public static @NonNull HeadlessMatchConfig fromArguments(@NonNull List<@NonNull String> args) {
        Ruleset ruleset = Ruleset.SKIRMISH_DEFAULT;
        Landscape.TerrainType terrain = Landscape.TerrainType.NATIVE;
        int size = Game.SIZE_MEDIUM;
        float hills = .5f;
        float vegetation = .5f;
        float supplies = .5f;
        int seed = 1;
        int max_ticks = DEFAULT_MAX_TICKS;
        List<PlayerConfig> players = new ArrayList<>();
        for (int i = 0; i < args.size(); i += 2) {
            String name = args.get(i);
            if (i + 1 >= args.size())
                throw new IllegalArgumentException("missing value for " + name);
            String value = args.get(i + 1);
            switch (name) {
                case "--ruleset" -> {
                    ruleset = Ruleset.fromId(value);
                    if (ruleset == null)
                        throw new IllegalArgumentException("unknown ruleset: " + value);
                }
                case "--terrain" -> terrain = Landscape.TerrainType.valueOf(value);
                case "--size" -> size = Integer.parseInt(value);
                case "--hills" -> hills = Float.parseFloat(value);
                case "--vegetation" -> vegetation = Float.parseFloat(value);
                case "--supplies" -> supplies = Float.parseFloat(value);
                case "--seed" -> seed = Integer.parseInt(value);
                case "--max-ticks" -> max_ticks = Integer.parseInt(value);
                case "--player" -> {
                    String[] parts = value.split(":");
                    players.add(new PlayerConfig(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2])));
                }
                default -> throw new IllegalArgumentException("unknown option: " + name);
            }
        }
        return new HeadlessMatchConfig(ruleset, terrain, size, hills, vegetation, supplies, seed, players, max_ticks);
    }
}
