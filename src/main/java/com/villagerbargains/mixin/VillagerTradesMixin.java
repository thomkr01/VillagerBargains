package com.villagerbargains.mixin;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.TradeDefinition;
import com.villagerbargains.trade.VanillaTrades;
import com.villagerbargains.util.ModLogger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects at TAIL of Villager#updateTrades(ServerLevel).
 *
 * Enchantment ID: Holder.getRegisteredName() returns "minecraft:power" directly.
 * Item path:      Item.toString()             returns "minecraft:iron_leggings".
 * No ResourceLocation, no BuiltInRegistries, no string format parsing.
 */
@Mixin(targets = "net.minecraft.world.entity.npc.villager.Villager")
public abstract class VillagerTradesMixin {

    @Inject(method = "updateTrades(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"))
    private void villagerbargains$onUpdateTrades(CallbackInfo ci) {
        MerchantOffers offers = ((Merchant)(Object)this).getOffers();
        if (offers == null || offers.isEmpty()) return;

        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        for (MerchantOffer offer : offers) {
            applyPrice(offer, config);
        }
    }

    private static void applyPrice(MerchantOffer offer, VillagerBargainsConfig config) {
        TradeDefinition def = resolveDefinition(offer);
        if (def == null) return;

        int desired = PriceResolver.resolve(def.tradeId());
        if (desired < 0) return;

        ItemStack costA = offer.getBaseCostA();
        int current = costA.getCount();
        if (current != desired) {
            costA.setCount(desired);
            ModLogger.get().debug("VillagerBargains: {} {} -> {}", def.tradeId(), current, desired);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Item.toString() = "minecraft:iron_leggings" — take everything after last ':'.
     */
    private static String itemPath(ItemStack stack) {
        String s = stack.getItem().toString();
        int i = s.lastIndexOf(':');
        return i >= 0 ? s.substring(i + 1) : s;
    }

    private static TradeDefinition resolveDefinition(MerchantOffer offer) {
        ItemStack result = offer.getResult();

        // 1. Enchanted book — use Holder.getRegisteredName() for a clean "namespace:path" string.
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                // getRegisteredName() returns e.g. "minecraft:power" — exactly what we need.
                String enchId = enchantments.entrySet().iterator().next()
                        .getKey().getRegisteredName();
                return VanillaTrades.getByBook("enchanted_book:" + enchId);
            }
            return null;
        }

        // 2. Sell trade — match by result item path.
        if (!result.isEmpty()) {
            TradeDefinition def = VanillaTrades.getByResultItem(itemPath(result));
            if (def != null) return def;
        }

        // 3. Buy trade — result is emerald, match by cost item path.
        ItemStack costA = offer.getBaseCostA();
        if (!costA.isEmpty()) {
            return VanillaTrades.getByResultItem(itemPath(costA));
        }

        return null;
    }
}
