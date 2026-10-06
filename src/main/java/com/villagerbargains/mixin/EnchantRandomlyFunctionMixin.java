package com.villagerbargains.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villagerbargains.price.PriceRolls;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Enchanted book price (librarians, and any trade using {@code enchant_randomly} with
 * {@code include_additional_cost_component}).
 *
 * <p>Vanilla, in {@code EnchantRandomlyFunction#enchantItem}, after the enchantment and its
 * level have been chosen:
 * <pre>{@code
 * itemStack.set(DataComponents.ADDITIONAL_TRADE_COST, 2 + random.nextInt(5 + level * 10) + 3 * level);
 * }</pre>
 * That {@code nextInt} is the only {@code RandomSource#nextInt(int)} call in the method
 * (the level uses {@code Mth.nextInt}), and it is the price roll. Its result ranges from
 * {@code 0} to {@code bound - 1}.
 */
@Mixin(EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyFunctionMixin {
    @WrapOperation(
            method = "enchantItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I")
    )
    private int villagerbargains$resolveBookPriceRoll(RandomSource random, int bound, Operation<Integer> original) {
        int rolled = original.call(random, bound); // Always roll, so the random sequence stays vanilla.
        return PriceRolls.resolve(rolled, 0, bound - 1);
    }
}
