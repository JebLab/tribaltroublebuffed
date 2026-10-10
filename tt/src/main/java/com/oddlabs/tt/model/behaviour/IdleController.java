package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.AttackScanFilter;
import com.oddlabs.tt.model.Hittable;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class IdleController extends Controller {
    private static final float MIN_SCAN_DELAY = 1f;
    private static final float MAX_SCAN_DELAY = 2f;

    private final @NonNull Unit unit;
    private final @NonNull AttackScanFilter scan_filter;
    private final @NonNull IdleBehaviour idle_behaviour;
    private final boolean can_move;
    private float redecide_time;

    public IdleController(@NonNull Unit unit, @NonNull AttackScanFilter filter, boolean can_move) {
        super(0);
        this.unit = unit;
        this.scan_filter = filter;
        this.idle_behaviour = new IdleBehaviour(this, unit);
        this.can_move = can_move;
    }

    public boolean shouldSleep(float t) {
        redecide_time -= t;
        return redecide_time > 0;
    }

    @Override
    public void decide() {
        unit.setBehaviour(idle_behaviour);
        if (shouldSleep(0f))
            return;
        redecide_time = MIN_SCAN_DELAY + unit.getOwner().getWorld().getRandom().nextFloat() * (MAX_SCAN_DELAY - MIN_SCAN_DELAY);
        if (unit.getAbilities().hasAbilities(Abilities.ATTACK))
            unit.scanVicinity(scan_filter);
        Hittable s = scan_filter.removeTarget();
        if (s != null) {
            if (can_move)
                unit.pushControllers(new WalkController(unit, new LandscapeTarget(unit.getGridX(), unit.getGridY()),
                        true), new HuntController(unit, s));
            else
                unit.pushController(new AttackController(unit, s));
        }
    }

    @Override
    public @NonNull Object getKey() {
        // Buffed's drummers and chicken catchers idle in a group of their own, apart from the army.
        return List.of(super.getKey(), unit.getAbilities().hasAbilities(Abilities.BUILD),
                unit.getAbilities().hasAbilities(Abilities.MAGIC), unit.isDrummer() || unit.isNetter());
    }
}
