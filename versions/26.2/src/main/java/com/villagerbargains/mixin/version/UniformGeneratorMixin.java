package com.villagerbargains.mixin.version;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villagerbargains.price.PriceRolls;
import com.villagerbargains.price.TradeCostScope;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Minecraft 26.2: {@code minecraft:uniform} number provider used inside a trade cost.
 *
 * <p>Vanilla:
 * <pre>{@code
 * getInt:   Mth.nextInt(random, min, max)     // min..max inclusive
 * getFloat: Mth.nextFloat(random, min, max)   // [min, max)
 * }</pre>
 * {@code minecraft:sum} (used by the Trade Rebalance experiment, e.g. {@code 11 + uniform(0, 35)})
 * adds its parts with {@code getFloat}, so both methods are covered.
 * Outside a {@link TradeCostScope} the roll is returned unchanged.
 */
@Mixin(UniformGenerator.class)
public abstract class UniformGeneratorMixin {
    @WrapOperation(
            method = "getInt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I")
    )
    private int villagerbargains$resolveIntRoll(RandomSource random, int min, int max, Operation<Integer> original) {
        int rolled = original.call(random, min, max);
        return TradeCostScope.isActive() ? PriceRolls.resolve(rolled, min, max) : rolled;
    }

    @WrapOperation(
            method = "getFloat",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;nextFloat(Lnet/minecraft/util/RandomSource;FF)F")
    )
    private float villagerbargains$resolveFloatRoll(RandomSource random, float min, float max, Operation<Float> original) {
        float rolled = original.call(random, min, max);
        return TradeCostScope.isActive() ? PriceRolls.resolve(rolled, min, max) : rolled;
    }
}
