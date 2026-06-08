package com.villagerbargains.mixin;

import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.util.ModLogger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Intercepts villager trade generation at TAIL of updateTrades.
 * Replaces each MerchantOffer with a new one whose baseCostA count is locked
 * to our configured price at construction time — since baseCostA is final.
 */
@Mixin(Villager.class)
public abstract class VillagerTradesMixin extends AbstractVillager {

    public VillagerTradesMixin(EntityType<? extends AbstractVillager> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void villagerbargains$repriceOffers(ServerLevel serverLevel, CallbackInfo ci) {
        MerchantOffers offers = this.getOffers();
        if (offers == null || offers.isEmpty()) {
            ModLogger.get().info("[VillagerBargains UpdateTrades] No offers to reprice.");
            return;
        }
        ModLogger.get().info("[VillagerBargains UpdateTrades] Repricing {} offer(s).", offers.size());
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer original = offers.get(i);
            MerchantOffer replaced = buildRepriced(original);
            if (replaced != null) {
                offers.set(i, replaced);
            }
        }
    }

    /**
     * Builds a new MerchantOffer identical to the original but with our price
     * locked into baseCostA at construction. Returns null if not in registry.
     */
    private static MerchantOffer buildRepriced(MerchantOffer original) {
        int price = resolvePrice(original);
        if (price < 0) {
            ModLogger.get().info("[VillagerBargains Lookup] Trade '{}' not in registry, leaving unchanged.",
                    describeResult(original.getResult()));
            return null;
        }
        price = Math.max(1, Math.min(64, price));
        int oldPrice = original.getBaseCostA().count();

        // baseCostA is final — we must construct a fresh offer with our price.
        MerchantOffer fresh = new MerchantOffer(
                new ItemCost(original.getBaseCostA().item(), price),
                original.getCostB(),           // Optional<ItemCost> — unchanged
                original.getResult().copy(),
                original.getUses(),
                original.getMaxUses(),
                original.getXp(),
                original.getPriceMultiplier()
        );
        // Zero out demand and reputation modifiers so the price stays exact.
        fresh.setSpecialPriceDiff(0);

        ModLogger.get().info("[VillagerBargains Repriced] '{}' : {} -> {} emeralds",
                describeResult(original.getResult()), oldPrice, price);
        return fresh;
    }

    /** Resolves configured price. Returns -1 if not in registry. */
    private static int resolvePrice(MerchantOffer offer) {
        ItemStack result = offer.getResult();

        // Enchanted book
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var    entry = enchantments.entrySet().iterator().next();
                String id    = entry.getKey().getRegisteredName();
                int    lvl   = entry.getValue();
                int    price = PriceResolver.resolveBook("enchanted_book:" + id, lvl);
                if (price < 0)
                    ModLogger.get().info("[VillagerBargains Lookup] Book '{}' lvl {} not in registry.", id, lvl);
                return price;
            }
            ModLogger.get().info("[VillagerBargains Lookup] Enchanted book with no stored enchantments.");
            return -1;
        }

        // Normal trade — try result item id first
        String resultId = itemId(result);
        int price = PriceResolver.resolve(resultId);
        if (price >= 0) return price;

        // Fallback: try the cost item id
        String costId = itemId(offer.getBaseCostA().item().value().asItem().toString());
        price = PriceResolver.resolve(costId);
        if (price < 0)
            ModLogger.get().info("[VillagerBargains Lookup] result='{}' cost='{}' not in registry.", resultId, costId);
        return price;
    }

    private static String itemId(ItemStack stack) {
        return itemId(stack.getItem().toString());
    }

    private static String itemId(String raw) {
        int i = raw.lastIndexOf(':');
        return i >= 0 ? raw.substring(i + 1) : raw;
    }

    private static String describeResult(ItemStack result) {
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enc = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enc != null && !enc.isEmpty()) {
                var e = enc.entrySet().iterator().next();
                return e.getKey().getRegisteredName() + " lvl " + e.getValue();
            }
            return "enchanted_book(unknown)";
        }
        return itemId(result);
    }
}
