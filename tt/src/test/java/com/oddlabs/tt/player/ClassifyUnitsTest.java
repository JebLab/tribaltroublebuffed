package com.oddlabs.tt.player;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.behaviour.Controller;
import com.oddlabs.tt.model.behaviour.GatherController;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Player.classifyUnits() needs a World, which needs OpenGL resources, so these tests cover the two parts it is built
 * from: {@link Player#groupByKey} and {@link Controller#getKey()}.
 */
class ClassifyUnitsTest {
    @Test
    void groupsComeInFirstAppearanceOrder() {
        // A HashMap would return these in key hash order: a, b, c.
        List<String> units = List.of("c1", "a1", "b1", "c2", "a2", "b2", "c3");

        List<List<String>> groups = Player.groupByKey(units, unit -> unit.charAt(0));

        assertEquals(List.of(List.of("c1", "c2", "c3"), List.of("a1", "a2"), List.of("b1", "b2")), groups);
    }

    @Test
    void groupOrderDoesNotDependOnIdentityHashCodes() {
        // Identity hash codes change between JVM runs, like the old Controller keys did.
        List<Object> keys = IntStream.range(0, 64).mapToObj(i -> new Object()).toList();
        List<Integer> units = new ArrayList<>();
        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < keys.size(); i++) {
                units.add(i);
            }
        }

        List<List<Integer>> groups = Player.groupByKey(units, keys::get);

        List<List<Integer>> expected = IntStream.range(0, keys.size()).mapToObj(i -> List.of(i, i)).toList();
        assertEquals(expected, groups);
    }

    @Test
    void controllerKeysCompareByValue() {
        assertEquals(new TestControllerA().getKey(), new TestControllerA().getKey());
        assertNotEquals(new TestControllerA().getKey(), new TestControllerB().getKey());
        assertEquals(TestControllerA.class, new TestControllerA().getKey());

        // getKey() only reads the supply type, so the unit can be left out.
        assertEquals(new GatherController<>(null, null, TreeSupply.class).getKey(),
                new GatherController<>(null, null, TreeSupply.class).getKey());
        assertNotEquals(new GatherController<>(null, null, TreeSupply.class).getKey(),
                new GatherController<>(null, null, RockSupply.class).getKey());
    }

    private static final class TestControllerA extends Controller {
        TestControllerA() {
            super(0);
        }

        @Override
        public void decide() {
        }
    }

    private static final class TestControllerB extends Controller {
        TestControllerB() {
            super(0);
        }

        @Override
        public void decide() {
        }
    }
}
