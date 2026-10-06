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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates the vanilla librarian enchanted-book trade many times with fixed seeds and checks:
 * <ul>
 *   <li>MINIMUM / MAXIMUM always give the vanilla extreme for the book that was rolled;</li>
 *   <li>NORMAL still varies like vanilla;</li>
 *   <li>the enchantment, its level and the random sequence afterwards are identical in every mode.</li>
 * </ul>
 */
public final class EnchantedBookPriceGameTest {
    private static final ResourceKey<VillagerTrade> LIBRARIAN_BOOK = ResourceKey.create(
            Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace("librarian/1/emerald_and_book_enchanted_book"));
    private static final int SAMPLES = 300;

    /** What one generated offer looked like, plus the next random value drawn after it. */
    private record Sample(String book, int level, boolean doubled, int price, long nextRandom) {}

    @GameTest
    public void minimumIsAlwaysTheCheapestVanillaPrice(GameTestHelper helper) {
        for (Sample sample : generate(helper, PricingMode.MINIMUM)) {
            int expected = clamp((2 + 3 * sample.level()) * (sample.doubled() ? 2 : 1));
            helper.assertValueEqual(sample.price(), expected, "MINIMUM price of " + sample.book() + " " + sample.level());
        }
        helper.succeed();
    }

    @GameTest
    public void maximumIsAlwaysTheMostExpensiveVanillaPrice(GameTestHelper helper) {
        for (Sample sample : generate(helper, PricingMode.MAXIMUM)) {
            int expected = clamp((6 + 13 * sample.level()) * (sample.doubled() ? 2 : 1));
            helper.assertValueEqual(sample.price(), expected, "MAXIMUM price of " + sample.book() + " " + sample.level());
        }
        helper.succeed();
    }

    @GameTest
    public void normalKeepsVanillaRandomness(GameTestHelper helper) {
        Set<Integer> pricesForLevelOne = new HashSet<>();
        for (Sample sample : generate(helper, PricingMode.NORMAL)) {
            int factor = sample.doubled() ? 2 : 1;
            helper.assertValueInBetween(
                    clamp((2 + 3 * sample.level()) * factor),
                    sample.price(),
                    clamp((6 + 13 * sample.level()) * factor),
                    "NORMAL price of " + sample.book() + " " + sample.level());
            if (sample.level() == 1 && !sample.doubled()) {
                pricesForLevelOne.add(sample.price());
            }
        }
        helper.assertTrue(pricesForLevelOne.size() > 5, "NORMAL should produce varied prices, got " + pricesForLevelOne);
        helper.succeed();
    }

    @GameTest
    public void onlyThePriceChangesBetweenModes(GameTestHelper helper) {
        List<Sample> normal = generate(helper, PricingMode.NORMAL);
        for (PricingMode mode : List.of(PricingMode.MINIMUM, PricingMode.MAXIMUM)) {
            List<Sample> pinned = generate(helper, mode);
            for (int i = 0; i < SAMPLES; i++) {
                Sample a = normal.get(i);
                Sample b = pinned.get(i);
                helper.assertValueEqual(b.book(), a.book(), mode + " enchantment for sample " + i);
                helper.assertValueEqual(b.level(), a.level(), mode + " enchantment level for sample " + i);
                helper.assertValueEqual(b.nextRandom(), a.nextRandom(), mode + " next random value for sample " + i);
            }
        }
        helper.succeed();
    }

    private static List<Sample> generate(GameTestHelper helper, PricingMode mode) {
        Holder<VillagerTrade> trade = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE).getOrThrow(LIBRARIAN_BOOK);
        Villager trader = TradeTestSupport.spawnTrader(helper);
        return TradeTestSupport.withMode(mode, () -> {
            List<Sample> samples = new ArrayList<>();
            for (int i = 0; i < SAMPLES; i++) {
                long seed = TradeTestSupport.seed(i);
                LootContext context = TradeTestSupport.tradeContext(helper, trader, seed);
                MerchantOffer offer = trade.value().getOffer(context);
                if (offer == null) {
                    throw helper.assertionException("No offer generated for seed %s", seed);
                }
                samples.add(describe(offer, context.getRandom().nextLong()));
            }
            return samples;
        });
    }

    private static Sample describe(MerchantOffer offer, long nextRandom) {
        ItemStack book = offer.getResult();
        ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
        Object2IntMap.Entry<Holder<Enchantment>> entry = stored.entrySet().iterator().next();
        Holder<Enchantment> enchantment = entry.getKey();
        return new Sample(
                enchantment.getRegisteredName(),
                entry.getIntValue(),
                enchantment.is(EnchantmentTags.DOUBLE_TRADE_PRICE),
                offer.getBaseCostA().getCount(),
                nextRandom);
    }

    /** Vanilla clamps every cost to the emerald stack size. */
    private static int clamp(int price) {
        return Math.min(price, 64);
    }
}
