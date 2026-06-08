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
 * Matching strategy (no profession needed):
 *   1. Enchanted book → match by enchantment ID on result item (BOOK_REGISTRY)
 *   2. All other trades → match by result item name (RESULT_REGISTRY)
 *      - Sell trades: result is the unique sold item (e.g. iron_leggings)
 *      - Buy trades:  result is emerald, so we fall back to the buy item name
 *
 * All lookups are O(1) HashMap gets — no iteration over the registry.
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

    private static TradeDefinition resolveDefinition(MerchantOffer offer) {
        ItemStack result = offer.getResult();

        // 1. Enchanted book: match by enchantment ID.
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var enchEntry = enchantments.entrySet().iterator().next();
                var keyOpt = enchEntry.getKey().unwrapKey();
                if (keyOpt.isPresent()) {
                    String keyStr = keyOpt.get().toString();
                    int sep = keyStr.lastIndexOf(" / ");
                    String enchId = sep >= 0 ? keyStr.substring(sep + 3, keyStr.length() - 1) : keyStr;
                    return VanillaTrades.getByBook("enchanted_book:" + enchId);
                }
            }
            return null;
        }

        // 2. Try result item name first (covers all sell trades uniquely).
        if (!result.isEmpty()) {
            String resultId = result.getItem().toString();
            String resultName = resultId.substring(resultId.lastIndexOf(':') + 1);
            TradeDefinition def = VanillaTrades.getByResultItem(resultName);
            if (def != null) return def;
        }

        // 3. Fall back to buy item name (for pure buy trades where result is emerald).
        ItemStack costA = offer.getBaseCostA();
        if (!costA.isEmpty()) {
            String costId = costA.getItem().toString();
            String costName = costId.substring(costId.lastIndexOf(':') + 1);
            return VanillaTrades.getByResultItem(costName);
        }

        return null;
    }
}
