package com.oddlabs.tt.ruleset;

import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ResourceBundle;

/**
 * The rule sets a game can be played under. Each one is a data file in {@code /rulesets/} holding the unit, building,
 * weapon and spell numbers; see {@code docs/rulesets.md}.
 * <ul>
 * <li>{@link #CLASSIC}: the 2004 Oddlabs release.</li>
 * <li>{@link #RESURRECTED}: Tribal Trouble: Resurrected's defaults at the time of the fork. Campaign, tutorials and
 * multiplayer always play under it, because their content and the servers expect those numbers.</li>
 * <li>{@link #BUFFED}: this fork's new content, layered on Resurrected.</li>
 * </ul>
 * The order of the constants is the order of the ruleset pulldown.
 */
public enum Ruleset {
    CLASSIC("classic"),
    RESURRECTED("resurrected"),
    BUFFED("buffed");

    /** The ruleset of every game that does not choose one: campaign, tutorials, multiplayer, old presets. */
    public static final @NonNull Ruleset DEFAULT = RESURRECTED;

    /** The ruleset preselected in the Single-player skirmish menu. */
    public static final @NonNull Ruleset SKIRMISH_DEFAULT = BUFFED;

    private static final ResourceBundle bundle = ResourceBundle.getBundle(Ruleset.class.getName());

    private final @NonNull String id;

    Ruleset(@NonNull String id) {
        this.id = id;
    }

    /** File name of the data file without extension, and the value stored in presets. */
    public @NonNull String getId() {
        return id;
    }

    public @NonNull RulesetStats getStats() {
        return RulesetLoader.get(this);
    }

    public @NonNull String getDisplayName() {
        return Utils.getBundleString(bundle, id);
    }

    public static @Nullable Ruleset fromId(@Nullable String id) {
        for (Ruleset ruleset : values()) {
            if (ruleset.id.equals(id)) {
                return ruleset;
            }
        }
        return null;
    }
}
