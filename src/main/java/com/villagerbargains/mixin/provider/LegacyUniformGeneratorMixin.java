package com.villagerbargains.mixin.provider;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.price.EnchantPowerRolls;
import com.villagerbargains.price.ThreadScope;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * <b>Minecraft 26.2 only.</b> The {@code minecraft:uniform} number provider used inside a trade cost.
 *
 * <p>The target class was removed in 26.3, so it is named as a string (the mod compiles against
 * 26.3) and {@link com.villagerbargains.mixin.VillagerBargainsMixinPlugin} skips this mixin when
 * the class does not exist.
 *
 * <p>Vanilla 26.2:
 * <pre>{@code
 * getInt:   Mth.nextInt(random, min, max)     // min..max inclusive
 * getFloat: Mth.nextFloat(random, min, max)   // [min, max)
 * }</pre>
 * {@code minecraft:sum} (used by the Trade Rebalance experiment, e.g. {@code 11 + uniform(0, 35)})
 * adds its parts with {@code getFloat}, so both methods are covered.
 * Outside {@link ThreadScope#TRADE_COST} the roll is returned unchanged; an integer roll is then
 * only recorded (and, with {@code gearStrength} set, pinned) in {@link EnchantPowerRolls} (the {@code levels} of {@code enchant_with_levels}).
 */
@Mixin(targets = "net.minecraft.world.level.storage.loot.providers.number.UniformGenerator")
public abstract class LegacyUniformGeneratorMixin {
    @WrapOperation(
            method = "getInt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I")
    )
    private int villagerbargains$resolveIntRoll(RandomSource random, int min, int max, Operation<Integer> original) {
        int rolled = original.call(random, min, max);
        if (ThreadScope.TRADE_COST.isActive()) {
            return VillagerBargainsConfig.pricingMode().pick(rolled, min, max);
        }
        return EnchantPowerRolls.record(rolled, min, max);
    }

    @WrapOperation(
            method = "getFloat",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;nextFloat(Lnet/minecraft/util/RandomSource;FF)F")
    )
    private float villagerbargains$resolveFloatRoll(RandomSource random, float min, float max, Operation<Float> original) {
        float rolled = original.call(random, min, max);
        return ThreadScope.TRADE_COST.isActive() ? VillagerBargainsConfig.pricingMode().pick(rolled, min, max) : rolled;
    }
}
