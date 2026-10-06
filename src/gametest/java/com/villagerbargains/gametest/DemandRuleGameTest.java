package com.villagerbargains.gametest;

import com.villagerbargains.config.PricingMode;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * The demand rule: how much a trade was bought between restocks raises its price.
 * <ul>
 *   <li>MINIMUM: demand never raises the price;</li>
 *   <li>NORMAL: vanilla demand;</li>
 *   <li>MAXIMUM: always at least the surcharge of a trade that was sold out before a restock,
 *       already on newly generated offers.</li>
 * </ul>
 */
public final class DemandRuleGameTest {
    private static final ResourceKey<VillagerTrade> LIBRARIAN_BOOK = ResourceKey.create(
            Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace("librarian/1/emerald_and_book_enchanted_book"));
    private static final int BASE_PRICE = 10;
    private static final int MAX_USES = 12;
    private static final float PRICE_MULTIPLIER = 0.05f;
    private static final int SAMPLES = 50;

    @GameTest
    public void soldOutRestockPrices(GameTestHelper helper) {
        // Vanilla: 10 + floor(10 * 12 * 0.05) = 16.
        assertCostAfterRestock(helper, PricingMode.MINIMUM, MAX_USES, 10);
        assertCostAfterRestock(helper, PricingMode.NORMAL, MAX_USES, 16);
        assertCostAfterRestock(helper, PricingMode.MAXIMUM, MAX_USES, 16);
        helper.succeed();
    }

    @GameTest
    public void unusedRestockPrices(GameTestHelper helper) {
        // Vanilla demand becomes -12, which adds nothing.
        assertCostAfterRestock(helper, PricingMode.MINIMUM, 0, 10);
        assertCostAfterRestock(helper, PricingMode.NORMAL, 0, 10);
        assertCostAfterRestock(helper, PricingMode.MAXIMUM, 0, 16);
        helper.succeed();
    }

    @GameTest
    public void newOffersStartWithPinnedDemand(GameTestHelper helper) {
        Holder<VillagerTrade> trade = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE).getOrThrow(LIBRARIAN_BOOK);
        Villager trader = TradeTestSupport.spawnTrader(helper);
        for (PricingMode mode : PricingMode.values()) {
            TradeTestSupport.withMode(mode, () -> {
                for (int i = 0; i < SAMPLES; i++) {
                    long seed = TradeTestSupport.seed(i);
                    LootContext context = TradeTestSupport.tradeContext(helper, trader, seed);
                    MerchantOffer offer = trade.value().getOffer(context);
                    if (offer == null) {
                        throw helper.assertionException("No offer generated for seed %s", seed);
                    }
                    int expected = mode == PricingMode.MAXIMUM ? offer.getMaxUses() : 0;
                    helper.assertValueEqual(offer.getDemand(), expected, mode + " demand of new offer for seed " + seed);
                }
                return null;
            });
        }
        helper.succeed();
    }

    /** Uses a fresh offer {@code uses} times, restocks it under {@code mode} and checks the price. */
    private static void assertCostAfterRestock(GameTestHelper helper, PricingMode mode, int uses, int expected) {
        MerchantOffer offer = new MerchantOffer(
                new ItemCost(Items.EMERALD, BASE_PRICE), new ItemStack(Items.BOOK), MAX_USES, 1, PRICE_MULTIPLIER);
        for (int i = 0; i < uses; i++) {
            offer.increaseUses();
        }
        TradeTestSupport.withMode(mode, () -> {
            offer.updateDemand();
            return null;
        });
        helper.assertValueEqual(offer.getCostA().getCount(), expected,
                mode + " price after a restock with " + uses + " uses");
    }
}
