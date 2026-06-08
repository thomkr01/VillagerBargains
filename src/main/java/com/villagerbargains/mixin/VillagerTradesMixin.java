package com.villagerbargains.mixin;

import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.TradeDefinition;
import com.villagerbargains.trade.VanillaTrades;
import com.villagerbargains.util.ModLogger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.npc.villager.Villager")
public abstract class VillagerTradesMixin extends Entity {

    // Required by Entity superclass — never called directly.
    public VillagerTradesMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    private boolean villagerbargains$applied = false;

    /** Fires when a villager generates new trades (level-up or fresh spawn). */
    @Inject(method = "updateTrades(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"))
    private void villagerbargains$onUpdateTrades(CallbackInfo ci) {
        ModLogger.get().info("[VillagerBargains TradeGen] Villager generated new trades, applying prices.");
        villagerbargains$applied = false;
        reapplyAllOffers();
        villagerbargains$applied = true;
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("TAIL"), require = 0)
    private void villagerbargains$onLoad(CompoundTag tag, CallbackInfo ci) {
        ModLogger.get().info("[VillagerBargains Load] Villager loaded from disk, will reprice on next tick.");
        villagerbargains$applied = false;
    }

    /** Fallback: apply on first tick after load. Server-side only. */
    @Inject(method = "tick()V", at = @At("HEAD"), require = 0)
    private void villagerbargains$onTick(CallbackInfo ci) {
        if (!villagerbargains$applied) {
            if (this.level().isClientSide()) {
                ModLogger.get().debug("[VillagerBargains Tick] Skipping reprice — client side.");
                villagerbargains$applied = true;
                return;
            }
            ModLogger.get().info("[VillagerBargains Tick] First server tick after load, applying prices.");
            reapplyAllOffers();
            villagerbargains$applied = true;
        }
    }

    private void reapplyAllOffers() {
        MerchantOffers offers = ((Merchant)(Object)this).getOffers();
        if (offers == null || offers.isEmpty()) {
            ModLogger.get().info("[VillagerBargains Reprice] No offers found on this villager, skipping.");
            return;
        }
        ModLogger.get().info("[VillagerBargains Reprice] Processing {} offer(s).", offers.size());
        for (MerchantOffer offer : offers) {
            applyPrice(offer);
        }
    }

    private static void applyPrice(MerchantOffer offer) {
        int desired = resolveDesiredPrice(offer);
        if (desired < 0) {
            ModLogger.get().info("[VillagerBargains Reprice] Offer '{}' not in registry, skipping.",
                    itemPath(offer.getResult()));
            return;
        }

        desired = Math.max(1, Math.min(64, desired));

        ItemStack costA   = offer.getBaseCostA();
        int       current = costA.getCount();

        costA.setCount(desired);
        ((MerchantOfferAccessor) offer).setDemand(0);
        offer.setSpecialPriceDiff(0);

        if (current != desired) {
            logRepriced(offer, current, desired);
        } else {
            ModLogger.get().debug("[VillagerBargains Reprice] Offer '{}' already at correct price ({}).",
                    itemPath(offer.getResult()), desired);
        }
    }

    private static int resolveDesiredPrice(MerchantOffer offer) {
        ItemStack result = offer.getResult();

        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var    entry  = enchantments.entrySet().iterator().next();
                String enchId = entry.getKey().getRegisteredName();
                int    level  = entry.getValue();
                String key    = "enchanted_book:" + enchId;
                int    price  = PriceResolver.resolveBook(key, level);
                if (price < 0) {
                    ModLogger.get().info("[VillagerBargains Lookup] Book '{}' lvl {} not found in registry.",
                            enchId, level);
                }
                return price;
            }
            ModLogger.get().info("[VillagerBargains Lookup] Enchanted book has no stored enchantments, skipping.");
            return -1;
        }

        TradeDefinition def = resolveNormalTrade(result, offer.getBaseCostA());
        if (def == null) {
            ModLogger.get().info("[VillagerBargains Lookup] Trade '{}' not found in registry.",
                    itemPath(result));
        }
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

    private static void logRepriced(MerchantOffer offer, int from, int to) {
        ItemStack result = offer.getResult();
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enc = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enc != null && !enc.isEmpty()) {
                var e = enc.entrySet().iterator().next();
                ModLogger.get().info("[VillagerBargains Repriced] Book '{}' lvl {} : {} -> {} emeralds",
                        e.getKey().getRegisteredName(), e.getValue(), from, to);
                return;
            }
        }
        ModLogger.get().info("[VillagerBargains Repriced] Trade '{}' : {} -> {} emeralds",
                itemPath(result), from, to);
    }
}
