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
        TradeDefinition def = resolveDefinition(offer);
        if (def == null) return;

        int desired = PriceResolver.resolve(def.tradeId());
        if (desired < 0) return;

        ItemStack costA = offer.getBaseCostA();
        int current = costA.getCount();
        if (current != desired) {
            costA.setCount(desired);
            ModLogger.get().info("[VB] applied: {} {} -> {}", def.tradeId(), current, desired);
        }
    }

    private static String itemPath(ItemStack stack) {
        String s = stack.getItem().toString();
        int i = s.lastIndexOf(':');
        return i >= 0 ? s.substring(i + 1) : s;
    }

    private static TradeDefinition resolveDefinition(MerchantOffer offer) {
        ItemStack result = offer.getResult();

        // 1. Enchanted book — log every possible string representation.
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var entry = enchantments.entrySet().iterator().next();
                var holder = entry.getKey();

                ModLogger.get().info("[VB-DEBUG] holder.toString()   = {}", holder.toString());

                try {
                    ModLogger.get().info("[VB-DEBUG] getRegisteredName() = {}", holder.getRegisteredName());
                } catch (Throwable t) {
                    ModLogger.get().info("[VB-DEBUG] getRegisteredName() THREW: {}", t.toString());
                }

                var keyOpt = holder.unwrapKey();
                if (keyOpt.isPresent()) {
                    ModLogger.get().info("[VB-DEBUG] key.toString()      = {}", keyOpt.get().toString());
                } else {
                    ModLogger.get().info("[VB-DEBUG] unwrapKey() = empty");
                }

                // Attempt lookup with getRegisteredName().
                try {
                    String enchId = holder.getRegisteredName();
                    String lookupKey = "enchanted_book:" + enchId;
                    TradeDefinition def = VanillaTrades.getByBook(lookupKey);
                    ModLogger.get().info("[VB-DEBUG] lookup='{}' result={}", lookupKey, def != null ? def.tradeId() : "NULL");
                    if (def != null) return def;
                } catch (Throwable t) {
                    ModLogger.get().info("[VB-DEBUG] lookup THREW: {}", t.toString());
                }
            }
            return null;
        }

        // 2. Sell trade.
        if (!result.isEmpty()) {
            String path = itemPath(result);
            TradeDefinition def = VanillaTrades.getByResultItem(path);
            ModLogger.get().debug("[VB-DEBUG] sell='{}' def={}", path, def != null ? def.tradeId() : "NULL");
            if (def != null) return def;
        }

        // 3. Buy trade.
        ItemStack costA = offer.getBaseCostA();
        if (!costA.isEmpty()) {
            String path = itemPath(costA);
            TradeDefinition def = VanillaTrades.getByResultItem(path);
            ModLogger.get().debug("[VB-DEBUG] buy='{}' def={}", path, def != null ? def.tradeId() : "NULL");
            return def;
        }

        return null;
    }
}
