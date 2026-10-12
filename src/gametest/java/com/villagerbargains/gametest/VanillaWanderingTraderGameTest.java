package com.villagerbargains.gametest;

import com.villagerbargains.config.PricingMode;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;

/**
 * The {@code vanillaWanderingTrader} setting: offers made for a wandering trader keep the vanilla
 * price and demand, while the same trade made for a villager stays pinned by the pricing mode.
 */
public final class VanillaWanderingTraderGameTest {
    private static final ResourceKey<VillagerTrade> LIBRARIAN_BOOK = ResourceKey.create(
            Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace("librarian/1/emerald_and_book_enchanted_book"));
    private static final int SAMPLES = 100;

    @GameTest
    public void wanderingTraderKeepsVanillaPricesWhenSet(GameTestHelper helper) {
        Holder<VillagerTrade> trade = librarianBook(helper);
        Entity wanderingTrader = helper.spawn(EntityTypes.WANDERING_TRADER, new BlockPos(1, 1, 1));
        Entity villager = TradeTestSupport.spawnTrader(helper);
        boolean pinnedDiffersFromVanilla = false;
        for (int i = 0; i < SAMPLES; i++) {
            long seed = TradeTestSupport.seed(i);
            int vanilla = TradeTestSupport.withMode(PricingMode.NORMAL, () -> price(helper, trade, wanderingTrader, seed));
            int pinned = TradeTestSupport.withMode(PricingMode.MINIMUM, () -> price(helper, trade, wanderingTrader, seed));
            int[] withSetting = TradeTestSupport.withMode(PricingMode.MINIMUM, () ->
                    TradeTestSupport.withVanillaWanderingTrader(true, () -> new int[] {
                            price(helper, trade, wanderingTrader, seed), price(helper, trade, villager, seed)}));
            helper.assertValueEqual(withSetting[0], vanilla, "wandering trader price with the setting on, seed " + seed);
            helper.assertValueEqual(withSetting[1], pinned, "villager price with the setting on, seed " + seed);
            helper.assertTrue(pinned <= vanilla, "MINIMUM price " + pinned + " is above the vanilla price " + vanilla);
            pinnedDiffersFromVanilla |= pinned != vanilla;
        }
        helper.assertTrue(pinnedDiffersFromVanilla, "MINIMUM should differ from vanilla for some seed, or the test proves nothing");
        helper.succeed();
    }

    @GameTest
    public void wanderingTraderDemandIsVanillaWhenSet(GameTestHelper helper) {
        Holder<VillagerTrade> trade = librarianBook(helper);
        Entity wanderingTrader = helper.spawn(EntityTypes.WANDERING_TRADER, new BlockPos(1, 1, 1));
        Entity villager = TradeTestSupport.spawnTrader(helper);
        long seed = TradeTestSupport.seed(0);
        TradeTestSupport.withMode(PricingMode.MAXIMUM, () -> {
            MerchantOffer pinned = offer(helper, trade, wanderingTrader, seed);
            helper.assertValueEqual(pinned.getDemand(), pinned.getMaxUses(), "pinned wandering trader demand");
            TradeTestSupport.withVanillaWanderingTrader(true, () -> {
                helper.assertValueEqual(offer(helper, trade, wanderingTrader, seed).getDemand(), 0, "vanilla wandering trader demand");
                MerchantOffer villagerOffer = offer(helper, trade, villager, seed);
                helper.assertValueEqual(villagerOffer.getDemand(), villagerOffer.getMaxUses(), "villager demand with the setting on");
                return null;
            });
            return null;
        });
        helper.succeed();
    }

    private static Holder<VillagerTrade> librarianBook(GameTestHelper helper) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).getOrThrow(LIBRARIAN_BOOK);
    }

    private static MerchantOffer offer(GameTestHelper helper, Holder<VillagerTrade> trade, Entity trader, long seed) {
        MerchantOffer offer = trade.value().getOffer(TradeTestSupport.tradeContext(helper, trader, seed));
        if (offer == null) {
            throw helper.assertionException("No offer generated for seed %s", seed);
        }
        return offer;
    }

    private static int price(GameTestHelper helper, Holder<VillagerTrade> trade, Entity trader, long seed) {
        return offer(helper, trade, trader, seed).getBaseCostA().getCount();
    }
}
