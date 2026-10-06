package com.villagerbargains.mixin.version;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villagerbargains.price.PriceRolls;
import com.villagerbargains.price.TradeCostScope;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.providers.number.floats.UniformGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Minecraft 26.3: float {@code minecraft:uniform} provider used inside a trade cost
 * (a data pack may convert a float provider to the integer count).
 *
 * <p>Vanilla: {@code getFloatUnsafe -> Mth.nextFloat(random, min, max)} in {@code [min, max)}.
 * Outside a {@link TradeCostScope} the roll is unchanged.
 */
@Mixin(UniformGenerator.class)
public abstract class UniformFloatProviderMixin {
    @WrapOperation(
            method = "getFloatUnsafe",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;nextFloat(Lnet/minecraft/util/RandomSource;FF)F")
    )
    private float villagerbargains$resolveFloatRoll(RandomSource random, float min, float max, Operation<Float> original) {
        float rolled = original.call(random, min, max);
        return TradeCostScope.isActive() ? PriceRolls.resolve(rolled, min, max) : rolled;
    }
}
