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
        ItemStack result = offer.getResult();

        // Enchanted book: resolve with level-aware formula.
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var entry = enchantments.entrySet().iterator().next();
                String enchId  = entry.getKey().getRegisteredName(); // "minecraft:power"
                int    level   = entry.getValue();                    // 1..5
                String sellKey = "enchanted_book:" + enchId;

                int desired = PriceResolver.resolveBook(sellKey, level);
                if (desired < 0) return;

                ItemStack costA = offer.getBaseCostA();
                int current = costA.getCount();
                if (current != desired) {
                    costA.setCount(desired);
                    ModLogger.get().info("[VB] book {} lvl{}: {} -> {}", enchId, level, current, desired);
                }
            }
            return;
        }

        // Normal trade: resolve by result/buy item name.
        TradeDefinition def = resolveNormalTrade(result, offer.getBaseCostA());
        if (def == null) return;

        int desired = PriceResolver.resolve(def.tradeId());
        if (desired < 0) return;

        ItemStack costA = offer.getBaseCostA();
        int current = costA.getCount();
        if (current != desired) {
            costA.setCount(desired);
            ModLogger.get().info("[VB] trade {}: {} -> {}", def.tradeId(), current, desired);
        }
    }

    private static String itemPath(ItemStack stack) {
        String s = stack.getItem().toString();
        int i = s.lastIndexOf(':');
        return i >= 0 ? s.substring(i + 1) : s;
    }

    private static TradeDefinition resolveNormalTrade(ItemStack result, ItemStack costA) {
        // Sell trade: match by result item.
        if (!result.isEmpty()) {
            TradeDefinition def = VanillaTrades.getByResultItem(itemPath(result));
            if (def != null) return def;
        }
        // Buy trade: result is emerald, match by cost item.
        if (!costA.isEmpty()) {
            return VanillaTrades.getByResultItem(itemPath(costA));
        }
        return null;
    }
}
