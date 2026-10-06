package com.villagerbargains.gametest;

import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Optional;
import java.util.function.Supplier;

/** Helpers shared by the game tests: seeded trade contexts and temporary pricing modes. */
final class TradeTestSupport {
    private TradeTestSupport() {}

    /** A villager to act as the trader ({@code this} entity) for trade generation. */
    static Villager spawnTrader(GameTestHelper helper) {
        return helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
    }

    /**
     * Builds the same loot context {@code AbstractVillager#addOffersFromTradeSet} uses,
     * but with a fixed seed so runs can be compared.
     */
    static LootContext tradeContext(GameTestHelper helper, Villager trader, long seed) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, trader.position())
                .withParameter(LootContextParams.THIS_ENTITY, trader)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE);
        return new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty());
    }

    /** Runs {@code action} with {@code mode} active, then restores the previous mode. */
    static <T> T withMode(PricingMode mode, Supplier<T> action) {
        PricingMode previous = VillagerBargainsConfig.pricingMode();
        VillagerBargainsConfig.setPricingMode(mode);
        try {
            return action.get();
        } finally {
            VillagerBargainsConfig.setPricingMode(previous);
        }
    }
}
