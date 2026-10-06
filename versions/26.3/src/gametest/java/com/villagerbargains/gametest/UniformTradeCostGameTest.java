package com.villagerbargains.gametest;

import com.villagerbargains.config.PricingMode;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.Sum;
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * Minecraft 26.3: a trade cost of {@code add(11, uniform(0, 35))}, exactly what the Trade Rebalance
 * experiment uses for level 5 librarian books. Also checks that uniform rolls outside a trade
 * cost (loot tables etc.) are left alone.
 */
public final class UniformTradeCostGameTest {
    private static final int SAMPLES = 200;
    private static final UniformGenerator UNIFORM_0_35 = new UniformGenerator(ContextIntProviders.exactly(0), ContextIntProviders.exactly(35));
    private static final Holder<ContextIntProvider> REBALANCE_BOOK_PRICE =
            Holder.direct(new Sum(HolderSet.direct(ContextIntProviders.exactly(11), Holder.direct(UNIFORM_0_35))));

    @GameTest
    public void tradeCostRangesArePinned(GameTestHelper helper) {
        TradeCost cost = new TradeCost(Items.EMERALD, REBALANCE_BOOK_PRICE);
        Villager trader = TradeTestSupport.spawnTrader(helper);

        assertAllCosts(helper, trader, cost, PricingMode.MINIMUM, 11);
        assertAllCosts(helper, trader, cost, PricingMode.MAXIMUM, 46);
        helper.succeed();
    }

    @GameTest
    public void rollsOutsideTradeCostsAreUntouched(GameTestHelper helper) {
        Villager trader = TradeTestSupport.spawnTrader(helper);
        Set<Integer> values = TradeTestSupport.withMode(PricingMode.MINIMUM, () -> {
            Set<Integer> seen = new HashSet<>();
            for (long seed = 0; seed < SAMPLES; seed++) {
                seen.add(UNIFORM_0_35.getIntUnsafe(TradeTestSupport.tradeContext(helper, trader, seed)));
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
