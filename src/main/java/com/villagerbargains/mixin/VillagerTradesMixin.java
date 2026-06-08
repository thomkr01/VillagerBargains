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
 * Injects at TAIL of the concrete Villager#updateTrades(ServerLevel).
 *
 * We avoid @Shadow entirely (no refMap needed) by accessing offers through
 * the Merchant interface that Villager already implements: getOffers().
 * This is zero-overhead — getOffers() returns the existing MerchantOffers
 * list directly, no allocation.
 */
@Mixin(targets = "net.minecraft.world.entity.npc.villager.Villager")
public abstract class VillagerTradesMixin {

    @Inject(method = "updateTrades(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"))
    private void villagerbargains$onUpdateTrades(CallbackInfo ci) {
        // Cast through Merchant interface - Villager implements it, no @Shadow needed.
        MerchantOffers offers = ((Merchant)(Object)this).getOffers();
        if (offers == null || offers.isEmpty()) return;

        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        for (MerchantOffer offer : offers) {
            applyPrice(offer, config);
        }
    }

    private static void applyPrice(MerchantOffer offer, VillagerBargainsConfig config) {
        ItemStack costA = offer.getBaseCostA();
        if (costA.isEmpty()) return;

        TradeDefinition def = resolveDefinition(offer);
        if (def == null) return;

        int desired = PriceResolver.resolve(def.tradeId());
        if (desired < 0) return;

        int current = costA.getCount();
        if (current != desired) {
            costA.setCount(desired);
            ModLogger.get().debug("VillagerBargains: {} {} -> {}", def.tradeId(), current, desired);
        }
    }

    private static TradeDefinition resolveDefinition(MerchantOffer offer) {
        // Enchanted book trade: match by enchantment ID on the result item.
        ItemStack result = offer.getResult();
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var enchEntry = enchantments.entrySet().iterator().next();
                var keyOpt = enchEntry.getKey().unwrapKey();
                if (keyOpt.isPresent()) {
                    String keyStr = keyOpt.get().toString();
                    int sep = keyStr.lastIndexOf(" / ");
                    String enchId = sep >= 0 ? keyStr.substring(sep + 3, keyStr.length() - 1) : keyStr;
                    TradeDefinition def = VanillaTrades.getByBook("enchanted_book:" + enchId);
                    if (def != null) return def;
                }
            }
        }

        // All other trades: match by the cost item name.
        String itemId   = offer.getBaseCostA().getItem().toString();
        int colon       = itemId.lastIndexOf(':');
        String itemName = colon >= 0 ? itemId.substring(colon + 1) : itemId;
        for (java.util.Map.Entry<String, TradeDefinition> e : VanillaTrades.getAll().entrySet()) {
            String tid       = e.getKey();
            int slash        = tid.lastIndexOf('/');
            String tradeName = slash >= 0 ? tid.substring(slash + 1) : tid;
            if (tradeName.equals(itemName) || tradeName.startsWith(itemName)) {
                return e.getValue();
            }
        }
        return null;
    }
}
