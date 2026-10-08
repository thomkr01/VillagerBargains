package com.villagerbargains.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villagerbargains.price.EnchantPowerRolls;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Enchanted gear price (armorer, toolsmith, weaponsmith, fletcher, fisherman and wandering
 * trader trades using {@code enchant_with_levels} with {@code include_additional_cost_component}).
 *
 * <p>Vanilla, in {@code EnchantWithLevelsFunction#run}:
 * <pre>{@code
 * int enchantmentCost = this.levels.value().getInt(context);   // 26.2: this.levels.getInt(context)
 * ItemStack result = EnchantmentHelper.enchantItem(random, itemStack, enchantmentCost, registryAccess, this.options);
 * if (this.includeAdditionalCostComponent && ... && enchantmentCost > 0) {
 *     result.set(DataComponents.ADDITIONAL_TRADE_COST, enchantmentCost);
 * }
 * }</pre>
 * {@code enchantmentCost} is both the enchanting power and the extra price. Its roll stays vanilla
 * (the {@code mixin/provider} mixins only record it in {@link EnchantPowerRolls}), so the
 * enchantments and the random sequence do not change; only the value written to
 * {@code ADDITIONAL_TRADE_COST} is pinned. With the opt-in {@code gearStrength} setting the power
 * itself is pinned too (after vanilla drew it), and the price is still pinned from that power's range.
 */
@Mixin(EnchantWithLevelsFunction.class)
public abstract class EnchantWithLevelsFunctionMixin {
    private static final String RUN =
            "run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;";

    @WrapMethod(method = RUN)
    private ItemStack villagerbargains$recordPowerRolls(ItemStack itemStack, LootContext context, Operation<ItemStack> original) {
        if (!context.hasParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED)) {
            return original.call(itemStack, context); // Not a trade (chest loot, mob gear): no price to pin.
        }
        return EnchantPowerRolls.run(() -> original.call(itemStack, context));
    }

    @WrapOperation(
            method = RUN,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private Object villagerbargains$pinGearPrice(ItemStack stack, DataComponentType<?> type, Object value, Operation<Object> original) {
        if (type == DataComponents.ADDITIONAL_TRADE_COST && value instanceof Integer cost) {
            value = EnchantPowerRolls.pinnedCost(cost);
        }
        return original.call(stack, type, value);
    }
}
