package com.villagerbargains.mixin;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.TradeDefinition;
import com.villagerbargains.trade.VanillaTrades;
import com.villagerbargains.util.ModLogger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
 * Matching strategy (all O(1)):
 *   1. Enchanted book -> match by enchantment ResourceLocation via ResourceKey.location()
 *   2. Sell trades    -> match by result item path via BuiltInRegistries.ITEM.getKey()
 *   3. Buy trades     -> match by cost item path  via BuiltInRegistries.ITEM.getKey()
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

        // 1. Enchanted book: get enchantment ID via ResourceKey.location() — no string parsing.
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var enchEntry = enchantments.entrySet().iterator().next();
                var keyOpt = enchEntry.getKey().unwrapKey();
                if (keyOpt.isPresent()) {
                    // ResourceKey.location() returns the ResourceLocation directly.
                    // toString() on a ResourceLocation is "namespace:path" — reliable.
                    ResourceLocation loc = keyOpt.get().location();
                    String enchId = loc.toString(); // e.g. "minecraft:power"
                    return VanillaTrades.getByBook("enchanted_book:" + enchId);
                }
            }
            return null;
        }

        // 2. Sell trade: match by result item registry path.
        if (!result.isEmpty()) {
            ResourceLocation loc = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (loc != null) {
                TradeDefinition def = VanillaTrades.getByResultItem(loc.getPath());
                if (def != null) return def;
            }
        }

        // 3. Buy trade: result is emerald, match by cost item registry path.
        ItemStack costA = offer.getBaseCostA();
        if (!costA.isEmpty()) {
            ResourceLocation loc = BuiltInRegistries.ITEM.getKey(costA.getItem());
            if (loc != null) {
                return VanillaTrades.getByResultItem(loc.getPath());
            }
        }

        return null;
    }
}
