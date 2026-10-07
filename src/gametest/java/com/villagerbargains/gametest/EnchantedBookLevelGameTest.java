package com.villagerbargains.gametest;

import com.villagerbargains.config.PricingMode;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Generates the vanilla librarian enchanted-book trade many times with fixed seeds and checks the
 * {@code bookLevels} setting:
 * <ul>
 *   <li>MINIMUM / MAXIMUM always sell the lowest / highest level of the book that was rolled;</li>
 *   <li>NORMAL still varies the level like vanilla;</li>
 *   <li>the enchantment and the random sequence afterwards are identical in every level mode;</li>
 *   <li>the price is the vanilla price for the level that is sold, in every pricing mode;</li>
 *   <li>{@code enchant_randomly} outside a trade keeps its vanilla level.</li>
 * </ul>
 */
public final class EnchantedBookLevelGameTest {
    private static final ResourceKey<VillagerTrade> LIBRARIAN_BOOK = ResourceKey.create(
            Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace("librarian/1/emerald_and_book_enchanted_book"));
    private static final int SAMPLES = 300;
    private static final List<PricingMode> MODES = List.of(PricingMode.MINIMUM, PricingMode.NORMAL, PricingMode.MAXIMUM);

    /** What one generated offer looked like, plus the next random value drawn after it. */
    private record Sample(String book, int level, int minLevel, int maxLevel, boolean doubled, int price, long nextRandom) {}

    @GameTest
    public void maximumAlwaysSellsTheHighestLevel(GameTestHelper helper) {
        for (Sample sample : generate(helper, PricingMode.NORMAL, PricingMode.MAXIMUM)) {
            helper.assertValueEqual(sample.level(), sample.maxLevel(), "MAXIMUM level of " + sample.book());
        }
        helper.succeed();
    }

    @GameTest
    public void minimumAlwaysSellsTheLowestLevel(GameTestHelper helper) {
        for (Sample sample : generate(helper, PricingMode.NORMAL, PricingMode.MINIMUM)) {
            helper.assertValueEqual(sample.level(), sample.minLevel(), "MINIMUM level of " + sample.book());
        }
        helper.succeed();
    }

    @GameTest
    public void normalKeepsVanillaLevels(GameTestHelper helper) {
        Map<String, Set<Integer>> levelsByBook = new HashMap<>();
        for (Sample sample : generate(helper, PricingMode.NORMAL, PricingMode.NORMAL)) {
            helper.assertValueInBetween(sample.minLevel(), sample.level(), sample.maxLevel(), "NORMAL level of " + sample.book());
            if (sample.maxLevel() > 1) {
                levelsByBook.computeIfAbsent(sample.book(), book -> new HashSet<>()).add(sample.level());
            }
        }
        boolean varied = levelsByBook.values().stream().anyMatch(levels -> levels.size() > 1);
        helper.assertTrue(varied, "NORMAL should produce varied book levels, got " + levelsByBook);
        helper.succeed();
    }

    @GameTest
    public void onlyTheLevelChangesBetweenLevelModes(GameTestHelper helper) {
        List<Sample> normal = generate(helper, PricingMode.NORMAL, PricingMode.NORMAL);
        for (PricingMode levels : List.of(PricingMode.MINIMUM, PricingMode.MAXIMUM)) {
            List<Sample> pinned = generate(helper, PricingMode.NORMAL, levels);
            for (int i = 0; i < SAMPLES; i++) {
                Sample a = normal.get(i);
                Sample b = pinned.get(i);
                helper.assertValueEqual(b.book(), a.book(), levels + " book levels: enchantment for sample " + i);
                helper.assertValueEqual(b.nextRandom(), a.nextRandom(), levels + " book levels: next random value for sample " + i);
            }
        }
        helper.succeed();
    }

    @GameTest
    public void priceMatchesTheSoldLevelInEveryMode(GameTestHelper helper) {
        for (PricingMode pricing : MODES) {
            for (PricingMode levels : MODES) {
                for (Sample sample : generate(helper, pricing, levels)) {
                    int factor = sample.doubled() ? 2 : 1;
                    int cheapest = clamp((2 + 3 * sample.level()) * factor);
                    int dearest = clamp((6 + 13 * sample.level()) * factor);
                    String what = pricing + " price, " + levels + " book levels: " + sample.book() + " " + sample.level();
                    switch (pricing) {
                        case MINIMUM -> helper.assertValueEqual(sample.price(), cheapest, what);
                        case MAXIMUM -> helper.assertValueEqual(sample.price(), dearest, what);
                        default -> helper.assertValueInBetween(cheapest, sample.price(), dearest, what);
                    }
                }
            }
        }
        helper.succeed();
    }

