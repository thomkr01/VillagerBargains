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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the concrete Villager#updateTrades(ServerLevel).
 *
 * AbstractVillager#updateTrades is abstract (no body = no RETURN to inject at).
 * The real implementation with a RETURN lives on the concrete Villager class.
 *
 * The `offers` field is inherited from AbstractVillager but is accessible
 * via @Shadow on the concrete subclass target.
 *
 * Both classes are in npc.villager.* in MC 26.1.x - we use targets strings
 * so javac never needs them on the compile classpath.
 */
@Mixin(targets = "net.minecraft.world.entity.npc.villager.Villager")
public abstract class VillagerTradesMixin {

    // Inherited from AbstractVillager - @Shadow resolves inherited fields fine.
    @Shadow protected MerchantOffers offers;

    @Inject(method = "updateTrades(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"))
    private void villagerbargains$onUpdateTrades(CallbackInfo ci) {
        if (this.offers == null || this.offers.isEmpty()) return;

        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        for (MerchantOffer offer : this.offers) {
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
        // Step 1: enchanted book — match by sell enchantment ID
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

        // Step 2: all other trades — match by buy item name
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
