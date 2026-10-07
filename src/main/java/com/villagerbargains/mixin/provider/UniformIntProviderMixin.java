package com.villagerbargains.mixin.provider;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.price.EnchantPowerRolls;
import com.villagerbargains.price.ThreadScope;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * <b>Minecraft 26.3 and newer.</b> Integer {@code minecraft:uniform} provider used inside a trade cost.
 *
 * <p>The target class does not exist in 26.2;
 * {@link com.villagerbargains.mixin.VillagerBargainsMixinPlugin} skips this mixin there.
 *
 * <p>Vanilla: {@code getIntUnsafe -> Mth.nextInt(random, min, max)} (both inclusive).
 * The Trade Rebalance experiment prices books as {@code add(11, uniform(0, 35))}, which
 * resolves through this method. Outside {@link ThreadScope#TRADE_COST} the roll is unchanged and only
 * recorded in {@link EnchantPowerRolls} (the {@code levels} of {@code enchant_with_levels}, e.g.
 * {@code uniform(5, 19)} for enchanted gear trades).
 */
@Mixin(UniformGenerator.class)
public abstract class UniformIntProviderMixin {
    @WrapOperation(
            method = "getIntUnsafe",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I")
    )
    private int villagerbargains$resolveIntRoll(RandomSource random, int min, int max, Operation<Integer> original) {
        int rolled = original.call(random, min, max);
        if (ThreadScope.TRADE_COST.isActive()) {
            return VillagerBargainsConfig.pricingMode().pick(rolled, min, max);
        }
        EnchantPowerRolls.record(rolled, min, max); // Enchanted gear price; the roll itself stays vanilla.
        return rolled;
    }
}