    /**
     * Runs the same librarian trade definition, but without the
     * {@code ADDITIONAL_COST_COMPONENT_ALLOWED} parameter, so its {@code enchant_randomly} runs the
     * way it does for chest loot (the mixin only treats a run as a trade when that parameter is
     * present). This reuses the trade's own vanilla {@code enchant_randomly} instead of building
     * the function by hand, like {@code UniformTradeCostGameTest} reuses a trade cost.
     */
    @GameTest
    public void nonTradeEnchantRandomlyKeepsVanillaLevels(GameTestHelper helper) {
        List<Sample> normal = generateOutsideTrade(helper, PricingMode.NORMAL);
        List<Sample> maximum = generateOutsideTrade(helper, PricingMode.MAXIMUM);
        boolean belowMaximum = false;
        for (int i = 0; i < SAMPLES; i++) {
            Sample a = normal.get(i);
            Sample b = maximum.get(i);
            helper.assertValueEqual(b.book(), a.book(), "non-trade enchantment for sample " + i);
            helper.assertValueEqual(b.level(), a.level(), "non-trade enchantment level for sample " + i);
            helper.assertValueEqual(b.nextRandom(), a.nextRandom(), "non-trade next random value for sample " + i);
            belowMaximum |= b.level() < b.maxLevel();
        }
        // Without this the comparison above could pass by accident if every roll were already the maximum.
        helper.assertTrue(belowMaximum, "MAXIMUM book levels must not raise non-trade levels, but every level was the maximum");
        helper.succeed();
    }

    private static List<Sample> generate(GameTestHelper helper, PricingMode pricing, PricingMode levels) {
        Holder<VillagerTrade> trade = librarianBook(helper);
        Villager trader = TradeTestSupport.spawnTrader(helper);
        return TradeTestSupport.withBookLevelMode(levels, () -> TradeTestSupport.withMode(pricing, () -> {
            List<Sample> samples = new ArrayList<>();
            for (int i = 0; i < SAMPLES; i++) {
                long seed = TradeTestSupport.seed(i);
                samples.add(sample(helper, trade, TradeTestSupport.tradeContext(helper, trader, seed), seed));
            }
            return samples;
        }));
    }

    private static List<Sample> generateOutsideTrade(GameTestHelper helper, PricingMode levels) {
        Holder<VillagerTrade> trade = librarianBook(helper);
        Villager trader = TradeTestSupport.spawnTrader(helper);
        return TradeTestSupport.withBookLevelMode(levels, () -> {
            List<Sample> samples = new ArrayList<>();
            for (int i = 0; i < SAMPLES; i++) {
                long seed = TradeTestSupport.seed(i);
                samples.add(sample(helper, trade, nonTradeContext(helper, trader, seed), seed));
            }
            return samples;
        });
    }

    private static Holder<VillagerTrade> librarianBook(GameTestHelper helper) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).getOrThrow(LIBRARIAN_BOOK);
    }

    /** Like {@link TradeTestSupport#tradeContext}, but without {@code ADDITIONAL_COST_COMPONENT_ALLOWED}. */
    private static LootContext nonTradeContext(GameTestHelper helper, Villager trader, long seed) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, trader.position())
                .withParameter(LootContextParams.THIS_ENTITY, trader)
                .create(LootContextParamSets.VILLAGER_TRADE);
        return new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty());
    }

    private static Sample sample(GameTestHelper helper, Holder<VillagerTrade> trade, LootContext context, long seed) {
        MerchantOffer offer = trade.value().getOffer(context);
        if (offer == null) {
            throw helper.assertionException("No offer generated for seed %s", seed);
        }
        return describe(offer, context.getRandom().nextLong());
    }

    private static Sample describe(MerchantOffer offer, long nextRandom) {
        ItemStack book = offer.getResult();
        ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
        Object2IntMap.Entry<Holder<Enchantment>> entry = stored.entrySet().iterator().next();
        Holder<Enchantment> enchantment = entry.getKey();
        return new Sample(
                enchantment.getRegisteredName(),
                entry.getIntValue(),
                enchantment.value().getMinLevel(),
                enchantment.value().getMaxLevel(),
                enchantment.is(EnchantmentTags.DOUBLE_TRADE_PRICE),
                offer.getBaseCostA().getCount(),
                nextRandom);
    }

    /** Vanilla clamps every cost to the emerald stack size. */
    private static int clamp(int price) {
        return Math.min(price, 64);
    }
}
