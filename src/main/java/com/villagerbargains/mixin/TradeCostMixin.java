package com.villagerbargains.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.villagerbargains.price.ThreadScope;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Opens {@link ThreadScope#TRADE_COST} while vanilla evaluates the {@code count} of a trade's
 * {@code wants} / {@code additional_wants}.
 *
 * <p>Vanilla trades use fixed counts, but data packs (including the vanilla
 * "Trade Rebalance" experiment) may use random number providers there. The
 * version-specific mixins in {@code mixin/provider} pin those rolls only inside this scope.
 */
@Mixin(TradeCost.class)
public abstract class TradeCostMixin {
    @WrapMethod(method = "toItemCost")
    private ItemCost villagerbargains$markTradeCost(LootContext context, int additionalCost, Operation<ItemCost> original) {
        return ThreadScope.TRADE_COST.run(() -> original.call(context, additionalCost));
    }
}
