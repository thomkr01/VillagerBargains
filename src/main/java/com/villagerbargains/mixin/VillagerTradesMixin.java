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
 * No ResourceLocation or BuiltInRegistries needed.
 * Item name: Item.toString() returns "minecraft:iron_leggings" — take substring after ':'.
 * Enchantment ID: ResourceKey.toString() returns "ResourceKey[... / minecraft:power]"
 *   — we take everything after the last ' / ' and strip the trailing ']'.
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

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Item.toString() returns the registry ID, e.g. "minecraft:iron_leggings".
     * We only need the path after ':', so: "iron_leggings".
     */
    private static String itemPath(ItemStack stack) {
        String s = stack.getItem().toString(); // "minecraft:iron_leggings"
        int colon = s.lastIndexOf(':');
        return colon >= 0 ? s.substring(colon + 1) : s;
    }

    /**
     * Extracts the enchantment registry ID from a ResourceKey toString().
     * ResourceKey.toString() = "ResourceKey[minecraft:enchantment / minecraft:power]"
     * We want: "minecraft:power"
     */
    private static String enchantmentId(Object resourceKey) {
        String s = resourceKey.toString();
        int sep = s.lastIndexOf(" / ");
        if (sep < 0) return s;
        String after = s.substring(sep + 3); // "minecraft:power]"
        if (after.endsWith("]")) after = after.substring(0, after.length() - 1);
        return after; // "minecraft:power"
    }

    private static TradeDefinition resolveDefinition(MerchantOffer offer) {
        ItemStack result = offer.getResult();

        // 1. Enchanted book — match by enchantment ID.
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var enchEntry = enchantments.entrySet().iterator().next();
                var keyOpt = enchEntry.getKey().unwrapKey();
                if (keyOpt.isPresent()) {
                    String enchId = enchantmentId(keyOpt.get()); // "minecraft:power"
                    return VanillaTrades.getByBook("enchanted_book:" + enchId);
                }
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
