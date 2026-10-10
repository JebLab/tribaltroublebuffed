package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.TargetedMagicFactory;
import org.jspecify.annotations.NonNull;

/**
 * A chieftain walks to within its spell's range of a target and casts it there (Buffed's Hammer of Thor). The order
 * ends without a cast when the target dies, the charge is spent, or the target cannot be reached.
 */
public final class CastController extends Controller {
    private final @NonNull Unit unit;
    private final int magic_index;
    private final @NonNull Selectable<?> target;

    public CastController(@NonNull Unit unit, int magic_index, @NonNull Selectable<?> target) {
        super(1);
        this.unit = unit;
        this.magic_index = magic_index;
        this.target = target;
    }

    @Override
    public void decide() {
        if (target.isDead() || !unit.canDoMagic(magic_index)) {
            unit.popController();
            return;
        }
        TargetedMagicFactory factory = (TargetedMagicFactory) unit.getOwner().getRace().getMagicFactory(magic_index);
        // Grid cells, as a weapon's range is.
        float range = factory.getRange() / HeightMap.METERS_PER_UNIT_GRID + target.getSize();
        if (unit.isCloseEnough(range, target)) {
            unit.popController();
            unit.doMagicAt(magic_index, target);
        } else if (!shouldGiveUp(0)) {
            unit.setBehaviour(new WalkBehaviour(unit, target, range, false));
        } else {
            unit.popController();
        }
    }
}
