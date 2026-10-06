package com.villagerbargains.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.villagerbargains.config.PricingMode;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Trade costs that use a random {@code minecraft:uniform} count, like the Trade Rebalance
 * experiment's level 5 librarian books ({@code 11 + uniform(0, 35)}).
 *
 * <p>The costs are built from JSON with vanilla's own codec, so one test jar works on every
 * supported version even though the number provider classes differ:
 * <ul>
 *   <li>26.2 writes the sum as {@code minecraft:sum}/{@code summands} and adds floats: 11..45</li>
 *   <li>26.3 writes it as {@code minecraft:add}/{@code inputs} and adds integers: 11..46</li>
 * </ul>
 */
public final class UniformTradeCostGameTest {
    private static final int SAMPLES = 200;
    private static final String UNIFORM_0_35 = "{\"type\": \"minecraft:uniform\", \"min\": 0, \"max\": 35}";

    /** The rebalance book price as written by each Minecraft version, with its vanilla maximum. */
    private record PriceFormat(String countJson, int highest) {}

    private static final PriceFormat[] REBALANCE_BOOK_PRICE = {
            new PriceFormat("{\"type\": \"minecraft:sum\", \"summands\": [11, " + UNIFORM_0_35 + "]}", 45), // 26.2
            new PriceFormat("{\"type\": \"minecraft:add\", \"inputs\": [11, " + UNIFORM_0_35 + "]}", 46),   // 26.3
    };

    @GameTest
    public void tradeCostRangesArePinned(GameTestHelper helper) {
        Villager trader = TradeTestSupport.spawnTrader(helper);
        int formatsFound = 0;
        for (PriceFormat format : REBALANCE_BOOK_PRICE) {
            Optional<TradeCost> cost = parseCost(helper, format.countJson());
            if (cost.isEmpty()) {
                continue; // This Minecraft version writes the sum differently.
            }
            formatsFound++;
            assertAllCosts(helper, trader, cost.get(), PricingMode.MINIMUM, 11);
            assertAllCosts(helper, trader, cost.get(), PricingMode.MAXIMUM, format.highest());
        }
        helper.assertValueEqual(formatsFound, 1, "number of rebalance price formats this version understands");
        helper.succeed();
    }

    @GameTest
    public void plainUniformCostIsPinned(GameTestHelper helper) {
        Villager trader = TradeTestSupport.spawnTrader(helper);
        TradeCost cost = parseCost(helper, UNIFORM_0_35).orElseThrow();
        assertAllCosts(helper, trader, cost, PricingMode.MINIMUM, 0);
        assertAllCosts(helper, trader, cost, PricingMode.MAXIMUM, 35);
        helper.succeed();
    }

    @GameTest
    public void rollsOutsideTradeCostsAreUntouched(GameTestHelper helper) {
        Villager trader = TradeTestSupport.spawnTrader(helper);
        TradeCost cost = parseCost(helper, UNIFORM_0_35).orElseThrow();
        Set<Integer> values = TradeTestSupport.withMode(PricingMode.MINIMUM, () -> {
            Set<Integer> seen = new HashSet<>();
            for (int i = 0; i < SAMPLES; i++) {
                seen.add(rollCountDirectly(cost, TradeTestSupport.tradeContext(helper, trader, TradeTestSupport.seed(i))));
            }
            return seen;
        });
        helper.assertTrue(values.size() > 10, "Uniform rolls outside a trade cost must stay random, got " + values);
        helper.succeed();
    }

    private static void assertAllCosts(GameTestHelper helper, Villager trader, TradeCost cost, PricingMode mode, int expected) {
        TradeTestSupport.withMode(mode, () -> {
            for (int i = 0; i < SAMPLES; i++) {
                LootContext context = TradeTestSupport.tradeContext(helper, trader, TradeTestSupport.seed(i));
                helper.assertValueEqual(cost.toItemCost(context, 0).count(), expected, mode + " trade cost for sample " + i);
            }
            return null;
        });
    }

    /** Parses {@code {"id": "minecraft:emerald", "count": <countJson>}}; empty if this version rejects it. */
    private static Optional<TradeCost> parseCost(GameTestHelper helper, String countJson) {
        JsonElement json = JsonParser.parseString("{\"id\": \"minecraft:emerald\", \"count\": " + countJson + "}");
        return TradeCost.CODEC
                .parse(helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE), json)
                .result();
    }

    /**
     * Evaluates the cost's count provider without going through {@code TradeCost#toItemCost}.
     * Uses reflection because {@code count()} returns a {@code NumberProvider} in 26.2 and a
     * {@code Holder<ContextIntProvider>} in 26.3; both have {@code getInt(LootContext)}.
     */
    private static int rollCountDirectly(TradeCost cost, LootContext context) {
        try {
            Object provider = TradeCost.class.getMethod("count").invoke(cost);
            if (provider instanceof Holder<?> holder) {
                provider = holder.value();
            }
            return (int) provider.getClass().getMethod("getInt", LootContext.class).invoke(provider, context);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not evaluate trade cost count", e);
        }
    }
}
