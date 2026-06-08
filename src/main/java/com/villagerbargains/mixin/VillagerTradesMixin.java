package com.villagerbargains.mixin;

import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.TradeDefinition;
import com.villagerbargains.trade.VanillaTrades;
import com.villagerbargains.util.ModLogger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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

    /**
     * Track whether we've already applied prices for this villager instance.
     * Resets to false whenever updateTrades fires (new trades generated).
     * Allows the tick-based fallback to apply once on first tick after load.
     */
    private boolean villagerbargains$applied = false;

    /** Fires when a villager generates new trades (level-up or fresh spawn). */
    @Inject(method = "updateTrades(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"))
    private void villagerbargains$onUpdateTrades(CallbackInfo ci) {
        villagerbargains$applied = false;
        reapplyAllOffers();
        villagerbargains$applied = true;
    }

    /**
     * Fires on load from disk if the method name is correct for this MC version.
     * require=0 means the game will NOT crash if the method is not found
     * (e.g. if it is obfuscated under a different name).
     * The tick-based fallback below handles that case.
     */
    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("TAIL"), require = 0)
    private void villagerbargains$onLoad(CompoundTag tag, CallbackInfo ci) {
        villagerbargains$applied = false;
    }

    /**
     * Fallback: on the first server tick after this villager is loaded,
     * apply prices if they haven't been applied yet.
     * This catches villagers loaded from disk regardless of method name.
     */
    @Inject(method = "tick()V", at = @At("HEAD"), require = 0)
    private void villagerbargains$onTick(CallbackInfo ci) {
        if (!villagerbargains$applied) {
            reapplyAllOffers();
            villagerbargains$applied = true;
        }
    }

    private void reapplyAllOffers() {
        MerchantOffers offers = ((Merchant)(Object)this).getOffers();
        if (offers == null || offers.isEmpty()) return;
        for (MerchantOffer offer : offers) {
            applyPrice(offer);
        }
    }

    /**
     * Sets our desired price and zeroes MC's demand/reputation modifiers.
     *
     * MC's displayed price formula (MerchantOffer#getAdjustedCostA):
     *   max(1, baseCostA + specialPriceDiff + floor(baseCostA * priceMultiplier * demand))
     * After zeroing demand and specialPriceDiff:
     *   max(1, desired + 0 + 0) = desired
     */
    private static void applyPrice(MerchantOffer offer) {
        int desired = resolveDesiredPrice(offer);
        if (desired < 0) return;

        desired = Math.max(1, Math.min(64, desired));

        ItemStack costA   = offer.getBaseCostA();
        int       current = costA.getCount();

        costA.setCount(desired);
        ((MerchantOfferAccessor) offer).setDemand(0);
        offer.setSpecialPriceDiff(0);

        if (current != desired) {
            logApplied(offer, current, desired);
        }
    }

    private static int resolveDesiredPrice(MerchantOffer offer) {
        ItemStack result = offer.getResult();

        // Enchanted book: level-aware formula.
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var    entry   = enchantments.entrySet().iterator().next();
                String enchId  = entry.getKey().getRegisteredName();
                int    level   = entry.getValue();
                return PriceResolver.resolveBook("enchanted_book:" + enchId, level);
            }
            return -1;
        }

        // Normal trade.
        TradeDefinition def = resolveNormalTrade(result, offer.getBaseCostA());
        return def != null ? PriceResolver.resolve(def.tradeId()) : -1;
    }

    private static String itemPath(ItemStack stack) {
        String s = stack.getItem().toString();
        int i = s.lastIndexOf(':');
        return i >= 0 ? s.substring(i + 1) : s;
    }

    private static TradeDefinition resolveNormalTrade(ItemStack result, ItemStack costA) {
        if (!result.isEmpty()) {
            TradeDefinition def = VanillaTrades.getByResultItem(itemPath(result));
            if (def != null) return def;
        }
        if (!costA.isEmpty()) {
            return VanillaTrades.getByResultItem(itemPath(costA));
        }
        return null;
    }

    private static void logApplied(MerchantOffer offer, int from, int to) {
        ItemStack result = offer.getResult();
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enc = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enc != null && !enc.isEmpty()) {
                var e = enc.entrySet().iterator().next();
                ModLogger.get().info("[VB] book {} lvl{}: {} -> {}",
                        e.getKey().getRegisteredName(), e.getValue(), from, to);
                return;
            }
        }
        ModLogger.get().info("[VB] trade {}: {} -> {}", itemPath(result), from, to);
    }
}
