package com.oddlabs.tt.ruleset;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reads {@code /rulesets/<id>.json} into {@link RulesetStats}. A file may name a parent with {@code "extends"}; its
 * objects are then merged key by key over the parent's, so a child lists only what it changes. The merged result must
 * set every field and nothing unknown, so a typo or a missing number fails at load instead of playing as zero.
 */
public final class RulesetLoader {
    private static final String DIRECTORY = "/rulesets/";
    private static final String EXTENDS = "extends";
    private static final String DESCRIPTION = "description";

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
            DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
            DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    private static final Map<Ruleset, RulesetStats> cache = new EnumMap<>(Ruleset.class);

    private RulesetLoader() {
    }

    static synchronized @NonNull RulesetStats get(@NonNull Ruleset ruleset) {
        return cache.computeIfAbsent(ruleset, r -> load(r.getId()));
    }

    /** Loads a ruleset file by id, resolving its {@code extends} chain. Not cached. */
    public static @NonNull RulesetStats load(@NonNull String id) {
        ObjectNode tree = resolve(id, new ArrayList<>());
        tree.remove(DESCRIPTION);
        try {
            return MAPPER.treeToValue(tree, RulesetStats.class);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid ruleset '" + id + "': " + e.getMessage(), e);
        }
    }

    private static @NonNull ObjectNode resolve(@NonNull String id, @NonNull List<String> chain) {
        if (chain.contains(id)) {
            throw new IllegalStateException("Ruleset inheritance loop: " + chain + " -> " + id);
        }
        chain.add(id);
        ObjectNode tree = read(id);
        JsonNode parent = tree.remove(EXTENDS);
        if (parent == null) {
            return tree;
        }
        ObjectNode base = resolve(parent.asText(), chain);
        merge(base, tree);
        return base;
    }

    private static @NonNull ObjectNode read(@NonNull String id) {
        String path = DIRECTORY + id + ".json";
        try (InputStream in = RulesetLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Ruleset file not found: " + path);
            }
            JsonNode tree = MAPPER.readTree(in);
            if (!(tree instanceof ObjectNode object)) {
                throw new IllegalStateException("Ruleset file is not a JSON object: " + path);
            }
            return object;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read ruleset " + path, e);
        }
    }

    private static void merge(@NonNull ObjectNode base, @NonNull ObjectNode overlay) {
        for (Map.Entry<String, JsonNode> entry : overlay.properties()) {
            JsonNode existing = base.get(entry.getKey());
            if (existing instanceof ObjectNode base_child && entry.getValue() instanceof ObjectNode overlay_child) {
                merge(base_child, overlay_child);
            } else {
                base.set(entry.getKey(), entry.getValue());
            }
        }
    }
}
