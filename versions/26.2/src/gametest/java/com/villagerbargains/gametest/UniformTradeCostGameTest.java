package com.villagerbargains.gametest;

import com.villagerbargains.config.PricingMode;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.Sum;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * Minecraft 26.2: a trade cost of {@code sum(11, uniform(0, 35))}, exactly what the Trade Rebalance
 * experiment uses for level 5 librarian books. Also checks that uniform rolls outside a trade
 * cost (loot tables etc.) are left alone.
 */
public final class UniformTradeCostGameTest {
    private static final int SAMPLES = 200;
    private static final NumberProvider REBALANCE_BOOK_PRICE = Sum.sum(ConstantValue.exactly(11f), UniformGenerator.between(0f, 35f));

    @GameTest
    public void tradeCostRangesArePinned(GameTestHelper helper) {
        TradeCost cost = new TradeCost(Items.EMERALD.builtInRegistryHolder(), REBALANCE_BOOK_PRICE, DataComponentExactPredicate.EMPTY);
        Villager trader = TradeTestSupport.spawnTrader(helper);

        assertAllCosts(helper, trader, cost, PricingMode.MINIMUM, 11);
        assertAllCosts(helper, trader, cost, PricingMode.MAXIMUM, 45);
        helper.succeed();
    }

    @GameTest
    public void rollsOutsideTradeCostsAreUntouched(GameTestHelper helper) {
        Villager trader = TradeTestSupport.spawnTrader(helper);
        UniformGenerator uniform = UniformGenerator.between(0f, 35f);
        Set<Integer> values = TradeTestSupport.withMode(PricingMode.MINIMUM, () -> {
            Set<Integer> seen = new HashSet<>();
            for (long seed = 0; seed < SAMPLES; seed++) {
                seen.add(uniform.getInt(TradeTestSupport.tradeContext(helper, trader, seed)));
            }
            return seen;
        });
        helper.assertTrue(values.size() > 10, "Uniform rolls outside a trade cost must stay random, got " + values);
        helper.succeed();
    }

    private static void assertAllCosts(GameTestHelper helper, Villager trader, TradeCost cost, PricingMode mode, int expected) {
        TradeTestSupport.withMode(mode, () -> {
            for (long seed = 0; seed < SAMPLES; seed++) {
                LootContext context = TradeTestSupport.tradeContext(helper, trader, seed);
                helper.assertValueEqual(cost.toItemCost(context, 0).count(), expected, mode + " trade cost for seed " + seed);
            }
            return null;
        });
    }
}
