package com.villagerbargains.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.price.BookLevels;
import com.villagerbargains.price.ThreadScope;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Enchanted book price and level (librarians, and any trade using {@code enchant_randomly} with
 * {@code include_additional_cost_component}).
 *
 * <p>Vanilla, in {@code EnchantRandomlyFunction#enchantItem}, after the enchantment has been chosen:
 * <pre>{@code
 * int level = Mth.nextInt(random, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
 * ...
 * itemStack.set(DataComponents.ADDITIONAL_TRADE_COST, 2 + random.nextInt(5 + level * 10) + 3 * level);
 * }</pre>
 * The {@code Mth.nextInt} call is the level roll, pinned by {@link BookLevels} (trades only).
 * The {@code nextInt} call is the only {@code RandomSource#nextInt(int)} call in the method, and it
 * is the price roll. Its result ranges from {@code 0} to {@code bound - 1}. The level is resolved
 * first, so the price is always computed for the level that is actually sold.
 */
@Mixin(EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyFunctionMixin {
    private static final String RUN =
            "run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;";

    @WrapMethod(method = RUN)
    private ItemStack villagerbargains$markTradeBook(ItemStack itemStack, LootContext context, Operation<ItemStack> original) {
        if (!context.hasParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED)) {
            return original.call(itemStack, context); // Not a trade (chest loot, mob gear): vanilla level.
        }
        return ThreadScope.TRADE_BOOK.run(() -> original.call(itemStack, context));
    }

    @WrapOperation(
            method = "enchantItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I")
    )
    private int villagerbargains$resolveBookLevelRoll(RandomSource random, int lowest, int highest, Operation<Integer> original) {
        int rolled = original.call(random, lowest, highest); // Always roll, so the random sequence stays vanilla.
        return BookLevels.resolve(rolled, lowest, highest);
    }

    @WrapOperation(
            method = "enchantItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I")
    )
    private int villagerbargains$resolveBookPriceRoll(RandomSource random, int bound, Operation<Integer> original) {
        int rolled = original.call(random, bound); // Always roll, so the random sequence stays vanilla.
        return VillagerBargainsConfig.pricingMode().pick(rolled, 0, bound - 1);
    }
}
