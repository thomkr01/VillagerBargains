package com.villagerbargains.price;

import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/**
 * Records the enchanting-power rolls of an {@code enchant_with_levels} function, so the trade
 * price derived from them can be pinned without touching the enchantments.
 *
 * <p>Vanilla gear trades (armorer, toolsmith, weaponsmith, fletcher, fisherman, wandering trader)
 * charge {@code base + levels}, where {@code levels} (e.g. {@code uniform(5, 19)}) is also the
 * enchanting power. The rolls themselves are left untouched so the item enchants exactly like
 * vanilla; only the extra cost is moved to what the cheapest / most expensive roll would have cost.
 * The one exception is the opt-in {@code gearStrength} setting, which replaces the power roll
 * itself (after vanilla drew it) with the lowest or highest value of its range.
 *
 * <p>A recording is open only while {@link #run} executes; outside of it {@link #record} does nothing.
 */
public final class EnchantPowerRolls {
    private record Roll(int rolled, int lowest, int highest) {}

    // A stack, so a nested enchant_with_levels (e.g. from a data pack) keeps its own rolls.
    private static final ThreadLocal<Deque<List<Roll>>> OPEN = ThreadLocal.withInitial(ArrayDeque::new);

    private EnchantPowerRolls() {}

    /** Runs {@code action} with a fresh recording open on this thread. */
    public static <T> T run(Supplier<T> action) {
        Deque<List<Roll>> open = OPEN.get();
        open.push(new ArrayList<>());
        try {
            return action.get();
        } finally {
            open.pop();
        }
    }

    /**
     * Inside a recording: resolves an integer power roll between {@code lowest} and {@code highest}
     * (inclusive) by the gear strength mode, records it and returns it. Outside: returns {@code rolled}.
     */
    public static int record(int rolled, int lowest, int highest) {
        return record(rolled, lowest, highest, VillagerBargainsConfig.gearStrengthMode());
    }

    /** {@link #record(int, int, int)} with an explicit gear strength mode. */
    public static int record(int rolled, int lowest, int highest, PricingMode strength) {
        List<Roll> rolls = OPEN.get().peek();
        if (rolls == null) {
            return rolled;
        }
        int power = strength.pick(rolled, lowest, highest);
        rolls.add(new Roll(power, lowest, highest)); // Vanilla prices the power it enchants with, so record that.
        return power;
    }

    /** The cost vanilla computed from the recorded rolls, with every roll resolved by the pricing mode. */
    public static int pinnedCost(int vanillaCost) {
        return pinnedCost(vanillaCost, VillagerBargainsConfig.pricingMode());
    }

    /**
     * {@link #pinnedCost(int)} with an explicit mode. Unchanged when no recording is open.
     * Never below 0: vanilla adds no extra cost at all when the rolled power is not positive.
     * Assumes the recorded rolls are summands of the cost, as in every vanilla trade.
     */
    public static int pinnedCost(int vanillaCost, PricingMode mode) {
        List<Roll> rolls = OPEN.get().peek();
        if (rolls == null) {
            return vanillaCost;
        }
        int cost = vanillaCost;
        for (Roll roll : rolls) {
            cost += mode.pick(roll.rolled(), roll.lowest(), roll.highest()) - roll.rolled();
        }
        return Math.max(0, cost);
    }
}
